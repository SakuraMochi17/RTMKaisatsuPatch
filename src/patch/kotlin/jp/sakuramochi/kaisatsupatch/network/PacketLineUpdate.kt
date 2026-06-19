package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
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
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        mode = Mode.values()[buf.readInt()]
        companyName = ByteBufUtils.readUTF8String(buf)
        oldLineID = ByteBufUtils.readUTF8String(buf)
        newLineID = ByteBufUtils.readUTF8String(buf)
        lineName = ByteBufUtils.readUTF8String(buf)
        baseFare = buf.readInt()
        costPerBlock = buf.readDouble()
        transferFee = buf.readInt()
        val size = buf.readInt()
        lineStations = (0 until size).map { ByteBufUtils.readUTF8String(buf) }
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeInt(mode.ordinal)
        ByteBufUtils.writeUTF8String(buf, companyName)
        ByteBufUtils.writeUTF8String(buf, oldLineID)
        ByteBufUtils.writeUTF8String(buf, newLineID)
        ByteBufUtils.writeUTF8String(buf, lineName)
        buf.writeInt(baseFare)
        buf.writeDouble(costPerBlock)
        buf.writeInt(transferFee)
        buf.writeInt(lineStations.size)
        lineStations.forEach { ByteBufUtils.writeUTF8String(buf, it) }
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
