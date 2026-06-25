package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketDepartureBoardSave
import jp.sakuramochi.kaisatsupatch.network.PacketOpenDepartureBoard
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiDepartureBoard(private val data: PacketOpenDepartureBoard) : GuiScreen() {

    private var page = if (data.isConfigMode) 1 else 0

    // 設定ページの状態
    private var selStation = data.availableStations.indexOfFirst { it == data.stationName }.coerceAtLeast(0)
    private var selDia     = data.availableDias.indexOfFirst    { it == data.diaName     }.coerceAtLeast(0)
    private var selDir     = DIRS.indexOfFirst { it == data.direction }.coerceAtLeast(0)
    private var selRows    = (data.displayRows - 1).coerceIn(0, 7)

    private lateinit var fldTitle: GuiTextField

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        when (page) {
            0 -> {
                // 発車表示ページ
                add(GuiButton(99, cx - 30, cy + 80, 60, 18, "閉じる"))
            }
            1 -> {
                // 設定ページ
                fldTitle = GuiTextField(fontRendererObj, cx - 60, cy - 75, 120, 15)
                fldTitle.text = data.title

                add(GuiButton(10, cx - 110, cy - 42, 20, 14, "<"))
                add(GuiButton(11, cx + 90,  cy - 42, 20, 14, ">"))
                add(GuiButton(12, cx - 110, cy - 22, 20, 14, "<"))
                add(GuiButton(13, cx + 90,  cy - 22, 20, 14, ">"))
                add(GuiButton(14, cx - 110, cy - 2,  20, 14, "<"))
                add(GuiButton(15, cx + 90,  cy - 2,  20, 14, ">"))
                add(GuiButton(16, cx - 50,  cy + 18, 20, 14, "-"))
                add(GuiButton(17, cx + 30,  cy + 18, 20, 14, "+"))
                add(GuiButton(0,  cx - 35,  cy + 45, 70, 18, "保存"))
                add(GuiButton(99, cx - 35,  cy + 68, 70, 18, "キャンセル"))
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (page) {
            0 -> if (button.id == 99) mc.thePlayer.closeScreen()
            1 -> when (button.id) {
                10 -> { selStation = cycle(selStation - 1, data.availableStations.size); initGui() }
                11 -> { selStation = cycle(selStation + 1, data.availableStations.size); initGui() }
                12 -> { selDia = cycle(selDia - 1, data.availableDias.size + 1); initGui() }
                13 -> { selDia = cycle(selDia + 1, data.availableDias.size + 1); initGui() }
                14 -> { selDir = cycle(selDir - 1, DIRS.size); initGui() }
                15 -> { selDir = cycle(selDir + 1, DIRS.size); initGui() }
                16 -> { selRows = (selRows - 1).coerceAtLeast(0); initGui() }
                17 -> { selRows = (selRows + 1).coerceAtMost(7); initGui() }
                0 -> {
                    val station = data.availableStations.getOrElse(selStation) { "" }
                    val dia     = data.availableDias.getOrElse(selDia - 1) { "" }  // index 0 = "すべて" → ""
                    KaizPatchNetwork.CHANNEL.sendToServer(PacketDepartureBoardSave().also { pkt ->
                        pkt.x = data.x; pkt.y = data.y; pkt.z = data.z
                        pkt.stationName  = station
                        pkt.diaName      = dia
                        pkt.direction    = DIRS[selDir]
                        pkt.displayRows  = selRows + 1
                        pkt.title        = if (::fldTitle.isInitialized) fldTitle.text.trim() else ""
                    })
                    mc.thePlayer.closeScreen()
                }
                99 -> mc.thePlayer.closeScreen()
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (page == 1 && ::fldTitle.isInitialized && fldTitle.textboxKeyTyped(typedChar, keyCode)) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        if (page == 1 && ::fldTitle.isInitialized) fldTitle.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2

        when (page) {
            0 -> drawDisplayPage(cx, cy)
            1 -> drawConfigPage(cx, cy)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawDisplayPage(cx: Int, cy: Int) {
        val title  = data.title.ifEmpty { data.stationName }
        val header = "§e$title  §7${data.currentTime}"
        drawCenteredString(fontRendererObj, header, cx, cy - 90, 0xFFFFFF)

        val diaLabel = if (data.diaName.isEmpty()) "" else "  §7[${data.diaName}]"
        val dirLabel = if (data.direction == "両方") "" else "  §7${data.direction}"
        drawCenteredString(fontRendererObj, "$diaLabel$dirLabel", cx, cy - 76, 0xAAAAAA)

        // 列ヘッダー
        val colX0 = cx - 140
        drawString(fontRendererObj, "§7時刻",   colX0,      cy - 60, 0xAAAAAA)
        drawString(fontRendererObj, "§7行き先", colX0 + 50, cy - 60, 0xAAAAAA)
        drawString(fontRendererObj, "§7種別",   colX0 + 160, cy - 60, 0xAAAAAA)
        drawString(fontRendererObj, "§7列車番号", colX0 + 220, cy - 60, 0xAAAAAA)

        if (data.departures.isEmpty()) {
            drawCenteredString(fontRendererObj,
                if (data.stationName.isEmpty()) "§7発車標が設定されていません（設定ツールで右クリック）"
                else "§7発車情報がありません", cx, cy, 0xAAAAAA)
        } else {
            data.departures.forEachIndexed { i, row ->
                val y = cy - 46 + i * 16
                drawString(fontRendererObj, "§f${row.time}",       colX0,       y, 0xFFFFFF)
                drawString(fontRendererObj, "§b${row.destination}", colX0 + 50,  y, 0xFFFFFF)
                drawString(fontRendererObj, "§e${row.typeName}",    colX0 + 160, y, 0xFFFFFF)
                drawString(fontRendererObj, "§7${row.trainNumber}",  colX0 + 220, y, 0xAAAAAA)
            }
        }
    }

    private fun drawConfigPage(cx: Int, cy: Int) {
        drawCenteredString(fontRendererObj, "§e発車標 — 設定", cx, cy - 100, 0xFFFFFF)
        drawString(fontRendererObj, "タイトル（空欄=駅名）", cx - 60, cy - 90, 0xAAAAAA)
        if (::fldTitle.isInitialized) fldTitle.drawTextBox()

        val stLabel = data.availableStations.getOrElse(selStation) { "§7（駅なし）" }
        val diaLabel = if (selDia == 0) "§7すべて" else data.availableDias.getOrElse(selDia - 1) { "" }
        val dirLabel = DIRS[selDir]
        val rowsLabel = "${selRows + 1}行"

        drawString(fontRendererObj, "駅",   cx - 110, cy - 55, 0xAAAAAA)
        drawString(fontRendererObj, "ダイヤ", cx - 110, cy - 35, 0xAAAAAA)
        drawString(fontRendererObj, "方向", cx - 110, cy - 15, 0xAAAAAA)
        drawString(fontRendererObj, "表示行数", cx - 110, cy + 5, 0xAAAAAA)

        drawCenteredString(fontRendererObj, stLabel,   cx, cy - 47, 0xFFFF55)
        drawCenteredString(fontRendererObj, diaLabel,  cx, cy - 27, 0xFFFF55)
        drawCenteredString(fontRendererObj, dirLabel,  cx, cy - 7,  0xFFFF55)
        drawCenteredString(fontRendererObj, rowsLabel, cx, cy + 13, 0xFFFF55)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)

    private fun cycle(v: Int, size: Int) = if (size == 0) 0 else ((v % size) + size) % size

    companion object {
        val DIRS = listOf("両方", "下り", "上り")
    }
}
