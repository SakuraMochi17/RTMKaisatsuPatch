package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft

/** サーバー→クライアント：駅管理GUIを開く */
class PacketOpenStationGui() : IMessage {
    var x = 0; var y = 0; var z = 0
    var stationName = ""
    var salesTotal = 0L

    constructor(x: Int, y: Int, z: Int, stationName: String, salesTotal: Long) : this() {
        this.x = x; this.y = y; this.z = z
        this.stationName = stationName
        this.salesTotal = salesTotal
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName = ByteBufUtils.readUTF8String(buf)
        salesTotal = buf.readLong()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, stationName)
        buf.writeLong(salesTotal)
    }

    class Handler : IMessageHandler<PacketOpenStationGui, IMessage> {
        @SideOnly(Side.CLIENT)
        override fun onMessage(msg: PacketOpenStationGui, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiStationManager(
                    msg.x, msg.y, msg.z, msg.stationName, msg.salesTotal)
            )
            return null
        }
    }
}
