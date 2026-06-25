package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketCompanyUpdate
import jp.sakuramochi.kaisatsupatch.network.PacketOpenCompanyManager
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiCompanyManager(
    companies: List<PacketOpenCompanyManager.CompanyInfo>,
    lines: List<PacketOpenCompanyManager.LineInfo>
) : GuiScreen() {

    private val companies: MutableList<PacketOpenCompanyManager.CompanyInfo> = companies.toMutableList()
    private val lines: MutableList<PacketOpenCompanyManager.LineInfo> = lines.toMutableList()

    private enum class Page { LIST, EDIT, MEMBERS, MUTUAL, LINES }

    private var page = Page.LIST
    private var selectedIdx = 0

    private var editIsNew = false
    private lateinit var fldID: GuiTextField
    private lateinit var fldName: GuiTextField
    private lateinit var fldColor: GuiTextField
    private lateinit var fldIC: GuiTextField
    private lateinit var fldBase: GuiTextField
    private lateinit var fldRate: GuiTextField
    private lateinit var fldAdd: GuiTextField

    private var mutualIdx = 0
    private var lineIdx = 0

    private val selectedCompany get() = companies.getOrNull(selectedIdx)

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        when (page) {
            Page.LIST -> {
                if (companies.isNotEmpty()) {
                    add(GuiButton(10, cx - 100, cy + 5,  20, 18, "<"))
                    add(GuiButton(11, cx + 80,  cy + 5,  20, 18, ">"))
                    add(GuiButton(20, cx - 60,  cy + 30, 56, 18, "編集"))
                    add(GuiButton(21, cx + 4,   cy + 30, 56, 18, "削除"))
                    add(GuiButton(23, cx + 4,   cy - 15, 70, 14, "メンバー管理"))
                    add(GuiButton(24, cx + 4,   cy + 1,  70, 14, "相互利用"))
                    add(GuiButton(25, cx + 4,   cy + 17, 70, 14, "路線紐付け"))
                }
                add(GuiButton(22, cx - 35, cy + 54, 70, 18, "+ 新規作成"))
                add(GuiButton(99, cx - 35, cy + 78, 70, 18, "閉じる"))
            }
            Page.EDIT -> {
                val ox = cx - 120
                fldID    = GuiTextField(fontRendererObj, ox,      cy - 75, 80,  15)
                fldName  = GuiTextField(fontRendererObj, ox + 90, cy - 75, 120, 15)
                fldColor = GuiTextField(fontRendererObj, ox,      cy - 42, 80,  15)
                fldIC    = GuiTextField(fontRendererObj, ox + 90, cy - 42, 120, 15)
                fldBase  = GuiTextField(fontRendererObj, ox,      cy - 10, 80,  15)
                fldRate  = GuiTextField(fontRendererObj, ox + 90, cy - 10, 80,  15)
                fldID.setEnabled(editIsNew)
                if (!editIsNew) {
                    selectedCompany?.let { c ->
                        fldID.text    = c.companyID
                        fldName.text  = c.companyName
                        fldColor.text = "%06X".format(c.color)
                        fldIC.text    = c.icCardName
                        fldBase.text  = c.defaultBaseFare.toString()
                        fldRate.text  = c.defaultCostPerBlock.toString()
                    }
                } else {
                    fldBase.text = "150"; fldRate.text = "0.1"
                }
                add(GuiButton(0,  cx - 35, cy + 30, 70, 18, "保存"))
                add(GuiButton(99, cx - 35, cy + 54, 70, 18, "← 戻る"))
            }
            Page.MEMBERS -> {
                fldAdd = GuiTextField(fontRendererObj, cx - 80, cy + 20, 130, 15)
                fldAdd.setFocused(true)
                selectedCompany?.members?.forEachIndexed { i, _ ->
                    add(GuiButton(300 + i, cx + 60, cy - 50 + i * 20, 50, 16, "除名"))
                }
                add(GuiButton(31, cx + 60, cy + 18, 50, 18, "追加"))
                add(GuiButton(99, cx - 35, cy + 50, 70, 18, "← 戻る"))
            }
            Page.MUTUAL -> {
                val co = selectedCompany ?: return
                co.allowedCompanies.forEachIndexed { i, _ ->
                    add(GuiButton(400 + i, cx + 60, cy - 50 + i * 20, 50, 16, "解除"))
                }
                val others = companies.map { it.companyID }
                    .filter { it != co.companyID && !co.allowedCompanies.contains(it) }
                add(GuiButton(40, cx - 120, cy + 30, 20, 18, "<").also { it.enabled = others.isNotEmpty() })
                add(GuiButton(41, cx - 20,  cy + 30, 20, 18, ">").also { it.enabled = others.isNotEmpty() })
                add(GuiButton(42, cx + 10,  cy + 30, 50, 18, "許可").also { it.enabled = others.isNotEmpty() })
                add(GuiButton(99, cx - 35,  cy + 55, 70, 18, "← 戻る"))
            }
            Page.LINES -> {
                val co = selectedCompany ?: return
                val assigned   = lines.filter { it.companyID == co.companyID }
                val unassigned = lines.filter { it.companyID != co.companyID }
                assigned.forEachIndexed { i, _ ->
                    add(GuiButton(500 + i, cx + 90, cy - 60 + i * 18, 45, 14, "解除"))
                }
                add(GuiButton(50, cx - 120, cy + 30, 20, 18, "<").also { it.enabled = unassigned.isNotEmpty() })
                add(GuiButton(51, cx - 20,  cy + 30, 20, 18, ">").also { it.enabled = unassigned.isNotEmpty() })
                add(GuiButton(52, cx + 10,  cy + 30, 50, 18, "紐付け").also { it.enabled = unassigned.isNotEmpty() })
                add(GuiButton(99, cx - 35,  cy + 55, 70, 18, "← 戻る"))
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        val co = selectedCompany

        when (page) {
            Page.LIST -> when (button.id) {
                10 -> { selectedIdx = (selectedIdx - 1 + companies.size) % companies.size; initGui() }
                11 -> { selectedIdx = (selectedIdx + 1) % companies.size; initGui() }
                20 -> if (co != null) { editIsNew = false; page = Page.EDIT; initGui() }
                21 -> if (co != null) {
                    send(PacketCompanyUpdate().also { it.mode = PacketCompanyUpdate.Mode.DELETE; it.companyID = co.companyID })
                    companies.removeAt(selectedIdx)
                    selectedIdx = maxOf(0, selectedIdx - 1)
                    initGui()
                }
                22 -> { editIsNew = true; page = Page.EDIT; initGui() }
                23 -> if (co != null) { page = Page.MEMBERS; initGui() }
                24 -> if (co != null) { page = Page.MUTUAL;  initGui() }
                25 -> if (co != null) { page = Page.LINES;   initGui() }
                99 -> mc.thePlayer.closeScreen()
            }
            Page.EDIT -> when (button.id) {
                0 -> {
                    val id   = fldID.text.trim()
                    val name = fldName.text.trim()
                    if (id.isBlank() || name.isBlank()) return
                    val color = fldColor.text.trim().toIntOrNull(16) ?: 0x1E90FF
                    val ic   = fldIC.text.trim()
                    val base = fldBase.text.toIntOrNull() ?: 150
                    val rate = fldRate.text.toDoubleOrNull() ?: 0.1

                    send(PacketCompanyUpdate().also {
                        it.mode = PacketCompanyUpdate.Mode.SAVE
                        it.companyID = id; it.companyName = name; it.color = color
                        it.icCardName = ic; it.defaultBaseFare = base; it.defaultCostPerBlock = rate
                    })
                    val newInfo = PacketOpenCompanyManager.CompanyInfo(
                        id, name, color, ic, base, rate,
                        co?.members ?: emptyList(), co?.allowedCompanies ?: emptyList()
                    )
                    if (editIsNew) { companies.add(newInfo); selectedIdx = companies.size - 1 }
                    else companies[selectedIdx] = newInfo
                    page = Page.LIST; initGui()
                }
                99 -> { page = Page.LIST; initGui() }
            }
            Page.MEMBERS -> {
                if (co == null) return
                when {
                    button.id == 99 -> { page = Page.LIST; initGui() }
                    button.id == 31 -> {
                        val player = if (::fldAdd.isInitialized) fldAdd.text.trim() else ""
                        if (player.isBlank()) return
                        send(PacketCompanyUpdate().also {
                            it.mode = PacketCompanyUpdate.Mode.ADD_MEMBER
                            it.companyID = co.companyID; it.targetParam = player
                        })
                        companies[selectedIdx] = co.copy(members = co.members + player)
                        page = Page.MEMBERS; initGui()
                    }
                    button.id in 300..399 -> {
                        val member = co.members.getOrNull(button.id - 300) ?: return
                        send(PacketCompanyUpdate().also {
                            it.mode = PacketCompanyUpdate.Mode.REMOVE_MEMBER
                            it.companyID = co.companyID; it.targetParam = member
                        })
                        companies[selectedIdx] = co.copy(members = co.members - member)
                        page = Page.MEMBERS; initGui()
                    }
                }
            }
            Page.MUTUAL -> {
                if (co == null) return
                val others = companies.map { it.companyID }
                    .filter { it != co.companyID && !co.allowedCompanies.contains(it) }
                when {
                    button.id == 99 -> { page = Page.LIST; initGui() }
                    button.id == 40 -> if (others.isNotEmpty()) { mutualIdx = (mutualIdx - 1 + others.size) % others.size; initGui() }
                    button.id == 41 -> if (others.isNotEmpty()) { mutualIdx = (mutualIdx + 1) % others.size; initGui() }
                    button.id == 42 -> {
                        val target = others.getOrNull(mutualIdx) ?: return
                        send(PacketCompanyUpdate().also {
                            it.mode = PacketCompanyUpdate.Mode.ADD_MUTUAL
                            it.companyID = co.companyID; it.targetParam = target
                        })
                        companies[selectedIdx] = co.copy(allowedCompanies = co.allowedCompanies + target)
                        mutualIdx = 0; page = Page.MUTUAL; initGui()
                    }
                    button.id in 400..499 -> {
                        val target = co.allowedCompanies.getOrNull(button.id - 400) ?: return
                        send(PacketCompanyUpdate().also {
                            it.mode = PacketCompanyUpdate.Mode.REMOVE_MUTUAL
                            it.companyID = co.companyID; it.targetParam = target
                        })
                        companies[selectedIdx] = co.copy(allowedCompanies = co.allowedCompanies - target)
                        page = Page.MUTUAL; initGui()
                    }
                }
            }
            Page.LINES -> {
                if (co == null) return
                val assigned   = lines.filter { it.companyID == co.companyID }
                val unassigned = lines.filter { it.companyID != co.companyID }
                when {
                    button.id == 99 -> { page = Page.LIST; initGui() }
                    button.id == 50 -> if (unassigned.isNotEmpty()) { lineIdx = (lineIdx - 1 + unassigned.size) % unassigned.size; initGui() }
                    button.id == 51 -> if (unassigned.isNotEmpty()) { lineIdx = (lineIdx + 1) % unassigned.size; initGui() }
                    button.id == 52 -> {
                        val target = unassigned.getOrNull(lineIdx) ?: return
                        send(PacketCompanyUpdate().also {
                            it.mode = PacketCompanyUpdate.Mode.ASSIGN_LINE
                            it.companyID = co.companyID; it.targetParam = target.lineID
                        })
                        val lIdx = lines.indexOfFirst { it.lineID == target.lineID }
                        if (lIdx >= 0) lines[lIdx] = target.copy(companyID = co.companyID)
                        lineIdx = 0; page = Page.LINES; initGui()
                    }
                    button.id in 500..599 -> {
                        val target = assigned.getOrNull(button.id - 500) ?: return
                        send(PacketCompanyUpdate().also {
                            it.mode = PacketCompanyUpdate.Mode.ASSIGN_LINE
                            it.companyID = ""; it.targetParam = target.lineID
                        })
                        val lIdx = lines.indexOfFirst { it.lineID == target.lineID }
                        if (lIdx >= 0) lines[lIdx] = target.copy(companyID = "")
                        page = Page.LINES; initGui()
                    }
                }
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (page == Page.EDIT) {
            if (::fldID.isInitialized    && fldID.textboxKeyTyped(typedChar, keyCode))    return
            if (::fldName.isInitialized  && fldName.textboxKeyTyped(typedChar, keyCode))  return
            if (::fldColor.isInitialized && fldColor.textboxKeyTyped(typedChar, keyCode)) return
            if (::fldIC.isInitialized    && fldIC.textboxKeyTyped(typedChar, keyCode))    return
            if (::fldBase.isInitialized  && fldBase.textboxKeyTyped(typedChar, keyCode))  return
            if (::fldRate.isInitialized  && fldRate.textboxKeyTyped(typedChar, keyCode))  return
        }
        if (page == Page.MEMBERS && ::fldAdd.isInitialized && fldAdd.textboxKeyTyped(typedChar, keyCode)) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        if (page == Page.EDIT) {
            if (::fldID.isInitialized)    fldID.mouseClicked(mouseX, mouseY, mouseButton)
            if (::fldName.isInitialized)  fldName.mouseClicked(mouseX, mouseY, mouseButton)
            if (::fldColor.isInitialized) fldColor.mouseClicked(mouseX, mouseY, mouseButton)
            if (::fldIC.isInitialized)    fldIC.mouseClicked(mouseX, mouseY, mouseButton)
            if (::fldBase.isInitialized)  fldBase.mouseClicked(mouseX, mouseY, mouseButton)
            if (::fldRate.isInitialized)  fldRate.mouseClicked(mouseX, mouseY, mouseButton)
        }
        if (page == Page.MEMBERS && ::fldAdd.isInitialized) fldAdd.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2

        when (page) {
            Page.LIST -> {
                drawCenteredString(fontRendererObj, "§e会社管理", cx, cy - 80, 0xFFFFFF)
                if (companies.isEmpty()) {
                    drawCenteredString(fontRendererObj, "会社が登録されていません", cx, cy, 0xAAAAAA)
                    drawCenteredString(fontRendererObj, "「+ 新規作成」で追加してください", cx, cy + 14, 0x555555)
                } else {
                    val co = companies[selectedIdx]
                    drawCenteredString(fontRendererObj, "${selectedIdx + 1} / ${companies.size}", cx, cy - 55, 0x555555)
                    drawCenteredString(fontRendererObj, "§e[${co.companyID}] ${co.companyName}", cx, cy - 42, 0xFFFFFF)
                    drawCenteredString(fontRendererObj, "#%06X  §7IC: ${co.icCardName}".format(co.color), cx, cy - 27, 0xAAAAAA)
                    drawCenteredString(fontRendererObj, "初乗り ${co.defaultBaseFare}円  加算 ${co.defaultCostPerBlock}円/m", cx, cy - 13, 0xAAAAAA)
                    drawString(fontRendererObj, "§7メンバー ${co.members.size}名", cx - 100, cy + 3, 0xAAAAAA)
                    drawString(fontRendererObj, "§7相互利用 ${co.allowedCompanies.size}社", cx + 20, cy + 3, 0xAAAAAA)
                }
            }
            Page.EDIT -> {
                drawCenteredString(fontRendererObj, if (editIsNew) "§e会社を新規作成" else "§e会社を編集", cx, cy - 100, 0xFFFFFF)
                val ox = cx - 120
                drawString(fontRendererObj, "会社ID" + if (!editIsNew) " §7(変更不可)" else "", ox, cy - 90, 0xAAAAAA)
                drawString(fontRendererObj, "会社名",                   ox + 90, cy - 90, 0xAAAAAA)
                drawString(fontRendererObj, "カラー(16進 例:FF0000)",   ox,      cy - 57, 0xAAAAAA)
                drawString(fontRendererObj, "ICカード名",               ox + 90, cy - 57, 0xAAAAAA)
                drawString(fontRendererObj, "デフォルト初乗り(円)",     ox,      cy - 25, 0xAAAAAA)
                drawString(fontRendererObj, "加算レート(円/ブロック)",  ox + 90, cy - 25, 0xAAAAAA)
                if (::fldID.isInitialized)    fldID.drawTextBox()
                if (::fldName.isInitialized)  fldName.drawTextBox()
                if (::fldColor.isInitialized) fldColor.drawTextBox()
                if (::fldIC.isInitialized)    fldIC.drawTextBox()
                if (::fldBase.isInitialized)  fldBase.drawTextBox()
                if (::fldRate.isInitialized)  fldRate.drawTextBox()
            }
            Page.MEMBERS -> {
                val co = selectedCompany ?: return super.drawScreen(mouseX, mouseY, partialTicks)
                drawCenteredString(fontRendererObj, "§e${co.companyName} — メンバー管理", cx, cy - 70, 0xFFFFFF)
                if (co.members.isEmpty()) {
                    drawString(fontRendererObj, "§7メンバーがいません", cx - 120, cy - 42, 0xAAAAAA)
                } else {
                    co.members.forEachIndexed { i, m ->
                        drawString(fontRendererObj, "§f$m", cx - 120, cy - 50 + i * 20, 0xFFFFFF)
                    }
                }
                drawString(fontRendererObj, "追加(プレイヤー名):", cx - 80, cy + 7, 0xAAAAAA)
                if (::fldAdd.isInitialized) fldAdd.drawTextBox()
            }
            Page.MUTUAL -> {
                val co = selectedCompany ?: return super.drawScreen(mouseX, mouseY, partialTicks)
                drawCenteredString(fontRendererObj, "§e${co.companyName} — 相互利用設定", cx, cy - 70, 0xFFFFFF)
                drawString(fontRendererObj, "§7許可済み:", cx - 120, cy - 55, 0xAAAAAA)
                if (co.allowedCompanies.isEmpty()) {
                    drawString(fontRendererObj, "§7なし", cx - 120, cy - 42, 0x555555)
                } else {
                    co.allowedCompanies.forEachIndexed { i, id ->
                        drawString(fontRendererObj, "§f→ $id", cx - 120, cy - 42 + i * 20, 0xFFFFFF)
                    }
                }
                val others = companies.map { it.companyID }
                    .filter { it != co.companyID && !co.allowedCompanies.contains(it) }
                drawString(fontRendererObj, "追加:", cx - 120, cy + 18, 0xAAAAAA)
                val sel = others.getOrElse(mutualIdx) { "（追加可能な会社なし）" }
                drawCenteredString(fontRendererObj, sel, cx, cy + 33, if (others.isEmpty()) 0x555555 else 0xFFFF55)
            }
            Page.LINES -> {
                val co = selectedCompany ?: return super.drawScreen(mouseX, mouseY, partialTicks)
                val assigned   = lines.filter { it.companyID == co.companyID }
                val unassigned = lines.filter { it.companyID != co.companyID }
                drawCenteredString(fontRendererObj, "§e${co.companyName} — 路線紐付け", cx, cy - 70, 0xFFFFFF)
                drawString(fontRendererObj, "§7紐付け済み:", cx - 120, cy - 55, 0xAAAAAA)
                if (assigned.isEmpty()) {
                    drawString(fontRendererObj, "§7なし", cx - 120, cy - 42, 0x555555)
                } else {
                    assigned.forEachIndexed { i, l ->
                        drawString(fontRendererObj, "§f${l.lineID} §7${l.lineName}", cx - 120, cy - 42 + i * 18, 0xFFFFFF)
                    }
                }
                drawString(fontRendererObj, "§7追加:", cx - 120, cy + 18, 0xAAAAAA)
                val sel = unassigned.getOrNull(lineIdx)
                val label = if (sel != null) "${sel.lineID} §7${sel.lineName}" else "§7（未紐付けの路線なし）"
                drawCenteredString(fontRendererObj, label, cx, cy + 33, 0xFFFF55)
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
    private fun send(pkt: PacketCompanyUpdate) = KaizPatchNetwork.CHANNEL.sendToServer(pkt)
}
