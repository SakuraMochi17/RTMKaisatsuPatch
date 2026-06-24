package jp.sakuramochi.kaisatsupatch.command

import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText
import java.text.SimpleDateFormat
import java.util.Date

/** /kaisatsulog [駅名] — 改札通過ログを表示（OP専用） */
class CommandKaisatsuLog : CommandBase() {

    override fun getCommandName() = "kaisatsulog"

    override fun getCommandUsage(sender: ICommandSender) =
        "/kaisatsulog — 記録のある駅一覧 / /kaisatsulog <駅名> — 直近の通過ログ"

    override fun getRequiredPermissionLevel() = 2

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val world = MinecraftServer.getServer().entityWorld
        val data = KaisatsuNetworkData.get(world) ?: run {
            sender.addChatMessage(ChatComponentText("§cデータが見つかりません"))
            return
        }

        if (args.isEmpty()) {
            val stations = data.gateLog.keys.sorted()
            if (stations.isEmpty()) {
                sender.addChatMessage(ChatComponentText("§e[改札ログ] 記録がありません"))
                return
            }
            sender.addChatMessage(ChatComponentText("§e[改札ログ] 記録のある駅一覧:"))
            stations.forEach { st ->
                sender.addChatMessage(ChatComponentText("  §f$st §7(${data.gateLog[st]?.size ?: 0}件)"))
            }
            sender.addChatMessage(ChatComponentText("§7/kaisatsulog <駅名> で詳細を表示"))
            return
        }

        val station = args.joinToString(" ")
        val logs = data.gateLog[station]
        if (logs.isNullOrEmpty()) {
            sender.addChatMessage(ChatComponentText("§e${station} の通過記録はありません"))
            return
        }

        val fmt = SimpleDateFormat("MM/dd HH:mm:ss")
        sender.addChatMessage(ChatComponentText("§e[改札ログ] ${station} 直近 ${logs.size} 件:"))
        logs.forEach { entry ->
            val time = fmt.format(Date(entry.timestamp))
            val actionColor = if (entry.action == "入場") "§a" else if (entry.action == "出場") "§c" else "§e"
            sender.addChatMessage(ChatComponentText(
                "§7[$time] §f${entry.playerName} $actionColor${entry.action} §7(${entry.itemType})"))
        }
    }

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<String>): List<*> {
        if (args.size == 1) {
            val world = MinecraftServer.getServer().entityWorld
            val data = KaisatsuNetworkData.get(world) ?: return emptyList<String>()
            return getListOfStringsMatchingLastWord(args, *data.globalStations.keys.toTypedArray())
        }
        return emptyList<String>()
    }
}
