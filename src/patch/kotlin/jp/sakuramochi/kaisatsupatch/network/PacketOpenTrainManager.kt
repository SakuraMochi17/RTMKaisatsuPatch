package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readCoords
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.readStringList
import jp.sakuramochi.kaisatsupatch.util.writeCoords
import jp.sakuramochi.kaisatsupatch.util.writeStr
import jp.sakuramochi.kaisatsupatch.util.writeStringList
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.client.Minecraft

/** S→C: 列車管理ブロックのGUIを開く */
class PacketOpenTrainManager() : IMessage {

    data class CarInfo(val carNumber: Int, val seatCount: Int, val carClass: String)
    data class TrainInfo(
        val trainID: String,
        val trainName: String,
        val trainType: String,
        val lineID: String,
        val stopStations: List<String>,
        val reservedFare: Int,
        val unreservedFare: Int,
        val cars: List<CarInfo>
    )
    data class LineInfo(val lineID: String, val lineName: String, val stations: List<String>)

    var x = 0; var y = 0; var z = 0
    var hasTrain = false
    var train: TrainInfo? = null
    var lines: List<LineInfo> = emptyList()

    constructor(
        x: Int, y: Int, z: Int,
        currentTrain: KaisatsuNetworkData.TrainData?,
        lines: List<LineInfo>
    ) : this() {
        this.x = x; this.y = y; this.z = z
        this.hasTrain = currentTrain != null
        this.train = currentTrain?.let { t ->
            TrainInfo(
                t.trainID, t.trainName, t.trainType, t.lineID,
                t.stopStations, t.reservedFare, t.unreservedFare,
                t.cars.map { CarInfo(it.carNumber, it.seatCount, it.carClass) }
            )
        }
        this.lines = lines
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeBoolean(hasTrain)
        if (hasTrain && train != null) {
            val t = train!!
            buf.writeStr(t.trainID)
            buf.writeStr(t.trainName)
            buf.writeStr(t.trainType)
            buf.writeStr(t.lineID)
            buf.writeInt(t.reservedFare)
            buf.writeInt(t.unreservedFare)
            buf.writeInt(t.stopStations.size)
            t.stopStations.forEach { buf.writeStr(it) }
            buf.writeInt(t.cars.size)
            t.cars.forEach { c ->
                buf.writeInt(c.carNumber)
                buf.writeInt(c.seatCount)
                buf.writeStr(c.carClass)
            }
        }
        buf.writeInt(lines.size)
        lines.forEach { line ->
            buf.writeStr(line.lineID)
            buf.writeStr(line.lineName)
            buf.writeInt(line.stations.size)
            line.stations.forEach { buf.writeStr(it) }
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        hasTrain = buf.readBoolean()
        if (hasTrain) {
            val trainID = buf.readStr()
            val trainName = buf.readStr()
            val trainType = buf.readStr()
            val lineID = buf.readStr()
            val reservedFare = buf.readInt()
            val unreservedFare = buf.readInt()
            val stopCount = buf.readInt()
            val stops = (0 until stopCount).map { buf.readStr() }
            val carCount = buf.readInt()
            val cars = (0 until carCount).map {
                CarInfo(buf.readInt(), buf.readInt(), buf.readStr())
            }
            train = TrainInfo(trainID, trainName, trainType, lineID, stops, reservedFare, unreservedFare, cars)
        }
        val lineCount = buf.readInt()
        lines = (0 until lineCount).map {
            val id = buf.readStr()
            val name = buf.readStr()
            val stCount = buf.readInt()
            val stations = (0 until stCount).map { buf.readStr() }
            LineInfo(id, name, stations)
        }
    }

    class Handler : IMessageHandler<PacketOpenTrainManager, IMessage> {
        @SideOnly(Side.CLIENT)
        override fun onMessage(msg: PacketOpenTrainManager, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiTrainManager(msg)
            )
            return null
        }
    }
}
