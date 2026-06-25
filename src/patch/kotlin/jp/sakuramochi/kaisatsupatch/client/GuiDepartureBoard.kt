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
    // 路線: index 0 = フィルターなし、1以降 = availableLines[i-1]
    private var selLine    = run {
        val idx = data.availableLines.indexOfFirst { it.first == data.lineID }
        if (idx < 0) 0 else idx + 1
    }
    private var selDia     = data.availableDias.indexOfFirst { it == data.diaName }.coerceAtLeast(0)
    private var selDir     = DIRS.indexOfFirst { it == data.direction }.coerceAtLeast(0)
    private var selRows    = (data.displayRows - 1).coerceIn(0, 7)

    private lateinit var fldTitle:    GuiTextField
    private lateinit var fldPlatform: GuiTextField

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        when (page) {
            0 -> {
                add(GuiButton(99, cx - 30, cy + 90, 60, 18, "閉じる"))
            }
            1 -> {
                fldTitle    = GuiTextField(fontRendererObj, cx - 80, cy - 90, 160, 14)
                fldPlatform = GuiTextField(fontRendererObj, cx - 30, cy + 18, 80,  14)
                fldTitle.text    = data.title
                fldPlatform.text = data.platform
                fldPlatform.setMaxStringLength(8)

                // 駅
                add(GuiButton(10, cx - 80, cy - 54, 18, 14, "<"))
                add(GuiButton(11, cx + 62, cy - 54, 18, 14, ">"))
                // 路線
                add(GuiButton(18, cx - 80, cy - 34, 18, 14, "<"))
                add(GuiButton(19, cx + 62, cy - 34, 18, 14, ">"))
                // ダイヤ
                add(GuiButton(12, cx - 80, cy - 14, 18, 14, "<"))
                add(GuiButton(13, cx + 62, cy - 14, 18, 14, ">"))
                // 方向
                add(GuiButton(14, cx - 80, cy + 6,  18, 14, "<"))
                add(GuiButton(15, cx + 62, cy + 6,  18, 14, ">"))
                // 表示行数
                add(GuiButton(16, cx - 50,  cy + 38, 18, 14, "-"))
                add(GuiButton(17, cx + 32,  cy + 38, 18, 14, "+"))

                add(GuiButton(0,  cx - 35, cy + 58, 70, 18, "保存"))
                add(GuiButton(99, cx - 35, cy + 80, 70, 18, "キャンセル"))
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        val nStations = data.availableStations.size
        val nLines    = data.availableLines.size + 1  // +1 for "なし"
        val nDias     = data.availableDias.size + 1   // +1 for "すべて"

        when (page) {
            0 -> if (button.id == 99) mc.thePlayer.closeScreen()
            1 -> when (button.id) {
                10 -> { selStation = cycle(selStation - 1, nStations); initGui() }
                11 -> { selStation = cycle(selStation + 1, nStations); initGui() }
                18 -> { selLine = cycle(selLine - 1, nLines); initGui() }
                19 -> { selLine = cycle(selLine + 1, nLines); initGui() }
                12 -> { selDia = cycle(selDia - 1, nDias); initGui() }
                13 -> { selDia = cycle(selDia + 1, nDias); initGui() }
                14 -> { selDir = cycle(selDir - 1, DIRS.size); initGui() }
                15 -> { selDir = cycle(selDir + 1, DIRS.size); initGui() }
                16 -> { selRows = (selRows - 1).coerceAtLeast(0); initGui() }
                17 -> { selRows = (selRows + 1).coerceAtMost(7); initGui() }
                0 -> {
                    val station = data.availableStations.getOrElse(selStation) { "" }
                    val lineID  = if (selLine == 0) "" else data.availableLines.getOrElse(selLine - 1) { "" to "" }.first
                    val dia     = if (selDia == 0)  "" else data.availableDias.getOrElse(selDia - 1) { "" }
                    KaizPatchNetwork.CHANNEL.sendToServer(PacketDepartureBoardSave().also { pkt ->
                        pkt.x = data.x; pkt.y = data.y; pkt.z = data.z
                        pkt.stationName  = station
                        pkt.lineID       = lineID
                        pkt.platform     = if (::fldPlatform.isInitialized) fldPlatform.text.trim() else ""
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
        if (page == 1) {
            if (::fldTitle.isInitialized    && fldTitle.textboxKeyTyped(typedChar, keyCode)) return
            if (::fldPlatform.isInitialized && fldPlatform.textboxKeyTyped(typedChar, keyCode)) return
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        if (page == 1) {
            if (::fldTitle.isInitialized)    fldTitle.mouseClicked(mouseX, mouseY, mouseButton)
            if (::fldPlatform.isInitialized) fldPlatform.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        if (page == 1) drawRect(cx - 140, cy - 118, cx + 140, cy + 106, 0xF0202030.toInt())
        super.drawScreen(mouseX, mouseY, partialTicks)
        when (page) {
            0 -> drawDisplayPage(cx, cy)
            1 -> drawConfigPage(cx, cy)
        }
    }

    // ── 表示ページ ───────────────────────────────────────────────────

    private fun drawDisplayPage(cx: Int, cy: Int) {
        val titleBase  = data.title.ifEmpty { data.stationName }
        val platSuffix = if (data.platform.isNotEmpty()) " §7${data.platform}番線" else ""
        drawCenteredString(fontRendererObj, "§e$titleBase$platSuffix  §7${data.currentTime}", cx, cy - 95, 0xFFFFFF)

        val diaLabel = if (data.diaName.isEmpty()) "" else "§7[${data.diaName}]  "
        val dirLabel = if (data.direction == "両方") "" else "§7${data.direction}  "
        val lineLabel = if (data.lineID.isNotEmpty()) "§8[${data.lineID}]" else ""
        drawCenteredString(fontRendererObj, "$diaLabel$dirLabel$lineLabel", cx, cy - 82, 0xAAAAAA)

        val colX0 = cx - 140
        drawString(fontRendererObj, "§7時刻",    colX0,       cy - 66, 0xAAAAAA)
        drawString(fontRendererObj, "§7行き先",  colX0 + 50,  cy - 66, 0xAAAAAA)
        drawString(fontRendererObj, "§7種別",    colX0 + 160, cy - 66, 0xAAAAAA)
        drawString(fontRendererObj, "§7列車番号", colX0 + 220, cy - 66, 0xAAAAAA)

        if (data.departures.isEmpty()) {
            drawCenteredString(fontRendererObj,
                if (data.stationName.isEmpty()) "§7発車標が設定されていません（設定ツールで右クリック）"
                else "§7発車情報がありません", cx, cy, 0xAAAAAA)
        } else {
            data.departures.forEachIndexed { i, row ->
                val y = cy - 50 + i * 16
                drawString(fontRendererObj, "§f${row.time}",        colX0,       y, 0xFFFFFF)
                drawString(fontRendererObj, "§b${row.destination}",  colX0 + 50,  y, 0xFFFFFF)
                drawString(fontRendererObj, "§e${row.typeName}",     colX0 + 160, y, 0xFFFFFF)
                drawString(fontRendererObj, "§7${row.trainNumber}",   colX0 + 220, y, 0xAAAAAA)
            }
        }
    }

    // ── 設定ページ ───────────────────────────────────────────────────

    private fun drawConfigPage(cx: Int, cy: Int) {
        drawCenteredString(fontRendererObj, "§e発車標 — 設定", cx, cy - 108, 0xFFFFFF)
        drawString(fontRendererObj, "タイトル（空欄=駅名）", cx - 60, cy - 102, 0xAAAAAA)
        if (::fldTitle.isInitialized) fldTitle.drawTextBox()

        val stLabel   = data.availableStations.getOrElse(selStation) { "§7（駅なし）" }
        val lineLabel = if (selLine == 0) "§7なし（フィルターなし）"
                        else data.availableLines.getOrElse(selLine - 1) { "" to "" }
                            .let { (id, name) -> "$name §8($id)" }
        val diaLabel  = if (selDia == 0) "§7すべて" else data.availableDias.getOrElse(selDia - 1) { "" }
        val dirLabel  = DIRS[selDir]
        val rowsLabel = "${selRows + 1}行"

        val labelX = cx - 130

        drawString(fontRendererObj, "駅",       labelX,  cy - 66, 0xAAAAAA)
        drawString(fontRendererObj, "路線",     labelX,  cy - 46, 0xAAAAAA)
        drawString(fontRendererObj, "ダイヤ",   labelX,  cy - 26, 0xAAAAAA)
        drawString(fontRendererObj, "方向",     labelX,  cy - 6,  0xAAAAAA)
        drawString(fontRendererObj, "番線",     cx - 60, cy + 6,  0xAAAAAA)
        drawString(fontRendererObj, "表示行数", labelX,  cy + 26, 0xAAAAAA)

        drawCenteredString(fontRendererObj, stLabel,   cx, cy - 58, 0xFFFF55)
        drawCenteredString(fontRendererObj, lineLabel, cx, cy - 38, 0xFFFF55)
        drawCenteredString(fontRendererObj, diaLabel,  cx, cy - 18, 0xFFFF55)
        drawCenteredString(fontRendererObj, dirLabel,  cx, cy + 2,  0xFFFF55)
        drawCenteredString(fontRendererObj, rowsLabel, cx, cy + 34, 0xFFFF55)

        if (::fldPlatform.isInitialized) fldPlatform.drawTextBox()
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)

    private fun cycle(v: Int, size: Int) = if (size == 0) 0 else ((v % size) + size) % size

    companion object {
        val DIRS = listOf("両方", "下り", "上り")
    }
}
