package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenTrainManager
import jp.sakuramochi.kaisatsupatch.network.PacketTrainUpdate
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard

@SideOnly(Side.CLIENT)
class GuiTrainManager(private val msg: PacketOpenTrainManager) : GuiScreen() {

    private var page = 0
    private var selectedTrainType = "特急"
    private var selectedLineIndex = 0
    private var selectedStations = mutableListOf<String>()
    // carNumber, seatCount, carClass
    private var cars = mutableListOf<Triple<Int, Int, String>>()

    private lateinit var trainIDField: GuiTextField
    private lateinit var trainNameField: GuiTextField
    private lateinit var reservedFareField: GuiTextField
    private lateinit var unreservedFareField: GuiTextField

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        @Suppress("UNCHECKED_CAST")
        (buttonList as MutableList<GuiButton>).clear()
        val cx = width / 2; val cy = height / 2

        when (page) {
            0 -> initPage0(cx, cy)
            1 -> initPage1(cx, cy)
            2 -> initPage2(cx, cy)
        }
    }

    private fun initPage0(cx: Int, cy: Int) {
        trainIDField = GuiTextField(fontRendererObj, cx - 100, cy - 70, 90, 15)
        trainNameField = GuiTextField(fontRendererObj, cx + 10, cy - 70, 90, 15)
        reservedFareField = GuiTextField(fontRendererObj, cx - 100, cy - 40, 60, 15)
        unreservedFareField = GuiTextField(fontRendererObj, cx + 10, cy - 40, 60, 15)
        trainIDField.maxStringLength = 20
        trainNameField.maxStringLength = 30
        reservedFareField.maxStringLength = 6
        unreservedFareField.maxStringLength = 6

        val existing = msg.train
        if (existing != null) {
            trainIDField.text = existing.trainID
            trainNameField.text = existing.trainName
            reservedFareField.text = existing.reservedFare.toString()
            unreservedFareField.text = existing.unreservedFare.toString()
            selectedTrainType = existing.trainType
            val lineIdx = msg.lines.indexOfFirst { it.lineID == existing.lineID }
            if (lineIdx >= 0) selectedLineIndex = lineIdx
        } else {
            reservedFareField.text = "0"
            unreservedFareField.text = "0"
        }

        // 列車種別ボタン
        add(GuiButton(100, cx - 120, cy - 12, 60, 18, "新幹線").also { it.enabled = selectedTrainType != "新幹線" })
        add(GuiButton(101, cx - 55,  cy - 12, 60, 18, "特急").also  { it.enabled = selectedTrainType != "特急" })
        add(GuiButton(102, cx + 10,  cy - 12, 60, 18, "急行").also  { it.enabled = selectedTrainType != "急行" })

        // 路線選択
        add(GuiButton(10, cx - 120, cy + 16, 20, 18, "<"))
        add(GuiButton(11, cx + 100, cy + 16, 20, 18, ">"))

        // ナビゲーション
        add(GuiButton(20, cx - 55, cy + 60, 110, 18, "次へ: 停車駅 →"))
        add(GuiButton(21, cx - 55, cy + 82, 110, 18, "削除").also { it.enabled = msg.hasTrain })
        add(GuiButton(22, cx - 55, cy + 104, 110, 18, "閉じる"))
    }

    private fun initPage1(cx: Int, cy: Int) {
        val lineStations = if (msg.lines.isNotEmpty()) msg.lines[selectedLineIndex].stations else emptyList()
        lineStations.forEachIndexed { i, station ->
            val row = i / 3; val col = i % 3
            val btnX = cx - 120 + col * 82
            val btnY = cy - 80 + row * 22
            val isSelected = selectedStations.contains(station)
            add(GuiButton(200 + i, btnX, btnY, 78, 18,
                if (isSelected) "✓ $station" else station
            ).also { it.enabled = !isSelected })
        }
        add(GuiButton(20, cx - 120, cy + 80, 110, 18, "← 戻る"))
        add(GuiButton(21, cx + 10,  cy + 80, 110, 18, "次へ: 号車設定 →"))
    }

    private fun initPage2(cx: Int, cy: Int) {
        cars.forEachIndexed { i, (_, _, carClass) ->
            val baseY = cy - 80 + i * 22
            add(GuiButton(300 + i, cx - 120, baseY, 60, 18,
                if (carClass == "グリーン") "グリーン" else "普通"
            ))
            add(GuiButton(400 + i * 2,     cx - 55,  baseY, 24, 18, "+"))
            add(GuiButton(400 + i * 2 + 1, cx - 28,  baseY, 24, 18, "-"))
        }

        add(GuiButton(30, cx - 120, cy + 60, 110, 18, "+ 号車追加"))
        add(GuiButton(31, cx + 10,  cy + 60, 110, 18, "- 号車削除").also { it.enabled = cars.isNotEmpty() })
        add(GuiButton(20, cx - 55,  cy + 82, 110, 18, "← 戻る"))
        add(GuiButton(0,  cx - 55,  cy + 104, 110, 18, "保存して終了"))
    }

    override fun onGuiClosed() { Keyboard.enableRepeatEvents(false) }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (page == 0) {
            if (::trainIDField.isInitialized && trainIDField.textboxKeyTyped(typedChar, keyCode)) return
            if (::trainNameField.isInitialized && trainNameField.textboxKeyTyped(typedChar, keyCode)) return
            if (::reservedFareField.isInitialized && reservedFareField.textboxKeyTyped(typedChar, keyCode)) return
            if (::unreservedFareField.isInitialized && unreservedFareField.textboxKeyTyped(typedChar, keyCode)) return
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        if (page == 0) {
            if (::trainIDField.isInitialized) trainIDField.mouseClicked(mouseX, mouseY, mouseButton)
            if (::trainNameField.isInitialized) trainNameField.mouseClicked(mouseX, mouseY, mouseButton)
            if (::reservedFareField.isInitialized) reservedFareField.mouseClicked(mouseX, mouseY, mouseButton)
            if (::unreservedFareField.isInitialized) unreservedFareField.mouseClicked(mouseX, mouseY, mouseButton)
        }
    }

    override fun actionPerformed(button: GuiButton) {
        when {
            page == 0 -> handlePage0(button)
            page == 1 -> handlePage1(button)
            page == 2 -> handlePage2(button)
        }
    }

    private fun handlePage0(button: GuiButton) {
        when (button.id) {
            100 -> { selectedTrainType = "新幹線"; initGui() }
            101 -> { selectedTrainType = "特急"; initGui() }
            102 -> { selectedTrainType = "急行"; initGui() }
            10  -> {
                if (msg.lines.isNotEmpty())
                    selectedLineIndex = (selectedLineIndex - 1 + msg.lines.size) % msg.lines.size
            }
            11  -> {
                if (msg.lines.isNotEmpty())
                    selectedLineIndex = (selectedLineIndex + 1) % msg.lines.size
            }
            20  -> {
                // 停車駅ページへ進む前に既存選択を初期化
                if (selectedStations.isEmpty() && msg.train != null) {
                    selectedStations = msg.train!!.stopStations.toMutableList()
                }
                page = 1; initGui()
            }
            21  -> {
                // 削除
                KaizPatchNetwork.CHANNEL.sendToServer(PacketTrainUpdate().also {
                    it.x = msg.x; it.y = msg.y; it.z = msg.z
                    it.delete = true
                    it.trainID = msg.train?.trainID ?: ""
                })
                mc.thePlayer.closeScreen()
            }
            22  -> mc.thePlayer.closeScreen()
        }
    }

    private fun handlePage1(button: GuiButton) {
        when {
            button.id in 200..299 -> {
                val lineStations = if (msg.lines.isNotEmpty()) msg.lines[selectedLineIndex].stations else emptyList()
                val idx = button.id - 200
                if (idx < lineStations.size) {
                    val station = lineStations[idx]
                    if (!selectedStations.contains(station)) selectedStations.add(station)
                    initGui()
                }
            }
            button.id == 20 -> { page = 0; initGui() }
            button.id == 21 -> {
                // 号車設定ページへ進む前に既存号車を初期化
                if (cars.isEmpty() && msg.train != null) {
                    cars = msg.train!!.cars.map { Triple(it.carNumber, it.seatCount, it.carClass) }.toMutableList()
                }
                page = 2; initGui()
            }
        }
    }

    private fun handlePage2(button: GuiButton) {
        when {
            button.id in 300..399 -> {
                val idx = button.id - 300
                if (idx < cars.size) {
                    val (num, count, cls) = cars[idx]
                    cars[idx] = Triple(num, count, if (cls == "グリーン") "普通" else "グリーン")
                    initGui()
                }
            }
            button.id in 400..499 -> {
                val carIdx = (button.id - 400) / 2
                val isPlusBtn = (button.id - 400) % 2 == 0
                if (carIdx < cars.size) {
                    val (num, count, cls) = cars[carIdx]
                    val newCount = if (isPlusBtn) count + 1 else maxOf(1, count - 1)
                    cars[carIdx] = Triple(num, newCount, cls)
                    initGui()
                }
            }
            button.id == 30 -> {
                val nextNum = (cars.maxOfOrNull { it.first } ?: 0) + 1
                cars.add(Triple(nextNum, 10, "普通"))
                initGui()
            }
            button.id == 31 -> {
                if (cars.isNotEmpty()) { cars.removeAt(cars.size - 1); initGui() }
            }
            button.id == 20 -> { page = 1; initGui() }
            button.id == 0  -> {
                val trainID = if (::trainIDField.isInitialized) trainIDField.text.trim() else msg.train?.trainID ?: ""
                val trainName = if (::trainNameField.isInitialized) trainNameField.text.trim() else msg.train?.trainName ?: ""
                val reservedFare = if (::reservedFareField.isInitialized) reservedFareField.text.toIntOrNull() ?: 0 else 0
                val unreservedFare = if (::unreservedFareField.isInitialized) unreservedFareField.text.toIntOrNull() ?: 0 else 0
                if (trainID.isEmpty() || trainName.isEmpty()) return
                val lineID = if (msg.lines.isNotEmpty()) msg.lines[selectedLineIndex].lineID else ""

                val pkt = PacketTrainUpdate().also {
                    it.x = msg.x; it.y = msg.y; it.z = msg.z
                    it.delete = false
                    it.trainID = trainID
                    it.trainName = trainName
                    it.trainType = selectedTrainType
                    it.lineID = lineID
                    it.reservedFare = reservedFare
                    it.unreservedFare = unreservedFare
                    it.stopStations = selectedStations.toList()
                    it.cars = cars.toList()
                }
                KaizPatchNetwork.CHANNEL.sendToServer(pkt)
                mc.thePlayer.closeScreen()
            }
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        val cx = width / 2; val cy = height / 2

        when (page) {
            0 -> {
                drawCenteredString(fontRendererObj, "列車管理ブロック 設定", cx, cy - 100, 0xFFFFFF)
                if (::trainIDField.isInitialized) {
                    drawString(fontRendererObj, "列車ID", cx - 100, cy - 85, 0xAAAAAA)
                    drawString(fontRendererObj, "列車名", cx + 10, cy - 85, 0xAAAAAA)
                    drawString(fontRendererObj, "指定席料金", cx - 100, cy - 55, 0xAAAAAA)
                    drawString(fontRendererObj, "自由席料金", cx + 10, cy - 55, 0xAAAAAA)
                    drawString(fontRendererObj, "種別:", cx - 120, cy - 25, 0xAAAAAA)
                    trainIDField.drawTextBox()
                    trainNameField.drawTextBox()
                    reservedFareField.drawTextBox()
                    unreservedFareField.drawTextBox()

                    drawString(fontRendererObj, "路線: ${if (msg.lines.isNotEmpty()) msg.lines[selectedLineIndex].lineName else "なし"}", cx - 95, cy + 22, 0xFFFF55)
                }
            }
            1 -> {
                drawCenteredString(fontRendererObj, "停車駅を選択 (路線: ${if (msg.lines.isNotEmpty()) msg.lines[selectedLineIndex].lineName else "なし"})", cx, cy - 100, 0xFFFF55)
                drawString(fontRendererObj, "選択済み: ${selectedStations.joinToString(", ").take(60)}", cx - 120, cy + 60, 0x55FF55)
            }
            2 -> {
                drawCenteredString(fontRendererObj, "号車設定", cx, cy - 100, 0xFFFF55)
                cars.forEachIndexed { i, (num, count, _) ->
                    val baseY = cy - 80 + i * 22
                    drawString(fontRendererObj, "${num}号車: ${count}席", cx + 5, baseY + 4, 0xFFFFFF)
                }
            }
        }
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame() = false

    @Suppress("UNCHECKED_CAST")
    private fun add(btn: GuiButton) = (buttonList as MutableList<GuiButton>).add(btn)
}
