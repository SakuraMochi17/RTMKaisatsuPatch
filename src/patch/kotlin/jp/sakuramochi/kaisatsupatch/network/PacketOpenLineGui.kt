package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft

/** サーバー→クライアント：路線管理GUIを開く */
class PacketOpenLineGui() : IMessage {

    data class LineInfo(
        val lineID: String, val lineName: String,
        val baseFare: Int, val costPerBlock: Double,
        val transferFee: Int,
        val stations: List<String>
    )

    var x = 0; var y = 0; var z = 0
    var companyName = ""
    var globalStations: List<String> = emptyList()
    var companyLines: List<LineInfo> = emptyList()

    constructor(x: Int, y: Int, z: Int, companyName: String, globalStations: List<String>, companyLines: List<LineInfo>) : this() {
        this.x = x; this.y = y; this.z = z
        this.companyName = companyName
        this.globalStations = globalStations
        this.companyLines = companyLines
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        companyName = ByteBufUtils.readUTF8String(buf)
        val stCount = buf.readInt()
        globalStations = (0 until stCount).map { ByteBufUtils.readUTF8String(buf) }
        val lineCount = buf.readInt()
        companyLines = (0 until lineCount).map {
            val id   = ByteBufUtils.readUTF8String(buf)
            val name = ByteBufUtils.readUTF8String(buf)
            val base = buf.readInt()
            val cost = buf.readDouble()
            val tf   = buf.readInt()
            val stSize = buf.readInt()
            val sts = (0 until stSize).map { ByteBufUtils.readUTF8String(buf) }
            LineInfo(id, name, base, cost, tf, sts)
        }
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, companyName)
        buf.writeInt(globalStations.size)
        globalStations.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        buf.writeInt(companyLines.size)
        companyLines.forEach { line ->
            ByteBufUtils.writeUTF8String(buf, line.lineID)
            ByteBufUtils.writeUTF8String(buf, line.lineName)
            buf.writeInt(line.baseFare)
            buf.writeDouble(line.costPerBlock)
            buf.writeInt(line.transferFee)
            buf.writeInt(line.stations.size)
            line.stations.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        }
    }

    class Handler : IMessageHandler<PacketOpenLineGui, IMessage> {
        @SideOnly(Side.CLIENT)
        override fun onMessage(msg: PacketOpenLineGui, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiLineManager(msg)
            )
            return null
        }
    }
}
