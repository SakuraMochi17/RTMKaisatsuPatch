package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.DepartureRow
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.readStringList
import jp.sakuramochi.kaisatsupatch.util.writeStr
import jp.sakuramochi.kaisatsupatch.util.writeStringList
import net.minecraft.client.Minecraft

/** S→C: 発車標GUI (設定 or 表示) を開く */
class PacketOpenDepartureBoard() : IMessage {

    var x = 0; var y = 0; var z = 0
    var isConfigMode    = false
    var stationName     = ""
    var lineID          = ""
    var platform        = ""
    var diaName         = ""
    var direction       = "両方"
    var displayRows     = 5
    var title           = ""
    var availableDias      : List<String>                = emptyList()
    var availableStations  : List<String>                = emptyList()
    var availableLines     : List<Pair<String, String>>  = emptyList()  // id to name
    var timeMode           = "real"
    var currentTime        = ""
    var departures         : List<DepartureRow>          = emptyList()

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeBoolean(isConfigMode)
        buf.writeStr(stationName)
        buf.writeStr(lineID)
        buf.writeStr(platform)
        buf.writeStr(diaName)
        buf.writeStr(direction)
        buf.writeInt(displayRows)
        buf.writeStr(title)
        buf.writeStr(timeMode)
        buf.writeStringList(availableDias)
        buf.writeStringList(availableStations)
        buf.writeInt(availableLines.size)
        availableLines.forEach { (id, name) ->
            buf.writeStr(id)
            buf.writeStr(name)
        }
        buf.writeStr(currentTime)
        buf.writeInt(departures.size)
        departures.forEach { r ->
            buf.writeStr(r.time)
            buf.writeStr(r.destination)
            buf.writeStr(r.typeName)
            buf.writeStr(r.trainNumber)
            buf.writeStr(r.trainName)
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        isConfigMode   = buf.readBoolean()
        stationName    = buf.readStr()
        lineID         = buf.readStr()
        platform       = buf.readStr()
        diaName        = buf.readStr()
        direction      = buf.readStr()
        displayRows    = buf.readInt()
        title          = buf.readStr()
        timeMode       = buf.readStr()
        availableDias      = buf.readStringList()
        availableStations  = buf.readStringList()
        availableLines     = (0 until buf.readInt()).map { buf.readStr() to buf.readStr() }
        currentTime    = buf.readStr()
        departures     = (0 until buf.readInt()).map {
            DepartureRow(buf.readStr(), buf.readStr(), buf.readStr(), buf.readStr(), buf.readStr())
        }
    }

    class Handler : IMessageHandler<PacketOpenDepartureBoard, IMessage> {
        override fun onMessage(msg: PacketOpenDepartureBoard, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiDepartureBoard(msg)
            )
            return null
        }
    }
}
