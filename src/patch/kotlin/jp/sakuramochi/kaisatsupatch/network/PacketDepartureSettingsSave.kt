package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureSettings
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr

/** C→S: 発車標設定ブロックの設定を保存する */
class PacketDepartureSettingsSave() : IMessage {

    var x = 0; var y = 0; var z = 0
    var stationName  = ""
    var lineID       = ""
    var diaName      = ""
    var timeMode     = "real"

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(stationName)
        buf.writeStr(lineID)
        buf.writeStr(diaName)
        buf.writeStr(timeMode)
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationName  = buf.readStr()
        lineID       = buf.readStr()
        diaName      = buf.readStr()
        timeMode     = buf.readStr()
    }

    class Handler : IMessageHandler<PacketDepartureSettingsSave, IMessage> {
        override fun onMessage(msg: PacketDepartureSettingsSave, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val tile = player.worldObj.getTileEntity(msg.x, msg.y, msg.z)
                as? TileEntityDepartureSettings ?: return null
            tile.stationName  = msg.stationName
            tile.lineID       = msg.lineID
            tile.diaName      = msg.diaName
            tile.timeMode     = msg.timeMode
            tile.markDirty()
            tile.worldObj?.markBlockForUpdate(msg.x, msg.y, msg.z)
            return null
        }
    }
}
