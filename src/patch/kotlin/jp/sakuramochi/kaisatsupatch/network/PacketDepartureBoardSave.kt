package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr

/** C→S: 発車標（表示体）の表示情報を保存する */
class PacketDepartureBoardSave() : IMessage {

    var x = 0; var y = 0; var z = 0
    var headerLine      = ""
    var headerDirection = ""
    var platform        = ""
    var lineColorHex    = 0x1E90FF

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(headerLine)
        buf.writeStr(headerDirection)
        buf.writeStr(platform)
        buf.writeInt(lineColorHex)
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        headerLine      = buf.readStr()
        headerDirection = buf.readStr()
        platform        = buf.readStr()
        lineColorHex    = buf.readInt()
    }

    class Handler : IMessageHandler<PacketDepartureBoardSave, IMessage> {
        override fun onMessage(msg: PacketDepartureBoardSave, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val tile = player.worldObj.getTileEntity(msg.x, msg.y, msg.z)
                as? TileEntityDepartureBoard ?: return null
            tile.headerLine      = msg.headerLine
            tile.headerDirection = msg.headerDirection
            tile.platform        = msg.platform
            tile.lineColorHex    = msg.lineColorHex
            tile.markDirty()
            tile.worldObj?.markBlockForUpdate(msg.x, msg.y, msg.z)
            return null
        }
    }
}
