package jp.sakuramochi.kaisatsupatch.command

import jp.sakuramochi.kaisatsupatch.web.KaisatsuWebServer
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import java.net.InetAddress

class CommandKaizWeb : CommandBase() {

    override fun getCommandName() = "kaizweb"

    override fun getCommandUsage(sender: ICommandSender) = "/kaizweb — 空席情報ページのURLを表示"

    override fun getRequiredPermissionLevel() = 0   // 全プレイヤーが使える

    private fun clickableLink(label: String, url: String): ChatComponentText {
        val link = ChatComponentText("§b§n[$label]")
        link.chatStyle = ChatStyle()
            .setChatClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .setChatHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§7$url")))
        return link
    }

    override fun processCommand(sender: ICommandSender, args: Array<String>) {
        val port = KaisatsuWebServer.PORT

        val localUrl    = "http://localhost:$port/"
        val externalUrl = try {
            "http://${InetAddress.getLocalHost().hostAddress}:$port/"
        } catch (e: Exception) {
            null
        }

        sender.addChatMessage(ChatComponentText("§e[KaizPatch] 指定席空席情報ページ"))

        // ローカル URL（クリックで開く・ホバーでURL表示）
        val localLink = clickableLink("ローカルで開く", localUrl)
        sender.addChatMessage(localLink)

        // 外部 IP URL
        if (externalUrl != null && externalUrl != localUrl) {
            val extLink = clickableLink("外部IPで開く", externalUrl)
            sender.addChatMessage(extLink)
        }
    }
}
