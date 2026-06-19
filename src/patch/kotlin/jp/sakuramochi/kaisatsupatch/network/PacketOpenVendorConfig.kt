package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft

/** S→C: 券売機の設置駅選択GUIを開く */
class PacketOpenVendorConfig() : IMessage {
    var x = 0; var y = 0; var z = 0
    var currentStation = ""
    var stationList: List<String> = emptyList()

    constructor(x: Int, y: Int, z: Int, currentStation: String, stationList: List<String>) : this() {
        this.x = x; this.y = y; this.z = z
        this.currentStation = currentStation
        this.stationList = stationList
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, currentStation)
        buf.writeInt(stationList.size)
        stationList.forEach { ByteBufUtils.writeUTF8String(buf, it) }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        currentStation = ByteBufUtils.readUTF8String(buf)
        val size = buf.readInt()
        stationList = (0 until size).map { ByteBufUtils.readUTF8String(buf) }
    }

    class Handler : IMessageHandler<PacketOpenVendorConfig, IMessage> {
        override fun onMessage(msg: PacketOpenVendorConfig, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiVendorStationConfig(
                    msg.x, msg.y, msg.z, msg.currentStation, msg.stationList
                )
            )
            return null
        }
    }
}
