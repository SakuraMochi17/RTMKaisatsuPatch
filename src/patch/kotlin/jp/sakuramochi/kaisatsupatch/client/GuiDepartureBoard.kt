package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketDepartureBoardSave
import jp.sakuramochi.kaisatsupatch.network.PacketOpenDepartureBoard
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.StatCollector
import org.lwjgl.input.Keyboard

/**
 * 発車標（表示体）の設定 GUI。この板に常時表示する静的情報（路線名・方面・番線・
 * 路線カラー）と、表示の絞り込み（方向・表示行数）、サンプル表示を編集する。
 * 発車情報のデータ源（駅・ダイヤ等）はバインドした設定ブロック側で設定する。
 */
@SideOnly(Side.CLIENT)
class GuiDepartureBoard(private val data: PacketOpenDepartureBoard) : GuiScreen() {

    private lateinit var fldLine:      GuiTextField
    private lateinit var fldDirection: GuiTextField
    private lateinit var fldPlatform:  GuiTextField
    private lateinit var fldColor:     GuiTextField
    private var sampleMode = data.sampleMode
    private var selDir  = DIRS.indexOf(data.direction).coerceAtLeast(0)
    private var selRows = (data.displayRows - 1).coerceIn(0, 7)

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        fldLine      = GuiTextField(fontRendererObj, cx - 40, cy - 78, 150, 14)
        fldDirection = GuiTextField(fontRendererObj, cx - 40, cy - 58, 150, 14)
        fldPlatform  = GuiTextField(fontRendererObj, cx - 40, cy - 38, 50,  14)
        fldColor     = GuiTextField(fontRendererObj, cx - 40, cy - 18, 60,  14)
        fldLine.text      = data.headerLine
        fldDirection.text = data.headerDirection
        fldPlatform.text  = data.platform
        fldPlatform.setMaxStringLength(8)
        fldColor.setMaxStringLength(7)
        fldColor.text = "%06X".format(data.lineColorHex and 0xFFFFFF)

        // 方向 < >
        add(GuiButton(30, cx - 40, cy + 2, 16, 14, "<"))
        add(GuiButton(31, cx + 60, cy + 2, 16, 14, ">"))
        // 表示行数 - +
        add(GuiButton(32, cx - 40, cy + 20, 16, 14, "-"))
        add(GuiButton(33, cx + 24, cy + 20, 16, 14, "+"))

        val sampleLabel = StatCollector.translateToLocal("gui.kaisatsu.departure_board.btn.sample") +
            ": " + StatCollector.translateToLocal(if (sampleMode) "gui.kaisatsu.on" else "gui.kaisatsu.off")
        add(GuiButton(1,  cx - 60, cy + 40, 160, 16, sampleLabel))
        add(GuiButton(0,  cx - 35, cy + 62, 70, 18, StatCollector.translateToLocal("gui.kaisatsu.btn.save")))
        add(GuiButton(99, cx - 35, cy + 84, 70, 18, StatCollector.translateToLocal("gui.kaisatsu.btn.cancel")))
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            30 -> { selDir = (selDir - 1 + DIRS.size) % DIRS.size; keepInit() }
            31 -> { selDir = (selDir + 1) % DIRS.size; keepInit() }
            32 -> { selRows = (selRows - 1).coerceAtLeast(0); keepInit() }
            33 -> { selRows = (selRows + 1).coerceAtMost(7); keepInit() }
            1  -> { sampleMode = !sampleMode; keepInit() }
            0 -> {
                val color = parseColor(fldColor.text) ?: data.lineColorHex
                KaizPatchNetwork.CHANNEL.sendToServer(PacketDepartureBoardSave().also { pkt ->
                    pkt.x = data.x; pkt.y = data.y; pkt.z = data.z
                    pkt.headerLine      = fldLine.text.trim()
                    pkt.headerDirection = fldDirection.text.trim()
                    pkt.platform        = fldPlatform.text.trim()
                    pkt.lineColorHex    = color
                    pkt.sampleMode      = sampleMode
                    pkt.direction       = DIRS[selDir]
                    pkt.displayRows     = selRows + 1
                })
                mc.thePlayer.closeScreen()
            }
            99 -> mc.thePlayer.closeScreen()
        }
    }

    /** テキスト欄の内容を保持したまま再構築 */
    private fun keepInit() {
        val l = fldLine.text; val d = fldDirection.text; val p = fldPlatform.text; val c = fldColor.text
        initGui()
        fldLine.text = l; fldDirection.text = d; fldPlatform.text = p; fldColor.text = c
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (fldLine.textboxKeyTyped(typedChar, keyCode)) return
        if (fldDirection.textboxKeyTyped(typedChar, keyCode)) return
        if (fldPlatform.textboxKeyTyped(typedChar, keyCode)) return
        if (fldColor.textboxKeyTyped(typedChar, keyCode)) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        fldLine.mouseClicked(mouseX, mouseY, mouseButton)
        fldDirection.mouseClicked(mouseX, mouseY, mouseButton)
        fldPlatform.mouseClicked(mouseX, mouseY, mouseButton)
        fldColor.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2
        drawRect(cx - 140, cy - 100, cx + 140, cy + 108, 0xF0202030.toInt())
        super.drawScreen(mouseX, mouseY, partialTicks)

        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.departure_board.title"), cx, cy - 94, 0xFFFFFF)
        drawCenteredString(fontRendererObj,
            "${tlc("gui.kaisatsu.departure_board.bound")}: §e${data.boundInfo}", cx, cy - 84, 0xAAAAAA)

        val labelX = cx - 130
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_board.lbl.line"),      labelX, cy - 75, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_board.lbl.direction"), labelX, cy - 55, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_board.lbl.platform"),  labelX, cy - 35, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_settings.lbl.line_color"), labelX, cy - 15, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.direction"),       labelX, cy + 5,  0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure.lbl.rows"),            labelX, cy + 23, 0xAAAAAA)

        fldLine.drawTextBox()
        fldDirection.drawTextBox()
        fldPlatform.drawTextBox()
        fldColor.drawTextBox()

        val preview = parseColor(fldColor.text)
        if (preview != null) drawRect(cx + 28, cy - 18, cx + 48, cy - 5, 0xFF000000.toInt() or preview)

        drawCenteredString(fontRendererObj, DIRS[selDir], cx + 10, cy + 5, 0xFFFF55)
        drawCenteredString(fontRendererObj, "${selRows + 1}${tlc("gui.kaisatsu.departure.rows_suffix")}", cx - 5, cy + 23, 0xFFFF55)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)

    private fun parseColor(s: String): Int? =
        s.trim().removePrefix("#").let { if (it.isEmpty()) null else it.toIntOrNull(16)?.and(0xFFFFFF) }

    companion object {
        val DIRS = listOf("両方", "下り", "上り")
    }
}
