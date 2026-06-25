package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readCoords
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeCoords
import jp.sakuramochi.kaisatsupatch.util.writeStr
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
    var salesTotal  = 0L
    var salesTicket = 0L
    var salesIC     = 0L
    var salesPass   = 0L
    var salesExpress = 0L
    var fareList: List<Pair<String, Int>> = emptyList()

    constructor(
        x: Int, y: Int, z: Int,
        stationName: String,
        salesTotal: Long,
        fareList: List<Pair<String, Int>> = emptyList(),
        salesTicket: Long = 0L,
        salesIC: Long = 0L,
        salesPass: Long = 0L,
        salesExpress: Long = 0L
    ) : this() {
        this.x = x; this.y = y; this.z = z
        this.stationName  = stationName
        this.salesTotal   = salesTotal
        this.fareList     = fareList
        this.salesTicket  = salesTicket
        this.salesIC      = salesIC
        this.salesPass    = salesPass
        this.salesExpress = salesExpress
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName  = buf.readStr()
        salesTotal   = buf.readLong()
        salesTicket  = buf.readLong()
        salesIC      = buf.readLong()
        salesPass    = buf.readLong()
        salesExpress = buf.readLong()
        val count = buf.readInt()
        val list = mutableListOf<Pair<String, Int>>()
        repeat(count) { list.add(buf.readStr() to buf.readInt()) }
        fareList = list
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(stationName)
        buf.writeLong(salesTotal)
        buf.writeLong(salesTicket)
        buf.writeLong(salesIC)
        buf.writeLong(salesPass)
        buf.writeLong(salesExpress)
        buf.writeInt(fareList.size)
        fareList.forEach { (dest, fare) ->
            buf.writeStr(dest)
            buf.writeInt(fare)
        }
    }

    class Handler : IMessageHandler<PacketOpenStationGui, IMessage> {
        @SideOnly(Side.CLIENT)
        override fun onMessage(msg: PacketOpenStationGui, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiStationManager(
                    msg.x, msg.y, msg.z, msg.stationName, msg.salesTotal, msg.fareList,
                    msg.salesTicket, msg.salesIC, msg.salesPass, msg.salesExpress
                )
            )
            return null
        }
    }
}
