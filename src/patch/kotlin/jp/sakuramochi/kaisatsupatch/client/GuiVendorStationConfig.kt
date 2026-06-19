package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketVendorStationSave
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiVendorStationConfig(
    private val x: Int, private val y: Int, private val z: Int,
    currentStation: String,
    private val stationList: List<String>
) : GuiScreen() {

    private var selectedIndex = stationList.indexOf(currentStation).coerceAtLeast(0)

    override fun initGui() {
        Keyboard.enableRepeatEvents(false)
        val cx = width / 2; val cy = height / 2
        @Suppress("UNCHECKED_CAST")
        val buttons = buttonList as MutableList<GuiButton>
        buttons.add(GuiButton(0, cx - 120, cy - 8, 20, 20, "<"))
        buttons.add(GuiButton(1, cx + 100, cy - 8, 20, 20, ">"))
        buttons.add(GuiButton(2, cx - 55, cy + 22, 110, 20, "適用 [Enter]"))
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> if (stationList.isNotEmpty()) selectedIndex = (selectedIndex - 1 + stationList.size) % stationList.size
            1 -> if (stationList.isNotEmpty()) selectedIndex = (selectedIndex + 1) % stationList.size
            2 -> applyAndClose()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 28) { applyAndClose(); return }
        super.keyTyped(typedChar, keyCode)
    }

    private fun applyAndClose() {
        val station = stationList.getOrElse(selectedIndex) { "" }
        if (station.isNotEmpty()) {
            KaizPatchNetwork.CHANNEL.sendToServer(PacketVendorStationSave(x, y, z, station))
        }
        mc.thePlayer.closeScreen()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        drawCenteredString(fontRendererObj, "券売機  設置駅設定", cx, cy - 35, 0xFFFFFF)
        drawCenteredString(fontRendererObj, "この券売機が設置されている駅:", cx, cy - 23, 0xAAAAAA)

        val label = if (stationList.isEmpty()) "（登録済み駅がありません）" else stationList[selectedIndex]
        drawCenteredString(fontRendererObj, label, cx, cy - 3, 0xFFFF55)

        if (stationList.size > 1) {
            drawCenteredString(fontRendererObj, "${selectedIndex + 1} / ${stationList.size}", cx, cy + 10, 0x555555)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false
}
