package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketResetSales
import jp.sakuramochi.kaisatsupatch.network.PacketStationUpdate
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

@SideOnly(Side.CLIENT)
class GuiStationManager(
    private val x: Int, private val y: Int, private val z: Int,
    private val originalName: String,
    private val salesTotal: Long = 0L,
    private val fareList: List<Pair<String, Int>> = emptyList()
) : GuiScreen() {

    private lateinit var nameField: GuiTextField

    // 運賃表スクロール
    private var fareScrollOffset = 0
    private val FARE_VISIBLE = 8

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        val cx = width / 2; val cy = height / 2
        nameField = GuiTextField(fontRendererObj, cx - 75, cy - 10, 150, 20)
        nameField.maxStringLength = 30
        nameField.setFocused(true)
        nameField.text = if (originalName == "未設定") "" else originalName

        @Suppress("UNCHECKED_CAST")
        val buttons = buttonList as MutableList<GuiButton>
        buttons.add(GuiButton(0, cx - 55, cy + 20, 110, 20, "ネットワークに登録"))
        if (originalName != "未設定") {
            buttons.add(GuiButton(1, cx - 55, cy + 44, 110, 20, "ネットワークから削除"))
            buttons.add(GuiButton(2, cx - 55, cy + 68, 110, 20, "売上をリセット"))
        }
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun handleMouseInput() {
        super.handleMouseInput()
        if (fareList.isEmpty()) return
        val wheel = Mouse.getEventDWheel()
        if (wheel > 0) fareScrollOffset = maxOf(0, fareScrollOffset - 1)
        else if (wheel < 0) fareScrollOffset = minOf(maxOf(0, fareList.size - FARE_VISIBLE), fareScrollOffset + 1)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (nameField.textboxKeyTyped(typedChar, keyCode)) return
        if (keyCode == 28) { register(); return }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        nameField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> register()
            1 -> deleteStation()
            2 -> resetSales()
        }
    }

    private fun register() {
        val newName = nameField.text.trim().ifEmpty { "未設定" }
        KaizPatchNetwork.CHANNEL.sendToServer(PacketStationUpdate(x, y, z, originalName, newName))
        mc.thePlayer.closeScreen()
    }

    private fun deleteStation() {
        KaizPatchNetwork.CHANNEL.sendToServer(PacketStationUpdate(x, y, z, originalName, ""))
        mc.thePlayer.closeScreen()
    }

    private fun resetSales() {
        KaizPatchNetwork.CHANNEL.sendToServer(PacketResetSales(originalName))
        mc.thePlayer.closeScreen()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2

        // ── 左パネル（既存コントロール） ──────────────────────
        drawCenteredString(fontRendererObj, "駅管理ブロック 設定", cx, cy - 50, 0xFFFFFF)
        drawString(fontRendererObj, "駅名を入力:", cx - 75, cy - 22, 0xAAAAAA)
        nameField.drawTextBox()

        if (originalName != "未設定") {
            val salesStr = "累計売上: ${"%,d".format(salesTotal)}円"
            drawCenteredString(fontRendererObj, salesStr, cx, cy - 36, 0x55FF55)
        }

        // ── 右パネル（運賃表） ──────────────────────────────
        if (originalName != "未設定") {
            val px = cx + 90          // パネル左端
            val py = cy - 60          // パネル上端（タイトル行を含む）
            val pw = 170              // パネル幅
            val ph = FARE_VISIBLE * 13 + 24  // パネル高さ

            // 背景
            drawRect(px - 4, py - 14, px + pw + 4, py + ph, 0xAA000000.toInt())

            drawString(fontRendererObj, "この駅からの運賃（切符）", px, py - 12, 0xFFDD00)

            if (fareList.isEmpty()) {
                drawString(fontRendererObj, "（路線データなし）", px, py, 0x888888)
            } else {
                val visible = fareList.drop(fareScrollOffset).take(FARE_VISIBLE)
                visible.forEachIndexed { i, (dest, fare) ->
                    drawString(fontRendererObj, dest, px, py + i * 13, 0xEEEEEE)
                    val fareStr = "${fare}円"
                    drawString(fontRendererObj, fareStr,
                        px + pw - fontRendererObj.getStringWidth(fareStr), py + i * 13, 0x55FF55)
                }
                if (fareList.size > FARE_VISIBLE) {
                    val end = minOf(fareScrollOffset + FARE_VISIBLE, fareList.size)
                    drawString(fontRendererObj,
                        "${fareScrollOffset + 1}-${end} / ${fareList.size}  ↑↓スクロール",
                        px, py + FARE_VISIBLE * 13 + 4, 0x888888)
                }
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false
}
