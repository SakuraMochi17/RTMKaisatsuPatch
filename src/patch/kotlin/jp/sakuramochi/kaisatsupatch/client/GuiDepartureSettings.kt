package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketDepartureSettingsSave
import jp.sakuramochi.kaisatsupatch.network.PacketOpenDepartureSettings
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard

/**
 * 発車標「設定ブロック」の設定 GUI。データ源（駅・路線フィルタ・ダイヤ・方向・
 * 行数・時刻モード）を編集する。路線名/番線/路線カラー等の表示情報は発車標
 * ブロック側の GUI で設定する。
 */
@SideOnly(Side.CLIENT)
class GuiDepartureSettings(private val data: PacketOpenDepartureSettings) : GuiScreen() {

    private var selStation  = data.availableStations.indexOfFirst { it == data.stationName }.coerceAtLeast(0)
    private var selTimeMode = if (data.timeMode == "game") 1 else 0
    private var selLine     = run {
        val idx = data.availableLines.indexOfFirst { it.first == data.lineID }
        if (idx < 0) 0 else idx + 1
    }
    private var selDia = data.availableDias.indexOfFirst { it == data.diaName }.coerceAtLeast(0)
    private var selDir = DIRS.indexOfFirst { it == data.direction }.coerceAtLeast(0)
    private var selRows = (data.displayRows - 1).coerceIn(0, 7)

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        add(GuiButton(10, cx - 80, cy - 56, 18, 14, "<"))
        add(GuiButton(11, cx + 62, cy - 56, 18, 14, ">"))
        add(GuiButton(18, cx - 80, cy - 36, 18, 14, "<"))
        add(GuiButton(19, cx + 62, cy - 36, 18, 14, ">"))
        add(GuiButton(12, cx - 80, cy - 16, 18, 14, "<"))
        add(GuiButton(13, cx + 62, cy - 16, 18, 14, ">"))
        add(GuiButton(14, cx - 80, cy + 4,  18, 14, "<"))
        add(GuiButton(15, cx + 62, cy + 4,  18, 14, ">"))
        add(GuiButton(16, cx - 50, cy + 22, 18, 14, "-"))
        add(GuiButton(17, cx + 32, cy + 22, 18, 14, "+"))
        add(GuiButton(20, cx - 80, cy + 42, 18, 14, "<"))
        add(GuiButton(21, cx + 62, cy + 42, 18, 14, ">"))

        add(GuiButton(0,  cx - 35, cy + 64, 70, 18, StatCollector.translateToLocal("gui.kaisatsu.btn.save")))
        add(GuiButton(99, cx - 35, cy + 86, 70, 18, StatCollector.translateToLocal("gui.kaisatsu.btn.cancel")))
    }

    override fun actionPerformed(button: GuiButton) {
        val nStations = data.availableStations.size
        val nLines    = data.availableLines.size + 1
        val nDias     = data.availableDias.size + 1
        when (button.id) {
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
            20 -> { selTimeMode = (selTimeMode - 1 + TIME_MODES.size) % TIME_MODES.size; initGui() }
            21 -> { selTimeMode = (selTimeMode + 1) % TIME_MODES.size; initGui() }
            0 -> {
                val station = data.availableStations.getOrElse(selStation) { "" }
                val lineID  = if (selLine == 0) "" else data.availableLines.getOrElse(selLine - 1) { "" to "" }.first
                val dia     = if (selDia == 0)  "" else data.availableDias.getOrElse(selDia - 1) { "" }
                KaizPatchNetwork.CHANNEL.sendToServer(PacketDepartureSettingsSave().also { pkt ->
                    pkt.x = data.x; pkt.y = data.y; pkt.z = data.z
                    pkt.stationName  = station
                    pkt.lineID       = lineID
                    pkt.diaName      = dia
                    pkt.direction    = DIRS[selDir]
                    pkt.displayRows  = selRows + 1
                    pkt.timeMode     = TIME_MODES[selTimeMode].first
                })
                mc.thePlayer.closeScreen()
            }
            99 -> mc.thePlayer.closeScreen()
        }
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        drawRect(cx - 140, cy - 96, cx + 140, cy + 110, 0xF0202030.toInt())
        super.drawScreen(mouseX, mouseY, partialTicks)

        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.departure_settings.title"), cx, cy - 88, 0xFFFFFF)

        val labelX = cx - 130
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.station"),   labelX, cy - 56, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.line"),      labelX, cy - 36, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.dia"),       labelX, cy - 16, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.direction"), labelX, cy + 4,  0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.rows"),      labelX, cy + 24, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.timemode"),  labelX, cy + 44, 0xAAAAAA)

        val stLabel   = data.availableStations.getOrElse(selStation) { tlc("gui.kaisatsu.departure.no_station") }
        val lineLabel = if (selLine == 0) tlc("gui.kaisatsu.departure.no_line")
                        else data.availableLines.getOrElse(selLine - 1) { "" to "" }.let { (id, name) -> "$name §8($id)" }
        val diaLabel  = if (selDia == 0) tlc("gui.kaisatsu.departure.all_dia") else data.availableDias.getOrElse(selDia - 1) { "" }
        val rowsLabel = "${selRows + 1}${tlc("gui.kaisatsu.departure.rows_suffix")}"

        drawCenteredString(fontRendererObj, stLabel,   cx, cy - 54, 0xFFFF55)
        drawCenteredString(fontRendererObj, lineLabel, cx, cy - 34, 0xFFFF55)
        drawCenteredString(fontRendererObj, diaLabel,  cx, cy - 14, 0xFFFF55)
        drawCenteredString(fontRendererObj, DIRS[selDir], cx, cy + 6, 0xFFFF55)
        drawCenteredString(fontRendererObj, rowsLabel, cx, cy + 26, 0xFFFF55)
        drawCenteredString(fontRendererObj, StatCollector.translateToLocal(TIME_MODES[selTimeMode].second), cx, cy + 46, 0xFFFF55)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)

    private fun cycle(v: Int, size: Int) = if (size == 0) 0 else ((v % size) + size) % size

    companion object {
        val DIRS = listOf("両方", "下り", "上り")
        val TIME_MODES = listOf("real" to "gui.kaisatsu.departure.time.real", "game" to "gui.kaisatsu.departure.time.game")
    }
}
