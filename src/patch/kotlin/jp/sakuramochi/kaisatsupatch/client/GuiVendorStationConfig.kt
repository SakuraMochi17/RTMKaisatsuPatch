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
    currentCompanyID: String,
    private val stationList: List<String>,
    private val companyList: List<Pair<String, String>> // id to 表示名
) : GuiScreen() {

    private var selectedStationIdx = stationList.indexOf(currentStation).coerceAtLeast(0)
    private var selectedCompanyIdx = companyList.indexOfFirst { it.first == currentCompanyID }.coerceAtLeast(0)

    // 後方互換コンストラクタ（会社なし）
    constructor(x: Int, y: Int, z: Int, currentStation: String, stationList: List<String>)
        : this(x, y, z, currentStation, "", stationList, emptyList())

    override fun initGui() {
        Keyboard.enableRepeatEvents(false)
        val cx = width / 2; val cy = height / 2
        @Suppress("UNCHECKED_CAST")
        val buttons = buttonList as MutableList<GuiButton>

        // 駅選択
        buttons.add(GuiButton(0, cx - 120, cy - 20, 20, 20, "<"))
        buttons.add(GuiButton(1, cx + 100, cy - 20, 20, 20, ">"))

        // 会社選択（会社が登録されている場合のみ）
        if (companyList.isNotEmpty()) {
            buttons.add(GuiButton(10, cx - 120, cy + 10, 20, 20, "<"))
            buttons.add(GuiButton(11, cx + 100, cy + 10, 20, 20, ">"))
        }

        buttons.add(GuiButton(2, cx - 55, cy + 40, 110, 20, "適用 [Enter]"))
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0  -> if (stationList.isNotEmpty())  selectedStationIdx  = (selectedStationIdx  - 1 + stationList.size)  % stationList.size
            1  -> if (stationList.isNotEmpty())  selectedStationIdx  = (selectedStationIdx  + 1) % stationList.size
            10 -> if (companyList.isNotEmpty()) selectedCompanyIdx = (selectedCompanyIdx - 1 + companyList.size) % companyList.size
            11 -> if (companyList.isNotEmpty()) selectedCompanyIdx = (selectedCompanyIdx + 1) % companyList.size
            2  -> applyAndClose()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == 28) { applyAndClose(); return }
        super.keyTyped(typedChar, keyCode)
    }

    private fun applyAndClose() {
        val station = stationList.getOrElse(selectedStationIdx) { "" }
        val companyID = companyList.getOrNull(selectedCompanyIdx)?.first ?: ""
        if (station.isNotEmpty()) {
            KaizPatchNetwork.CHANNEL.sendToServer(PacketVendorStationSave(x, y, z, station, companyID))
        }
        mc.thePlayer.closeScreen()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        drawCenteredString(fontRendererObj, "券売機  設定", cx, cy - 50, 0xFFFFFF)

        // 駅
        drawCenteredString(fontRendererObj, "設置駅:", cx, cy - 34, 0xAAAAAA)
        val stLabel = if (stationList.isEmpty()) "（登録済み駅がありません）" else stationList[selectedStationIdx]
        drawCenteredString(fontRendererObj, stLabel, cx, cy - 18, 0xFFFF55)
        if (stationList.size > 1)
            drawCenteredString(fontRendererObj, "${selectedStationIdx + 1} / ${stationList.size}", cx, cy - 6, 0x555555)

        // 会社
        if (companyList.isNotEmpty()) {
            drawCenteredString(fontRendererObj, "所属会社:", cx, cy - 2, 0xAAAAAA)
            val co = companyList[selectedCompanyIdx]
            drawCenteredString(fontRendererObj, "[${co.first}] ${co.second}", cx, cy + 14, 0x55FFFF)
            drawCenteredString(fontRendererObj, "${selectedCompanyIdx + 1} / ${companyList.size}", cx, cy + 26, 0x555555)
        } else {
            drawCenteredString(fontRendererObj, "§7会社未登録 (/kaisatsu company create で追加)", cx, cy + 8, 0x555555)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false
}
