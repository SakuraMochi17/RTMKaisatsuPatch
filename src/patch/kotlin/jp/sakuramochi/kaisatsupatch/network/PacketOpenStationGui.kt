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
    var fareList: List<Pair<String, Int>> = emptyList()

    constructor(x: Int, y: Int, z: Int, stationName: String, salesTotal: Long,
                fareList: List<Pair<String, Int>> = emptyList()) : this() {
        this.x = x; this.y = y; this.z = z
        this.stationName = stationName
        this.salesTotal = salesTotal
        this.fareList = fareList
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName = ByteBufUtils.readUTF8String(buf)
        salesTotal = buf.readLong()
        val count = buf.readInt()
        val list = mutableListOf<Pair<String, Int>>()
        repeat(count) { list.add(ByteBufUtils.readUTF8String(buf) to buf.readInt()) }
        fareList = list
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, stationName)
        buf.writeLong(salesTotal)
        buf.writeInt(fareList.size)
        fareList.forEach { (dest, fare) ->
            ByteBufUtils.writeUTF8String(buf, dest)
            buf.writeInt(fare)
        }
    }

    class Handler : IMessageHandler<PacketOpenStationGui, IMessage> {
        @SideOnly(Side.CLIENT)
        override fun onMessage(msg: PacketOpenStationGui, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiStationManager(
                    msg.x, msg.y, msg.z, msg.stationName, msg.salesTotal, msg.fareList)
            )
            return null
        }
    }
}
