package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketExportTemplate
import jp.sakuramochi.kaisatsupatch.network.PacketLineUpdate
import jp.sakuramochi.kaisatsupatch.network.PacketOpenLineGui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiLineManager(private val data: PacketOpenLineGui) : GuiScreen() {

    // ── ページ定義 ────────────────────────────────────────────────────
    private enum class Page { TOP, LINE_EDIT, COMPANY_PROPS, MEMBERS, MUTUAL }

    private var page = Page.TOP

    // ── 路線編集状態 ──────────────────────────────────────────────────
    private var selectedLineIndex = 0
    private var globalIndex       = 0
    private var stationIndex      = 0
    private var currentOldLineID  = ""
    private var editLineStations  = mutableListOf<String>()

    private val globalStations = data.globalStations.toMutableList().also {
        if (it.isEmpty()) it.add("駅が見つかりません")
    }

    // ── 会社メンバー・相互利用（楽観的更新） ─────────────────────────
    private val members          = data.members.toMutableList()
    private val allowedCompanies = data.allowedCompanies.toMutableList()
    private var mutualIdx        = 0

    // ── テキストフィールド ────────────────────────────────────────────
    // 路線編集
    private lateinit var idField:   GuiTextField
    private lateinit var nameField: GuiTextField
    private lateinit var baseField: GuiTextField
    private lateinit var costField: GuiTextField
    private lateinit var tfField:   GuiTextField

    // 会社プロパティ
    private lateinit var fldCID:   GuiTextField
    private lateinit var fldCName: GuiTextField
    private lateinit var fldColor: GuiTextField
    private lateinit var fldIC:    GuiTextField
    private lateinit var fldBase:  GuiTextField
    private lateinit var fldRate:  GuiTextField

    // メンバー追加
    private lateinit var fldMember: GuiTextField

    // ── companyID が未設定かどうか ────────────────────────────────────
    private val companyIsNew get() = data.companyID.isEmpty()

    // ── initGui ───────────────────────────────────────────────────────
    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        val tlc = StatCollector::translateToLocal
        when (page) {
            Page.TOP -> {
                if (companyIsNew) {
                    add(GuiButton(15, cx - 35, cy + 20, 70, 20, tlc("gui.kaisatsu.line.btn.company_setup")))
                } else {
                    add(GuiButton(15, cx - 100, cy + 50, 60, 18, tlc("gui.kaisatsu.line.btn.company_props")))
                    add(GuiButton(18, cx - 30,  cy + 50, 60, 18, tlc("gui.kaisatsu.line.btn.members")))
                    add(GuiButton(19, cx + 40,  cy + 50, 60, 18, tlc("gui.kaisatsu.line.btn.mutual")))
                    add(GuiButton(11, cx - 100, cy + 5,  20, 20, "<"))
                    add(GuiButton(12, cx + 80,  cy + 5,  20, 20, ">"))
                    add(GuiButton(13, cx - 100, cy + 75, 90, 20, tlc("gui.kaisatsu.line.btn.edit_line")).also {
                        it.enabled = data.companyLines.isNotEmpty()
                    })
                    add(GuiButton(14, cx + 10,  cy + 75, 90, 20, tlc("gui.kaisatsu.line.btn.new_line")))
                    add(GuiButton(17, cx - 100, cy + 100, 90, 16, tlc("gui.kaisatsu.line.btn.oudia")).also {
                        it.enabled = data.companyLines.isNotEmpty()
                    })
                }
            }

            Page.LINE_EDIT -> {
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
                    baseField.text = data.defaultBaseFare.toString()
                    costField.text = data.defaultCostPerBlock.toString()
                    tfField.text   = "0"
                }
                add(GuiButton(1,  cx - 115, cy + 20, 20, 20, "<"))
                add(GuiButton(2,  cx - 25,  cy + 20, 20, 20, ">"))
                add(GuiButton(3,  cx - 100, cy + 50, 80, 20, tlc("gui.kaisatsu.line.edit.btn.add_station")))
                add(GuiButton(4,  cx + 95,  cy - 5,  20, 20, "∧"))
                add(GuiButton(5,  cx + 95,  cy + 25, 20, 20, "∨"))
                add(GuiButton(6,  cx + 120, cy - 5,  30, 20, tlc("gui.kaisatsu.line.edit.btn.up")))
                add(GuiButton(7,  cx + 120, cy + 25, 30, 20, tlc("gui.kaisatsu.line.edit.btn.down")))
                add(GuiButton(8,  cx + 95,  cy + 50, 55, 20, tlc("gui.kaisatsu.btn.delete")))
                add(GuiButton(20, cx - 130, cy + 85, 70, 20, tlc("gui.kaisatsu.line.edit.btn.back")))
                add(GuiButton(9,  cx - 50,  cy + 85, 60, 20, tlc("gui.kaisatsu.line.edit.btn.delete_line")))
                add(GuiButton(0,  cx + 20,  cy + 85, 110, 20, tlc("gui.kaisatsu.line.edit.btn.save")))
            }

            Page.COMPANY_PROPS -> {
                val ox = cx - 120
                fldCID   = GuiTextField(fontRendererObj, ox,      cy - 80, 80,  15).also { it.setEnabled(companyIsNew) }
                fldCName = GuiTextField(fontRendererObj, ox + 90, cy - 80, 130, 15)
                fldColor = GuiTextField(fontRendererObj, ox,      cy - 47, 80,  15)
                fldIC    = GuiTextField(fontRendererObj, ox + 90, cy - 47, 130, 15)
                fldBase  = GuiTextField(fontRendererObj, ox,      cy - 14, 80,  15)
                fldRate  = GuiTextField(fontRendererObj, ox + 90, cy - 14, 80,  15)
                fldCID.text   = data.companyID
                fldCName.text = data.companyName
                fldColor.text = "%06X".format(data.companyColor)
                fldIC.text    = data.icCardName
                fldBase.text  = data.defaultBaseFare.toString()
                fldRate.text  = data.defaultCostPerBlock.toString()
                add(GuiButton(0,  cx - 35, cy + 20, 70, 18, tlc("gui.kaisatsu.btn.save")))
                add(GuiButton(99, cx - 35, cy + 44, 70, 18, tlc("gui.kaisatsu.btn.back")))
            }

            Page.MEMBERS -> {
                fldMember = GuiTextField(fontRendererObj, cx - 80, cy + 30, 130, 15)
                fldMember.setFocused(true)
                add(GuiButton(31, cx + 60, cy + 28, 50, 18, tlc("gui.kaisatsu.btn.add")))
                add(GuiButton(99, cx - 35, cy + 60, 70, 18, tlc("gui.kaisatsu.btn.back")))
                members.forEachIndexed { i, _ ->
                    add(GuiButton(300 + i, cx + 60, cy - 60 + i * 20, 50, 16, tlc("gui.kaisatsu.line.members.btn.remove")))
                }
            }

            Page.MUTUAL -> {
                val others = otherCandidates()
                add(GuiButton(40, cx - 120, cy + 30, 20, 18, "<").also { it.enabled = others.isNotEmpty() })
                add(GuiButton(41, cx - 20,  cy + 30, 20, 18, ">").also { it.enabled = others.isNotEmpty() })
                add(GuiButton(42, cx + 10,  cy + 30, 50, 18, tlc("gui.kaisatsu.line.mutual.btn.allow")).also  { it.enabled = others.isNotEmpty() })
                add(GuiButton(99, cx - 35,  cy + 55, 70, 18, tlc("gui.kaisatsu.btn.back")))
                allowedCompanies.forEachIndexed { i, _ ->
                    add(GuiButton(400 + i, cx + 60, cy - 60 + i * 20, 50, 16, tlc("gui.kaisatsu.line.mutual.btn.revoke")))
                }
            }
        }
    }

    // ── actionPerformed ───────────────────────────────────────────────
    override fun actionPerformed(button: GuiButton) {
        val isLoop = page == Page.LINE_EDIT && editLineStations.size > 1 &&
                editLineStations.first() == editLineStations.last()

        when (page) {
            Page.TOP -> when (button.id) {
                15 -> { page = Page.COMPANY_PROPS; initGui() }
                18 -> { page = Page.MEMBERS; initGui() }
                19 -> { page = Page.MUTUAL;  initGui() }
                11 -> if (data.companyLines.isNotEmpty())
                        selectedLineIndex = (selectedLineIndex - 1 + data.companyLines.size) % data.companyLines.size
                12 -> if (data.companyLines.isNotEmpty())
                        selectedLineIndex = (selectedLineIndex + 1) % data.companyLines.size
                13 -> {
                    currentOldLineID = data.companyLines[selectedLineIndex].lineID
                    editLineStations = data.companyLines[selectedLineIndex].stations.toMutableList()
                    page = Page.LINE_EDIT; initGui()
                }
                14 -> { currentOldLineID = ""; editLineStations = mutableListOf(); page = Page.LINE_EDIT; initGui() }
                17 -> {
                    val lineID = data.companyLines.getOrNull(selectedLineIndex)?.lineID ?: ""
                    send(PacketLineUpdate().also { it.mode = PacketLineUpdate.Mode.SAVE_LINE; it.oldLineID = lineID })
                    // サーバーからチャットでファイルパスが通知される（PacketExportTemplate）
                    KaizPatchNetwork.CHANNEL.sendToServer(PacketExportTemplate().also {
                        it.lineID = data.companyLines.getOrNull(selectedLineIndex)?.lineID ?: ""
                    })
                }
            }

            Page.LINE_EDIT -> when (button.id) {
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
                8 -> {
                    editLineStations.removeAt(stationIndex)
                    if (stationIndex >= editLineStations.size) stationIndex = maxOf(0, editLineStations.size - 1)
                }
                20 -> { page = Page.TOP; initGui() }
                0 -> {
                    if (editLineStations.size < 2) return
                    send(PacketLineUpdate().also {
                        it.mode         = PacketLineUpdate.Mode.SAVE_LINE
                        it.companyID    = data.companyID
                        it.companyName  = data.companyName
                        it.oldLineID    = currentOldLineID
                        it.newLineID    = idField.text
                        it.lineName     = nameField.text
                        it.baseFare     = baseField.text.toIntOrNull()    ?: 150
                        it.costPerBlock = costField.text.toDoubleOrNull() ?: 0.15
                        it.transferFee  = tfField.text.toIntOrNull()      ?: 0
                        it.lineStations = editLineStations.toList()
                    })
                    mc.thePlayer.closeScreen()
                }
                9 -> {
                    send(PacketLineUpdate().also {
                        it.mode      = PacketLineUpdate.Mode.DELETE_LINE
                        it.companyID = data.companyID
                        it.oldLineID = currentOldLineID
                    })
                    mc.thePlayer.closeScreen()
                }
            }

            Page.COMPANY_PROPS -> when (button.id) {
                0 -> {
                    val id   = fldCID.text.trim()
                    val name = fldCName.text.trim()
                    if (id.isBlank() || name.isBlank()) return
                    val color = fldColor.text.trim().toIntOrNull(16) ?: 0x1E90FF
                    send(PacketLineUpdate().also {
                        it.mode                = PacketLineUpdate.Mode.SAVE_COMPANY_PROPS
                        it.companyID           = id
                        it.companyName         = name
                        it.companyColor        = color
                        it.icCardName          = fldIC.text.trim()
                        it.defaultBaseFare     = fldBase.text.toIntOrNull() ?: 150
                        it.defaultCostPerBlock = fldRate.text.toDoubleOrNull() ?: 0.1
                    })
                    // ローカルキャッシュ更新
                    data.companyID           = id
                    data.companyName         = name
                    data.companyColor        = color
                    data.icCardName          = fldIC.text.trim()
                    data.defaultBaseFare     = fldBase.text.toIntOrNull() ?: 150
                    data.defaultCostPerBlock = fldRate.text.toDoubleOrNull() ?: 0.1
                    page = Page.TOP; initGui()
                }
                99 -> { page = Page.TOP; initGui() }
            }

            Page.MEMBERS -> when {
                button.id == 99 -> { page = Page.TOP; initGui() }
                button.id == 31 -> {
                    val name = if (::fldMember.isInitialized) fldMember.text.trim() else ""
                    if (name.isBlank()) return
                    send(PacketLineUpdate().also {
                        it.mode        = PacketLineUpdate.Mode.ADD_MEMBER
                        it.companyID   = data.companyID
                        it.targetParam = name
                    })
                    members.add(name)
                    page = Page.MEMBERS; initGui()
                }
                button.id in 300..399 -> {
                    val member = members.getOrNull(button.id - 300) ?: return
                    send(PacketLineUpdate().also {
                        it.mode        = PacketLineUpdate.Mode.REMOVE_MEMBER
                        it.companyID   = data.companyID
                        it.targetParam = member
                    })
                    members.remove(member)
                    page = Page.MEMBERS; initGui()
                }
            }

            Page.MUTUAL -> {
                val others = otherCandidates()
                when {
                    button.id == 99 -> { page = Page.TOP; initGui() }
                    button.id == 40 -> if (others.isNotEmpty()) { mutualIdx = (mutualIdx - 1 + others.size) % others.size; initGui() }
                    button.id == 41 -> if (others.isNotEmpty()) { mutualIdx = (mutualIdx + 1) % others.size; initGui() }
                    button.id == 42 -> {
                        val target = others.getOrNull(mutualIdx) ?: return
                        send(PacketLineUpdate().also {
                            it.mode        = PacketLineUpdate.Mode.ADD_MUTUAL
                            it.companyID   = data.companyID
                            it.targetParam = target.first
                        })
                        allowedCompanies.add(target.first)
                        mutualIdx = 0; page = Page.MUTUAL; initGui()
                    }
                    button.id in 400..499 -> {
                        val target = allowedCompanies.getOrNull(button.id - 400) ?: return
                        send(PacketLineUpdate().also {
                            it.mode        = PacketLineUpdate.Mode.REMOVE_MUTUAL
                            it.companyID   = data.companyID
                            it.targetParam = target
                        })
                        allowedCompanies.remove(target)
                        page = Page.MUTUAL; initGui()
                    }
                }
            }
        }
    }

    // ── キー・マウス入力 ──────────────────────────────────────────────
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (page) {
            Page.LINE_EDIT -> {
                if (::idField.isInitialized) {
                    if (idField.textboxKeyTyped(typedChar, keyCode)   ||
                        nameField.textboxKeyTyped(typedChar, keyCode) ||
                        baseField.textboxKeyTyped(typedChar, keyCode) ||
                        costField.textboxKeyTyped(typedChar, keyCode) ||
                        tfField.textboxKeyTyped(typedChar, keyCode)) return
                }
            }
            Page.COMPANY_PROPS -> {
                if (::fldCID.isInitialized) {
                    if (fldCID.textboxKeyTyped(typedChar, keyCode)   ||
                        fldCName.textboxKeyTyped(typedChar, keyCode) ||
                        fldColor.textboxKeyTyped(typedChar, keyCode) ||
                        fldIC.textboxKeyTyped(typedChar, keyCode)    ||
                        fldBase.textboxKeyTyped(typedChar, keyCode)  ||
                        fldRate.textboxKeyTyped(typedChar, keyCode)) return
                }
            }
            Page.MEMBERS -> {
                if (::fldMember.isInitialized && fldMember.textboxKeyTyped(typedChar, keyCode)) return
            }
            else -> {}
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        when (page) {
            Page.LINE_EDIT -> {
                if (::idField.isInitialized) {
                    idField.mouseClicked(mouseX, mouseY, mouseButton)
                    nameField.mouseClicked(mouseX, mouseY, mouseButton)
                    baseField.mouseClicked(mouseX, mouseY, mouseButton)
                    costField.mouseClicked(mouseX, mouseY, mouseButton)
                    tfField.mouseClicked(mouseX, mouseY, mouseButton)
                }
            }
            Page.COMPANY_PROPS -> {
                if (::fldCID.isInitialized) {
                    fldCID.mouseClicked(mouseX, mouseY, mouseButton)
                    fldCName.mouseClicked(mouseX, mouseY, mouseButton)
                    fldColor.mouseClicked(mouseX, mouseY, mouseButton)
                    fldIC.mouseClicked(mouseX, mouseY, mouseButton)
                    fldBase.mouseClicked(mouseX, mouseY, mouseButton)
                    fldRate.mouseClicked(mouseX, mouseY, mouseButton)
                }
            }
            Page.MEMBERS -> {
                if (::fldMember.isInitialized) fldMember.mouseClicked(mouseX, mouseY, mouseButton)
            }
            else -> {}
        }
    }

    // ── 描画 ─────────────────────────────────────────────────────────
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        when (page) {
            Page.TOP          -> drawTopPage(cx, cy)
            Page.LINE_EDIT    -> drawLineEditPage(cx, cy)
            Page.COMPANY_PROPS -> drawCompanyPropsPage(cx, cy)
            Page.MEMBERS      -> drawMembersPage(cx, cy)
            Page.MUTUAL       -> drawMutualPage(cx, cy)
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun drawTopPage(cx: Int, cy: Int) {
        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.title"), cx, cy - 90, 0xFFFFFF)
        if (companyIsNew) {
            drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.company_new.msg"), cx, cy - 10, 0xAAAAAA)
            drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.company_new.hint"), cx, cy + 5, 0x555555)
        } else {
            drawString(fontRendererObj, "${tlc("gui.kaisatsu.line.lbl.company_id")} §f${data.companyID}", cx - 100, cy - 70, 0xAAAAAA)
            drawString(fontRendererObj, "${tlc("gui.kaisatsu.line.lbl.company_name")} §f${data.companyName}", cx - 100, cy - 55, 0xAAAAAA)
            drawString(fontRendererObj, "${tlc("gui.kaisatsu.line.lbl.ic")} §f${data.icCardName.ifEmpty { tlc("gui.kaisatsu.line.ic.not_set") }}", cx - 100, cy - 40, 0xAAAAAA)
            drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.divider"), cx, cy - 20, 0x555555)
            if (data.companyLines.isNotEmpty()) {
                val info = data.companyLines[selectedLineIndex]
                drawCenteredString(fontRendererObj, "§f${info.lineName} §7(${info.lineID})", cx, cy + 8, 0xFFFFFF)
                drawCenteredString(fontRendererObj, "${selectedLineIndex + 1} / ${data.companyLines.size}", cx, cy + 22, 0x555555)
            } else {
                drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.no_lines"), cx, cy + 8, 0xAAAAAA)
            }
        }
    }

    private fun drawLineEditPage(cx: Int, cy: Int) {
        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj,
            if (currentOldLineID.isEmpty()) tlc("gui.kaisatsu.line.edit.new_title") else tlc("gui.kaisatsu.line.edit.edit_title"),
            cx, cy - 110, 0xFFFF55)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.id"),             cx - 110, cy - 95, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.name"),           cx + 10,  cy - 95, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.base_fare"),      cx - 85,  cy - 60, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.cost_per_block"), cx - 5,   cy - 60, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.transfer_fee"),   cx + 75,  cy - 60, 0xAAAAAA)
        idField.drawTextBox(); nameField.drawTextBox()
        baseField.drawTextBox(); costField.drawTextBox(); tfField.drawTextBox()
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.available"), cx - 60, cy, 0xAAFFFF)
        drawCenteredString(fontRendererObj, globalStations[globalIndex], cx - 60, cy + 26, 0xFFFFFF)
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.edit.lbl.order"), cx + 50, cy - 25, 0xAAFFFF)
        if (editLineStations.isNotEmpty()) {
            val isLoop = editLineStations.first() == editLineStations.last() && editLineStations.size > 1
            val start = maxOf(0, stationIndex - 2)
            val end   = minOf(editLineStations.size - 1, start + 4)
            var drawY = cy - 5
            for (i in start..end) {
                val prefix = if (i == stationIndex) "▶ " else "   "
                var text = "$prefix${i + 1}. ${editLineStations[i]}"
                if (isLoop && i == editLineStations.size - 1) text += " ${tlc("gui.kaisatsu.line.edit.loop_suffix")}"
                drawString(fontRendererObj, text, cx + 10, drawY, if (i == stationIndex) 0xFFFF55 else 0xDDDDDD)
                drawY += 12
            }
        } else {
            drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.line.edit.no_stations"), cx + 50, cy + 5, 0xAAAAAA)
        }
    }

    private fun drawCompanyPropsPage(cx: Int, cy: Int) {
        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj,
            if (companyIsNew) tlc("gui.kaisatsu.line.company.new_title") else tlc("gui.kaisatsu.line.company.edit_title"),
            cx, cy - 108, 0xFFFFFF)
        val ox = cx - 120
        drawString(fontRendererObj, if (!companyIsNew) tlc("gui.kaisatsu.line.company.lbl.id_fixed") else tlc("gui.kaisatsu.line.company.lbl.id"), ox, cy - 95, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.company.lbl.name"),      ox + 90, cy - 95, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.company.lbl.color"),     ox,      cy - 62, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.company.lbl.ic"),        ox + 90, cy - 62, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.company.lbl.base_fare"), ox,      cy - 29, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.company.lbl.rate"),      ox + 90, cy - 29, 0xAAAAAA)
        if (::fldCID.isInitialized)   fldCID.drawTextBox()
        if (::fldCName.isInitialized) fldCName.drawTextBox()
        if (::fldColor.isInitialized) fldColor.drawTextBox()
        if (::fldIC.isInitialized)    fldIC.drawTextBox()
        if (::fldBase.isInitialized)  fldBase.drawTextBox()
        if (::fldRate.isInitialized)  fldRate.drawTextBox()
    }

    private fun drawMembersPage(cx: Int, cy: Int) {
        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, "${tlc("gui.kaisatsu.line.members.title")} — ${data.companyName}", cx, cy - 80, 0xFFFFFF)
        if (members.isEmpty()) {
            drawString(fontRendererObj, tlc("gui.kaisatsu.line.members.none"), cx - 120, cy - 52, 0xAAAAAA)
        } else {
            members.forEachIndexed { i, m ->
                drawString(fontRendererObj, "§f$m", cx - 120, cy - 60 + i * 20, 0xFFFFFF)
            }
        }
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.members.lbl.add"), cx - 80, cy + 17, 0xAAAAAA)
        if (::fldMember.isInitialized) fldMember.drawTextBox()
    }

    private fun drawMutualPage(cx: Int, cy: Int) {
        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, "${tlc("gui.kaisatsu.line.mutual.title")} — ${data.companyName}", cx, cy - 80, 0xFFFFFF)
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.mutual.lbl.allowed"), cx - 120, cy - 62, 0xAAAAAA)
        if (allowedCompanies.isEmpty()) {
            drawString(fontRendererObj, tlc("gui.kaisatsu.line.mutual.none"), cx - 120, cy - 48, 0x555555)
        } else {
            allowedCompanies.forEachIndexed { i, id ->
                drawString(fontRendererObj, "§f→ $id", cx - 120, cy - 60 + i * 20, 0xFFFFFF)
            }
        }
        val others = otherCandidates()
        drawString(fontRendererObj, tlc("gui.kaisatsu.line.mutual.lbl.add"), cx - 120, cy + 18, 0xAAAAAA)
        val sel = others.getOrNull(mutualIdx)
        val label = if (sel != null) "${sel.first} §7${sel.second}" else tlc("gui.kaisatsu.line.mutual.no_candidates")
        drawCenteredString(fontRendererObj, label, cx, cy + 33, if (others.isEmpty()) 0x555555 else 0xFFFF55)
    }

    // ── ユーティリティ ────────────────────────────────────────────────
    override fun onGuiClosed()    { Keyboard.enableRepeatEvents(false) }
    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
    private fun send(msg: PacketLineUpdate) { msg.x = data.x; msg.y = data.y; msg.z = data.z; KaizPatchNetwork.CHANNEL.sendToServer(msg) }
    private fun <T> MutableList<T>.swap(a: Int, b: Int) { val t = this[a]; this[a] = this[b]; this[b] = t }
    private fun otherCandidates() = data.otherCompanies.filter { !allowedCompanies.contains(it.first) }
}
