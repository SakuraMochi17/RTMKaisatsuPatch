package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft

/** S→C: 会社管理GUIを開く */
class PacketOpenCompanyManager() : IMessage {

    data class CompanyInfo(
        val companyID: String,
        val companyName: String,
        val color: Int,
        val icCardName: String,
        val defaultBaseFare: Int,
        val defaultCostPerBlock: Double,
        val members: List<String>,
        val allowedCompanies: List<String>
    )

    data class LineInfo(val lineID: String, val lineName: String, val companyID: String)

    var companies: List<CompanyInfo> = emptyList()
    var lines: List<LineInfo> = emptyList()

    constructor(companies: List<CompanyInfo>, lines: List<LineInfo>) : this() {
        this.companies = companies; this.lines = lines
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(companies.size)
        companies.forEach { c ->
            ByteBufUtils.writeUTF8String(buf, c.companyID)
            ByteBufUtils.writeUTF8String(buf, c.companyName)
            buf.writeInt(c.color)
            ByteBufUtils.writeUTF8String(buf, c.icCardName)
            buf.writeInt(c.defaultBaseFare)
            buf.writeDouble(c.defaultCostPerBlock)
            buf.writeInt(c.members.size)
            c.members.forEach { ByteBufUtils.writeUTF8String(buf, it) }
            buf.writeInt(c.allowedCompanies.size)
            c.allowedCompanies.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        }
        buf.writeInt(lines.size)
        lines.forEach { l ->
            ByteBufUtils.writeUTF8String(buf, l.lineID)
            ByteBufUtils.writeUTF8String(buf, l.lineName)
            ByteBufUtils.writeUTF8String(buf, l.companyID)
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        val cSize = buf.readInt()
        companies = (0 until cSize).map {
            val id   = ByteBufUtils.readUTF8String(buf)
            val name = ByteBufUtils.readUTF8String(buf)
            val col  = buf.readInt()
            val ic   = ByteBufUtils.readUTF8String(buf)
            val base = buf.readInt()
            val rate = buf.readDouble()
            val mems = (0 until buf.readInt()).map { ByteBufUtils.readUTF8String(buf) }
            val mutu = (0 until buf.readInt()).map { ByteBufUtils.readUTF8String(buf) }
            CompanyInfo(id, name, col, ic, base, rate, mems, mutu)
        }
        val lSize = buf.readInt()
        lines = (0 until lSize).map {
            LineInfo(ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf), ByteBufUtils.readUTF8String(buf))
        }
    }

    class Handler : IMessageHandler<PacketOpenCompanyManager, IMessage> {
        override fun onMessage(msg: PacketOpenCompanyManager, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiCompanyManager(msg.companies, msg.lines)
            )
            return null
        }
    }
}
