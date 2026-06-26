package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityLineManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.util.readCoords
import jp.sakuramochi.kaisatsupatch.util.readEnum
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.readStringList
import jp.sakuramochi.kaisatsupatch.util.writeCoords
import jp.sakuramochi.kaisatsupatch.util.writeEnum
import jp.sakuramochi.kaisatsupatch.util.writeStr
import jp.sakuramochi.kaisatsupatch.util.writeStringList
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/** クライアント→サーバー：路線・会社データの保存・更新 */
class PacketLineUpdate() : IMessage {

    enum class Mode {
        SAVE_COMPANY_PROPS,  // 会社プロパティ保存（ID/名前/色/IC名/運賃デフォルト）
        SAVE_LINE,           // 路線保存
        DELETE_LINE,         // 路線削除
        ADD_MEMBER,          // メンバー追加
        REMOVE_MEMBER,       // メンバー除名
        ADD_MUTUAL,          // 相互利用許可追加
        REMOVE_MUTUAL        // 相互利用許可解除
    }

    var x = 0; var y = 0; var z = 0
    var mode = Mode.SAVE_LINE

    // 会社操作
    var companyID           = ""
    var companyName         = ""
    var companyColor        = 0x1E90FF
    var icCardName          = ""
    var defaultBaseFare     = 150
    var defaultCostPerBlock = 0.1
    var targetParam         = "" // ADD/REMOVE_MEMBER: プレイヤー名 / ADD/REMOVE_MUTUAL: 会社ID

    // 路線操作
    var oldLineID    = ""; var newLineID = ""; var lineName = ""
    var baseFare     = 150; var costPerBlock = 0.15; var transferFee = 0
    var lineStations : List<String> = emptyList()

    override fun toBytes(buf: ByteBuf) {
        buf.writeCoords(x, y, z)
        buf.writeEnum(mode)
        buf.writeStr(companyID)
        buf.writeStr(companyName)
        buf.writeInt(companyColor)
        buf.writeStr(icCardName)
        buf.writeInt(defaultBaseFare)
        buf.writeDouble(defaultCostPerBlock)
        buf.writeStr(targetParam)
        buf.writeStr(oldLineID); buf.writeStr(newLineID); buf.writeStr(lineName)
        buf.writeInt(baseFare); buf.writeDouble(costPerBlock); buf.writeInt(transferFee)
        buf.writeStringList(lineStations)
    }

    override fun fromBytes(buf: ByteBuf) {
        buf.readCoords().also { x = it.x; y = it.y; z = it.z }
        mode                = buf.readEnum()
        companyID           = buf.readStr()
        companyName         = buf.readStr()
        companyColor        = buf.readInt()
        icCardName          = buf.readStr()
        defaultBaseFare     = buf.readInt()
        defaultCostPerBlock = buf.readDouble()
        targetParam         = buf.readStr()
        oldLineID    = buf.readStr(); newLineID = buf.readStr(); lineName = buf.readStr()
        baseFare     = buf.readInt(); costPerBlock = buf.readDouble(); transferFee = buf.readInt()
        lineStations = buf.readStringList()
    }

    class Handler : IMessageHandler<PacketLineUpdate, IMessage> {
        override fun onMessage(msg: PacketLineUpdate, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world  = player.worldObj
            val tile   = world.getTileEntity(msg.x, msg.y, msg.z) as? TileEntityLineManager ?: return null
            val data   = KaisatsuNetworkData.get(world) ?: return null

            when (msg.mode) {
                Mode.SAVE_COMPANY_PROPS -> {
                    tile.companyID   = msg.companyID
                    tile.companyName = msg.companyName
                    tile.markDirty()
                    val existing = data.companies[msg.companyID]
                    if (existing != null) {
                        existing.companyName        = msg.companyName
                        existing.color              = msg.companyColor
                        existing.icCardName         = msg.icCardName
                        existing.defaultBaseFare    = msg.defaultBaseFare
                        existing.defaultCostPerBlock = msg.defaultCostPerBlock
                    } else {
                        data.companies[msg.companyID] = KaisatsuNetworkData.CompanyData(
                            companyID           = msg.companyID,
                            companyName         = msg.companyName,
                            color               = msg.companyColor,
                            icCardName          = msg.icCardName,
                            defaultBaseFare     = msg.defaultBaseFare,
                            defaultCostPerBlock = msg.defaultCostPerBlock
                        )
                    }
                    // 路線の表示名も同期
                    data.companyLines.values
                        .filter { it.companyID == msg.companyID }
                        .forEach { it.companyName = msg.companyName }
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}会社「${msg.companyName}」を保存しました"
                    ))
                }

                Mode.SAVE_LINE -> {
                    if (msg.oldLineID.isNotEmpty() && msg.oldLineID != msg.newLineID)
                        data.companyLines.remove(msg.oldLineID)
                    val line = KaisatsuNetworkData.LineData(
                        lineID       = msg.newLineID,
                        lineName     = msg.lineName,
                        companyName  = msg.companyName,
                        companyID    = msg.companyID,
                        baseFare     = msg.baseFare,
                        costPerBlock = msg.costPerBlock,
                        transferFee  = msg.transferFee
                    )
                    line.stationOrder.addAll(msg.lineStations)
                    data.companyLines[msg.newLineID] = line
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.GREEN}路線「${msg.lineName}」を保存しました（${msg.lineStations.size}駅）"
                    ))
                }

                Mode.DELETE_LINE -> {
                    data.companyLines.remove(msg.oldLineID)
                    data.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.YELLOW}路線「${msg.oldLineID}」を削除しました"
                    ))
                }

                Mode.ADD_MEMBER -> {
                    data.companies[tile.companyID]?.members?.add(msg.targetParam)
                    data.markDirty()
                }

                Mode.REMOVE_MEMBER -> {
                    data.companies[tile.companyID]?.members?.remove(msg.targetParam)
                    data.markDirty()
                }

                Mode.ADD_MUTUAL -> {
                    data.companies[tile.companyID]?.allowedCompanies?.add(msg.targetParam)
                    data.markDirty()
                }

                Mode.REMOVE_MUTUAL -> {
                    data.companies[tile.companyID]?.allowedCompanies?.remove(msg.targetParam)
                    data.markDirty()
                }
            }
            return null
        }
    }
}
