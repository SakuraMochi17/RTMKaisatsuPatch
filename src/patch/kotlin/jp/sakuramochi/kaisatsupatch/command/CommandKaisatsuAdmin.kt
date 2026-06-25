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
 * /kaisatsu company member add|remove|list <id> [player]
 * /kaisatsu company mutual add|remove|list <idA> [idB]
 * /kaisatsu company fare <id> <初乗り運賃> <加算レート>
 * /kaisatsu line assign <lineID> <companyID>
 */
class CommandKaisatsuAdmin : CommandBase() {

    override fun getCommandName() = "kaisatsu"
    override fun getCommandUsage(sender: ICommandSender) =
        "/kaisatsu company <create|list|delete|info|member|mutual|fare> ..."
    override fun getRequiredPermissionLevel() = 2

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val world = MinecraftServer.getServer().entityWorld
        val data = KaisatsuNetworkData.get(world) ?: run {
            sender.addChatMessage(ChatComponentText("§cデータが見つかりません")); return
        }

        val sub0 = args.getOrNull(0) ?: run { printHelp(sender); return }

        when (sub0) {
            "company" -> handleCompany(sender, args.drop(1).toTypedArray(), data)
            "line"    -> handleLine(sender, args.drop(1).toTypedArray(), data)
            else      -> printHelp(sender)
        }
    }

    // ── /kaisatsu company ... ─────────────────────────────────────────

    private fun handleCompany(sender: ICommandSender, args: Array<String>, data: KaisatsuNetworkData) {
        when (val sub = args.getOrNull(0)) {
            "create" -> {
                if (args.size < 5) { sender.addChatMessage(ChatComponentText(
                    "§e使い方: /kaisatsu company create <ID> <表示名> <色16進> <ICカード名>")); return }
                val id = args[1]; val name = args[2]
                val color = args[3].trimStart('#').toIntOrNull(16) ?: run {
                    sender.addChatMessage(ChatComponentText("§c色の指定が無効です（例: FF0000）")); return }
                val icName = args[4]
                if (data.companies.containsKey(id)) {
                    sender.addChatMessage(ChatComponentText("§c会社ID '$id' はすでに存在します")); return }
                data.companies[id] = KaisatsuNetworkData.CompanyData(id, name, color, icName)
                data.markDirty()
                sender.addChatMessage(ChatComponentText(
                    "§a会社を登録しました: §f[$id] $name §7(#${"${args[3]}".trimStart('#').uppercase()} / $icName)"))
            }

            "list" -> {
                if (data.companies.isEmpty()) { sender.addChatMessage(ChatComponentText("§e登録済みの会社はありません")); return }
                sender.addChatMessage(ChatComponentText("§e登録済み会社一覧:"))
                data.companies.values.sortedBy { it.companyID }.forEach { c ->
                    sender.addChatMessage(ChatComponentText(
                        "  §f[${c.companyID}] §r${c.companyName} §7(#${"${c.color}".let { "%06X".format(c.color) }} / ${c.icCardName} / メンバー${c.members.size}名)"))
                }
            }

            "delete" -> {
                val id = args.getOrNull(1) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company delete <ID>")); return }
                if (data.companies.remove(id) != null) {
                    data.markDirty()
                    sender.addChatMessage(ChatComponentText("§a会社 '$id' を削除しました"))
                } else sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません"))
            }

            "info" -> {
                val id = args.getOrNull(1) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company info <ID>")); return }
                val c = data.companies[id] ?: run {
                    sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません")); return }
                val lines = data.companyLines.values.filter {
                    it.companyID == id || (it.companyID.isEmpty() && it.companyName == c.companyName) }
                sender.addChatMessage(ChatComponentText("§e[${c.companyID}] ${c.companyName}"))
                sender.addChatMessage(ChatComponentText("  カラー: §f#${"${c.color}".let { "%06X".format(c.color) }}  ICカード名: §f${c.icCardName}"))
                sender.addChatMessage(ChatComponentText("  デフォルト運賃: §f初乗り${c.defaultBaseFare}円 / ${c.defaultCostPerBlock}円/m"))
                sender.addChatMessage(ChatComponentText("  所属路線: §f${if (lines.isEmpty()) "なし" else lines.joinToString { it.lineName }}"))
                sender.addChatMessage(ChatComponentText("  メンバー: §f${if (c.members.isEmpty()) "なし" else c.members.joinToString()}"))
                sender.addChatMessage(ChatComponentText("  相互利用: §f${if (c.allowedCompanies.isEmpty()) "なし" else c.allowedCompanies.joinToString()}"))
            }

            "member" -> {
                val msub = args.getOrNull(1)
                val id   = args.getOrNull(2)
                val co   = id?.let { data.companies[it] }
                when (msub) {
                    "add" -> {
                        val player = args.getOrNull(3) ?: run {
                            sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company member add <ID> <プレイヤー名>")); return }
                        co ?: run { sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません")); return }
                        co.members.add(player); data.markDirty()
                        sender.addChatMessage(ChatComponentText("§a${player} を ${co.companyName} のメンバーに追加しました"))
                    }
                    "remove" -> {
                        val player = args.getOrNull(3) ?: run {
                            sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company member remove <ID> <プレイヤー名>")); return }
                        co ?: run { sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません")); return }
                        if (co.members.remove(player)) { data.markDirty()
                            sender.addChatMessage(ChatComponentText("§a${player} を ${co.companyName} から除名しました"))
                        } else sender.addChatMessage(ChatComponentText("§c${player} はメンバーではありません"))
                    }
                    "list" -> {
                        co ?: run { sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません")); return }
                        sender.addChatMessage(ChatComponentText(
                            "§e${co.companyName} のメンバー: §f${if (co.members.isEmpty()) "なし" else co.members.joinToString()}"))
                    }
                    else -> sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company member <add|remove|list> <ID> [プレイヤー名]"))
                }
            }

            "mutual" -> {
                val msub = args.getOrNull(1)
                val idA  = args.getOrNull(2)
                val coA  = idA?.let { data.companies[it] }
                when (msub) {
                    "add" -> {
                        val idB = args.getOrNull(3) ?: run {
                            sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company mutual add <IDa> <IDb>")); return }
                        coA ?: run { sender.addChatMessage(ChatComponentText("§c会社 '$idA' は存在しません")); return }
                        if (!data.companies.containsKey(idB)) {
                            sender.addChatMessage(ChatComponentText("§c会社 '$idB' は存在しません")); return }
                        coA.allowedCompanies.add(idB); data.markDirty()
                        sender.addChatMessage(ChatComponentText(
                            "§a${coA.companyName} → $idB の相互利用を許可しました（双方向にするには逆も設定してください）"))
                    }
                    "remove" -> {
                        val idB = args.getOrNull(3) ?: run {
                            sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company mutual remove <IDa> <IDb>")); return }
                        coA ?: run { sender.addChatMessage(ChatComponentText("§c会社 '$idA' は存在しません")); return }
                        if (coA.allowedCompanies.remove(idB)) { data.markDirty()
                            sender.addChatMessage(ChatComponentText("§a${coA.companyName} → $idB の相互利用を解除しました"))
                        } else sender.addChatMessage(ChatComponentText("§c設定されていません"))
                    }
                    "list" -> {
                        coA ?: run { sender.addChatMessage(ChatComponentText("§c会社 '$idA' は存在しません")); return }
                        sender.addChatMessage(ChatComponentText(
                            "§e${coA.companyName} の相互利用許可: §f${if (coA.allowedCompanies.isEmpty()) "なし" else coA.allowedCompanies.joinToString()}"))
                    }
                    else -> sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company mutual <add|remove|list> <IDa> [IDb]"))
                }
            }

            "fare" -> {
                if (args.size < 4) { sender.addChatMessage(ChatComponentText(
                    "§e使い方: /kaisatsu company fare <ID> <初乗り運賃> <加算レート(円/ブロック)>")); return }
                val id = args[1]
                val co = data.companies[id] ?: run {
                    sender.addChatMessage(ChatComponentText("§c会社 '$id' は存在しません")); return }
                val base = args[2].toIntOrNull() ?: run {
                    sender.addChatMessage(ChatComponentText("§c初乗り運賃は整数で入力してください")); return }
                val rate = args[3].toDoubleOrNull() ?: run {
                    sender.addChatMessage(ChatComponentText("§c加算レートは小数で入力してください（例: 0.05）")); return }
                co.defaultBaseFare = base; co.defaultCostPerBlock = rate; data.markDirty()
                sender.addChatMessage(ChatComponentText(
                    "§a${co.companyName} のデフォルト運賃を設定しました: 初乗り${base}円 / ${rate}円/ブロック"))
            }

            else -> sender.addChatMessage(ChatComponentText(
                "§e使い方: /kaisatsu company <create|list|delete|info|member|mutual|fare>"))
        }
    }

    // ── /kaisatsu line ... ────────────────────────────────────────────

    private fun handleLine(sender: ICommandSender, args: Array<String>, data: KaisatsuNetworkData) {
        when (args.getOrNull(0)) {
            "assign" -> {
                val lineID = args.getOrNull(1) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu line assign <路線ID> <会社ID>")); return }
                val compID = args.getOrNull(2) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu line assign <路線ID> <会社ID>")); return }
                val line = data.companyLines[lineID] ?: run {
                    sender.addChatMessage(ChatComponentText("§c路線 '$lineID' は存在しません")); return }
                if (!data.companies.containsKey(compID)) {
                    sender.addChatMessage(ChatComponentText("§c会社 '$compID' は存在しません")); return }
                line.companyID = compID; data.markDirty()
                sender.addChatMessage(ChatComponentText(
                    "§a路線 '${line.lineName}' を会社 '$compID' に紐付けました"))
            }
            "list" -> {
                sender.addChatMessage(ChatComponentText("§e路線一覧:"))
                data.companyLines.values.sortedBy { it.lineID }.forEach { l ->
                    val comp = if (l.companyID.isNotEmpty()) "[${l.companyID}]" else "(会社未設定)"
                    sender.addChatMessage(ChatComponentText("  §f${l.lineID} §7${l.lineName} $comp"))
                }
            }
            else -> sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu line <assign|list>"))
        }
    }

    private fun printHelp(sender: ICommandSender) {
        sender.addChatMessage(ChatComponentText("§e/kaisatsu company <create|list|delete|info|member|mutual|fare>"))
        sender.addChatMessage(ChatComponentText("§e/kaisatsu line <assign|list>"))
    }

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<String>): List<*> {
        val world = MinecraftServer.getServer().entityWorld
        val data = KaisatsuNetworkData.get(world)
        val companyIDs = data?.companies?.keys?.toTypedArray() ?: emptyArray()
        val lineIDs    = data?.companyLines?.keys?.toTypedArray() ?: emptyArray()
        return when {
            args.size == 1 -> getListOfStringsMatchingLastWord(args, "company", "line")
            args.size == 2 && args[0] == "company" ->
                getListOfStringsMatchingLastWord(args, "create", "list", "delete", "info", "member", "mutual", "fare")
            args.size == 2 && args[0] == "line" ->
                getListOfStringsMatchingLastWord(args, "assign", "list")
            args.size == 3 && args[0] == "company" && args[1] in listOf("delete","info","member","mutual","fare") ->
                getListOfStringsMatchingLastWord(args, *companyIDs)
            args.size == 3 && args[0] == "line" && args[1] == "assign" ->
                getListOfStringsMatchingLastWord(args, *lineIDs)
            args.size == 4 && args[0] == "company" && args[1] == "mutual" && args[2] in listOf("add","remove") ->
                getListOfStringsMatchingLastWord(args, *companyIDs)
            args.size == 4 && args[0] == "line" && args[1] == "assign" ->
                getListOfStringsMatchingLastWord(args, *companyIDs)
            args.size == 3 && args[0] == "company" && args[1] == "member" ->
                getListOfStringsMatchingLastWord(args, "add", "remove", "list")
            args.size == 3 && args[0] == "company" && args[1] == "mutual" ->
                getListOfStringsMatchingLastWord(args, "add", "remove", "list")
            else -> emptyList<String>()
        }
    }
}
