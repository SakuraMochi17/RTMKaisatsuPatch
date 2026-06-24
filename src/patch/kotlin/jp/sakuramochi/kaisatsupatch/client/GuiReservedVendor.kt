package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenReservedVendor
import jp.sakuramochi.kaisatsupatch.network.PacketPurchaseExpressTicket
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen

@SideOnly(Side.CLIENT)
class GuiReservedVendor(
    private val vendorStation: String,
    private val trains: List<PacketOpenReservedVendor.TrainInfo>,
    private val lines: List<PacketOpenReservedVendor.LineInfo>
) : GuiScreen() {

    private enum class Page { TOP, DESTINATION, TRAIN, CAR, CONFIRM }

    private var page = Page.TOP
    private var isReserved = true
    private var selectedDestination = ""
    private var selectedTrainID = ""
    private var selectedCarNumber = 0
    private var includeTicket = false

    // 計算済み購入金額キャッシュ（CONFIRM表示用）
    private var expressFare = 0
    private var trainName = ""
    private var trainType = ""

    override fun initGui() {
        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        when (page) {
            Page.TOP -> {
                add(GuiButton(0,  cx - 60, cy - 10, 120, 20, "指定席を買う"))
                add(GuiButton(1,  cx - 60, cy + 14, 120, 20, "自由席を買う"))
                add(GuiButton(99, cx - 60, cy + 38, 120, 20, "閉じる"))
            }
            Page.DESTINATION -> {
                val destinations = getAvailableDestinations()
                destinations.forEachIndexed { i, dest ->
                    val row = i / 2; val col = i % 2
                    add(GuiButton(100 + i, cx - 90 + col * 90, cy - 70 + row * 22, 86, 18, dest))
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, "← 戻る"))
            }
            Page.TRAIN -> {
                val availableTrains = getTrainsForRoute()
                availableTrains.forEachIndexed { i, train ->
                    val totalAvail = if (isReserved) train.cars.sumOf { it.availableSeats } else Int.MAX_VALUE
                    val isFull = isReserved && totalAvail == 0
                    val fare = if (isReserved) train.reservedFare else train.unreservedFare
                    val label = "[${train.trainType}] ${train.trainName} ${fare}円${if (isFull) " (満席)" else ""}"
                    add(GuiButton(200 + i, cx - 110, cy - 70 + i * 22, 220, 18, label).also { it.enabled = !isFull })
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, "← 戻る"))
            }
            Page.CAR -> {
                val selectedTrain = trains.find { it.trainID == selectedTrainID }
                selectedTrain?.cars?.forEachIndexed { i, car ->
                    val isFull = car.availableSeats == 0
                    val label = "${car.carNumber}号車 [${car.carClass}] 残席${car.availableSeats}${if (isFull) " (満席)" else ""}"
                    add(GuiButton(300 + i, cx - 110, cy - 70 + i * 22, 220, 18, label).also { it.enabled = !isFull })
                }
                add(GuiButton(99, cx - 60, cy + 70, 120, 18, "← 戻る"))
            }
            Page.CONFIRM -> {
                add(GuiButton(50, cx - 120, cy + 10, 240, 18,
                    if (includeTicket) "[✓] 乗車券もセットで購入" else "[ ] 乗車券もセットで購入"))
                add(GuiButton(0,  cx - 60, cy + 34, 120, 18, "購入"))
                add(GuiButton(99, cx - 60, cy + 56, 120, 18, "← 戻る"))
            }
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when (page) {
            Page.TOP -> {
                when (button.id) {
                    0  -> { isReserved = true;  page = Page.DESTINATION; initGui() }
                    1  -> { isReserved = false; page = Page.DESTINATION; initGui() }
                    99 -> mc.thePlayer.closeScreen()
                }
            }
            Page.DESTINATION -> {
                when {
                    button.id == 99 -> { page = Page.TOP; initGui() }
                    button.id in 100..199 -> {
                        val dests = getAvailableDestinations()
                        val idx = button.id - 100
                        if (idx < dests.size) {
                            selectedDestination = dests[idx]
                            page = if (isReserved) Page.TRAIN else Page.TRAIN
                            initGui()
                        }
                    }
                }
            }
            Page.TRAIN -> {
                when {
                    button.id == 99 -> { page = Page.DESTINATION; initGui() }
                    button.id in 200..299 -> {
                        val availTrains = getTrainsForRoute()
                        val idx = button.id - 200
                        if (idx < availTrains.size) {
                            val train = availTrains[idx]
                            selectedTrainID = train.trainID
                            trainName = train.trainName
                            trainType = train.trainType
                            expressFare = if (isReserved) train.reservedFare else train.unreservedFare
                            if (isReserved) {
                                page = Page.CAR; initGui()
                            } else {
                                selectedCarNumber = 0
                                page = Page.CONFIRM; initGui()
                            }
                        }
                    }
                }
            }
            Page.CAR -> {
                when {
                    button.id == 99 -> { page = Page.TRAIN; initGui() }
                    button.id in 300..399 -> {
                        val selectedTrain = trains.find { it.trainID == selectedTrainID }
                        val idx = button.id - 300
                        selectedTrain?.cars?.getOrNull(idx)?.let { car ->
                            selectedCarNumber = car.carNumber
                            page = Page.CONFIRM; initGui()
                        }
                    }
                }
            }
            Page.CONFIRM -> {
                when (button.id) {
                    50  -> { includeTicket = !includeTicket; initGui() }
                    99  -> {
                        page = if (isReserved) Page.CAR else Page.TRAIN
                        initGui()
                    }
                    0   -> {
                        KaizPatchNetwork.CHANNEL.sendToServer(
                            PacketPurchaseExpressTicket(
                                0, 0, 0, // vendorX/Y/Z は不要（インベントリ支払いのため）
                                selectedTrainID,
                                vendorStation,
                                selectedDestination,
                                isReserved,
                                selectedCarNumber,
                                includeTicket
                            )
                        )
                        mc.thePlayer.closeScreen()
                    }
                }
            }
        }
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

        when (page) {
            Page.TOP -> {
                drawCenteredString(fontRendererObj, "指定席券売機  $vendorStation", cx, cy - 50, 0xFFFFFF)
                drawCenteredString(fontRendererObj, "種別を選択してください", cx, cy - 30, 0xAAAAAA)
            }
            Page.DESTINATION -> {
                val typeStr = if (isReserved) "指定席" else "自由席"
                drawCenteredString(fontRendererObj, "$typeStr  行き先を選択", cx, cy - 90, 0xFFFFFF)
                if (getAvailableDestinations().isEmpty()) {
                    drawCenteredString(fontRendererObj, "行き先がありません", cx, cy, 0xFF5555)
                }
            }
            Page.TRAIN -> {
                val typeStr = if (isReserved) "指定席" else "自由席"
                drawCenteredString(fontRendererObj, "$typeStr  $vendorStation → $selectedDestination", cx, cy - 90, 0xFFFFFF)
                drawCenteredString(fontRendererObj, "列車を選択", cx, cy - 76, 0xAAAAAA)
                if (getTrainsForRoute().isEmpty()) {
                    drawCenteredString(fontRendererObj, "利用可能な列車がありません", cx, cy, 0xFF5555)
                }
            }
            Page.CAR -> {
                drawCenteredString(fontRendererObj, "$trainName  号車を選択", cx, cy - 90, 0xFFFFFF)
            }
            Page.CONFIRM -> {
                drawCenteredString(fontRendererObj, "購入確認", cx, cy - 80, 0xFFFFFF)
                val seatStr = if (isReserved) "${selectedCarNumber}号車 (指定席)" else "自由席"
                drawString(fontRendererObj, "列車: $trainName", cx - 120, cy - 60, 0xFFFFFF)
                drawString(fontRendererObj, "区間: $vendorStation → $selectedDestination", cx - 120, cy - 48, 0xFFFFFF)
                drawString(fontRendererObj, "席: $seatStr", cx - 120, cy - 36, 0xFFFFFF)
                drawString(fontRendererObj, "特急料金: ${expressFare}円", cx - 120, cy - 24, 0x55FF55)
                drawString(fontRendererObj, "（乗車券は別途必要です）", cx - 120, cy - 12, 0xAAAAAA)
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
}
