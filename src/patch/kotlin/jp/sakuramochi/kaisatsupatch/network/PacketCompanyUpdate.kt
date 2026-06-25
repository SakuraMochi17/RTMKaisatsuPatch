package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.isOp
import jp.sakuramochi.kaisatsupatch.util.readEnum
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.sendError
import jp.sakuramochi.kaisatsupatch.util.writeEnum
import jp.sakuramochi.kaisatsupatch.util.writeStr
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/** C→S: 会社データの変更リクエスト */
class PacketCompanyUpdate() : IMessage {

    enum class Mode { SAVE, DELETE, ADD_MEMBER, REMOVE_MEMBER, ADD_MUTUAL, REMOVE_MUTUAL, ASSIGN_LINE }

    var mode = Mode.SAVE
    var companyID    = ""
    var companyName  = ""
    var color        = 0
    var icCardName   = ""
    var defaultBaseFare = 150
    var defaultCostPerBlock = 0.1
    var targetParam  = ""  // メンバー名 / 相互利用会社ID / 路線ID

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(mode.ordinal)
        buf.writeStr(companyID)
        buf.writeStr(companyName)
        buf.writeInt(color)
        buf.writeStr(icCardName)
        buf.writeInt(defaultBaseFare)
        buf.writeDouble(defaultCostPerBlock)
        buf.writeStr(targetParam)
    }

    override fun fromBytes(buf: ByteBuf) {
        mode = Mode.values()[buf.readInt()]
        companyID    = buf.readStr()
        companyName  = buf.readStr()
        color        = buf.readInt()
        icCardName   = buf.readStr()
        defaultBaseFare     = buf.readInt()
        defaultCostPerBlock = buf.readDouble()
        targetParam  = buf.readStr()
    }

    class Handler : IMessageHandler<PacketCompanyUpdate, IMessage> {
        override fun onMessage(msg: PacketCompanyUpdate, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            if (!player.isOp()) { player.sendError("権限がありません"); return null }
            val world = player.worldObj
            val data = KaisatsuNetworkData.get(world) ?: return null

            when (msg.mode) {
                Mode.SAVE -> {
                    val existing = data.companies[msg.companyID]
                    if (existing != null) {
                        existing.companyName = msg.companyName; existing.color = msg.color
                        existing.icCardName = msg.icCardName
                        existing.defaultBaseFare = msg.defaultBaseFare
                        existing.defaultCostPerBlock = msg.defaultCostPerBlock
                    } else {
                        data.companies[msg.companyID] = KaisatsuNetworkData.CompanyData(
                            msg.companyID, msg.companyName, msg.color, msg.icCardName,
                            defaultBaseFare = msg.defaultBaseFare,
                            defaultCostPerBlock = msg.defaultCostPerBlock
                        )
                    }
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}会社 [${msg.companyID}] ${msg.companyName} を保存しました"))
                }
                Mode.DELETE -> {
                    data.companies.remove(msg.companyID)
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.YELLOW}会社 [${msg.companyID}] を削除しました"))
                }
                Mode.ADD_MEMBER -> {
                    val co = data.companies[msg.companyID] ?: return null
                    co.members.add(msg.targetParam); data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}${msg.targetParam} を ${co.companyName} のメンバーに追加しました"))
                }
                Mode.REMOVE_MEMBER -> {
                    val co = data.companies[msg.companyID] ?: return null
                    co.members.remove(msg.targetParam); data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.YELLOW}${msg.targetParam} を ${co.companyName} から除名しました"))
                }
                Mode.ADD_MUTUAL -> {
                    val co = data.companies[msg.companyID] ?: return null
                    co.allowedCompanies.add(msg.targetParam); data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}${co.companyName} → ${msg.targetParam} の相互利用を許可しました"))
                }
                Mode.REMOVE_MUTUAL -> {
                    val co = data.companies[msg.companyID] ?: return null
                    co.allowedCompanies.remove(msg.targetParam); data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.YELLOW}${co.companyName} → ${msg.targetParam} の相互利用を解除しました"))
                }
                Mode.ASSIGN_LINE -> {
                    val line = data.companyLines[msg.targetParam] ?: return null
                    line.companyID = msg.companyID; data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}路線 ${line.lineName} を会社 [${msg.companyID}] に紐付けました"))
                }
            }
            return null
        }
    }
}
