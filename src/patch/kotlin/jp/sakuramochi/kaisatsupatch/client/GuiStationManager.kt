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

@SideOnly(Side.CLIENT)
class GuiStationManager(
    private val x: Int, private val y: Int, private val z: Int,
    private val originalName: String,
    private val salesTotal: Long = 0L
) : GuiScreen() {

    private lateinit var nameField: GuiTextField

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
        drawCenteredString(fontRendererObj, "駅管理ブロック 設定", cx, cy - 50, 0xFFFFFF)
        drawString(fontRendererObj, "駅名を入力:", cx - 75, cy - 22, 0xAAAAAA)
        nameField.drawTextBox()

        if (originalName != "未設定") {
            val salesStr = "累計売上: ${"%,d".format(salesTotal)}円"
            drawCenteredString(fontRendererObj, salesStr, cx, cy - 36, 0x55FF55)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false
}
