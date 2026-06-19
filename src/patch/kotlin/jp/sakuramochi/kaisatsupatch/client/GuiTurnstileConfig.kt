package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketTurnstileConfig
import jp.sakuramochi.kaisatsupatch.network.displayName
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField

@SideOnly(Side.CLIENT)
class GuiTurnstileConfig(private val tile: TileEntityCustomTurnstile) : GuiScreen() {

    private lateinit var stationField: GuiTextField
    private lateinit var modeButton: GuiButton
    private var currentMode = tile.gateMode

    override fun initGui() {
        val cx = width / 2
        val cy = height / 2

        stationField = GuiTextField(fontRendererObj, cx - 100, cy - 10, 200, 20)
        stationField.text = tile.stationCode
        stationField.setFocused(true)
        stationField.maxStringLength = 32

        modeButton = GuiButton(0, cx - 100, cy + 20, 95, 20, modeLabel())
        val applyButton = GuiButton(1, cx + 5, cy + 20, 95, 20, "適用")

        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).apply {
            add(modeButton)
            add(applyButton)
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> {
                currentMode = if (currentMode == TileEntityCustomTurnstile.GateMode.ENTRY)
                    TileEntityCustomTurnstile.GateMode.EXIT
                else
                    TileEntityCustomTurnstile.GateMode.ENTRY
                modeButton.displayString = modeLabel()
            }
            1 -> applyAndClose()
        }
    }

    private fun applyAndClose() {
        KaizPatchNetwork.CHANNEL.sendToServer(
            PacketTurnstileConfig(
                tile.xCoord, tile.yCoord, tile.zCoord,
                stationField.text.trim(),
                currentMode.name
            )
        )
        mc.thePlayer.closeScreen()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2
        val cy = height / 2
        drawCenteredString(fontRendererObj, "改札機設定", cx, cy - 30, 0xFFFFFF)
        drawString(fontRendererObj, "駅コード:", cx - 100, cy - 22, 0xAAAAAA)
        stationField.drawTextBox()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Enterキーで即時適用
        if (keyCode == 28) {
            applyAndClose()
            return
        }
        if (!stationField.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode)
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        stationField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun doesGuiPauseGame() = false

    private fun modeLabel() = "モード: ${currentMode.displayName}"
}
