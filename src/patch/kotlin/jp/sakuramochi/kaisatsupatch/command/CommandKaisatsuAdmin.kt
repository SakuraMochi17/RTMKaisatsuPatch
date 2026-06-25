package jp.sakuramochi.kaisatsupatch.command

import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText

/**
 * /kaisatsu company create <id> <表示名> <色16進> <ICカード名>
 * /kaisatsu company list
 * /kaisatsu company delete <id>
 * /kaisatsu company info <id>
 */
class CommandKaisatsuAdmin : CommandBase() {

    override fun getCommandName() = "kaisatsu"
    override fun getCommandUsage(sender: ICommandSender) =
        "/kaisatsu company <create|list|delete|info> ..."
    override fun getRequiredPermissionLevel() = 2

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val world = MinecraftServer.getServer().entityWorld
        val data = KaisatsuNetworkData.get(world) ?: run {
            sender.addChatMessage(ChatComponentText("§cデータが見つかりません"))
            return
        }

        if (args.isEmpty() || args[0] != "company") {
            sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company <create|list|delete|info>"))
            return
        }

        val sub = args.getOrNull(1) ?: run {
            sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company <create|list|delete|info>"))
            return
        }

        when (sub) {
            "create" -> {
                // /kaisatsu company create <id> <表示名> <色16進> <ICカード名>
                if (args.size < 6) {
                    sender.addChatMessage(ChatComponentText(
                        "§e使い方: /kaisatsu company create <ID> <表示名> <色16進 例:FF0000> <ICカード名>"))
                    return
                }
                val id = args[2]
                val name = args[3]
                val colorStr = args[4].trimStart('#')
                val icName = args[5]

                val color = colorStr.toIntOrNull(16) ?: run {
                    sender.addChatMessage(ChatComponentText("§c色の指定が無効です（例: FF0000）"))
                    return
                }
                if (data.companies.containsKey(id)) {
                    sender.addChatMessage(ChatComponentText("§c会社ID '$id' はすでに存在します。変更は /kaisatsu company delete して再作成してください"))
                    return
                }
                data.companies[id] = KaisatsuNetworkData.CompanyData(id, name, color, icName)
                data.markDirty()
                sender.addChatMessage(ChatComponentText(
                    "§a会社を登録しました: §f[$id] $name §7(${colorStr.uppercase()} / ${icName})"))
            }

            "list" -> {
                if (data.companies.isEmpty()) {
                    sender.addChatMessage(ChatComponentText("§e登録済みの会社はありません"))
                    return
                }
                sender.addChatMessage(ChatComponentText("§e登録済み会社一覧:"))
                data.companies.values.sortedBy { it.companyID }.forEach { c ->
                    val hexColor = "%06X".format(c.color)
                    sender.addChatMessage(ChatComponentText(
                        "  §f[${c.companyID}] §r${c.companyName} §7(#$hexColor / ${c.icCardName})"))
                }
            }

            "delete" -> {
                val id = args.getOrNull(2) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company delete <ID>"))
                    return
                }
                if (data.companies.remove(id) != null) {
                    data.markDirty()
                    sender.addChatMessage(ChatComponentText("§a会社 '$id' を削除しました"))
                } else {
                    sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません"))
                }
            }

            "info" -> {
                val id = args.getOrNull(2) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company info <ID>"))
                    return
                }
                val c = data.companies[id] ?: run {
                    sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません"))
                    return
                }
                sender.addChatMessage(ChatComponentText("§e会社情報: ${c.companyID}"))
                sender.addChatMessage(ChatComponentText("  表示名: §f${c.companyName}"))
                sender.addChatMessage(ChatComponentText("  カラー: §f#${"${c.color}".let { "%06X".format(c.color) }}"))
                sender.addChatMessage(ChatComponentText("  ICカード名: §f${c.icCardName}"))
            }

            else -> sender.addChatMessage(ChatComponentText("§eサブコマンド: create / list / delete / info"))
        }
    }

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<String>): List<*> {
        return when {
            args.size == 1 -> getListOfStringsMatchingLastWord(args, "company")
            args.size == 2 && args[0] == "company" ->
                getListOfStringsMatchingLastWord(args, "create", "list", "delete", "info")
            args.size == 3 && args[0] == "company" && args[1] in listOf("delete", "info") -> {
                val world = MinecraftServer.getServer().entityWorld
                val data = KaisatsuNetworkData.get(world) ?: return emptyList<String>()
                getListOfStringsMatchingLastWord(args, *data.companies.keys.toTypedArray())
            }
            else -> emptyList<String>()
        }
    }
}
