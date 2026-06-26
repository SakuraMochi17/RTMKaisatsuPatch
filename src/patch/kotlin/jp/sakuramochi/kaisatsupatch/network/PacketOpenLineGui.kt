package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.readStringList
import jp.sakuramochi.kaisatsupatch.util.writeStr
import jp.sakuramochi.kaisatsupatch.util.writeStringList
import net.minecraft.client.Minecraft

/** サーバー→クライアント：路線管理GUIを開く */
class PacketOpenLineGui() : IMessage {

    data class LineInfo(
        val lineID: String, val lineName: String,
        val baseFare: Int, val costPerBlock: Double,
        val transferFee: Int,
        val stations: List<String>
    )

    // ブロック座標
    var x = 0; var y = 0; var z = 0

    // 会社情報（このブロックが管轄する会社）
    var companyID           = ""
    var companyName         = ""
    var companyColor        = 0x1E90FF
    var icCardName          = ""
    var defaultBaseFare     = 150
    var defaultCostPerBlock = 0.1
    var members             : List<String>              = emptyList()
    var allowedCompanies    : List<String>              = emptyList()
    var otherCompanies      : List<Pair<String,String>> = emptyList() // id to name（相互利用選択用）

    // 路線・駅
    var globalStations: List<String>  = emptyList()
    var companyLines  : List<LineInfo> = emptyList()

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        // 会社情報
        buf.writeStr(companyID)
        buf.writeStr(companyName)
        buf.writeInt(companyColor)
        buf.writeStr(icCardName)
        buf.writeInt(defaultBaseFare)
        buf.writeDouble(defaultCostPerBlock)
        buf.writeStringList(members)
        buf.writeStringList(allowedCompanies)
        buf.writeInt(otherCompanies.size)
        otherCompanies.forEach { (id, name) -> buf.writeStr(id); buf.writeStr(name) }
        // 路線・駅
        buf.writeStringList(globalStations)
        buf.writeInt(companyLines.size)
        companyLines.forEach { line ->
            buf.writeStr(line.lineID); buf.writeStr(line.lineName)
            buf.writeInt(line.baseFare); buf.writeDouble(line.costPerBlock)
            buf.writeInt(line.transferFee)
            buf.writeInt(line.stations.size)
            line.stations.forEach { buf.writeStr(it) }
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        // 会社情報
        companyID           = buf.readStr()
        companyName         = buf.readStr()
        companyColor        = buf.readInt()
        icCardName          = buf.readStr()
        defaultBaseFare     = buf.readInt()
        defaultCostPerBlock = buf.readDouble()
        members             = buf.readStringList()
        allowedCompanies    = buf.readStringList()
        otherCompanies      = (0 until buf.readInt()).map { buf.readStr() to buf.readStr() }
        // 路線・駅
        globalStations = buf.readStringList()
        val lineCount = buf.readInt()
        companyLines = (0 until lineCount).map {
            val id   = buf.readStr(); val name = buf.readStr()
            val base = buf.readInt(); val cost = buf.readDouble(); val tf = buf.readInt()
            val sts  = (0 until buf.readInt()).map { buf.readStr() }
            LineInfo(id, name, base, cost, tf, sts)
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
