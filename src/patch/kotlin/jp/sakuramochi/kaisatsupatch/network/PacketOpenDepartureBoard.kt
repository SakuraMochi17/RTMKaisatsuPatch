package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.DepartureRow
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
    var currentTime        = ""
    var departures         : List<DepartureRow>          = emptyList()

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeBoolean(isConfigMode)
        ByteBufUtils.writeUTF8String(buf, stationName)
        ByteBufUtils.writeUTF8String(buf, lineID)
        ByteBufUtils.writeUTF8String(buf, platform)
        ByteBufUtils.writeUTF8String(buf, diaName)
        ByteBufUtils.writeUTF8String(buf, direction)
        buf.writeInt(displayRows)
        ByteBufUtils.writeUTF8String(buf, title)
        buf.writeInt(availableDias.size)
        availableDias.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        buf.writeInt(availableStations.size)
        availableStations.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        buf.writeInt(availableLines.size)
        availableLines.forEach { (id, name) ->
            ByteBufUtils.writeUTF8String(buf, id)
            ByteBufUtils.writeUTF8String(buf, name)
        }
        ByteBufUtils.writeUTF8String(buf, currentTime)
        buf.writeInt(departures.size)
        departures.forEach { r ->
            ByteBufUtils.writeUTF8String(buf, r.time)
            ByteBufUtils.writeUTF8String(buf, r.destination)
            ByteBufUtils.writeUTF8String(buf, r.typeName)
            ByteBufUtils.writeUTF8String(buf, r.trainNumber)
            ByteBufUtils.writeUTF8String(buf, r.trainName)
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        isConfigMode   = buf.readBoolean()
        stationName    = ByteBufUtils.readUTF8String(buf)
        lineID         = ByteBufUtils.readUTF8String(buf)
        platform       = ByteBufUtils.readUTF8String(buf)
        diaName        = ByteBufUtils.readUTF8String(buf)
        direction      = ByteBufUtils.readUTF8String(buf)
        displayRows    = buf.readInt()
        title          = ByteBufUtils.readUTF8String(buf)
        availableDias      = (0 until buf.readInt()).map { ByteBufUtils.readUTF8String(buf) }
        availableStations  = (0 until buf.readInt()).map { ByteBufUtils.readUTF8String(buf) }
        availableLines     = (0 until buf.readInt()).map {
            ByteBufUtils.readUTF8String(buf) to ByteBufUtils.readUTF8String(buf)
        }
        currentTime    = ByteBufUtils.readUTF8String(buf)
        departures     = (0 until buf.readInt()).map {
            DepartureRow(
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf),
                ByteBufUtils.readUTF8String(buf)
            )
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
