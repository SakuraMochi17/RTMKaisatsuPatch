package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.ngt.rtm.RTMItem
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor
import jp.sakuramochi.kaisatsupatch.item.ItemCustomExpressTicket
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/** C→S: 特急券の予約をキャンセルして払い戻しを受ける */
class PacketCancelReservation() : IMessage {
    var trainID = ""; var carNumber = 0; var seatNumber = 0

    constructor(trainID: String, carNumber: Int, seatNumber: Int) : this() {
        this.trainID = trainID; this.carNumber = carNumber; this.seatNumber = seatNumber
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeStr(trainID)
        buf.writeInt(carNumber); buf.writeInt(seatNumber)
    }

    override fun fromBytes(buf: ByteBuf) {
        trainID = buf.readStr()
        carNumber = buf.readInt(); seatNumber = buf.readInt()
    }

    class Handler : IMessageHandler<PacketCancelReservation, IMessage> {
        override fun onMessage(msg: PacketCancelReservation, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val data = KaisatsuNetworkData.get(world) ?: return null

            val reservationKey = "${msg.trainID}:${msg.carNumber}:${msg.seatNumber}"
            val reservedPlayer = data.reservations[reservationKey]

            if (reservedPlayer != player.gameProfile.name) {
                player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}この予約はあなたのものではありません"))
                return null
            }

            // インベントリから対象の特急券を探す
            val inv = player.inventory
            var ticketSlot = -1
            var ticketFare = 0
            for (i in 0 until inv.sizeInventory) {
                val s = inv.getStackInSlot(i) ?: continue
                if (s.item !is ItemCustomExpressTicket) continue
                if (!ItemCustomExpressTicket.isReserved(s)) continue
                if (ItemCustomExpressTicket.getTrainID(s) == msg.trainID &&
                    ItemCustomExpressTicket.getCarNumber(s) == msg.carNumber &&
                    ItemCustomExpressTicket.getSeatNumber(s) == msg.seatNumber) {
                    ticketFare = ItemCustomExpressTicket.getExpressFare(s)
                    ticketSlot = i
                    break
                }
            }

            if (ticketSlot < 0) {
                player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}対応する特急券がインベントリに見つかりません"))
                return null
            }

            // 予約削除・切符回収
            data.reservations.remove(reservationKey)
            inv.setInventorySlotContents(ticketSlot, null)

            // 払い戻し額 = 70%（10円単位切り捨て）
            val refund = ticketFare * 7 / 10 / 10 * 10
            val fee = ticketFare - refund

            if (refund > 0) {
                var remaining = refund
                for ((id, value) in InventoryCustomVendor.DENOMINATIONS) {
                    val count = remaining / value
                    if (count > 0) {
                        val s = ItemStack(RTMItem.money, count, id)
                        if (!inv.addItemStackToInventory(s))
                            player.dropPlayerItemWithRandomChoice(s, false)
                        remaining -= count * value
                    }
                }
            }

            data.markDirty()
            player.addChatMessage(ChatComponentText(
                "${EnumChatFormatting.GREEN}予約をキャンセルしました（払い戻し: ${refund}円 / 手数料: ${fee}円）"))
            return null
        }
    }
}
