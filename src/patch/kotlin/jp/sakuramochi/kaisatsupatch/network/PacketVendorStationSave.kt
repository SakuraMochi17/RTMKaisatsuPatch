package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityFareAdjustment
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityReservedVendor

/** C→S: 券売機の設置駅を保存する */
class PacketVendorStationSave() : IMessage {
    var x = 0; var y = 0; var z = 0
    var stationName = ""

    constructor(x: Int, y: Int, z: Int, stationName: String) : this() {
        this.x = x; this.y = y; this.z = z; this.stationName = stationName
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, stationName)
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName = ByteBufUtils.readUTF8String(buf)
    }

    class Handler : IMessageHandler<PacketVendorStationSave, IMessage> {
        override fun onMessage(msg: PacketVendorStationSave, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val tile = player.worldObj.getTileEntity(msg.x, msg.y, msg.z)
            when (tile) {
                is TileEntityCustomTicketVendor -> { tile.stationName = msg.stationName; tile.markDirty() }
                is TileEntityReservedVendor     -> { tile.stationName = msg.stationName; tile.markDirty() }
                is TileEntityFareAdjustment     -> { tile.stationName = msg.stationName; tile.markDirty() }
            }
            return null
        }
    }
}
