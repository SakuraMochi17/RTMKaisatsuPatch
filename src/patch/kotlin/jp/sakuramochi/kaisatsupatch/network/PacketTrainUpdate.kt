package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityTrainManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData

/** C→S: 列車データを保存・更新する */
class PacketTrainUpdate() : IMessage {

    var x = 0; var y = 0; var z = 0
    var delete = false
    var trainID = ""
    var trainName = ""
    var trainType = ""
    var lineID = ""
    var reservedFare = 0
    var unreservedFare = 0
    var stopStations: List<String> = emptyList()
    // Triple<carNumber, seatCount, carClass>
    var cars: List<Triple<Int, Int, String>> = emptyList()

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeBoolean(delete)
        ByteBufUtils.writeUTF8String(buf, trainID)
        ByteBufUtils.writeUTF8String(buf, trainName)
        ByteBufUtils.writeUTF8String(buf, trainType)
        ByteBufUtils.writeUTF8String(buf, lineID)
        buf.writeInt(reservedFare)
        buf.writeInt(unreservedFare)
        buf.writeInt(stopStations.size)
        stopStations.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        buf.writeInt(cars.size)
        cars.forEach { (num, count, cls) ->
            buf.writeInt(num)
            buf.writeInt(count)
            ByteBufUtils.writeUTF8String(buf, cls)
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        delete = buf.readBoolean()
        trainID = ByteBufUtils.readUTF8String(buf)
        trainName = ByteBufUtils.readUTF8String(buf)
        trainType = ByteBufUtils.readUTF8String(buf)
        lineID = ByteBufUtils.readUTF8String(buf)
        reservedFare = buf.readInt()
        unreservedFare = buf.readInt()
        val stopCount = buf.readInt()
        stopStations = (0 until stopCount).map { ByteBufUtils.readUTF8String(buf) }
        val carCount = buf.readInt()
        cars = (0 until carCount).map {
            Triple(buf.readInt(), buf.readInt(), ByteBufUtils.readUTF8String(buf))
        }
    }

    class Handler : IMessageHandler<PacketTrainUpdate, IMessage> {
        override fun onMessage(msg: PacketTrainUpdate, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val tile = world.getTileEntity(msg.x, msg.y, msg.z) as? TileEntityTrainManager ?: return null
            val data = KaisatsuNetworkData.get(world) ?: return null

            if (msg.delete) {
                data.trainData.remove(tile.trainID)
                val prefix = tile.trainID + ":"
                data.reservations.keys.filter { it.startsWith(prefix) }.forEach { data.reservations.remove(it) }
                tile.trainID = ""
            } else {
                val train = KaisatsuNetworkData.TrainData(
                    trainID = msg.trainID,
                    trainName = msg.trainName,
                    trainType = msg.trainType,
                    lineID = msg.lineID,
                    stopStations = msg.stopStations,
                    reservedFare = msg.reservedFare,
                    unreservedFare = msg.unreservedFare,
                    cars = msg.cars.map { KaisatsuNetworkData.CarData(it.first, it.second, it.third) }
                )
                data.trainData[msg.trainID] = train
                tile.trainID = msg.trainID
            }
            data.markDirty()
            tile.markDirty()
            return null
        }
    }
}
