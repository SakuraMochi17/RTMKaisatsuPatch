package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketStationUpdate
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiStationManager(
    private val x: Int, private val y: Int, private val z: Int,
    private val originalName: String   // サーバーから受け取った現在の駅名
) : GuiScreen() {

    private lateinit var nameField: GuiTextField

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        val cx = width / 2; val cy = height / 2
        nameField = GuiTextField(fontRendererObj, cx - 75, cy - 10, 150, 20)
        nameField.maxStringLength = 30
        nameField.setFocused(true)
        // 未設定のときは空欄、それ以外は現在値を表示（再オープン時も維持）
        nameField.text = if (originalName == "未設定") "" else originalName

        @Suppress("UNCHECKED_CAST")
        val buttons = buttonList as MutableList<GuiButton>
        buttons.add(GuiButton(0, cx - 55, cy + 20, 110, 20, "ネットワークに登録"))
        // 既登録駅のみ削除ボタンを表示
        if (originalName != "未設定") {
            buttons.add(GuiButton(1, cx - 55, cy + 44, 110, 20, "ネットワークから削除"))
        }
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

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
        }
    }

    private fun register() {
        val newName = nameField.text.trim().ifEmpty { "未設定" }
        KaizPatchNetwork.CHANNEL.sendToServer(PacketStationUpdate(x, y, z, originalName, newName))
        mc.thePlayer.closeScreen()
    }

    private fun deleteStation() {
        // newName = "" で削除モード
        KaizPatchNetwork.CHANNEL.sendToServer(PacketStationUpdate(x, y, z, originalName, ""))
        mc.thePlayer.closeScreen()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        drawCenteredString(fontRendererObj, "駅管理ブロック 設定", cx, cy - 35, 0xFFFFFF)
        drawString(fontRendererObj, "駅名を入力:", cx - 75, cy - 22, 0xAAAAAA)
        nameField.drawTextBox()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false
}
