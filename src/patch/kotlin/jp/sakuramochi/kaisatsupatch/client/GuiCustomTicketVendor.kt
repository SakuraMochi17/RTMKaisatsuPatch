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
    // 定期券 (日数 to 割引率)  料金 = 片道運賃 × 日数 × 割引率
    private val passDurations = listOf(7 to 0.90, 30 to 0.75, 90 to 0.60)
    private var tab = 0   // 0=切符, 1=ICチャージ, 2=定期券

    private val rightPanelWidth get() = xSize - RIGHT_PANEL_X - 4

    init { xSize = GUI_WIDTH; ySize = GUI_HEIGHT }

    override fun initGui() { super.initGui(); buildButtons() }

    @Suppress("UNCHECKED_CAST")
    private fun buildButtons() {
        (buttonList as MutableList<GuiButton>).clear()
        val lx = guiLeft; val ty = guiTop
        val tabW = (xSize - 4) / 3

        // タブ（3分割）
        add(GuiButton(200, lx + 2,          ty + 4, tabW, 18, if (tab == 0) "▶ 切符購入"  else "  切符購入"))
        add(GuiButton(201, lx + 2 + tabW,   ty + 4, tabW, 18, if (tab == 1) "▶ ICチャージ" else "  ICチャージ"))
        add(GuiButton(202, lx + 2 + tabW*2, ty + 4, tabW, 18, if (tab == 2) "▶ 定期券"    else "  定期券"))

        val btnW = 82; val btnH = 20; val col0 = lx + 4; val col1 = lx + 90

        when (tab) {
            0 -> {
                // 切符購入ボタン（2列）
                val maxFare = minOf(8, fares.size)
                for (i in 0 until maxFare) {
                    val (dest, fare) = fares[i]
                    add(GuiButton(i, if (i % 2 == 0) col0 else col1, ty + 28 + (i / 2) * 24, btnW, btnH, "$dest  ${fare}円"))
                }
                val entryRow = maxFare / 2
                add(GuiButton(100, if (maxFare % 2 == 0) col0 else col1, ty + 28 + entryRow * 24, btnW, btnH, "入場券  140円"))
                add(GuiButton(150, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, "購入"))
            }
            1 -> {
                // ICチャージ金額ボタン（2列）
                for ((i, amount) in chargeOptions.withIndex())
                    add(GuiButton(300 + i, if (i % 2 == 0) col0 else col1, ty + 28 + (i / 2) * 24, btnW, btnH, "${amount}円"))
                add(GuiButton(350, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, "チャージ"))
            }
            2 -> {
                // 定期券：行き先選択（左）× 期間選択（右下）
                val maxFare = minOf(6, fares.size)
                for (i in 0 until maxFare) {
                    val (dest, _) = fares[i]
                    add(GuiButton(400 + i, if (i % 2 == 0) col0 else col1, ty + 28 + (i / 2) * 24, btnW, btnH, dest))
                }
                // 期間ボタン
                for ((i, pair) in passDurations.withIndex()) {
                    val (days, _) = pair
                    add(GuiButton(500 + i, col0 + i * 56, ty + 106, 52, btnH, "${days}日"))
                }
                add(GuiButton(550, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, "定期購入"))
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
            button.id == 200 -> { tab = 0; resetSelection(); buildButtons() }
            button.id == 201 -> { tab = 1; resetSelection(); buildButtons() }
            button.id == 202 -> { tab = 2; resetSelection(); buildButtons() }

            // 切符選択
            button.id == 100 -> { isEntry = true; selectedFare = 140; selectedDest = stationName; dimOthers(button, 0..100) }
            button.id < fares.size -> {
                val (dest, fare) = fares[button.id]
                selectedFare = fare; selectedDest = dest; isEntry = false; dimOthers(button, 0..100)
            }

            // ICチャージ選択
            button.id in 300..304 -> { selectedFare = chargeOptions[button.id - 300]; dimOthers(button, 300..304) }

            // 定期券 行き先選択
            button.id in 400..405 -> {
                val idx = button.id - 400
                if (idx < fares.size) {
                    selectedPassDest = fares[idx].first
                    updatePassFare()
                    dimOthers(button, 400..405)
                }
            }
            // 定期券 期間選択
            button.id in 500..502 -> {
                val idx = button.id - 500
                selectedPassDays = passDurations[idx].first
                updatePassFare()
                dimOthers(button, 500..502)
            }

            // 購入実行
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
    private fun dimOthers(selected: GuiButton, range: IntRange) {
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

        // 選択状態
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
                // 割引表示
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
