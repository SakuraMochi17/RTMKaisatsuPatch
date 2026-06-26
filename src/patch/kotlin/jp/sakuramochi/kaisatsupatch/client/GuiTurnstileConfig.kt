package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile.GateMode
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketTurnstileConfig
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiTurnstileConfig(
    private val x: Int, private val y: Int, private val z: Int,
    currentStation: String, gateMode: String,
    private val stationList: List<String>,
    openTicks: Int = 40,
    private val initialPassMessage: String = ""
) : GuiScreen() {

    private var selectedStationIndex: Int = stationList.indexOf(currentStation).coerceAtLeast(0)
    private var currentMode: GateMode = runCatching { GateMode.valueOf(gateMode) }.getOrDefault(GateMode.ENTRY)
    private var currentOpenTicks: Int = openTicks

    private lateinit var prevBtn: GuiButton
    private lateinit var nextBtn: GuiButton
    private lateinit var modeBtn: GuiButton
    private lateinit var openTicksBtn: GuiButton
    private lateinit var msgField: GuiTextField

    private val OPEN_TICKS_OPTIONS = listOf(
        20 to "gui.kaisatsu.turnstile.time.1s",
        40 to "gui.kaisatsu.turnstile.time.2s",
        60 to "gui.kaisatsu.turnstile.time.3s",
        80 to "gui.kaisatsu.turnstile.time.4s",
        100 to "gui.kaisatsu.turnstile.time.5s"
    )

    init {
        // openTicks が選択肢にない場合は最も近い値に丸める
        currentOpenTicks = OPEN_TICKS_OPTIONS.minByOrNull { kotlin.math.abs(it.first - openTicks) }?.first ?: 40
    }

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        val cx = width / 2
        val cy = height / 2

        prevBtn      = GuiButton(0, cx - 120, cy - 8,  20,  20, "<")
        nextBtn      = GuiButton(1, cx + 100, cy - 8,  20,  20, ">")
        modeBtn      = GuiButton(2, cx - 60,  cy + 20, 120, 20, modeLabel())
        openTicksBtn = GuiButton(3, cx - 60,  cy + 44, 120, 20, openTicksLabel())
        val applyBtn = GuiButton(4, cx - 55,  cy + 96, 110, 20, StatCollector.translateToLocal("gui.kaisatsu.btn.apply"))

        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).addAll(listOf(prevBtn, nextBtn, modeBtn, openTicksBtn, applyBtn))

        msgField = GuiTextField(fontRendererObj, cx - 75, cy + 72, 150, 16)
        msgField.maxStringLength = 40
        msgField.text = initialPassMessage
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> { if (stationList.isNotEmpty()) selectedStationIndex = (selectedStationIndex - 1 + stationList.size) % stationList.size }
            1 -> { if (stationList.isNotEmpty()) selectedStationIndex = (selectedStationIndex + 1) % stationList.size }
            2 -> {
                currentMode = when (currentMode) {
                    GateMode.ENTRY       -> GateMode.EXIT
                    GateMode.EXIT        -> GateMode.BOTH
                    GateMode.BOTH        -> GateMode.IC_ONLY
                    GateMode.IC_ONLY     -> GateMode.TICKET_ONLY
                    GateMode.TICKET_ONLY -> GateMode.PASS_ONLY
                    GateMode.PASS_ONLY   -> GateMode.ENTRY
                }
                modeBtn.displayString = modeLabel()
            }
            3 -> {
                val idx = OPEN_TICKS_OPTIONS.indexOfFirst { it.first == currentOpenTicks }
                currentOpenTicks = OPEN_TICKS_OPTIONS[(idx + 1) % OPEN_TICKS_OPTIONS.size].first
                openTicksBtn.displayString = openTicksLabel()
            }
            4 -> applyAndClose()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (msgField.textboxKeyTyped(typedChar, keyCode)) return
        if (keyCode == 28) { applyAndClose(); return }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        msgField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    private fun applyAndClose() {
        val station = stationList.getOrElse(selectedStationIndex) { "未設定" }
        KaizPatchNetwork.CHANNEL.sendToServer(
            PacketTurnstileConfig(x, y, z, station, currentMode.name, currentOpenTicks, msgField.text.trim())
        )
        mc.thePlayer.closeScreen()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2
        val cy = height / 2

        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.turnstile.title"), cx, cy - 38, 0xFFFFFF)
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.turnstile.lbl.station"), cx, cy - 26, 0xAAAAAA)

        val stationLabel = if (stationList.isEmpty()) tlc("gui.kaisatsu.turnstile.no_station")
                           else stationList[selectedStationIndex]
        drawCenteredString(fontRendererObj, stationLabel, cx, cy - 3, 0xFFFF55)

        if (stationList.size > 1) {
            drawCenteredString(fontRendererObj, "${selectedStationIndex + 1} / ${stationList.size}", cx, cy + 9, 0x555555)
        }

        drawString(fontRendererObj, tlc("gui.kaisatsu.turnstile.lbl.message"), cx - 75, cy + 62, 0xAAAAAA)
        msgField.drawTextBox()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    private fun modeLabel() = String.format(
        StatCollector.translateToLocal("gui.kaisatsu.turnstile.btn.mode"), currentMode.displayName)
    private fun openTicksLabel(): String {
        val key = OPEN_TICKS_OPTIONS.firstOrNull { it.first == currentOpenTicks }?.second ?: ""
        val label = if (key.isNotEmpty()) StatCollector.translateToLocal(key) else "${currentOpenTicks}t"
        return String.format(StatCollector.translateToLocal("gui.kaisatsu.turnstile.btn.open_ticks"), label)
    }
}

val GateMode.displayName: String
    get() = StatCollector.translateToLocal(when (this) {
        GateMode.ENTRY        -> "gui.kaisatsu.turnstile.mode.entry_all"
        GateMode.EXIT         -> "gui.kaisatsu.turnstile.mode.exit_all"
        GateMode.BOTH         -> "gui.kaisatsu.turnstile.mode.both_all"
        GateMode.IC_ONLY      -> "gui.kaisatsu.turnstile.mode.ic_only"
        GateMode.TICKET_ONLY  -> "gui.kaisatsu.turnstile.mode.ticket_only"
        GateMode.PASS_ONLY    -> "gui.kaisatsu.turnstile.mode.pass_only"
    })
