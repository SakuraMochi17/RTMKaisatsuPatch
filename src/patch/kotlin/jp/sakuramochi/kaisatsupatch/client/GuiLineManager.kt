package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketLineUpdate
import jp.sakuramochi.kaisatsupatch.network.PacketOpenLineGui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiLineManager(private val data: PacketOpenLineGui) : GuiScreen() {

    // 0=トップ, 1=路線編集, 2=会社名編集
    private var page = 0
    private var selectedLineIndex = 0
    private var globalIndex = 0
    private var stationIndex = 0
    private var currentOldLineID = ""
    private var editLineStations = mutableListOf<String>()

    private val globalStations = data.globalStations.toMutableList().also {
        if (it.isEmpty()) it.add("駅が見つかりません")
    }

    private lateinit var compField: GuiTextField
    private lateinit var idField: GuiTextField
    private lateinit var nameField: GuiTextField
    private lateinit var baseField: GuiTextField
    private lateinit var costField: GuiTextField
    private lateinit var tfField: GuiTextField   // 乗換料金

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        when (page) {
            0 -> {
                add(GuiButton(15, cx + 60, cy - 62, 50, 20, "編集"))
                add(GuiButton(11, cx - 100, cy + 5, 20, 20, "<"))
                add(GuiButton(12, cx + 80, cy + 5, 20, 20, ">"))
                add(GuiButton(13, cx - 100, cy + 40, 90, 20, "設定画面へ ->").also {
                    it.enabled = data.companyLines.isNotEmpty()
                })
                add(GuiButton(14, cx + 10, cy + 40, 90, 20, "+ 新規路線作成"))
            }
            1 -> {
                idField   = GuiTextField(fontRendererObj, cx - 110, cy - 80, 90, 15)
                nameField = GuiTextField(fontRendererObj, cx + 10,  cy - 80, 90, 15)
                baseField = GuiTextField(fontRendererObj, cx - 85,  cy - 45, 50, 15)
                costField = GuiTextField(fontRendererObj, cx - 5,   cy - 45, 50, 15)
                tfField   = GuiTextField(fontRendererObj, cx + 75,  cy - 45, 50, 15)
                if (currentOldLineID.isNotEmpty()) {
                    data.companyLines.find { it.lineID == currentOldLineID }?.let {
                        idField.text = it.lineID; nameField.text = it.lineName
                        baseField.text = it.baseFare.toString()
                        costField.text = it.costPerBlock.toString()
                        tfField.text   = it.transferFee.toString()
                    }
                } else {
                    baseField.text = "150"; costField.text = "0.15"; tfField.text = "0"
                }
                add(GuiButton(1,  cx - 115, cy + 20, 20, 20, "<"))
                add(GuiButton(2,  cx - 25,  cy + 20, 20, 20, ">"))
                add(GuiButton(3,  cx - 100, cy + 50, 80, 20, "路線に追加 ->"))
                add(GuiButton(4,  cx + 95,  cy - 5,  20, 20, "∧"))
                add(GuiButton(5,  cx + 95,  cy + 25, 20, 20, "∨"))
                add(GuiButton(6,  cx + 120, cy - 5,  30, 20, "上へ"))
                add(GuiButton(7,  cx + 120, cy + 25, 30, 20, "下へ"))
                add(GuiButton(8,  cx + 95,  cy + 50, 55, 20, "削除"))
                add(GuiButton(20, cx - 130, cy + 85, 70, 20, "<- トップへ"))
                add(GuiButton(9,  cx - 50,  cy + 85, 60, 20, "路線を削除"))
                add(GuiButton(0,  cx + 20,  cy + 85,110, 20, "設定を保存して終了"))
            }
            2 -> {
                compField = GuiTextField(fontRendererObj, cx - 50, cy - 20, 100, 15)
                compField.text = data.companyName
                compField.setFocused(true)
                add(GuiButton(10, cx - 55, cy + 10, 50, 20, "保存"))
                add(GuiButton(16, cx + 5,  cy + 10, 50, 20, "ｷｬﾝｾﾙ"))
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (page == 2 && ::compField.isInitialized && compField.textboxKeyTyped(typedChar, keyCode)) return
        if (page == 1 && ::idField.isInitialized) {
            if (idField.textboxKeyTyped(typedChar, keyCode) ||
                nameField.textboxKeyTyped(typedChar, keyCode) ||
                baseField.textboxKeyTyped(typedChar, keyCode) ||
                costField.textboxKeyTyped(typedChar, keyCode) ||
                tfField.textboxKeyTyped(typedChar, keyCode)) return
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        if (page == 2 && ::compField.isInitialized) compField.mouseClicked(mouseX, mouseY, mouseButton)
        if (page == 1 && ::idField.isInitialized) {
            idField.mouseClicked(mouseX, mouseY, mouseButton)
            nameField.mouseClicked(mouseX, mouseY, mouseButton)
            baseField.mouseClicked(mouseX, mouseY, mouseButton)
            costField.mouseClicked(mouseX, mouseY, mouseButton)
            tfField.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }

    override fun actionPerformed(button: GuiButton) {
        val isLoop = page == 1 && editLineStations.size > 1 &&
                editLineStations.first() == editLineStations.last()

        when (button.id) {
            // トップ
            15 -> { page = 2; initGui() }
            11 -> if (data.companyLines.isNotEmpty()) selectedLineIndex = (selectedLineIndex - 1 + data.companyLines.size) % data.companyLines.size
            12 -> if (data.companyLines.isNotEmpty()) selectedLineIndex = (selectedLineIndex + 1) % data.companyLines.size
            13 -> { currentOldLineID = data.companyLines[selectedLineIndex].lineID
                    editLineStations = data.companyLines[selectedLineIndex].stations.toMutableList()
                    page = 1; initGui() }
            14 -> { currentOldLineID = ""; editLineStations = mutableListOf(); page = 1; initGui() }
            // 会社名編集
            10 -> {
                send(PacketLineUpdate().also {
                    it.x = data.x; it.y = data.y; it.z = data.z
                    it.mode = PacketLineUpdate.Mode.SAVE_COMPANY
                    it.companyName = compField.text
                })
                data.companyName = compField.text; page = 0; initGui()
            }
            16 -> { page = 0; initGui() }
            // 路線編集 - 駅選択
            1 -> globalIndex = (globalIndex - 1 + globalStations.size) % globalStations.size
            2 -> globalIndex = (globalIndex + 1) % globalStations.size
            3 -> {
                if (isLoop) return
                val target = globalStations[globalIndex]
                if (target == "駅が見つかりません") return
                val count = editLineStations.count { it == target }
                if (count == 0) {
                    editLineStations.add(target); stationIndex = editLineStations.size - 1
                } else if (count == 1 && editLineStations.first() == target && editLineStations.size >= 3) {
                    editLineStations.add(target); stationIndex = editLineStations.size - 1
                }
            }
            // 路線編集 - 駅順操作
            4 -> if (editLineStations.isNotEmpty()) stationIndex = (stationIndex - 1 + editLineStations.size) % editLineStations.size
            5 -> if (editLineStations.isNotEmpty()) stationIndex = (stationIndex + 1) % editLineStations.size
            6 -> if (stationIndex > 0) {
                if (isLoop && (stationIndex == editLineStations.size - 1 || stationIndex == 1)) return
                editLineStations.swap(stationIndex, stationIndex - 1); stationIndex--
            }
            7 -> if (stationIndex < editLineStations.size - 1) {
                if (isLoop && (stationIndex == 0 || stationIndex == editLineStations.size - 2)) return
                editLineStations.swap(stationIndex, stationIndex + 1); stationIndex++
            }
            8 -> { editLineStations.removeAt(stationIndex)
                   if (stationIndex >= editLineStations.size) stationIndex = maxOf(0, editLineStations.size - 1) }
            // 保存・削除・戻る
            20 -> { page = 0; initGui() }
            0 -> {
                if (editLineStations.size < 2) return
                send(PacketLineUpdate().also {
                    it.x = data.x; it.y = data.y; it.z = data.z
                    it.mode = PacketLineUpdate.Mode.SAVE_LINE
                    it.companyName  = data.companyName
                    it.oldLineID    = currentOldLineID
                    it.newLineID    = idField.text
                    it.lineName     = nameField.text
                    it.baseFare     = baseField.text.toIntOrNull() ?: 150
                    it.costPerBlock = costField.text.toDoubleOrNull() ?: 0.15
                    it.transferFee  = tfField.text.toIntOrNull() ?: 0
                    it.lineStations = editLineStations.toList()
                })
                mc.thePlayer.closeScreen()
            }
            9 -> {
                send(PacketLineUpdate().also {
                    it.x = data.x; it.y = data.y; it.z = data.z
                    it.mode = PacketLineUpdate.Mode.DELETE_LINE
                    it.oldLineID = currentOldLineID
                })
                mc.thePlayer.closeScreen()
            }
        }
    }

    private fun send(msg: PacketLineUpdate) = KaizPatchNetwork.CHANNEL.sendToServer(msg)
    private fun <T> MutableList<T>.swap(a: Int, b: Int) { val t = this[a]; this[a] = this[b]; this[b] = t }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        when (page) {
            0 -> {
                drawCenteredString(fontRendererObj, "路線管理管制塔 - 管轄トップ", cx, cy - 90, 0xFFFF55)
                drawCenteredString(fontRendererObj, "会社名", cx, cy - 75, 0xAAAAAA)
                drawString(fontRendererObj, data.companyName.ifEmpty { "未設定" }, cx - 50, cy - 56, 0xFFFFFF)
                drawCenteredString(fontRendererObj, "[ 登録済みの路線 ]", cx, cy - 20, 0xAAFFFF)
                if (data.companyLines.isNotEmpty()) {
                    val info = data.companyLines[selectedLineIndex]
                    drawCenteredString(fontRendererObj, "${info.lineName} (${info.lineID})", cx, cy + 10, 0xFFFFFF)
                    drawCenteredString(fontRendererObj, "${selectedLineIndex + 1} / ${data.companyLines.size}", cx, cy + 25, 0x555555)
                } else {
                    drawCenteredString(fontRendererObj, "路線がありません", cx, cy + 10, 0xAAAAAA)
                }
            }
            1 -> {
                drawCenteredString(fontRendererObj,
                    if (currentOldLineID.isEmpty()) "路線管理 - 新規路線" else "路線管理 - 路線編集",
                    cx, cy - 110, 0xFFFF55)
                drawString(fontRendererObj, "路線ID",       cx - 110, cy - 95, 0xAAAAAA)
                drawString(fontRendererObj, "路線名",       cx + 10,  cy - 95, 0xAAAAAA)
                drawString(fontRendererObj, "初乗り(円)",   cx - 85,  cy - 60, 0xAAAAAA)
                drawString(fontRendererObj, "1B単価(円)",   cx - 5,   cy - 60, 0xAAAAAA)
                drawString(fontRendererObj, "乗換料金(円)", cx + 75,  cy - 60, 0xAAAAAA)
                idField.drawTextBox(); nameField.drawTextBox()
                baseField.drawTextBox(); costField.drawTextBox(); tfField.drawTextBox()
                drawCenteredString(fontRendererObj, "[ 登録可能な駅 ]", cx - 60, cy, 0xAAFFFF)
                drawCenteredString(fontRendererObj, globalStations[globalIndex], cx - 60, cy + 26, 0xFFFFFF)
                drawCenteredString(fontRendererObj, "[ 路線の駅順 ]", cx + 50, cy - 25, 0xAAFFFF)
                if (editLineStations.isNotEmpty()) {
                    val isLoop = editLineStations.first() == editLineStations.last() && editLineStations.size > 1
                    val start = maxOf(0, stationIndex - 2)
                    val end = minOf(editLineStations.size - 1, start + 4)
                    var drawY = cy - 5
                    for (i in start..end) {
                        val prefix = if (i == stationIndex) "▶ " else "   "
                        var text = "$prefix${i + 1}. ${editLineStations[i]}"
                        if (isLoop && i == editLineStations.size - 1) text += " (環状)"
                        val color = if (i == stationIndex) 0xFFFF55 else 0xDDDDDD
                        drawString(fontRendererObj, text, cx + 10, drawY, color)
                        drawY += 12
                    }
                } else {
                    drawCenteredString(fontRendererObj, "未登録", cx + 50, cy + 5, 0xAAAAAA)
                }
            }
            2 -> {
                drawCenteredString(fontRendererObj, "路線管理 - 会社名編集", cx, cy - 50, 0xFFFF55)
                if (::compField.isInitialized) compField.drawTextBox()
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false
}
