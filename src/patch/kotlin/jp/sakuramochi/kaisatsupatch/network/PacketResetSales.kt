package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData

class PacketResetSales() : IMessage {
    var stationName = ""

    constructor(stationName: String) : this() {
        this.stationName = stationName
    }

    override fun fromBytes(buf: ByteBuf) { stationName = ByteBufUtils.readUTF8String(buf) }
    override fun toBytes(buf: ByteBuf) { ByteBufUtils.writeUTF8String(buf, stationName) }

    class Handler : IMessageHandler<PacketResetSales, IMessage> {
        override fun onMessage(msg: PacketResetSales, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val data = KaisatsuNetworkData.get(world) ?: return null
            data.stationSales.remove(msg.stationName)
            data.markDirty()
            return null
        }
    }
}
