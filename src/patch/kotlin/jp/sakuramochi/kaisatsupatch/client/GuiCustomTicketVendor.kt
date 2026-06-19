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
    private var tab = 0

    private val rightPanelWidth get() = xSize - RIGHT_PANEL_X - 4

    init {
        xSize = GUI_WIDTH
        ySize = GUI_HEIGHT
    }

    override fun initGui() {
        super.initGui()
        buildButtons()
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildButtons() {
        (buttonList as MutableList<GuiButton>).clear()
        val lx = guiLeft; val ty = guiTop

        // タブ（全幅）
        add(GuiButton(200, lx + 2, ty + 4, xSize / 2 - 3, 18,
            if (tab == 0) "▶ 切符購入" else "  切符購入"))
        add(GuiButton(201, lx + xSize / 2 + 1, ty + 4, xSize / 2 - 3, 18,
            if (tab == 1) "▶ ICチャージ" else "  ICチャージ"))

        // 左パネル：選択ボタン（2列）
        val btnW = 82; val btnH = 20; val col0 = lx + 4; val col1 = lx + 90
        if (tab == 0) {
            val maxFare = minOf(8, fares.size)
            for (i in 0 until maxFare) {
                val (dest, fare) = fares[i]
                add(GuiButton(i,
                    if (i % 2 == 0) col0 else col1,
                    ty + 28 + (i / 2) * 24, btnW, btnH, "$dest  ${fare}円"))
            }
            val entryRow = maxFare / 2
            add(GuiButton(100,
                if (maxFare % 2 == 0) col0 else col1,
                ty + 28 + entryRow * 24, btnW, btnH, "入場券  140円"))
        } else {
            for ((i, amount) in chargeOptions.withIndex()) {
                add(GuiButton(300 + i,
                    if (i % 2 == 0) col0 else col1,
                    ty + 28 + (i / 2) * 24, btnW, btnH, "${amount}円"))
            }
        }

        // 右パネル下：購入/チャージボタン（ホットバー高さ）
        add(GuiButton(if (tab == 0) 150 else 350,
            lx + RIGHT_PANEL_X, ty + INV_Y + 60,
            rightPanelWidth, 18,
            if (tab == 0) "購入" else "チャージ"))
    }

    private var selectedFare = 0
    private var selectedDest = ""
    private var isEntry = false

    @Suppress("UNCHECKED_CAST")
    override fun actionPerformed(button: GuiButton) {
        when {
            button.id == 200 -> { tab = 0; selectedFare = 0; selectedDest = ""; isEntry = false; buildButtons() }
            button.id == 201 -> { tab = 1; selectedFare = 0; buildButtons() }
            button.id == 100 -> { isEntry = true; selectedFare = 140; selectedDest = stationName; dimOthers(button) }
            button.id < fares.size -> {
                val (dest, fare) = fares[button.id]
                selectedFare = fare; selectedDest = dest; isEntry = false; dimOthers(button)
            }
            button.id in 300..304 -> { selectedFare = chargeOptions[button.id - 300]; dimOthers(button) }
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
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun dimOthers(selected: GuiButton) {
        (buttonList as MutableList<GuiButton>)
            .filter { it.id in 0..100 || it.id in 300..304 }
            .forEach { it.enabled = it.id != selected.id }
    }

    override fun drawGuiContainerBackgroundLayer(partialTicks: Float, mouseX: Int, mouseY: Int) {
        GL11.glColor4f(1f, 1f, 1f, 1f)
        val lx = guiLeft; val ty = guiTop

        // 背景
        drawRect(lx, ty, lx + xSize, ty + ySize, 0xFFC6C6C6.toInt())
        drawRect(lx + 1, ty + 1, lx + xSize - 1, ty + ySize - 1, 0xFFF0F0F0.toInt())

        // タブ区切り
        drawHorizontalLine(lx + 2, lx + xSize - 2, ty + 24, 0xFF888888.toInt())

        // 左右パネル仕切り
        drawVerticalLine(lx + RIGHT_PANEL_X - 3, ty + 24, ty + ySize - 2, 0xFF888888.toInt())

        // インベントリ上部仕切り
        drawHorizontalLine(lx + 2, lx + xSize - 2, ty + INV_Y - 4, 0xFF888888.toInt())

        // お金スロット 2×2
        for ((sx, sy) in MONEY_POSITIONS) drawSlotBg(lx + sx, ty + sy)

        // ICカードスロット
        drawSlotBg(lx + CARD_X, ty + CARD_Y)

        // プレイヤーインベントリ
        for (row in 0..2) for (col in 0..8)
            drawSlotBg(lx + INV_X + col * 18, ty + INV_Y + row * 18)
        for (col in 0..8)
            drawSlotBg(lx + INV_X + col * 18, ty + INV_Y + 58)
    }

    private fun drawSlotBg(x: Int, y: Int) {
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF888888.toInt())
        drawRect(x, y, x + 16, y + 16, 0xFF8B8B8B.toInt())
    }

    override fun drawGuiContainerForegroundLayer(mouseX: Int, mouseY: Int) {
        val title = "券売機  $stationName"
        fontRendererObj.drawString(title, xSize / 2 - fontRendererObj.getStringWidth(title) / 2, -9, 0x404040)

        val rpx = RIGHT_PANEL_X + 2

        // お金スロット 2×2 ラベル
        fontRendererObj.drawString("お金投入", rpx, MONEY_POSITIONS[0].second - 12, 0x404040)
        val moneyYen = container.vendorInv.getMoneyYen()
        fontRendererObj.drawString("合計: ${moneyYen}円", rpx, MONEY_POSITIONS[2].second + 20, 0x006600)

        // ICカードスロット ラベル
        fontRendererObj.drawString("ICカード", rpx, CARD_Y - 12, 0x404040)
        val balance = container.vendorInv.getICBalance()
        fontRendererObj.drawString(
            if (balance != null) "残高:${balance}円" else "未挿入",
            rpx, CARD_Y + 20,
            if (balance != null) 0x0000AA else 0x888888)

        // 選択状態（インベントリ上）
        val selY = INV_Y - 14
        if (tab == 0) {
            if (selectedFare > 0)
                fontRendererObj.drawString(
                    if (isEntry) "入場券 140円" else "$selectedDest ${selectedFare}円",
                    rpx, selY, 0x0000CC)
            else
                fontRendererObj.drawString("行き先を選択", rpx, selY, 0x888888)
        } else {
            if (selectedFare > 0)
                fontRendererObj.drawString("${selectedFare}円", rpx, selY, 0x0000CC)
            else
                fontRendererObj.drawString("金額を選択", rpx, selY, 0x888888)
        }

        fontRendererObj.drawString("持ち物", INV_X, INV_Y - 13, 0x404040)

        if (tab == 0 && fares.isEmpty())
            fontRendererObj.drawString("行き先なし", 4, 60, 0xFF0000)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
}
