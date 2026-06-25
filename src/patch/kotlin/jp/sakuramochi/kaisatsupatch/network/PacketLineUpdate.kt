package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import jp.sakuramochi.kaisatsupatch.util.readCoords
import jp.sakuramochi.kaisatsupatch.util.readEnum
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.readStringList
import jp.sakuramochi.kaisatsupatch.util.writeCoords
import jp.sakuramochi.kaisatsupatch.util.writeEnum
import jp.sakuramochi.kaisatsupatch.util.writeStr
import jp.sakuramochi.kaisatsupatch.util.writeStringList
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityLineManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/** クライアント→サーバー：路線データを保存・削除・会社名変更する */
class PacketLineUpdate() : IMessage {

    enum class Mode { SAVE_COMPANY, SAVE_LINE, DELETE_LINE }

    var x = 0; var y = 0; var z = 0
    var mode = Mode.SAVE_LINE
    var companyName = ""
    var oldLineID = ""; var newLineID = ""; var lineName = ""
    var baseFare = 150; var costPerBlock = 0.15; var transferFee = 0
    var lineStations: List<String> = emptyList()

    override fun fromBytes(buf: ByteBuf) {
        buf.readCoords().also { x = it.x; y = it.y; z = it.z }
        mode = buf.readEnum<Mode>()
        companyName = buf.readStr()
        oldLineID = buf.readStr()
        newLineID = buf.readStr()
        lineName = buf.readStr()
        baseFare = buf.readInt()
        costPerBlock = buf.readDouble()
        transferFee = buf.readInt()
        lineStations = buf.readStringList()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeCoords(x, y, z)
        buf.writeEnum(mode)
        buf.writeStr(companyName)
        buf.writeStr(oldLineID)
        buf.writeStr(newLineID)
        buf.writeStr(lineName)
        buf.writeInt(baseFare)
        buf.writeDouble(costPerBlock)
        buf.writeInt(transferFee)
        buf.writeStringList(lineStations)
    }

    class Handler : IMessageHandler<PacketLineUpdate, IMessage> {
        override fun onMessage(msg: PacketLineUpdate, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val tile = world.getTileEntity(msg.x, msg.y, msg.z) as? TileEntityLineManager ?: return null
            val data = KaisatsuNetworkData.get(world) ?: return null

            when (msg.mode) {
                PacketLineUpdate.Mode.SAVE_COMPANY -> {
                    tile.companyName = msg.companyName
                    tile.markDirty()
                    // 既存路線の会社名も更新
                    data.companyLines.values.filter { it.companyName == tile.companyName }
                        .forEach { it.companyName = msg.companyName }
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}会社名を「${msg.companyName}」に変更しました"
                    ))
                }
                PacketLineUpdate.Mode.SAVE_LINE -> {
                    if (msg.oldLineID.isNotEmpty() && msg.oldLineID != msg.newLineID) {
                        data.companyLines.remove(msg.oldLineID)
                    }
                    val line = KaisatsuNetworkData.LineData(
                        lineID = msg.newLineID, lineName = msg.lineName,
                        companyName = msg.companyName,
                        baseFare = msg.baseFare, costPerBlock = msg.costPerBlock,
                        transferFee = msg.transferFee
                    )
                    line.stationOrder.addAll(msg.lineStations)
                    data.companyLines[msg.newLineID] = line
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}路線「${msg.lineName}」を保存しました（${msg.lineStations.size}駅）"
                    ))
                }
                PacketLineUpdate.Mode.DELETE_LINE -> {
                    data.companyLines.remove(msg.oldLineID)
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.YELLOW}路線「${msg.oldLineID}」を削除しました"
                    ))
                }
            }
            return null
        }
    }
}
