package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.RTMKaisatsuPatchCore
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor
import jp.ngt.rtm.RTMItem
import jp.sakuramochi.kaisatsupatch.item.ItemCustomCouponTicket
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import jp.sakuramochi.kaisatsupatch.item.ItemCustomPass
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/**
 * クライアント→サーバー：購入リクエスト
 *   mode=TICKET  : 切符購入 (fare=-1で入場券)
 *   mode=CHARGE  : ICカードチャージ
 *   mode=PASS    : 定期券購入 (fare=価格, destStation=着駅, passDays=有効日数)
 */
class PacketPurchaseTicket() : IMessage {

    enum class Mode { TICKET, CHARGE, PASS, BUY_IC, RETURN_IC, COUPON, DAY_PASS }

    var x = 0; var y = 0; var z = 0
    var mode = Mode.TICKET
    var destStation = ""
    var fare = 0
    var passDays = 0

    constructor(x: Int, y: Int, z: Int, destStation: String, fare: Int, useICCard: Boolean) : this() {
        this.x = x; this.y = y; this.z = z
        this.destStation = destStation; this.fare = fare
        this.mode = if (useICCard) Mode.CHARGE else Mode.TICKET
    }

    constructor(x: Int, y: Int, z: Int, destStation: String, fare: Int, passDays: Int, @Suppress("UNUSED_PARAMETER") passMode: Boolean) : this() {
        this.x = x; this.y = y; this.z = z
        this.destStation = destStation; this.fare = fare; this.passDays = passDays
        this.mode = Mode.PASS
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        mode = Mode.values()[buf.readInt()]
        destStation = ByteBufUtils.readUTF8String(buf)
        fare = buf.readInt(); passDays = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeInt(mode.ordinal)
        ByteBufUtils.writeUTF8String(buf, destStation)
        buf.writeInt(fare); buf.writeInt(passDays)
    }

    class Handler : IMessageHandler<PacketPurchaseTicket, IMessage> {
        override fun onMessage(msg: PacketPurchaseTicket, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val tile = world.getTileEntity(msg.x, msg.y, msg.z) as? TileEntityCustomTicketVendor ?: return null

            val container = player.openContainer as? ContainerCustomVendor ?: run {
                player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}券売機が開いていません"))
                return null
            }
            val vendorInv = container.vendorInv
            val fromStation = tile.stationName

            when (msg.mode) {
                PacketPurchaseTicket.Mode.CHARGE -> {
                    // ── ICカードチャージ ──────────────────────────
                    val cardStack = vendorInv.getICCardStack() ?: run {
                        player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}スロットにICカードを入れてください"))
                        return null
                    }
                    val total = vendorInv.getMoneyYen()
                    if (total < msg.fare) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}お金が不足しています（必要: ${msg.fare}円 / 所持: ${total}円）"))
                        return null
                    }
                    if (!ItemCustomICCard.charge(cardStack, msg.fare)) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}これ以上チャージできません（上限: ${ItemCustomICCard.MAX_BALANCE}円）"))
                        return null
                    }
                    vendorInv.payAndChange(msg.fare, player)
                    addSales(world, fromStation, msg.fare.toLong(), SaleType.IC)
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}${msg.fare}円チャージ完了　残高: ${ItemCustomICCard.getBalance(cardStack)}円"))
                }

                PacketPurchaseTicket.Mode.TICKET -> {
                    // ── 切符購入 ──────────────────────────────────
                    val isEntry = msg.fare == -1
                    val cost = if (isEntry) 140 else msg.fare
                    if (!vendorInv.payAndChange(cost, player)) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}お金が不足しています（必要: ${cost}円 / 所持: ${vendorInv.getMoneyYen()}円）"))
                        return null
                    }
                    val customTicket = RTMKaisatsuPatchCore.registeredItems["custom_ticket"] as? ItemCustomTicket
                    val ticketStack = if (customTicket != null) {
                        val s = ItemStack(customTicket)
                        if (isEntry) ItemCustomTicket.initTicket(s, fromStation, fromStation)
                        else         ItemCustomTicket.initTicket(s, fromStation, msg.destStation)
                        s
                    } else ItemStack(net.minecraft.init.Items.paper)
                    if (!player.inventory.addItemStackToInventory(ticketStack))
                        player.dropPlayerItemWithRandomChoice(ticketStack, false)
                    addSales(world, fromStation, cost.toLong(), SaleType.TICKET)
                    val destLabel = if (isEntry) "入場券" else msg.destStation
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}${destLabel}の切符を購入しました（${cost}円）"))
                }

                PacketPurchaseTicket.Mode.BUY_IC -> {
                    // ── ICカード新規発行 ───────────────────────────
                    val cost = msg.fare
                    val initBalance = cost - 500  // 預り金500円を除いた残高
                    if (initBalance < 0) return null
                    if (!vendorInv.payAndChange(cost, player)) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}お金が不足しています（必要: ${cost}円 / 所持: ${vendorInv.getMoneyYen()}円）"))
                        return null
                    }
                    val icItem = RTMKaisatsuPatchCore.registeredItems["custom_ic_card"] as? ItemCustomICCard ?: return null
                    val icStack = ItemStack(icItem)
                    ItemCustomICCard.charge(icStack, initBalance)
                    if (!player.inventory.addItemStackToInventory(icStack))
                        player.dropPlayerItemWithRandomChoice(icStack, false)
                    addSales(world, fromStation, cost.toLong(), SaleType.IC)
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}ICカードを発行しました（残高: ${initBalance}円 / 預り金: 500円）"))
                }

                PacketPurchaseTicket.Mode.RETURN_IC -> {
                    // ── ICカード返却 ──────────────────────────────
                    val cardStack = vendorInv.getICCardStack() ?: run {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}スロットにICカードを入れてください"))
                        return null
                    }
                    if (ItemCustomICCard.getEntryStation(cardStack).isNotEmpty()) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}入場中のICカードは返却できません（先に出場してください）"))
                        return null
                    }
                    val balance = ItemCustomICCard.getBalance(cardStack)
                    val refund = maxOf(0, balance - 220) + 500

                    // カードを回収
                    vendorInv.setInventorySlotContents(jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor.SLOT_ICCARD, null)

                    // 返却金を払い出し
                    var remaining = refund
                    for ((id, value) in jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor.DENOMINATIONS) {
                        val count = remaining / value
                        if (count > 0) {
                            val s = ItemStack(RTMItem.money, count, id)
                            if (!player.inventory.addItemStackToInventory(s))
                                player.dropPlayerItemWithRandomChoice(s, false)
                            remaining -= count * value
                        }
                    }
                    val msg2 = if (balance > 0)
                        "残高${balance}円 - 手数料220円 + 預り金500円 = ${refund}円"
                    else
                        "預り金 500円"
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}ICカードを返却しました（${msg2}）"))
                }

                PacketPurchaseTicket.Mode.COUPON -> {
                    // ── 回数券発行 ────────────────────────────────
                    val cost = (Math.ceil(msg.fare * 10 * 0.9 / 10.0) * 10).toInt()
                    if (!vendorInv.payAndChange(cost, player)) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}お金が不足しています（必要: ${cost}円 / 所持: ${vendorInv.getMoneyYen()}円）"))
                        return null
                    }
                    val couponItem = RTMKaisatsuPatchCore.registeredItems["coupon_ticket"] as? ItemCustomCouponTicket ?: return null
                    val s = ItemStack(couponItem)
                    ItemCustomCouponTicket.initTicket(s, fromStation, msg.destStation)
                    if (!player.inventory.addItemStackToInventory(s))
                        player.dropPlayerItemWithRandomChoice(s, false)
                    addSales(world, fromStation, cost.toLong(), SaleType.TICKET)
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}回数券を発行しました（${fromStation}→${msg.destStation} 10回分 ${cost}円）"))
                }

                PacketPurchaseTicket.Mode.DAY_PASS -> {
                    // ── 一日フリーパス ────────────────────────────
                    if (!vendorInv.payAndChange(msg.fare, player)) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}お金が不足しています（必要: ${msg.fare}円 / 所持: ${vendorInv.getMoneyYen()}円）"))
                        return null
                    }
                    val passItem = RTMKaisatsuPatchCore.registeredItems["custom_pass"] as? ItemCustomPass ?: return null
                    val s = ItemStack(passItem)
                    ItemCustomPass.initDayPass(s, ItemCustomPass.currentDay(world))
                    if (!player.inventory.addItemStackToInventory(s))
                        player.dropPlayerItemWithRandomChoice(s, false)
                    addSales(world, fromStation, msg.fare.toLong(), SaleType.PASS)
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}一日フリーパスを購入しました（${msg.fare}円）全区間 1日間有効"))
                }

                PacketPurchaseTicket.Mode.PASS -> {
                    // ── 定期券購入 ────────────────────────────────
                    if (!vendorInv.payAndChange(msg.fare, player)) {
                        player.addChatMessage(ChatComponentText(
                            "${EnumChatFormatting.RED}お金が不足しています（必要: ${msg.fare}円 / 所持: ${vendorInv.getMoneyYen()}円）"))
                        return null
                    }
                    val passItem = RTMKaisatsuPatchCore.registeredItems["custom_pass"] as? ItemCustomPass
                    val passStack = if (passItem != null) {
                        val s = ItemStack(passItem)
                        ItemCustomPass.init(s, fromStation, msg.destStation,
                            ItemCustomPass.currentDay(world), msg.passDays)
                        s
                    } else ItemStack(net.minecraft.init.Items.paper)
                    if (!player.inventory.addItemStackToInventory(passStack))
                        player.dropPlayerItemWithRandomChoice(passStack, false)
                    addSales(world, fromStation, msg.fare.toLong(), SaleType.PASS)
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}定期券を発行しました（${fromStation}⇔${msg.destStation} / ${msg.passDays}日間 / ${msg.fare}円）"))
                }
            }
            return null
        }

        enum class SaleType { TICKET, IC, PASS, EXPRESS }

        private fun addSales(world: net.minecraft.world.World, stationName: String, amount: Long, type: SaleType) {
            val data = KaisatsuNetworkData.get(world) ?: return
            data.stationSales[stationName] = (data.stationSales[stationName] ?: 0L) + amount
            val bd = data.stationSalesDetail.getOrPut(stationName) { KaisatsuNetworkData.SalesBreakdown() }
            when (type) {
                SaleType.TICKET  -> bd.ticket  += amount
                SaleType.IC      -> bd.ic      += amount
                SaleType.PASS    -> bd.pass    += amount
                SaleType.EXPRESS -> bd.express += amount
            }
            data.markDirty()
        }
    }
}
