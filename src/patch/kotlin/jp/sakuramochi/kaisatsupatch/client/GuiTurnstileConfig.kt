package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile.GateMode
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketTurnstileConfig
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
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

    // 開放時間の選択肢: ticks → 表示名
    private val OPEN_TICKS_OPTIONS = listOf(20 to "1秒", 40 to "2秒", 60 to "3秒", 80 to "4秒", 100 to "5秒")

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
        val applyBtn = GuiButton(4, cx - 55,  cy + 96, 110, 20, "適用 [Enter]")

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

        drawCenteredString(fontRendererObj, "改札機設定", cx, cy - 38, 0xFFFFFF)
        drawCenteredString(fontRendererObj, "駅を選択:", cx, cy - 26, 0xAAAAAA)

        val stationLabel = if (stationList.isEmpty()) "（駅が登録されていません）"
                           else stationList[selectedStationIndex]
        drawCenteredString(fontRendererObj, stationLabel, cx, cy - 3, 0xFFFF55)

        if (stationList.size > 1) {
            drawCenteredString(fontRendererObj, "${selectedStationIndex + 1} / ${stationList.size}", cx, cy + 9, 0x555555)
        }

        // 通過メッセージラベル
        drawString(fontRendererObj, "通過メッセージ（省略可）:", cx - 75, cy + 62, 0xAAAAAA)
        msgField.drawTextBox()

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    private fun modeLabel() = "モード: ${currentMode.displayName}"
    private fun openTicksLabel(): String {
        val label = OPEN_TICKS_OPTIONS.firstOrNull { it.first == currentOpenTicks }?.second ?: "${currentOpenTicks}t"
        return "開放時間: $label"
    }
}

val GateMode.displayName: String
    get() = when (this) {
        GateMode.ENTRY        -> "入場専用（全種別）"
        GateMode.EXIT         -> "出場専用（全種別）"
        GateMode.BOTH         -> "入出場兼用（全種別）"
        GateMode.IC_ONLY      -> "IC専用（入出場兼用）"
        GateMode.TICKET_ONLY  -> "切符専用（入出場兼用）"
        GateMode.PASS_ONLY    -> "定期専用（入出場兼用）"
    }
