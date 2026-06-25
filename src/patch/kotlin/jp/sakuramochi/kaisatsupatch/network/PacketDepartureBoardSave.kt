package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr

/** C→S: 発車標の設定を保存する */
class PacketDepartureBoardSave() : IMessage {

    var x = 0; var y = 0; var z = 0
    var stationName = ""
    var lineID      = ""
    var platform    = ""
    var diaName     = ""
    var direction   = "両方"
    var displayRows = 5
    var title       = ""

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(stationName)
        buf.writeStr(lineID)
        buf.writeStr(platform)
        buf.writeStr(diaName)
        buf.writeStr(direction)
        buf.writeInt(displayRows)
        buf.writeStr(title)
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName = buf.readStr()
        lineID      = buf.readStr()
        platform    = buf.readStr()
        diaName     = buf.readStr()
        direction   = buf.readStr()
        displayRows = buf.readInt()
        title       = buf.readStr()
    }

    class Handler : IMessageHandler<PacketDepartureBoardSave, IMessage> {
        override fun onMessage(msg: PacketDepartureBoardSave, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val tile = player.worldObj.getTileEntity(msg.x, msg.y, msg.z)
                as? TileEntityDepartureBoard ?: return null
            tile.stationName  = msg.stationName
            tile.lineID       = msg.lineID
            tile.platform     = msg.platform
            tile.diaName      = msg.diaName
            tile.direction    = msg.direction
            tile.displayRows  = msg.displayRows
            tile.title        = msg.title
            tile.markDirty()
            tile.recomputeDepartures()
            return null
        }
    }
}
