package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.readStringList
import jp.sakuramochi.kaisatsupatch.util.writeStr
import jp.sakuramochi.kaisatsupatch.util.writeStringList
import net.minecraft.client.Minecraft

/** S→C: 発車標設定ブロックの設定 GUI を開く */
class PacketOpenDepartureSettings() : IMessage {

    var x = 0; var y = 0; var z = 0
    var stationName  = ""
    var lineID       = ""
    var diaName      = ""
    var direction    = "両方"
    var displayRows  = 5
    var title        = ""
    var timeMode     = "real"
    var availableDias     : List<String>               = emptyList()
    var availableStations : List<String>               = emptyList()
    var availableLines    : List<Pair<String, String>> = emptyList()  // id to name

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(stationName)
        buf.writeStr(lineID)
        buf.writeStr(diaName)
        buf.writeStr(direction)
        buf.writeInt(displayRows)
        buf.writeStr(title)
        buf.writeStr(timeMode)
        buf.writeStringList(availableDias)
        buf.writeStringList(availableStations)
        buf.writeInt(availableLines.size)
        availableLines.forEach { (id, name) -> buf.writeStr(id); buf.writeStr(name) }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName  = buf.readStr()
        lineID       = buf.readStr()
        diaName      = buf.readStr()
        direction    = buf.readStr()
        displayRows  = buf.readInt()
        title        = buf.readStr()
        timeMode     = buf.readStr()
        availableDias     = buf.readStringList()
        availableStations = buf.readStringList()
        availableLines    = (0 until buf.readInt()).map { buf.readStr() to buf.readStr() }
    }

    class Handler : IMessageHandler<PacketOpenDepartureSettings, IMessage> {
        override fun onMessage(msg: PacketOpenDepartureSettings, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiDepartureSettings(msg)
            )
            return null
        }
    }
}
