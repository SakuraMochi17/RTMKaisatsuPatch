package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.item.ItemCustomExpressTicket
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketCancelReservation
import jp.sakuramochi.kaisatsupatch.network.PacketOpenReservedVendor
import jp.sakuramochi.kaisatsupatch.network.PacketPurchaseExpressTicket
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.item.ItemStack
import net.minecraft.util.StatCollector

@SideOnly(Side.CLIENT)
class GuiReservedVendor(
    private val vendorStation: String,
    private val trains: List<PacketOpenReservedVendor.TrainInfo>,
    private val lines: List<PacketOpenReservedVendor.LineInfo>
) : GuiScreen() {

    private enum class Page { TOP, DESTINATION, TRAIN, CAR, CONFIRM, CANCEL, CANCEL_CONFIRM }

    private var page = Page.TOP
    private var isReserved = true
    private var selectedDestination = ""
    private var selectedTrainID = ""
    private var selectedCarNumber = 0
    private var includeTicket = false

    // 購入確認用
    private var expressFare = 0
    private var trainName = ""
    private var trainType = ""

    // キャンセル用
    private var cancelTickets: List<ItemStack> = emptyList()
    private var selectedCancelIdx = -1

    override fun initGui() {
        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        val tlc = StatCollector::translateToLocal
        when (page) {
            Page.TOP -> {
                add(GuiButton(0,  cx - 60, cy - 22, 120, 20, tlc("gui.kaisatsu.reserved.btn.buy_reserved")))
                add(GuiButton(1,  cx - 60, cy + 2,  120, 20, tlc("gui.kaisatsu.reserved.btn.buy_unreserved")))
                val hasReserved = getReservedTicketsInInventory().isNotEmpty()
                add(GuiButton(2,  cx - 60, cy + 26, 120, 20, tlc("gui.kaisatsu.reserved.btn.cancel_res")).also { it.enabled = hasReserved })
                add(GuiButton(99, cx - 60, cy + 50, 120, 20, tlc("gui.kaisatsu.btn.close")))
            }
            Page.DESTINATION -> {
                val destinations = getAvailableDestinations()
                destinations.forEachIndexed { i, dest ->
                    val row = i / 2; val col = i % 2
                    add(GuiButton(100 + i, cx - 90 + col * 90, cy - 70 + row * 22, 86, 18, dest))
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, tlc("gui.kaisatsu.btn.back")))
            }
            Page.TRAIN -> {
                val availableTrains = getTrainsForRoute()
                availableTrains.forEachIndexed { i, train ->
                    val totalAvail = if (isReserved) train.cars.sumOf { it.availableSeats } else Int.MAX_VALUE
                    val isFull = isReserved && totalAvail == 0
                    val fare = if (isReserved) train.reservedFare else train.unreservedFare
                    val label = "[${train.trainType}] ${train.trainName} ${fare}円${if (isFull) " ${tlc("gui.kaisatsu.reserved.full")}" else ""}"
                    add(GuiButton(200 + i, cx - 110, cy - 70 + i * 22, 220, 18, label).also { it.enabled = !isFull })
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, tlc("gui.kaisatsu.btn.back")))
            }
            Page.CAR -> {
                val selectedTrain = trains.find { it.trainID == selectedTrainID }
                selectedTrain?.cars?.forEachIndexed { i, car ->
                    val isFull = car.availableSeats == 0
                    val label = "${car.carNumber}号車 [${car.carClass}] 残席${car.availableSeats}${if (isFull) " ${tlc("gui.kaisatsu.reserved.full")}" else ""}"
                    add(GuiButton(300 + i, cx - 110, cy - 70 + i * 22, 220, 18, label).also { it.enabled = !isFull })
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, tlc("gui.kaisatsu.btn.back")))
            }
            Page.CONFIRM -> {
                add(GuiButton(50, cx - 120, cy + 10, 240, 18,
                    if (includeTicket) tlc("gui.kaisatsu.reserved.include_ticket.on") else tlc("gui.kaisatsu.reserved.include_ticket.off")))
                add(GuiButton(0,  cx - 60, cy + 34, 120, 18, tlc("gui.kaisatsu.reserved.btn.buy")))
                add(GuiButton(99, cx - 60, cy + 56, 120, 18, tlc("gui.kaisatsu.btn.back")))
            }
            Page.CANCEL -> {
                cancelTickets = getReservedTicketsInInventory()
                cancelTickets.forEachIndexed { i, stack ->
                    val name = ItemCustomExpressTicket.getTrainName(stack)
                    val car  = ItemCustomExpressTicket.getCarNumber(stack)
                    val seat = ItemCustomExpressTicket.getSeatNumber(stack)
                    val from = ItemCustomExpressTicket.getFromStation(stack)
                    val to   = ItemCustomExpressTicket.getToStation(stack)
                    add(GuiButton(400 + i, cx - 140, cy - 60 + i * 22, 280, 18, "${name} ${car}号車${seat}番 ${from}→${to}"))
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, tlc("gui.kaisatsu.btn.back")))
            }
            Page.CANCEL_CONFIRM -> {
                val stack = cancelTickets.getOrNull(selectedCancelIdx) ?: run { page = Page.TOP; initGui(); return }
                val fare    = ItemCustomExpressTicket.getExpressFare(stack)
                val refund  = fare * 7 / 10 / 10 * 10
                add(GuiButton(0,  cx - 60, cy + 10, 120, 18, tlc("gui.kaisatsu.reserved.btn.do_cancel")))
                add(GuiButton(99, cx - 60, cy + 34, 120, 18, tlc("gui.kaisatsu.btn.back")))
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (page) {
            Page.TOP -> when (button.id) {
                0  -> { isReserved = true;  page = Page.DESTINATION; initGui() }
                1  -> { isReserved = false; page = Page.DESTINATION; initGui() }
                2  -> { page = Page.CANCEL; initGui() }
                99 -> mc.thePlayer.closeScreen()
            }
            Page.DESTINATION -> when {
                button.id == 99 -> { page = Page.TOP; initGui() }
                button.id in 100..199 -> {
                    val dests = getAvailableDestinations()
                    val idx = button.id - 100
                    if (idx < dests.size) {
                        selectedDestination = dests[idx]; page = Page.TRAIN; initGui()
                    }
                }
            }
            Page.TRAIN -> when {
                button.id == 99 -> { page = Page.DESTINATION; initGui() }
                button.id in 200..299 -> {
                    val availTrains = getTrainsForRoute()
                    val idx = button.id - 200
                    if (idx < availTrains.size) {
                        val train = availTrains[idx]
                        selectedTrainID = train.trainID; trainName = train.trainName; trainType = train.trainType
                        expressFare = if (isReserved) train.reservedFare else train.unreservedFare
                        if (isReserved) { page = Page.CAR; initGui() } else { selectedCarNumber = 0; page = Page.CONFIRM; initGui() }
                    }
                }
            }
            Page.CAR -> when {
                button.id == 99 -> { page = Page.TRAIN; initGui() }
                button.id in 300..399 -> {
                    val selectedTrain = trains.find { it.trainID == selectedTrainID }
                    val idx = button.id - 300
                    selectedTrain?.cars?.getOrNull(idx)?.let { car ->
                        selectedCarNumber = car.carNumber; page = Page.CONFIRM; initGui()
                    }
                }
            }
            Page.CONFIRM -> when (button.id) {
                50  -> { includeTicket = !includeTicket; initGui() }
                99  -> { page = if (isReserved) Page.CAR else Page.TRAIN; initGui() }
                0   -> {
                    KaizPatchNetwork.CHANNEL.sendToServer(
                        PacketPurchaseExpressTicket(
                            0, 0, 0, selectedTrainID, vendorStation, selectedDestination,
                            isReserved, selectedCarNumber, includeTicket
                        )
                    )
                    mc.thePlayer.closeScreen()
                }
            }
            Page.CANCEL -> when {
                button.id == 99 -> { page = Page.TOP; initGui() }
                button.id in 400..499 -> {
                    selectedCancelIdx = button.id - 400
                    page = Page.CANCEL_CONFIRM; initGui()
                }
            }
            Page.CANCEL_CONFIRM -> when (button.id) {
                99 -> { page = Page.CANCEL; initGui() }
                0  -> {
                    val stack = cancelTickets.getOrNull(selectedCancelIdx)
                    if (stack != null) {
                        KaizPatchNetwork.CHANNEL.sendToServer(
                            PacketCancelReservation(
                                ItemCustomExpressTicket.getTrainID(stack),
                                ItemCustomExpressTicket.getCarNumber(stack),
                                ItemCustomExpressTicket.getSeatNumber(stack)
                            )
                        )
                    }
                    mc.thePlayer.closeScreen()
                }
            }
        }
    }

    private fun getReservedTicketsInInventory(): List<ItemStack> {
        val inv = mc.thePlayer.inventory
        val result = mutableListOf<ItemStack>()
        for (i in 0 until inv.sizeInventory) {
            val s = inv.getStackInSlot(i) ?: continue
            if (s.item is ItemCustomExpressTicket && ItemCustomExpressTicket.isReserved(s))
                result.add(s)
        }
        return result
    }

    private fun getAvailableDestinations(): List<String> {
        val result = mutableSetOf<String>()
        trains.forEach { train ->
            val stationsIdx = train.stopStations.indexOf(vendorStation)
            if (stationsIdx >= 0) {
                train.stopStations.forEachIndexed { idx, st ->
                    if (idx != stationsIdx && st != vendorStation) result.add(st)
                }
            }
        }
        return result.sorted()
    }

    private fun getTrainsForRoute(): List<PacketOpenReservedVendor.TrainInfo> {
        return trains.filter { train ->
            val fromIdx = train.stopStations.indexOf(vendorStation)
            val toIdx   = train.stopStations.indexOf(selectedDestination)
            fromIdx >= 0 && toIdx >= 0 && fromIdx != toIdx
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2

        val tlc = StatCollector::translateToLocal
        when (page) {
            Page.TOP -> {
                drawCenteredString(fontRendererObj, "指定席券売機  $vendorStation", cx, cy - 46, 0xFFFFFF)
                drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.subtitle.select_type"), cx, cy - 34, 0xAAAAAA)
            }
            Page.DESTINATION -> {
                val typeStr = if (isReserved) tlc("gui.kaisatsu.reserved.seat.reserved") else tlc("gui.kaisatsu.reserved.seat.unreserved")
                drawCenteredString(fontRendererObj, "$typeStr  ${tlc("gui.kaisatsu.reserved.lbl.destination")}", cx, cy - 90, 0xFFFFFF)
                if (getAvailableDestinations().isEmpty())
                    drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.no_destination"), cx, cy, 0xFF5555)
            }
            Page.TRAIN -> {
                val typeStr = if (isReserved) tlc("gui.kaisatsu.reserved.seat.reserved") else tlc("gui.kaisatsu.reserved.seat.unreserved")
                drawCenteredString(fontRendererObj, "$typeStr  $vendorStation → $selectedDestination", cx, cy - 90, 0xFFFFFF)
                drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.lbl.select_train"), cx, cy - 76, 0xAAAAAA)
                if (getTrainsForRoute().isEmpty())
                    drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.no_train"), cx, cy, 0xFF5555)
            }
            Page.CAR -> {
                drawCenteredString(fontRendererObj, "$trainName  ${tlc("gui.kaisatsu.reserved.lbl.select_car")}", cx, cy - 90, 0xFFFFFF)
            }
            Page.CONFIRM -> {
                drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.confirm.title"), cx, cy - 80, 0xFFFFFF)
                val seatStr = if (isReserved) "${selectedCarNumber}号車 ${tlc("gui.kaisatsu.reserved.seat.reserved")}" else tlc("gui.kaisatsu.reserved.seat.unreserved")
                drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.train")} $trainName", cx - 120, cy - 60, 0xFFFFFF)
                drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.section")} $vendorStation → $selectedDestination", cx - 120, cy - 48, 0xFFFFFF)
                drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.seat")} $seatStr", cx - 120, cy - 36, 0xFFFFFF)
                drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.fare")} ${expressFare}円", cx - 120, cy - 24, 0x55FF55)
                drawString(fontRendererObj, tlc("gui.kaisatsu.reserved.ticket_needed"), cx - 120, cy - 12, 0xAAAAAA)
            }
            Page.CANCEL -> {
                drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.cancel.title"), cx, cy - 82, 0xFFFFFF)
                if (cancelTickets.isEmpty())
                    drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.no_ticket"), cx, cy, 0xFF5555)
            }
            Page.CANCEL_CONFIRM -> {
                val stack = cancelTickets.getOrNull(selectedCancelIdx)
                if (stack != null) {
                    val name   = ItemCustomExpressTicket.getTrainName(stack)
                    val car    = ItemCustomExpressTicket.getCarNumber(stack)
                    val seat   = ItemCustomExpressTicket.getSeatNumber(stack)
                    val from   = ItemCustomExpressTicket.getFromStation(stack)
                    val to     = ItemCustomExpressTicket.getToStation(stack)
                    val fare   = ItemCustomExpressTicket.getExpressFare(stack)
                    val refund = fare * 7 / 10 / 10 * 10
                    val fee    = fare - refund
                    drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.cancel_confirm.title"), cx, cy - 80, 0xFFFFFF)
                    drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.train")} $name", cx - 120, cy - 60, 0xFFFFFF)
                    drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.section")} $from → $to", cx - 120, cy - 48, 0xFFFFFF)
                    drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.seat")} ${car}号車 ${seat}番席 ${tlc("gui.kaisatsu.reserved.seat.reserved")}", cx - 120, cy - 36, 0xFFFFFF)
                    drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.fare")} ${fare}円", cx - 120, cy - 24, 0xAAAAAA)
                    drawString(fontRendererObj, "${tlc("gui.kaisatsu.reserved.lbl.refund")} ${refund}円 (手数料 ${fee}円)", cx - 120, cy - 12, 0x55FF55)
                    drawCenteredString(fontRendererObj, tlc("gui.kaisatsu.reserved.cancel.warning"), cx, cy + 2, 0xFF8888)
                }
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
}
