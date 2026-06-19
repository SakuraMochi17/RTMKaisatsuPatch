package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.RTMKaisatsuPatchCore
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/** クライアント→サーバー：切符購入またはICカードチャージ（fare=-1は入場券） */
class PacketPurchaseTicket() : IMessage {
    var x = 0; var y = 0; var z = 0
    var destStation = ""
    var fare = 0
    var useICCard = false

    constructor(x: Int, y: Int, z: Int, destStation: String, fare: Int, useICCard: Boolean) : this() {
        this.x = x; this.y = y; this.z = z
        this.destStation = destStation; this.fare = fare; this.useICCard = useICCard
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        destStation = ByteBufUtils.readUTF8String(buf)
        fare = buf.readInt(); useICCard = buf.readBoolean()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, destStation)
        buf.writeInt(fare); buf.writeBoolean(useICCard)
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

            if (msg.useICCard) {
                // ── ICカードチャージ ──────────────────────────
                val cardStack = vendorInv.getICCardStack() ?: run {
                    player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}スロットにICカードを入れてください"))
                    return null
                }
                val total = vendorInv.getMoneyYen()
                if (total < msg.fare) {
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.RED}お金が不足しています（必要: ${msg.fare}円 / 所持: ${total}円）"
                    ))
                    return null
                }
                if (!ItemCustomICCard.charge(cardStack, msg.fare)) {
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.RED}これ以上チャージできません（上限: ${ItemCustomICCard.MAX_BALANCE}円）"
                    ))
                    return null
                }
                vendorInv.payAndChange(msg.fare, player)
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.GREEN}${msg.fare}円チャージ完了　残高: ${ItemCustomICCard.getBalance(cardStack)}円"
                ))
                return null
            }

            // ── 切符購入 ──────────────────────────────────────
            val isEntry = msg.fare == -1
            val cost = if (isEntry) 140 else msg.fare
            val total = vendorInv.getMoneyYen()

            if (total < cost) {
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.RED}お金が不足しています（必要: ${cost}円 / 所持: ${total}円）"
                ))
                return null
            }

            // お金消費＋お釣り返却
            vendorInv.payAndChange(cost, player)

            // 切符を生成してインベントリへ
            val customTicket = RTMKaisatsuPatchCore.registeredItems["custom_ticket"] as? ItemCustomTicket
            val ticketStack = if (customTicket != null) {
                val s = ItemStack(customTicket)
                if (isEntry) ItemCustomTicket.initTicket(s, fromStation, fromStation)
                else         ItemCustomTicket.initTicket(s, fromStation, msg.destStation)
                s
            } else {
                ItemStack(net.minecraft.init.Items.paper)
            }

            if (!player.inventory.addItemStackToInventory(ticketStack))
                player.dropPlayerItemWithRandomChoice(ticketStack, false)

            val change = total - cost
            val changeMsg = if (change > 0) "　お釣り: ${change}円" else ""
            val destLabel = if (isEntry) "入場券" else msg.destStation
            player.addChatMessage(ChatComponentText(
                "${EnumChatFormatting.GREEN}${destLabel}の切符を購入しました（${cost}円）${changeMsg}"
            ))
            return null
        }
    }
}
