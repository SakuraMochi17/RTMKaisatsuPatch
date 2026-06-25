package jp.sakuramochi.kaisatsupatch.command

import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.web.KaisatsuWebServer
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date

/**
 * /kaisatsu <サブコマンド> ...
 *
 * 全プレイヤー:
 *   /kaisatsu web
 *
 * OP専用 (lv.2):
 *   /kaisatsu log [駅名]
 *   /kaisatsu company create|list|delete|info|member|mutual|fare ...
 *   /kaisatsu line assign|list ...
 */
class CommandKaisatsuAdmin : CommandBase() {

    override fun getCommandName() = "kaisatsu"
    override fun getCommandUsage(sender: ICommandSender) =
        "/kaisatsu <web|log|company|line> ..."

    // web は lv.0、それ以外は lv.2 — onCommand 内で個別チェック
    override fun getRequiredPermissionLevel() = 0

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val world = MinecraftServer.getServer().entityWorld

        when (val sub = args.getOrNull(0)) {
            "web"     -> handleWeb(sender)
            "log"     -> {
                checkOp(sender) ?: return
                val data = data(sender, world) ?: return
                handleLog(sender, args.drop(1).toTypedArray(), data)
            }
            "company" -> {
                checkOp(sender) ?: return
                val data = data(sender, world) ?: return
                handleCompany(sender, args.drop(1).toTypedArray(), data)
            }
            "line"    -> {
                checkOp(sender) ?: return
                val data = data(sender, world) ?: return
                handleLine(sender, args.drop(1).toTypedArray(), data)
            }
            else      -> printHelp(sender)
        }
    }

    // ── 権限・データヘルパー ──────────────────────────────────────────

    private fun checkOp(sender: ICommandSender): Unit? {
        if (!sender.canCommandSenderUseCommand(2, commandName)) {
            sender.addChatMessage(ChatComponentText("§cこのコマンドはOP専用です")); return null
        }
        return Unit
    }

    private fun data(sender: ICommandSender, world: net.minecraft.world.World): KaisatsuNetworkData? {
        val d = KaisatsuNetworkData.get(world)
        if (d == null) sender.addChatMessage(ChatComponentText("§cデータが見つかりません"))
        return d
    }

    // ── /kaisatsu web ─────────────────────────────────────────────────

    private fun handleWeb(sender: ICommandSender) {
        val port = KaisatsuWebServer.PORT
        val localUrl = "http://localhost:$port/"
        val extUrl   = try { "http://${InetAddress.getLocalHost().hostAddress}:$port/" } catch (_: Exception) { null }

        sender.addChatMessage(ChatComponentText("§e[KaizPatch] 指定席空席情報ページ"))
        sender.addChatMessage(link("ローカルで開く", localUrl))
        if (extUrl != null && extUrl != localUrl)
            sender.addChatMessage(link("外部IPで開く", extUrl))
    }

    private fun link(label: String, url: String): ChatComponentText {
        val c = ChatComponentText("§b§n[$label]")
        c.chatStyle = ChatStyle()
            .setChatClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§7$url")))
        return c
    }

    // ── /kaisatsu log [駅名] ──────────────────────────────────────────

    private fun handleLog(sender: ICommandSender, args: Array<String>, data: KaisatsuNetworkData) {
        if (args.isEmpty()) {
            val stations = data.gateLog.keys.sorted()
            if (stations.isEmpty()) { sender.addChatMessage(ChatComponentText("§e[改札ログ] 記録がありません")); return }
            sender.addChatMessage(ChatComponentText("§e[改札ログ] 記録のある駅一覧:"))
            stations.forEach { sender.addChatMessage(ChatComponentText("  §f$it §7(${data.gateLog[it]?.size ?: 0}件)")) }
            sender.addChatMessage(ChatComponentText("§7/kaisatsu log <駅名> で詳細を表示"))
            return
        }
        val station = args.joinToString(" ")
        val logs = data.gateLog[station]
        if (logs.isNullOrEmpty()) { sender.addChatMessage(ChatComponentText("§e${station} の通過記録はありません")); return }
        val fmt = SimpleDateFormat("MM/dd HH:mm:ss")
        sender.addChatMessage(ChatComponentText("§e[改札ログ] ${station} 直近 ${logs.size} 件:"))
        logs.forEach { entry ->
            val time = fmt.format(Date(entry.timestamp))
            val col  = if (entry.action == "入場") "§a" else if (entry.action == "出場") "§c" else "§e"
            sender.addChatMessage(ChatComponentText("§7[$time] §f${entry.playerName} $col${entry.action} §7(${entry.itemType})"))
        }
    }

    // ── /kaisatsu company ... ─────────────────────────────────────────

    private fun handleCompany(sender: ICommandSender, args: Array<String>, data: KaisatsuNetworkData) {
        when (args.getOrNull(0)) {
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
                    "§a会社を登録しました: §f[$id] $name §7(#${args[3].trimStart('#').uppercase()} / $icName)"))
            }
            "list" -> {
                if (data.companies.isEmpty()) { sender.addChatMessage(ChatComponentText("§e登録済みの会社はありません")); return }
                sender.addChatMessage(ChatComponentText("§e登録済み会社一覧:"))
                data.companies.values.sortedBy { it.companyID }.forEach { c ->
                    sender.addChatMessage(ChatComponentText(
                        "  §f[${c.companyID}] §r${c.companyName} §7(#${"%06X".format(c.color)} / ${c.icCardName} / メンバー${c.members.size}名)"))
                }
            }
            "delete" -> {
                val id = args.getOrNull(1) ?: run {
                    sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu company delete <ID>")); return }
                if (data.companies.remove(id) != null) { data.markDirty()
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
                sender.addChatMessage(ChatComponentText("  カラー: §f#${"%06X".format(c.color)}  ICカード名: §f${c.icCardName}"))
                sender.addChatMessage(ChatComponentText("  デフォルト運賃: §f初乗り${c.defaultBaseFare}円 / ${c.defaultCostPerBlock}円/m"))
                sender.addChatMessage(ChatComponentText("  所属路線: §f${if (lines.isEmpty()) "なし" else lines.joinToString { it.lineName }}"))
                sender.addChatMessage(ChatComponentText("  メンバー: §f${if (c.members.isEmpty()) "なし" else c.members.joinToString()}"))
                sender.addChatMessage(ChatComponentText("  相互利用: §f${if (c.allowedCompanies.isEmpty()) "なし" else c.allowedCompanies.joinToString()}"))
            }
            "member" -> {
                val msub = args.getOrNull(1); val id = args.getOrNull(2)
                val co = id?.let { data.companies[it] }
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
                val msub = args.getOrNull(1); val idA = args.getOrNull(2)
                val coA = idA?.let { data.companies[it] }
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
                    "§a${co.companyName} のデフォルト運賃: 初乗り${base}円 / ${rate}円/ブロック"))
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
                sender.addChatMessage(ChatComponentText("§a路線 '${line.lineName}' を会社 '$compID' に紐付けました"))
            }
            "list" -> {
                sender.addChatMessage(ChatComponentText("§e路線一覧:"))
                data.companyLines.values.sortedBy { it.lineID }.forEach { l ->
                    val comp = if (l.companyID.isNotEmpty()) "[${l.companyID}]" else "§7(会社未設定)"
                    sender.addChatMessage(ChatComponentText("  §f${l.lineID} §7${l.lineName} $comp"))
                }
            }
            else -> sender.addChatMessage(ChatComponentText("§e使い方: /kaisatsu line <assign|list>"))
        }
    }

    // ── ヘルプ ────────────────────────────────────────────────────────

    private fun printHelp(sender: ICommandSender) {
        sender.addChatMessage(ChatComponentText("§e[KaizPatch] コマンド一覧"))
        sender.addChatMessage(ChatComponentText("  §f/kaisatsu web §7— 空席情報ページURLを表示"))
        sender.addChatMessage(ChatComponentText("  §f/kaisatsu log §7[駅名] — 改札通過ログ表示 §c(OP)"))
        sender.addChatMessage(ChatComponentText("  §f/kaisatsu company §7<create|list|delete|info|member|mutual|fare> §c(OP)"))
        sender.addChatMessage(ChatComponentText("  §f/kaisatsu line §7<assign|list> §c(OP)"))
    }

    // ── タブ補完 ──────────────────────────────────────────────────────

    override fun addTabCompletionOptions(sender: ICommandSender, args: Array<String>): List<*> {
        val world = MinecraftServer.getServer().entityWorld
        val data = KaisatsuNetworkData.get(world)
        val companyIDs = data?.companies?.keys?.toTypedArray() ?: emptyArray()
        val lineIDs    = data?.companyLines?.keys?.toTypedArray() ?: emptyArray()
        val stationIDs = data?.globalStations?.keys?.toTypedArray() ?: emptyArray()

        return when {
            args.size == 1 ->
                getListOfStringsMatchingLastWord(args, "web", "log", "company", "line")
            args.size == 2 && args[0] == "log" ->
                getListOfStringsMatchingLastWord(args, *stationIDs)
            args.size == 2 && args[0] == "company" ->
                getListOfStringsMatchingLastWord(args, "create", "list", "delete", "info", "member", "mutual", "fare")
            args.size == 2 && args[0] == "line" ->
                getListOfStringsMatchingLastWord(args, "assign", "list")
            args.size == 3 && args[0] == "company" && args[1] in listOf("delete","info","member","mutual","fare") ->
                getListOfStringsMatchingLastWord(args, *companyIDs)
            args.size == 3 && args[0] == "line" && args[1] == "assign" ->
                getListOfStringsMatchingLastWord(args, *lineIDs)
            args.size == 3 && args[0] == "company" && args[1] == "member" ->
                getListOfStringsMatchingLastWord(args, "add", "remove", "list")
            args.size == 3 && args[0] == "company" && args[1] == "mutual" ->
                getListOfStringsMatchingLastWord(args, "add", "remove", "list")
            args.size == 4 && args[0] == "company" && args[1] == "mutual" && args[2] in listOf("add","remove") ->
                getListOfStringsMatchingLastWord(args, *companyIDs)
            args.size == 4 && args[0] == "line" && args[1] == "assign" ->
                getListOfStringsMatchingLastWord(args, *companyIDs)
            else -> emptyList<String>()
        }
    }
}
