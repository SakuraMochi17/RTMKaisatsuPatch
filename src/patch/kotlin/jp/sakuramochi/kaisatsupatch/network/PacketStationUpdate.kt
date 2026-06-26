package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readCoords
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeCoords
import jp.sakuramochi.kaisatsupatch.util.writeStr
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityStationManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

/** クライアント→サーバー：駅名を登録・更新する */
class PacketStationUpdate() : IMessage {
    var x = 0; var y = 0; var z = 0
    var oldName = ""; var newName = ""

    constructor(x: Int, y: Int, z: Int, oldName: String, newName: String) : this() {
        this.x = x; this.y = y; this.z = z; this.oldName = oldName; this.newName = newName
    }

    override fun fromBytes(buf: ByteBuf) {
        buf.readCoords().also { x = it.x; y = it.y; z = it.z }
        oldName = buf.readStr()
        newName = buf.readStr()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeCoords(x, y, z)
        buf.writeStr(oldName)
        buf.writeStr(newName)
    }

    class Handler : IMessageHandler<PacketStationUpdate, IMessage> {
        override fun onMessage(msg: PacketStationUpdate, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val tile = world.getTileEntity(msg.x, msg.y, msg.z) as? TileEntityStationManager ?: return null
            val data = KaisatsuNetworkData.get(world) ?: return null

            // newName が空 = 削除モード
            if (msg.newName.isEmpty()) {
                if (msg.oldName != "未設定" && data.globalStations.containsKey(msg.oldName)) {
                    data.globalStations.remove(msg.oldName)
                    data.companyLines.values.forEach { line -> line.stationOrder.remove(msg.oldName) }
                    data.markDirty()
                    tile.stationName = "未設定"
                    tile.markDirty()
                    player.addChatMessage(ChatComponentText(
                        "${EnumChatFormatting.YELLOW}駅「${msg.oldName}」をネットワークから削除しました"
                    ))
                }
                return null
            }

            // 別の場所にすでに同名の駅が登録されていたら拒否
            if (msg.newName != msg.oldName && data.globalStations.containsKey(msg.newName)) {
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.RED}駅名「${msg.newName}」はすでに別の場所に登録されています"
                ))
                return null
            }

            val coords = data.globalStations[msg.oldName]
                ?: KaisatsuNetworkData.StationCoords(msg.x, msg.y, msg.z)

            if (msg.oldName != msg.newName && msg.oldName != "未設定") {
                data.globalStations.remove(msg.oldName)
                data.companyLines.values.forEach { line ->
                    for (i in line.stationOrder.indices) {
                        if (line.stationOrder[i] == msg.oldName) line.stationOrder[i] = msg.newName
                    }
                }
            }
            data.globalStations[msg.newName] = coords
            data.markDirty()

            tile.stationName = msg.newName
            tile.markDirty()

            player.addChatMessage(ChatComponentText(
                "${EnumChatFormatting.GREEN}駅「${msg.newName}」をネットワークに登録しました"
            ))
            return null
        }
    }
}
