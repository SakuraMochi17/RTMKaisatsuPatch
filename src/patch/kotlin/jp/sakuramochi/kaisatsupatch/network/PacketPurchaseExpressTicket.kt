package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.ngt.rtm.RTMItem
import jp.sakuramochi.kaisatsupatch.RTMKaisatsuPatchCore
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkManager
import jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor
import jp.sakuramochi.kaisatsupatch.item.ItemCustomExpressTicket
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText

/** C→S: 特急券（または自由席特急券）を購入する */
class PacketPurchaseExpressTicket() : IMessage {

    var vendorX = 0; var vendorY = 0; var vendorZ = 0
    var trainID = ""
    var fromStation = ""
    var toStation = ""
    var isReserved = false
    var carNumber = 0
    var includeTicket = false

    constructor(
        vendorX: Int, vendorY: Int, vendorZ: Int,
        trainID: String,
        fromStation: String,
        toStation: String,
        isReserved: Boolean,
        carNumber: Int,
        includeTicket: Boolean
    ) : this() {
        this.vendorX = vendorX; this.vendorY = vendorY; this.vendorZ = vendorZ
        this.trainID = trainID
        this.fromStation = fromStation
        this.toStation = toStation
        this.isReserved = isReserved
        this.carNumber = carNumber
        this.includeTicket = includeTicket
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(vendorX); buf.writeInt(vendorY); buf.writeInt(vendorZ)
        ByteBufUtils.writeUTF8String(buf, trainID)
        ByteBufUtils.writeUTF8String(buf, fromStation)
        ByteBufUtils.writeUTF8String(buf, toStation)
        buf.writeBoolean(isReserved)
        buf.writeInt(carNumber)
        buf.writeBoolean(includeTicket)
    }

    override fun fromBytes(buf: ByteBuf) {
        vendorX = buf.readInt(); vendorY = buf.readInt(); vendorZ = buf.readInt()
        trainID = ByteBufUtils.readUTF8String(buf)
        fromStation = ByteBufUtils.readUTF8String(buf)
        toStation = ByteBufUtils.readUTF8String(buf)
        isReserved = buf.readBoolean()
        carNumber = buf.readInt()
        includeTicket = buf.readBoolean()
    }

    class Handler : IMessageHandler<PacketPurchaseExpressTicket, IMessage> {
        override fun onMessage(msg: PacketPurchaseExpressTicket, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val data = KaisatsuNetworkData.get(world) ?: return null
            val train = data.trainData[msg.trainID] ?: return null

            val expressFare = if (msg.isReserved) train.reservedFare else train.unreservedFare
            val baseFare = if (msg.includeTicket)
                KaisatsuNetworkManager.calculateFare(world, msg.fromStation, msg.toStation, isICCard = false)
            else 0
            val total = expressFare + maxOf(0, baseFare)

            var seatNumber = 0
            if (msg.isReserved) {
                val car = train.cars.find { it.carNumber == msg.carNumber } ?: return null
                val takenSeats = data.reservations.keys
                    .filter { it.startsWith("${msg.trainID}:${msg.carNumber}:") }
                    .mapNotNull { it.split(":").getOrNull(2)?.toIntOrNull() }
                    .toSet()
                val foundSeat = (1..car.seatCount).firstOrNull { it !in takenSeats }
                if (foundSeat == null) {
                    player.addChatMessage(ChatComponentText("§c満席です"))
                    return null
                }
                seatNumber = foundSeat
            }

            if (!deductMoneyFromPlayerInventory(player, total)) {
                player.addChatMessage(ChatComponentText("§c残高不足です（必要: ${total}円）"))
                return null
            }

            if (msg.isReserved) {
                data.reservations["${msg.trainID}:${msg.carNumber}:${seatNumber}"] = player.gameProfile.name
            }

            val expressItem = RTMKaisatsuPatchCore.registeredItems["express_ticket"] as? ItemCustomExpressTicket ?: return null
            val stack = ItemStack(expressItem)
            ItemCustomExpressTicket.init(
                stack, msg.trainID, train.trainName,
                msg.fromStation, msg.toStation,
                msg.isReserved, msg.carNumber, seatNumber, expressFare
            )
            if (!player.inventory.addItemStackToInventory(stack))
                player.dropPlayerItemWithRandomChoice(stack, false)

            if (msg.includeTicket && baseFare > 0) {
                val ticketItem = RTMKaisatsuPatchCore.registeredItems["custom_ticket"] as? ItemCustomTicket
                if (ticketItem != null) {
                    val ticketStack = ItemStack(ticketItem)
                    ItemCustomTicket.initTicket(ticketStack, msg.fromStation, msg.toStation)
                    if (!player.inventory.addItemStackToInventory(ticketStack))
                        player.dropPlayerItemWithRandomChoice(ticketStack, false)
                }
            }

            addSales(world, msg.fromStation, total.toLong())
            data.markDirty()

            val seatStr = if (msg.isReserved) "${msg.carNumber}号車${seatNumber}番席" else "自由席"
            player.addChatMessage(ChatComponentText(
                "§a${train.trainName} $seatStr ${msg.fromStation}→${msg.toStation} ${total}円"
            ))
            return null
        }

        private fun addSales(world: net.minecraft.world.World, stationName: String, amount: Long) {
            val data = KaisatsuNetworkData.get(world) ?: return
            data.stationSales[stationName] = (data.stationSales[stationName] ?: 0L) + amount
            val bd = data.stationSalesDetail.getOrPut(stationName) { KaisatsuNetworkData.SalesBreakdown() }
            bd.express += amount
            data.markDirty()
        }

        private fun deductMoneyFromPlayerInventory(player: EntityPlayer, amount: Int): Boolean {
            if (amount <= 0) return true
            // プレイヤーインベントリのRTMお金を全部集計
            val inv = player.inventory
            var total = 0
            for (i in 0 until inv.sizeInventory) {
                val s = inv.getStackInSlot(i) ?: continue
                if (s.item != RTMItem.money) continue
                total += RTMItem.MoneyType.getPrice(s.itemDamage) * s.stackSize
            }
            if (total < amount) return false

            // 全部消費してお釣りを返す
            for (i in 0 until inv.sizeInventory) {
                val s = inv.getStackInSlot(i) ?: continue
                if (s.item != RTMItem.money) continue
                inv.setInventorySlotContents(i, null)
            }
            var change = total - amount
            for ((id, value) in InventoryCustomVendor.DENOMINATIONS) {
                val count = change / value
                if (count > 0) {
                    val s = ItemStack(RTMItem.money, count, id)
                    if (!inv.addItemStackToInventory(s))
                        player.dropPlayerItemWithRandomChoice(s, false)
                    change -= count * value
                }
            }
            return true
        }
    }
}
