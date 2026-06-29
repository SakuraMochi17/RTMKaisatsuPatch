package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr
import net.minecraft.client.Minecraft

/** S→C: 発車標（表示体）の設定 GUI を開く（路線名/方面/番線/路線カラー） */
class PacketOpenDepartureBoard() : IMessage {

    var x = 0; var y = 0; var z = 0
    var headerLine      = ""
    var headerDirection = ""
    var platform        = ""
    var lineColorHex    = 0x1E90FF
    var boundInfo       = ""   // バインド先の表示用情報（駅名 or 未バインド）

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(headerLine)
        buf.writeStr(headerDirection)
        buf.writeStr(platform)
        buf.writeInt(lineColorHex)
        buf.writeStr(boundInfo)
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        headerLine      = buf.readStr()
        headerDirection = buf.readStr()
        platform        = buf.readStr()
        lineColorHex    = buf.readInt()
        boundInfo       = buf.readStr()
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
