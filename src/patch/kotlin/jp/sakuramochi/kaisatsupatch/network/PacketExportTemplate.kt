package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.core.OuDiaExporter
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.DimensionManager
import java.io.File

/**
 * C→S: 路線 ID を指定して OuDia テンプレートをサーバー側で書き出す。
 * lineID が空文字のときは全駅テンプレートを出力する。
 */
class PacketExportTemplate : IMessage {

    var lineID: String = ""

    override fun fromBytes(buf: ByteBuf) {
        val len = buf.readInt()
        lineID = buf.readBytes(len).toString(Charsets.UTF_8)
    }

    override fun toBytes(buf: ByteBuf) {
        val bytes = lineID.toByteArray(Charsets.UTF_8)
        buf.writeInt(bytes.size)
        buf.writeBytes(bytes)
    }

    class Handler : IMessageHandler<PacketExportTemplate, IMessage> {
        override fun onMessage(msg: PacketExportTemplate, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world  = DimensionManager.getWorld(0)

            if (!player.mcServer.configurationManager.func_152596_g(player.gameProfile)) {
                player.addChatMessage(ChatComponentText("§cOP 権限が必要です")); return null
            }

            val data = KaisatsuNetworkData.get(world) ?: run {
                player.addChatMessage(ChatComponentText("§cネットワークデータが見つかりません")); return null
            }

            val configDir = Loader.instance().configDir
            val outputDir = File(configDir, "kaizpatch/timetables")

            try {
                val file = if (msg.lineID.isEmpty()) {
                    OuDiaExporter.exportAll(data, outputDir)
                } else {
                    val line = data.companyLines[msg.lineID] ?: run {
                        player.addChatMessage(ChatComponentText("§c路線 '${msg.lineID}' が見つかりません")); return null
                    }
                    OuDiaExporter.exportLine(line, outputDir)
                }
                player.addChatMessage(ChatComponentText("§a[OuDia] テンプレートを出力しました:"))
                player.addChatMessage(ChatComponentText("  §f${file.name}  §7(${file.parentFile.absolutePath})"))
                val tt = data.timetable
                if (tt == null) {
                    player.addChatMessage(ChatComponentText(
                        "  §7ヒント: /kaisatsu timetable load ${file.name}  で読み込めます"))
                }
            } catch (e: Exception) {
                player.addChatMessage(ChatComponentText("§c書き出しエラー: ${e.message}"))
            }
            return null
        }
    }
}
