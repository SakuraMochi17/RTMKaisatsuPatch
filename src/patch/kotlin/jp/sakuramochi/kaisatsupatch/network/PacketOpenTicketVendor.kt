package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf

/**
 * S→C: 券売機を開く直前に運賃リストをクライアントへキャッシュする。
 * このパケットの直後にサーバーが player.openGui() を呼び Forge の S2EPacketOpenWindow が届く。
 * GuiHandler の getClientGuiElement で FaresCache を読んで GUI を構築する。
 */
class PacketOpenTicketVendor() : IMessage {
    var stationName = ""
    var fares: List<Pair<String, Int>> = emptyList()

    constructor(stationName: String, fares: List<Pair<String, Int>>) : this() {
        this.stationName = stationName
        this.fares = fares
    }

    override fun toBytes(buf: ByteBuf) {
        ByteBufUtils.writeUTF8String(buf, stationName)
        buf.writeInt(fares.size)
        fares.forEach { (dest, fare) ->
            ByteBufUtils.writeUTF8String(buf, dest)
            buf.writeInt(fare)
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        stationName = ByteBufUtils.readUTF8String(buf)
        val size = buf.readInt()
        fares = (0 until size).map { ByteBufUtils.readUTF8String(buf) to buf.readInt() }
    }

    class Handler : IMessageHandler<PacketOpenTicketVendor, IMessage> {
        override fun onMessage(msg: PacketOpenTicketVendor, ctx: MessageContext): IMessage? {
            FaresCache.station = msg.stationName
            FaresCache.fares   = msg.fares
            return null
        }
    }
}

/** クライアント専用の静的キャッシュ（GuiHandler から参照する）*/
object FaresCache {
    var station: String = ""
    var fares: List<Pair<String, Int>> = emptyList()
}
