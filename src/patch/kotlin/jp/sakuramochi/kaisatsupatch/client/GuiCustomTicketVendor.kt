package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.CARD_X
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.CARD_Y
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.GUI_HEIGHT
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.GUI_WIDTH
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.INV_X
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.INV_Y
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.MONEY_POSITIONS
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor.Companion.RIGHT_PANEL_X
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketPurchaseTicket
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

@SideOnly(Side.CLIENT)
class GuiCustomTicketVendor(
    private val playerInv: InventoryPlayer,
    private val tile: TileEntityCustomTicketVendor,
    private val stationName: String,
    private val fares: List<Pair<String, Int>>
) : GuiContainer(ContainerCustomVendor(playerInv, tile)) {

    private val container get() = inventorySlots as ContainerCustomVendor
    private val chargeOptions = listOf(1000, 2000, 3000, 5000, 10000)
    private val passDurations = listOf(7 to 0.90, 30 to 0.75, 90 to 0.60)
    private var tab = 0

    // スクロール
    private var ticketScrollOffset = 0
    private var passScrollOffset   = 0
    private val TICKET_VISIBLE = 8
    private val PASS_VISIBLE   = 6

    private val rightPanelWidth get() = xSize - RIGHT_PANEL_X - 4

    init { xSize = GUI_WIDTH; ySize = GUI_HEIGHT }

    override fun initGui() { super.initGui(); buildButtons() }

    @Suppress("UNCHECKED_CAST")
    private fun buildButtons() {
        (buttonList as MutableList<GuiButton>).clear()
        val lx = guiLeft; val ty = guiTop
        val tabW = (xSize - 4) / 3

        add(GuiButton(200, lx + 2,          ty + 4, tabW, 18, if (tab == 0) "▶ 切符購入"  else "  切符購入"))
        add(GuiButton(201, lx + 2 + tabW,   ty + 4, tabW, 18, if (tab == 1) "▶ ICチャージ" else "  ICチャージ"))
        add(GuiButton(202, lx + 2 + tabW*2, ty + 4, tabW, 18, if (tab == 2) "▶ 定期券"    else "  定期券"))

        val btnW = 82; val btnH = 20; val col0 = lx + 4; val col1 = lx + 90

        when (tab) {
            0 -> {
                val visible = fares.drop(ticketScrollOffset).take(TICKET_VISIBLE)
                for (i in visible.indices) {
                    val (dest, fare) = visible[i]
                    add(GuiButton(i, if (i % 2 == 0) col0 else col1, ty + 28 + (i / 2) * 24, btnW, btnH, "$dest  ${fare}円"))
                }
                val entryRow = (visible.size + 1) / 2
                add(GuiButton(100, col0, ty + 28 + entryRow * 24, btnW, btnH, "入場券  140円"))
                if (fares.size > TICKET_VISIBLE) {
                    add(GuiButton(600, lx + 172, ty + 28, 10, 18, "▲").also { it.enabled = ticketScrollOffset > 0 })
                    add(GuiButton(601, lx + 172, ty + 50, 10, 18, "▼").also { it.enabled = ticketScrollOffset + TICKET_VISIBLE < fares.size })
                }
                add(GuiButton(150, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, "購入"))
            }
            1 -> {
                for ((i, amount) in chargeOptions.withIndex())
                    add(GuiButton(300 + i, if (i % 2 == 0) col0 else col1, ty + 28 + (i / 2) * 24, btnW, btnH, "${amount}円"))
                add(GuiButton(350, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, "チャージ"))
            }
            2 -> {
                val visible = fares.drop(passScrollOffset).take(PASS_VISIBLE)
                for (i in visible.indices) {
                    val (dest, _) = visible[i]
                    add(GuiButton(400 + i, if (i % 2 == 0) col0 else col1, ty + 28 + (i / 2) * 24, btnW, btnH, dest))
                }
                if (fares.size > PASS_VISIBLE) {
                    add(GuiButton(602, lx + 172, ty + 28, 10, 18, "▲").also { it.enabled = passScrollOffset > 0 })
                    add(GuiButton(603, lx + 172, ty + 50, 10, 18, "▼").also { it.enabled = passScrollOffset + PASS_VISIBLE < fares.size })
                }
                for ((i, pair) in passDurations.withIndex()) {
                    val (days, _) = pair
                    add(GuiButton(500 + i, col0 + i * 56, ty + 106, 52, btnH, "${days}日"))
                }
                add(GuiButton(550, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, "定期購入"))
            }
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val wheel = Mouse.getEventDWheel()
        if (wheel == 0) return
        when (tab) {
            0 -> {
                ticketScrollOffset = if (wheel > 0)
                    maxOf(0, ticketScrollOffset - 1)
                else
                    minOf(maxOf(0, fares.size - TICKET_VISIBLE), ticketScrollOffset + 1)
                resetSelection(); buildButtons()
            }
            2 -> {
                passScrollOffset = if (wheel > 0)
                    maxOf(0, passScrollOffset - 1)
                else
                    minOf(maxOf(0, fares.size - PASS_VISIBLE), passScrollOffset + 1)
                resetSelection(); buildButtons()
            }
        }
    }

    private var selectedFare = 0
    private var selectedDest = ""
    private var isEntry = false
    private var selectedPassDays = 0
    private var selectedPassDest = ""
    private var selectedPassFare = 0

    @Suppress("UNCHECKED_CAST")
    override fun actionPerformed(button: GuiButton) {
        when {
            button.id == 200 -> { tab = 0; ticketScrollOffset = 0; resetSelection(); buildButtons() }
            button.id == 201 -> { tab = 1; resetSelection(); buildButtons() }
            button.id == 202 -> { tab = 2; passScrollOffset = 0; resetSelection(); buildButtons() }

            button.id == 600 -> { ticketScrollOffset = maxOf(0, ticketScrollOffset - 1); resetSelection(); buildButtons() }
            button.id == 601 -> { ticketScrollOffset = minOf(maxOf(0, fares.size - TICKET_VISIBLE), ticketScrollOffset + 1); resetSelection(); buildButtons() }

            button.id == 100 -> { isEntry = true; selectedFare = 140; selectedDest = stationName; dimOthers(button, 0 until TICKET_VISIBLE) }
            button.id in 0 until TICKET_VISIBLE -> {
                val actualIdx = ticketScrollOffset + button.id
                if (actualIdx < fares.size) {
                    val (dest, fare) = fares[actualIdx]
                    selectedFare = fare; selectedDest = dest; isEntry = false
                    dimOthers(button, 0 until TICKET_VISIBLE)
                }
            }

            button.id in 300..304 -> { selectedFare = chargeOptions[button.id - 300]; dimOthers(button, 300..304) }

            button.id == 602 -> { passScrollOffset = maxOf(0, passScrollOffset - 1); resetSelection(); buildButtons() }
            button.id == 603 -> { passScrollOffset = minOf(maxOf(0, fares.size - PASS_VISIBLE), passScrollOffset + 1); resetSelection(); buildButtons() }

            button.id in 400..405 -> {
                val actualIdx = passScrollOffset + (button.id - 400)
                if (actualIdx < fares.size) {
                    selectedPassDest = fares[actualIdx].first
                    updatePassFare()
                    dimOthers(button, 400..405)
                }
            }
            button.id in 500..502 -> {
                selectedPassDays = passDurations[button.id - 500].first
                updatePassFare()
                dimOthers(button, 500..502)
            }

            button.id == 150 -> {
                if (selectedFare > 0) KaizPatchNetwork.CHANNEL.sendToServer(
                    PacketPurchaseTicket(tile.xCoord, tile.yCoord, tile.zCoord,
                        if (isEntry) stationName else selectedDest,
                        if (isEntry) -1 else selectedFare, false))
            }
            button.id == 350 -> {
                if (selectedFare > 0) KaizPatchNetwork.CHANNEL.sendToServer(
                    PacketPurchaseTicket(tile.xCoord, tile.yCoord, tile.zCoord, "", selectedFare, true))
            }
            button.id == 550 -> {
                if (selectedPassFare > 0 && selectedPassDays > 0 && selectedPassDest.isNotEmpty())
                    KaizPatchNetwork.CHANNEL.sendToServer(
                        PacketPurchaseTicket(tile.xCoord, tile.yCoord, tile.zCoord,
                            selectedPassDest, selectedPassFare, selectedPassDays, true))
            }
        }
    }

    private fun updatePassFare() {
        if (selectedPassDest.isEmpty() || selectedPassDays == 0) { selectedPassFare = 0; return }
        val baseFare = fares.find { it.first == selectedPassDest }?.second ?: 0
        val discount = passDurations.find { it.first == selectedPassDays }?.second ?: 1.0
        selectedPassFare = Math.ceil(baseFare * selectedPassDays * discount / 10.0).toInt() * 10
    }

    private fun resetSelection() {
        selectedFare = 0; selectedDest = ""; isEntry = false
        selectedPassDays = 0; selectedPassDest = ""; selectedPassFare = 0
    }

    @Suppress("UNCHECKED_CAST")
    private fun dimOthers(selected: GuiButton, range: Iterable<Int>) {
        (buttonList as MutableList<GuiButton>)
            .filter { it.id in range }
            .forEach { it.enabled = it.id != selected.id }
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GL11.glColor4f(1f, 1f, 1f, 1f)
        val lx = guiLeft; val ty = guiTop
        drawRect(lx, ty, lx + xSize, ty + ySize, 0xFFC6C6C6.toInt())
        drawRect(lx + 1, ty + 1, lx + xSize - 1, ty + ySize - 1, 0xFFF0F0F0.toInt())
        drawHorizontalLine(lx + 2, lx + xSize - 2, ty + 24, 0xFF888888.toInt())
        drawVerticalLine(lx + RIGHT_PANEL_X - 3, ty + 24, ty + ySize - 2, 0xFF888888.toInt())
        drawHorizontalLine(lx + 2, lx + xSize - 2, ty + INV_Y - 4, 0xFF888888.toInt())
        for ((sx, sy) in MONEY_POSITIONS) drawSlotBg(lx + sx, ty + sy)
        drawSlotBg(lx + CARD_X, ty + CARD_Y)
        for (row in 0..2) for (col in 0..8) drawSlotBg(lx + INV_X + col * 18, ty + INV_Y + row * 18)
        for (col in 0..8) drawSlotBg(lx + INV_X + col * 18, ty + INV_Y + 58)
    }

    private fun drawSlotBg(x: Int, y: Int) {
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF888888.toInt())
        drawRect(x, y, x + 16, y + 16, 0xFF8B8B8B.toInt())
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        val title = "券売機  $stationName"
        fontRendererObj.drawString(title, xSize / 2 - fontRendererObj.getStringWidth(title) / 2, -9, 0x404040)

        val rpx = RIGHT_PANEL_X + 2
        fontRendererObj.drawString("お金投入", rpx, MONEY_POSITIONS[0].second - 12, 0x404040)
        val moneyYen = container.vendorInv.getMoneyYen()
        fontRendererObj.drawString("合計: ${moneyYen}円", rpx, MONEY_POSITIONS[2].second + 20, 0x006600)

        fontRendererObj.drawString("ICカード", rpx, CARD_Y - 12, 0x404040)
        val balance = container.vendorInv.getICBalance()
        fontRendererObj.drawString(
            if (balance != null) "残高:${balance}円" else "未挿入",
            rpx, CARD_Y + 20, if (balance != null) 0x0000AA else 0x888888)

        // スクロールインジケーター
        if (tab == 0 && fares.size > TICKET_VISIBLE) {
            val end = minOf(ticketScrollOffset + TICKET_VISIBLE, fares.size)
            fontRendererObj.drawString("${ticketScrollOffset + 1}-${end} / ${fares.size}", 4, 22, 0x888888)
        }
        if (tab == 2 && fares.size > PASS_VISIBLE) {
            val end = minOf(passScrollOffset + PASS_VISIBLE, fares.size)
            fontRendererObj.drawString("${passScrollOffset + 1}-${end} / ${fares.size}", 4, 22, 0x888888)
        }

        val selY = INV_Y - 14
        when (tab) {
            0 -> if (selectedFare > 0)
                    fontRendererObj.drawString(if (isEntry) "入場券 140円" else "$selectedDest ${selectedFare}円", rpx, selY, 0x0000CC)
                 else fontRendererObj.drawString("行き先を選択", rpx, selY, 0x888888)
            1 -> if (selectedFare > 0)
                    fontRendererObj.drawString("${selectedFare}円", rpx, selY, 0x0000CC)
                 else fontRendererObj.drawString("金額を選択", rpx, selY, 0x888888)
            2 -> {
                if (selectedPassFare > 0)
                    fontRendererObj.drawString("${selectedPassDest} ${selectedPassDays}日 ${selectedPassFare}円", rpx, selY - 8, 0x0000CC)
                else {
                    val destStr = if (selectedPassDest.isEmpty()) "行き先を選択" else selectedPassDest
                    val dayStr  = if (selectedPassDays == 0) "期間を選択" else "${selectedPassDays}日"
                    fontRendererObj.drawString("$destStr  $dayStr", rpx, selY - 8, 0x888888)
                }
                fontRendererObj.drawString("7日:10%割引", rpx, selY + 4, 0x555555)
                fontRendererObj.drawString("30日:25%  90日:40%", rpx, selY + 14, 0x555555)
            }
        }

        fontRendererObj.drawString("持ち物", INV_X, INV_Y - 13, 0x404040)
        if (tab == 0 && fares.isEmpty()) fontRendererObj.drawString("行き先なし", 4, 60, 0xFF0000)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
}
