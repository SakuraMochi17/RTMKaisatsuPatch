package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile.GateMode
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketTurnstileConfig
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiTurnstileConfig(
    private val x: Int, private val y: Int, private val z: Int,
    currentStation: String, gateMode: String,
    private val stationList: List<String>
) : GuiScreen() {

    private var selectedStationIndex: Int = stationList.indexOf(currentStation).coerceAtLeast(0)
    private var currentMode: GateMode = runCatching { GateMode.valueOf(gateMode) }.getOrDefault(GateMode.ENTRY)

    private lateinit var prevBtn: GuiButton
    private lateinit var nextBtn: GuiButton
    private lateinit var modeBtn: GuiButton

    override fun initGui() {
        Keyboard.enableRepeatEvents(false)
        val cx = width / 2
        val cy = height / 2

        // テキスト行: タイトル cy-38, ラベル cy-26, 駅名 cy-3, ページ番号 cy+9
        prevBtn  = GuiButton(0, cx - 120, cy - 8, 20, 20, "<")
        nextBtn  = GuiButton(1, cx + 100, cy - 8, 20, 20, ">")
        modeBtn  = GuiButton(2, cx - 60,  cy + 20, 120, 20, modeLabel())
        val applyBtn = GuiButton(3, cx - 55, cy + 46, 110, 20, "適用 [Enter]")

        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).addAll(listOf(prevBtn, nextBtn, modeBtn, applyBtn))
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> { if (stationList.isNotEmpty()) selectedStationIndex = (selectedStationIndex - 1 + stationList.size) % stationList.size }
            1 -> { if (stationList.isNotEmpty()) selectedStationIndex = (selectedStationIndex + 1) % stationList.size }
            2 -> {
                currentMode = when (currentMode) {
                    GateMode.ENTRY        -> GateMode.EXIT
                    GateMode.EXIT         -> GateMode.BOTH
                    GateMode.BOTH         -> GateMode.IC_ONLY
                    GateMode.IC_ONLY      -> GateMode.TICKET_ONLY
                    GateMode.TICKET_ONLY  -> GateMode.PASS_ONLY
                    GateMode.PASS_ONLY    -> GateMode.ENTRY
                }
                modeBtn.displayString = modeLabel()
            }
            3 -> applyAndClose()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 28) { applyAndClose(); return }
        super.keyTyped(typedChar, keyCode)
    }

    private fun applyAndClose() {
        val station = stationList.getOrElse(selectedStationIndex) { "未設定" }
        KaizPatchNetwork.CHANNEL.sendToServer(
            PacketTurnstileConfig(x, y, z, station, currentMode.name)
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

        // ページ番号は駅名の下に独立して表示
        if (stationList.size > 1) {
            drawCenteredString(fontRendererObj, "${selectedStationIndex + 1} / ${stationList.size}", cx, cy + 9, 0x555555)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    private fun modeLabel() = "モード: ${currentMode.displayName}"
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
