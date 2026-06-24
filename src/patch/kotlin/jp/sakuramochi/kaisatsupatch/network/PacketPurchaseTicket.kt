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

    enum class Mode { TICKET, CHARGE, PASS }

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
                    addSales(world, fromStation, msg.fare.toLong())
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
                    addSales(world, fromStation, cost.toLong())
                    val destLabel = if (isEntry) "入場券" else msg.destStation
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}${destLabel}の切符を購入しました（${cost}円）"))
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
                    addSales(world, fromStation, msg.fare.toLong())
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}定期券を発行しました（${fromStation}⇔${msg.destStation} / ${msg.passDays}日間 / ${msg.fare}円）"))
                }
            }
            return null
        }

        private fun addSales(world: net.minecraft.world.World, stationName: String, amount: Long) {
            val data = KaisatsuNetworkData.get(world)
            data.stationSales[stationName] = (data.stationSales[stationName] ?: 0L) + amount
            data.markDirty()
        }
    }
}
