package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readCoords
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeCoords
import jp.sakuramochi.kaisatsupatch.util.writeStr
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityFareAdjustment
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityReservedVendor

/** C→S: 券売機の設置駅・所属会社を保存する */
class PacketVendorStationSave() : IMessage {
    var x = 0; var y = 0; var z = 0
    var stationName = ""
    var companyID = ""

    constructor(x: Int, y: Int, z: Int, stationName: String, companyID: String = "") : this() {
        this.x = x; this.y = y; this.z = z
        this.stationName = stationName
        this.companyID = companyID
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeCoords(x, y, z)
        buf.writeStr(stationName)
        buf.writeStr(companyID)
    }

    override fun fromBytes(buf: ByteBuf) {
        buf.readCoords().also { x = it.x; y = it.y; z = it.z }
        stationName = buf.readStr()
        companyID = buf.readStr()
    }

    class Handler : IMessageHandler<PacketVendorStationSave, IMessage> {
        override fun onMessage(msg: PacketVendorStationSave, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val tile = player.worldObj.getTileEntity(msg.x, msg.y, msg.z)
            when (tile) {
                is TileEntityCustomTicketVendor -> {
                    tile.stationName = msg.stationName
                    tile.companyID = msg.companyID
                    tile.markDirty()
                }
                is TileEntityReservedVendor -> { tile.stationName = msg.stationName; tile.markDirty() }
                is TileEntityFareAdjustment -> { tile.stationName = msg.stationName; tile.markDirty() }
            }
            return null
        }
    }
}
