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
import jp.sakuramochi.kaisatsupatch.item.ItemCustomPass
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketPurchaseTicket
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector
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
    // ICタブのサブモード: 0=チャージ, 1=新規購入, 2=返却
    private var icMode = 0
    // 切符タブのサブモード: false=普通切符, true=回数券
    private var couponMode = false
    // 定期券タブのサブモード: 0=新規, 1=継続
    private var passSubMode = 0

    // スクロール
    private var ticketScrollOffset = 0
    private var passScrollOffset   = 0
    private val TICKET_VISIBLE = 8
    private val PASS_VISIBLE   = 4

    private val rightPanelWidth get() = xSize - RIGHT_PANEL_X - 4

    init { xSize = GUI_WIDTH; ySize = GUI_HEIGHT }

    override fun initGui() { super.initGui(); buildButtons() }

    /** プレイヤーインベントリから期限切れ間近（残り7日以下）または期限切れの定期券を返す（フリーパス除く） */
    private fun getExpiringPasses(): List<Pair<Int, ItemStack>> {
        val world = playerInv.player?.worldObj ?: return emptyList()
        val currentDay = ItemCustomPass.currentDay(world)
        val result = mutableListOf<Pair<Int, ItemStack>>()
        for (i in 0 until playerInv.getSizeInventory()) {
            val stack = playerInv.getStackInSlot(i) ?: continue
            if (stack.item !is ItemCustomPass) continue
            if (ItemCustomPass.isFreePast(stack)) continue
            val remaining = ItemCustomPass.remainingDays(stack, currentDay)
            if (remaining <= 7) result.add(i to stack)
        }
        return result
    }

    /** 定期券の「もう一方の駅」を返す（fromStation が当駅なら toStation、そうでなければ fromStation） */
    private fun passOtherStation(stack: ItemStack): String {
        val from = ItemCustomPass.getFromStation(stack)
        val to   = ItemCustomPass.getToStation(stack)
        return if (from == stationName) to else from
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildButtons() {
        (buttonList as MutableList<GuiButton>).clear()
        val lx = guiLeft; val ty = guiTop
        val tabW = (xSize - 4) / 3

        val tlc = StatCollector::translateToLocal
        add(GuiButton(200, lx + 2,          ty + 4, tabW, 18, if (tab == 0) "▶ ${tlc("gui.kaisatsu.vendor.tab.ticket")}" else "   ${tlc("gui.kaisatsu.vendor.tab.ticket")}"))
        add(GuiButton(201, lx + 2 + tabW,   ty + 4, tabW, 18, if (tab == 1) "▶ ${tlc("gui.kaisatsu.vendor.tab.ic")}"    else "   ${tlc("gui.kaisatsu.vendor.tab.ic")}"))
        add(GuiButton(202, lx + 2 + tabW*2, ty + 4, tabW, 18, if (tab == 2) "▶ ${tlc("gui.kaisatsu.vendor.tab.pass")}"  else "   ${tlc("gui.kaisatsu.vendor.tab.pass")}"))

        val btnW = 82; val btnH = 20; val col0 = lx + 4; val col1 = lx + 90

        when (tab) {
            0 -> {
                val smW2 = (RIGHT_PANEL_X - 6) / 2
                add(GuiButton(155, lx + 2,        ty + 25, smW2, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.ticket.normal")).also { it.enabled = couponMode })
                add(GuiButton(156, lx + 2 + smW2, ty + 25, smW2, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.ticket.coupon")).also { it.enabled = !couponMode })

                val visible = fares.drop(ticketScrollOffset).take(TICKET_VISIBLE)
                for (i in visible.indices) {
                    val (dest, fare) = visible[i]
                    val label = if (couponMode) "$dest  ${calcCouponPrice(fare)}円" else "$dest  ${fare}円"
                    add(GuiButton(i, if (i % 2 == 0) col0 else col1, ty + 40 + (i / 2) * 22, btnW, 18, label))
                }
                if (!couponMode) {
                    val entryRow = (visible.size + 1) / 2
                    add(GuiButton(100, col0, ty + 40 + entryRow * 22, btnW, 18, StatCollector.translateToLocal("gui.kaisatsu.vendor.entry_ticket")))
                }
                if (fares.size > TICKET_VISIBLE) {
                    add(GuiButton(600, lx + 172, ty + 40, 10, 18, "▲").also { it.enabled = ticketScrollOffset > 0 })
                    add(GuiButton(601, lx + 172, ty + 58, 10, 18, "▼").also { it.enabled = ticketScrollOffset + TICKET_VISIBLE < fares.size })
                }
                val buyLabel = if (couponMode) StatCollector.translateToLocal("gui.kaisatsu.vendor.btn.buy_coupon") else StatCollector.translateToLocal("gui.kaisatsu.vendor.btn.buy")
                add(GuiButton(150, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, buyLabel))
            }
            1 -> {
                val smW = (RIGHT_PANEL_X - 6) / 3
                add(GuiButton(360, lx + 2,          ty + 25, smW, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.ic.btn.charge")).also { it.enabled = icMode != 0 })
                add(GuiButton(361, lx + 2 + smW,    ty + 25, smW, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.ic.btn.new")).also    { it.enabled = icMode != 1 })
                add(GuiButton(362, lx + 2 + smW*2,  ty + 25, smW, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.ic.btn.return")).also  { it.enabled = icMode != 2 })

                when (icMode) {
                    0 -> {
                        for ((i, amount) in chargeOptions.withIndex())
                            add(GuiButton(300 + i, if (i % 2 == 0) col0 else col1, ty + 44 + (i / 2) * 24, btnW, btnH, "${amount}円"))
                        add(GuiButton(350, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, StatCollector.translateToLocal("gui.kaisatsu.vendor.ic.act.charge"))
                            .also { it.enabled = selectedFare > 0 })
                    }
                    1 -> {
                        for ((i, amount) in chargeOptions.withIndex()) {
                            val balance = amount - 500
                            add(GuiButton(700 + i, if (i % 2 == 0) col0 else col1, ty + 44 + (i / 2) * 24, btnW, btnH, "${amount}円→残高${balance}円"))
                        }
                        add(GuiButton(350, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, StatCollector.translateToLocal("gui.kaisatsu.vendor.ic.act.issue"))
                            .also { it.enabled = selectedFare > 0 })
                    }
                    2 -> {
                        val hasCard = container.vendorInv.getICBalance() != null
                        add(GuiButton(350, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, StatCollector.translateToLocal("gui.kaisatsu.vendor.ic.act.return"))
                            .also { it.enabled = hasCard })
                    }
                }
            }
            2 -> {
                val smW2 = (RIGHT_PANEL_X - 6) / 2
                add(GuiButton(570, lx + 2,        ty + 25, smW2, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.pass.btn.new")).also   { it.enabled = passSubMode != 0 })
                add(GuiButton(571, lx + 2 + smW2, ty + 25, smW2, 14, StatCollector.translateToLocal("gui.kaisatsu.vendor.pass.btn.renew")).also { it.enabled = passSubMode != 1 })

                if (passSubMode == 0) {
                    // ── 新規定期券 ──────────────────────────────────
                    val visible = fares.drop(passScrollOffset).take(PASS_VISIBLE)
                    for (i in visible.indices) {
                        val (dest, _) = visible[i]
                        add(GuiButton(400 + i, if (i % 2 == 0) col0 else col1, ty + 42 + (i / 2) * 22, btnW, btnH, dest))
                    }
                    if (fares.size > PASS_VISIBLE) {
                        add(GuiButton(602, lx + 172, ty + 42, 10, 18, "▲").also { it.enabled = passScrollOffset > 0 })
                        add(GuiButton(603, lx + 172, ty + 64, 10, 18, "▼").also { it.enabled = passScrollOffset + PASS_VISIBLE < fares.size })
                    }
                    for ((i, pair) in passDurations.withIndex()) {
                        val (days, _) = pair
                        add(GuiButton(500 + i, col0 + i * 56, ty + 92, 52, btnH, "${days}日"))
                    }
                    add(GuiButton(550, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, StatCollector.translateToLocal("gui.kaisatsu.vendor.pass.btn.buy")))
                    val dayPassPrice = calcDayPassPrice()
                    add(GuiButton(560, col0, ty + 114, btnW * 2 + 4, 18, String.format(StatCollector.translateToLocal("gui.kaisatsu.vendor.pass.free_pass"), dayPassPrice)))
                } else {
                    // ── 継続定期券 ──────────────────────────────────
                    val expiring = getExpiringPasses()
                    if (expiring.isNotEmpty()) {
                        val world = playerInv.player?.worldObj
                        val currentDay = if (world != null) ItemCustomPass.currentDay(world) else 0L
                        for ((i, pair) in expiring.take(4).withIndex()) {
                            val (_, stack) = pair
                            val dest = passOtherStation(stack)
                            val remaining = ItemCustomPass.remainingDays(stack, currentDay)
                            val label = if (remaining > 0)
                                "$dest (${String.format(StatCollector.translateToLocal("gui.kaisatsu.vendor.remaining_days"), remaining)})"
                            else
                                "$dest (${StatCollector.translateToLocal("gui.kaisatsu.vendor.expired")})"
                            add(GuiButton(580 + i, if (i % 2 == 0) col0 else col1, ty + 42 + (i / 2) * 22, btnW, btnH, label))
                        }
                    }
                    for ((i, pair) in passDurations.withIndex()) {
                        val (days, _) = pair
                        add(GuiButton(500 + i, col0 + i * 56, ty + 92, 52, btnH, "${days}日"))
                    }
                    add(GuiButton(550, lx + RIGHT_PANEL_X, ty + INV_Y + 60, rightPanelWidth, 18, StatCollector.translateToLocal("gui.kaisatsu.vendor.pass.btn.renew_buy")))
                }
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
                if (passSubMode == 0) {
                    passScrollOffset = if (wheel > 0)
                        maxOf(0, passScrollOffset - 1)
                    else
                        minOf(maxOf(0, fares.size - PASS_VISIBLE), passScrollOffset + 1)
                    resetSelection(); buildButtons()
                }
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
            button.id == 201 -> { tab = 1; icMode = 0; resetSelection(); buildButtons() }
            button.id == 202 -> { tab = 2; passScrollOffset = 0; resetSelection(); buildButtons() }

            // 切符タブ: 普通/回数券 切り替え
            button.id == 155 -> { couponMode = false; resetSelection(); buildButtons() }
            button.id == 156 -> { couponMode = true;  resetSelection(); buildButtons() }

            // ICサブモード切り替え
            button.id == 360 -> { icMode = 0; resetSelection(); buildButtons() }
            button.id == 361 -> { icMode = 1; resetSelection(); buildButtons() }
            button.id == 362 -> { icMode = 2; resetSelection(); buildButtons() }

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

            // ICチャージ金額選択
            button.id in 300..304 -> { selectedFare = chargeOptions[button.id - 300]; dimOthers(button, 300..304); buildButtons() }

            // IC新規購入金額選択
            button.id in 700..704 -> { selectedFare = chargeOptions[button.id - 700]; dimOthers(button, 700..704); buildButtons() }

            button.id == 602 -> { passScrollOffset = maxOf(0, passScrollOffset - 1); resetSelection(); buildButtons() }
            button.id == 603 -> { passScrollOffset = minOf(maxOf(0, fares.size - PASS_VISIBLE), passScrollOffset + 1); resetSelection(); buildButtons() }

            // 定期券タブ: 新規/継続 切り替え
            button.id == 570 -> { passSubMode = 0; resetSelection(); buildButtons() }
            button.id == 571 -> { passSubMode = 1; resetSelection(); buildButtons() }

            // 新規定期券: 行き先選択
            button.id in 400..405 -> {
                val actualIdx = passScrollOffset + (button.id - 400)
                if (actualIdx < fares.size) {
                    selectedPassDest = fares[actualIdx].first
                    updatePassFare()
                    dimOthers(button, 400..405)
                }
            }

            // 継続定期券: 更新対象の定期券選択
            button.id in 580..583 -> {
                val expiring = getExpiringPasses()
                val idx = button.id - 580
                if (idx < expiring.size) {
                    val (_, stack) = expiring[idx]
                    selectedPassDest = passOtherStation(stack)
                    updatePassFare()
                    dimOthers(button, 580..583)
                }
            }

            button.id in 500..502 -> {
                selectedPassDays = passDurations[button.id - 500].first
                updatePassFare()
                dimOthers(button, 500..502)
            }

            button.id == 150 -> {
                if (selectedFare > 0) {
                    if (couponMode) {
                        val pkt = PacketPurchaseTicket()
                        pkt.x = tile.xCoord; pkt.y = tile.yCoord; pkt.z = tile.zCoord
                        pkt.mode = PacketPurchaseTicket.Mode.COUPON
                        pkt.destStation = selectedDest; pkt.fare = selectedFare
                        KaizPatchNetwork.CHANNEL.sendToServer(pkt)
                    } else {
                        KaizPatchNetwork.CHANNEL.sendToServer(
                            PacketPurchaseTicket(tile.xCoord, tile.yCoord, tile.zCoord,
                                if (isEntry) stationName else selectedDest,
                                if (isEntry) -1 else selectedFare, false))
                    }
                }
            }
            // ICタブの実行ボタン（チャージ/発行/返却）
            button.id == 350 -> when (icMode) {
                0 -> if (selectedFare > 0) KaizPatchNetwork.CHANNEL.sendToServer(
                        PacketPurchaseTicket(tile.xCoord, tile.yCoord, tile.zCoord, "", selectedFare, true))
                1 -> if (selectedFare > 0) {
                        val pkt = PacketPurchaseTicket()
                        pkt.x = tile.xCoord; pkt.y = tile.yCoord; pkt.z = tile.zCoord
                        pkt.mode = PacketPurchaseTicket.Mode.BUY_IC
                        pkt.fare = selectedFare
                        KaizPatchNetwork.CHANNEL.sendToServer(pkt)
                    }
                2 -> {
                    val pkt = PacketPurchaseTicket()
                    pkt.x = tile.xCoord; pkt.y = tile.yCoord; pkt.z = tile.zCoord
                    pkt.mode = PacketPurchaseTicket.Mode.RETURN_IC
                    KaizPatchNetwork.CHANNEL.sendToServer(pkt)
                }
            }
            button.id == 550 -> {
                if (selectedPassFare > 0 && selectedPassDays > 0 && selectedPassDest.isNotEmpty()) {
                    if (passSubMode == 1) {
                        // 継続購入: 既存定期券の有効期限を延長
                        val pkt = PacketPurchaseTicket()
                        pkt.x = tile.xCoord; pkt.y = tile.yCoord; pkt.z = tile.zCoord
                        pkt.mode = PacketPurchaseTicket.Mode.RENEW_PASS
                        pkt.destStation = selectedPassDest
                        pkt.fare = selectedPassFare
                        pkt.passDays = selectedPassDays
                        KaizPatchNetwork.CHANNEL.sendToServer(pkt)
                    } else {
                        KaizPatchNetwork.CHANNEL.sendToServer(
                            PacketPurchaseTicket(tile.xCoord, tile.yCoord, tile.zCoord,
                                selectedPassDest, selectedPassFare, selectedPassDays, true))
                    }
                }
            }
            button.id == 560 -> {
                val dayPassPrice = calcDayPassPrice()
                val pkt = PacketPurchaseTicket()
                pkt.x = tile.xCoord; pkt.y = tile.yCoord; pkt.z = tile.zCoord
                pkt.mode = PacketPurchaseTicket.Mode.DAY_PASS
                pkt.fare = dayPassPrice
                KaizPatchNetwork.CHANNEL.sendToServer(pkt)
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

    private fun calcCouponPrice(baseFare: Int): Int =
        (Math.ceil(baseFare * 10 * 0.9 / 10.0) * 10).toInt()

    private fun calcDayPassPrice(): Int {
        val maxFare = fares.maxOfOrNull { it.second } ?: 0
        if (maxFare == 0) return 1000
        return (Math.ceil(maxFare * 2 * 0.8 / 100.0) * 100).toInt().coerceAtLeast(1000)
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
        val tlc = StatCollector::translateToLocal
        val title = "${tlc("gui.kaisatsu.vendor.title")}  $stationName"
        fontRendererObj.drawString(title, xSize / 2 - fontRendererObj.getStringWidth(title) / 2, -9, 0x404040)

        val rpx = RIGHT_PANEL_X + 2
        fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.lbl.money"), rpx, MONEY_POSITIONS[0].second - 12, 0x404040)
        val moneyYen = container.vendorInv.getMoneyYen()
        fontRendererObj.drawString("${tlc("gui.kaisatsu.vendor.lbl.money_total")} ${moneyYen}円", rpx, MONEY_POSITIONS[2].second + 20, 0x006600)

        fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.lbl.ic"), rpx, CARD_Y - 12, 0x404040)
        val icBalance = container.vendorInv.getICBalance()
        fontRendererObj.drawString(
            if (icBalance != null) "${tlc("gui.kaisatsu.vendor.ic.balance")}${icBalance}円" else tlc("gui.kaisatsu.vendor.ic.not_inserted"),
            rpx, CARD_Y + 20, if (icBalance != null) 0x0000AA else 0x888888)

        if (tab == 0 && fares.size > TICKET_VISIBLE) {
            val end = minOf(ticketScrollOffset + TICKET_VISIBLE, fares.size)
            fontRendererObj.drawString("${ticketScrollOffset + 1}-${end} / ${fares.size}", 4, 22, 0x888888)
        }
        if (tab == 2 && passSubMode == 0 && fares.size > PASS_VISIBLE) {
            val end = minOf(passScrollOffset + PASS_VISIBLE, fares.size)
            fontRendererObj.drawString("${passScrollOffset + 1}-${end} / ${fares.size}", 4, 22, 0x888888)
        }

        val selY = INV_Y - 14
        when (tab) {
            0 -> if (selectedFare > 0) {
                    if (couponMode) {
                        val couponPrice = calcCouponPrice(selectedFare)
                        fontRendererObj.drawString("$selectedDest 10回 ${couponPrice}円", rpx, selY - 8, 0x0000CC)
                        fontRendererObj.drawString("（1回あたり ${selectedFare}×0.9円）", rpx, selY + 4, 0x555555)
                    } else {
                        fontRendererObj.drawString(if (isEntry) tlc("gui.kaisatsu.vendor.entry_ticket.select") else "$selectedDest ${selectedFare}円", rpx, selY, 0x0000CC)
                    }
                } else fontRendererObj.drawString(if (couponMode) tlc("gui.kaisatsu.vendor.select.destination.coupon") else tlc("gui.kaisatsu.vendor.select.destination"), rpx, selY, 0x888888)
            1 -> when (icMode) {
                0 -> if (selectedFare > 0)
                        fontRendererObj.drawString("${selectedFare}円${tlc("gui.kaisatsu.vendor.ic.act.charge")}", rpx, selY, 0x0000CC)
                     else fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.select.amount"), rpx, selY, 0x888888)
                1 -> if (selectedFare > 0) {
                        fontRendererObj.drawString("${selectedFare}円（残高${selectedFare - 500}円）", rpx, selY - 8, 0x0000CC)
                        fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.ic.deposit"), rpx, selY + 4, 0x555555)
                    } else fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.select.amount.new_ic"), rpx, selY, 0x888888)
                2 -> if (icBalance != null) {
                        val refund = maxOf(0, icBalance - 220) + 500
                        fontRendererObj.drawString("${tlc("gui.kaisatsu.vendor.ic.return_amount")} ${refund}円", rpx, selY - 8, 0x55AA00)
                        if (icBalance > 0)
                            fontRendererObj.drawString(String.format(tlc("gui.kaisatsu.vendor.ic.deposit_detail"), icBalance), rpx, selY + 4, 0x555555)
                        else
                            fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.ic.deposit"), rpx, selY + 4, 0x555555)
                    } else fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.ic.insert_card"), rpx, selY, 0x888888)
            }
            2 -> {
                if (selectedPassFare > 0) {
                    val prefix = if (passSubMode == 1) "${tlc("gui.kaisatsu.vendor.pass.btn.renew")} " else ""
                    fontRendererObj.drawString("${prefix}${selectedPassDest} ${selectedPassDays}日 ${selectedPassFare}円", rpx, selY - 8, 0x0000CC)
                } else {
                    if (passSubMode == 1 && getExpiringPasses().isEmpty()) {
                        fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.pass.no_target"), rpx, selY - 8, 0x888888)
                    } else {
                        val destStr = if (selectedPassDest.isEmpty()) tlc("gui.kaisatsu.vendor.pass.select") else selectedPassDest
                        val dayStr  = if (selectedPassDays == 0) tlc("gui.kaisatsu.vendor.pass.select.duration") else "${selectedPassDays}日"
                        fontRendererObj.drawString("$destStr  $dayStr", rpx, selY - 8, 0x888888)
                    }
                }
                if (passSubMode == 0) {
                    fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.pass.discount.7"), rpx, selY + 4, 0x555555)
                    fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.pass.discount.other"), rpx, selY + 14, 0x555555)
                } else {
                    fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.pass.renew.hint"), rpx, selY + 4, 0x555555)
                }
            }
        }

        fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.lbl.inventory"), INV_X, INV_Y - 13, 0x404040)
        if (tab == 0 && fares.isEmpty()) fontRendererObj.drawString(tlc("gui.kaisatsu.vendor.no_destination"), 4, 60, 0xFF0000)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
}
