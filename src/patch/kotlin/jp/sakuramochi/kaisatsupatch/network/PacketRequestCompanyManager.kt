package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.entity.player.EntityPlayerMP

/** C→S: GuiLineManager の「会社管理」ボタンから会社管理GUIの表示を要求する */
class PacketRequestCompanyManager : IMessage {
    override fun toBytes(buf: ByteBuf) {}
    override fun fromBytes(buf: ByteBuf) {}

    class Handler : IMessageHandler<PacketRequestCompanyManager, IMessage> {
        override fun onMessage(msg: PacketRequestCompanyManager, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity as EntityPlayerMP
            val data = KaisatsuNetworkData.get(player.worldObj) ?: return null
            val companyInfos = data.companies.values.sortedBy { it.companyID }.map { c ->
                PacketOpenCompanyManager.CompanyInfo(
                    c.companyID, c.companyName, c.color, c.icCardName,
                    c.defaultBaseFare, c.defaultCostPerBlock,
                    c.members.toList(), c.allowedCompanies.toList()
                )
            }
            val lineInfos = data.companyLines.values.sortedBy { it.lineID }.map { l ->
                PacketOpenCompanyManager.LineInfo(l.lineID, l.lineName, l.companyID)
            }
            KaizPatchNetwork.CHANNEL.sendTo(PacketOpenCompanyManager(companyInfos, lineInfos), player)
            return null
        }
    }
}
