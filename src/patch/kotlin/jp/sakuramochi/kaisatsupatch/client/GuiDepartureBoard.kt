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
 * 発車標（表示体）の設定 GUI。この板に常時表示する静的情報
 * （路線名・方面・番線・路線カラー）を編集する。発車情報の中身は
 * バインドした設定ブロック側で設定する。
 */
@SideOnly(Side.CLIENT)
class GuiDepartureBoard(private val data: PacketOpenDepartureBoard) : GuiScreen() {

    private lateinit var fldLine:      GuiTextField
    private lateinit var fldDirection: GuiTextField
    private lateinit var fldPlatform:  GuiTextField
    private lateinit var fldColor:     GuiTextField
    private var sampleMode = data.sampleMode

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST") (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        fldLine      = GuiTextField(fontRendererObj, cx - 50, cy - 56, 150, 14)
        fldDirection = GuiTextField(fontRendererObj, cx - 50, cy - 32, 150, 14)
        fldPlatform  = GuiTextField(fontRendererObj, cx - 50, cy - 8,  60,  14)
        fldColor     = GuiTextField(fontRendererObj, cx - 50, cy + 16, 70,  14)
        fldLine.text      = data.headerLine
        fldDirection.text = data.headerDirection
        fldPlatform.text  = data.platform
        fldPlatform.setMaxStringLength(8)
        fldColor.setMaxStringLength(7)
        fldColor.text = "%06X".format(data.lineColorHex and 0xFFFFFF)

        val sampleLabel = StatCollector.translateToLocal("gui.kaisatsu.departure_board.btn.sample") +
            ": " + StatCollector.translateToLocal(if (sampleMode) "gui.kaisatsu.on" else "gui.kaisatsu.off")
        add(GuiButton(1,  cx - 50, cy + 40, 150, 16, sampleLabel))
        add(GuiButton(0,  cx - 35, cy + 64, 70, 18, StatCollector.translateToLocal("gui.kaisatsu.btn.save")))
        add(GuiButton(99, cx - 35, cy + 86, 70, 18, StatCollector.translateToLocal("gui.kaisatsu.btn.cancel")))
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> {
                // テキスト欄の内容を保持したままトグル
                val l = fldLine.text; val d = fldDirection.text; val p = fldPlatform.text; val c = fldColor.text
                sampleMode = !sampleMode
                initGui()
                fldLine.text = l; fldDirection.text = d; fldPlatform.text = p; fldColor.text = c
            }
            0 -> {
                val color = parseColor(fldColor.text) ?: data.lineColorHex
                KaizPatchNetwork.CHANNEL.sendToServer(PacketDepartureBoardSave().also { pkt ->
                    pkt.x = data.x; pkt.y = data.y; pkt.z = data.z
                    pkt.headerLine      = fldLine.text.trim()
                    pkt.headerDirection = fldDirection.text.trim()
                    pkt.platform        = fldPlatform.text.trim()
                    pkt.lineColorHex    = color
                    pkt.sampleMode      = sampleMode
                })
                mc.thePlayer.closeScreen()
            }
            99 -> mc.thePlayer.closeScreen()
        }
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
        drawRect(cx - 130, cy - 92, cx + 130, cy + 94, 0xF0202030.toInt())
        super.drawScreen(mouseX, mouseY, partialTicks)

        val tlc = StatCollector::translateToLocal
        drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.departure_board.title"), cx, cy - 84, 0xFFFFFF)
        drawCenteredString(fontRendererObj,
            "${tlc("gui.kaisatsu.departure_board.bound")}: §e${data.boundInfo}", cx, cy - 72, 0xAAAAAA)

        val labelX = cx - 120
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_board.lbl.line"),      labelX, cy - 53, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_board.lbl.direction"), labelX, cy - 29, 0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_board.lbl.platform"),  labelX, cy - 5,  0xAAAAAA)
        drawString(fontRendererObj, tlc("gui.kaisatsu.departure_settings.lbl.line_color"), labelX, cy + 19, 0xAAAAAA)

        fldLine.drawTextBox()
        fldDirection.drawTextBox()
        fldPlatform.drawTextBox()
        fldColor.drawTextBox()

        val preview = parseColor(fldColor.text)
        if (preview != null) drawRect(cx + 28, cy + 16, cx + 50, cy + 30, 0xFF000000.toInt() or preview)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)

    private fun parseColor(s: String): Int? =
        s.trim().removePrefix("#").let { if (it.isEmpty()) null else it.toIntOrNull(16)?.and(0xFFFFFF) }
}
