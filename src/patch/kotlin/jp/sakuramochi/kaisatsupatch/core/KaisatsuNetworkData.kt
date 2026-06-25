package jp.sakuramochi.kaisatsupatch.core

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.world.World
import net.minecraft.world.WorldSavedData
import net.minecraftforge.common.util.Constants
import java.io.File

class KaisatsuNetworkData(name: String) : WorldSavedData(name) {

    // 駅名 → 座標
    val globalStations: MutableMap<String, StationCoords> = mutableMapOf()
    // 路線ID → 路線データ
    val companyLines: MutableMap<String, LineData> = mutableMapOf()
    // 駅名 → 累計売上（円）
    val stationSales: MutableMap<String, Long> = mutableMapOf()
    // 駅名 → 品目別売上内訳
    val stationSalesDetail: MutableMap<String, SalesBreakdown> = mutableMapOf()
    // 列車ID → 列車データ
    val trainData: MutableMap<String, TrainData> = mutableMapOf()
    // "trainID:carNum:seatNum" → playerName
    val reservations: MutableMap<String, String> = mutableMapOf()
    // 駅名 → 改札通過ログ（新しい順、最大50件）
    val gateLog: MutableMap<String, MutableList<GateLogEntry>> = mutableMapOf()
    // 会社ID → 会社データ
    val companies: MutableMap<String, CompanyData> = mutableMapOf()
    // OuDia 時刻表ファイル名（config/kaizpatch/timetables/ 以下）
    var timetableFilePath: String = ""
    // 解析済みタイムテーブル（メモリキャッシュ、NBT未保存）
    var timetable: TimetableData? = null

    /** configDir = Minecraft の config/ フォルダ */
    fun loadTimetable(configDir: File): Boolean {
        if (timetableFilePath.isEmpty()) return false
        val file = File(configDir, "kaizpatch/timetables/$timetableFilePath")
        if (!file.exists()) return false
        return try {
            timetable = OuDiaParser.parse(file)
            true
        } catch (_: Exception) {
            false
        }
    }

    data class StationCoords(val x: Int, val y: Int, val z: Int)

    data class CompanyData(
        val companyID: String,
        var companyName: String,
        var color: Int,                                      // 0xRRGGBB
        var icCardName: String,                              // "メトロIC" など
        val members: MutableSet<String> = mutableSetOf(),   // 操作権限を持つプレイヤー名
        val allowedCompanies: MutableSet<String> = mutableSetOf(), // IC相互利用を許可する会社ID
        var defaultBaseFare: Int = 150,                     // 会社デフォルト初乗り運賃（路線未設定時のフォールバック）
        var defaultCostPerBlock: Double = 0.1               // 会社デフォルト加算レート
    ) {
        fun isMember(playerName: String): Boolean = members.contains(playerName)
    }

    data class GateLogEntry(
        val playerName: String,
        val stationName: String,
        val action: String,   // "入場" / "出場"
        val itemType: String, // "IC" / "切符" / "定期券" / "回数券" / "RTM切符"
        val timestamp: Long
    )

    data class SalesBreakdown(
        var ticket: Long = 0L,
        var ic: Long = 0L,
        var pass: Long = 0L,
        var express: Long = 0L
    ) {
        val total: Long get() = ticket + ic + pass + express
    }

    data class CarData(val carNumber: Int, val seatCount: Int, val carClass: String)

    data class TrainData(
        val trainID: String,
        val trainName: String,
        val trainType: String,
        val lineID: String,
        val stopStations: List<String>,
        val reservedFare: Int,
        val unreservedFare: Int,
        val cars: List<CarData>
    )

    data class LineData(
        var lineID: String,
        var lineName: String,
        var companyName: String,
        var baseFare: Int,
        var costPerBlock: Double,
        val stationOrder: MutableList<String> = mutableListOf(),
        var transferFee: Int = 0,
        var companyID: String = ""   // 所属会社ID（空の場合はcompanyNameで照合）
    )

    fun addGateLog(stationName: String, playerName: String, action: String, itemType: String) {
        val list = gateLog.getOrPut(stationName) { mutableListOf() }
        list.add(0, GateLogEntry(playerName, stationName, action, itemType, System.currentTimeMillis()))
        if (list.size > 50) list.subList(50, list.size).clear()
        markDirty()
    }

    companion object {
        private const val DATA_NAME = "KaizPatchNetworkData"

        fun get(world: World?): KaisatsuNetworkData? {
            if (world == null || world.isRemote) return null
            var inst = world.loadItemData(KaisatsuNetworkData::class.java, DATA_NAME) as? KaisatsuNetworkData
            if (inst == null) {
                inst = KaisatsuNetworkData(DATA_NAME)
                world.setItemData(DATA_NAME, inst)
            }
            return inst
        }
    }

    override fun readFromNBT(nbt: NBTTagCompound) {
        globalStations.clear()
        val stationList = nbt.getTagList("GlobalStations", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until stationList.tagCount()) {
            val s = stationList.getCompoundTagAt(i)
            globalStations[s.getString("Name")] = StationCoords(s.getInteger("X"), s.getInteger("Y"), s.getInteger("Z"))
        }

        stationSales.clear()
        val salesList = nbt.getTagList("StationSales", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until salesList.tagCount()) {
            val s = salesList.getCompoundTagAt(i)
            stationSales[s.getString("Name")] = s.getLong("Sales")
        }

        stationSalesDetail.clear()
        val detailList = nbt.getTagList("StationSalesDetail", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until detailList.tagCount()) {
            val d = detailList.getCompoundTagAt(i)
            stationSalesDetail[d.getString("Name")] = SalesBreakdown(
                ticket  = d.getLong("Ticket"),
                ic      = d.getLong("IC"),
                pass    = d.getLong("Pass"),
                express = d.getLong("Express")
            )
        }

        trainData.clear()
        val trainList = nbt.getTagList("TrainData", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until trainList.tagCount()) {
            val t = trainList.getCompoundTagAt(i)
            val stopList = t.getTagList("StopStations", Constants.NBT.TAG_STRING)
            val stops = (0 until stopList.tagCount()).map { stopList.getStringTagAt(it) }
            val carList = t.getTagList("Cars", Constants.NBT.TAG_COMPOUND)
            val cars = (0 until carList.tagCount()).map { ci ->
                val c = carList.getCompoundTagAt(ci)
                CarData(c.getInteger("CarNumber"), c.getInteger("SeatCount"), c.getString("CarClass"))
            }
            val train = TrainData(
                trainID = t.getString("TrainID"),
                trainName = t.getString("TrainName"),
                trainType = t.getString("TrainType"),
                lineID = t.getString("LineID"),
                stopStations = stops,
                reservedFare = t.getInteger("ReservedFare"),
                unreservedFare = t.getInteger("UnreservedFare"),
                cars = cars
            )
            trainData[train.trainID] = train
        }

        reservations.clear()
        val resvList = nbt.getTagList("Reservations", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until resvList.tagCount()) {
            val r = resvList.getCompoundTagAt(i)
            reservations[r.getString("Key")] = r.getString("Player")
        }

        companies.clear()
        val companyList = nbt.getTagList("Companies", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until companyList.tagCount()) {
            val c = companyList.getCompoundTagAt(i)
            val id = c.getString("CompanyID")
            val co = CompanyData(
                id, c.getString("CompanyName"), c.getInteger("Color"), c.getString("ICCardName"),
                defaultBaseFare = c.getInteger("DefaultBaseFare").let { if (it == 0) 150 else it },
                defaultCostPerBlock = c.getDouble("DefaultCostPerBlock").let { if (it == 0.0) 0.1 else it }
            )
            val memberList = c.getTagList("Members", Constants.NBT.TAG_STRING)
            for (j in 0 until memberList.tagCount()) co.members.add(memberList.getStringTagAt(j))
            val mutualList = c.getTagList("AllowedCompanies", Constants.NBT.TAG_STRING)
            for (j in 0 until mutualList.tagCount()) co.allowedCompanies.add(mutualList.getStringTagAt(j))
            companies[id] = co
        }

        gateLog.clear()
        val gateLogList = nbt.getTagList("GateLog", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until gateLogList.tagCount()) {
            val e = gateLogList.getCompoundTagAt(i)
            val station = e.getString("Station")
            gateLog.getOrPut(station) { mutableListOf() }.add(
                GateLogEntry(e.getString("Player"), station, e.getString("Action"), e.getString("ItemType"), e.getLong("Time"))
            )
        }

        timetableFilePath = nbt.getString("TimetableFilePath")

        companyLines.clear()
        val lineList = nbt.getTagList("CompanyLines", Constants.NBT.TAG_COMPOUND)
        for (i in 0 until lineList.tagCount()) {
            val l = lineList.getCompoundTagAt(i)
            val line = LineData(
                lineID      = l.getString("LineID"),
                lineName    = l.getString("LineName"),
                companyName = l.getString("CompanyName"),
                baseFare    = l.getInteger("BaseFare"),
                costPerBlock = l.getDouble("CostPerBlock"),
                transferFee = l.getInteger("TransferFee"),
                companyID   = l.getString("CompanyID")
            )
            val order = l.getTagList("StationOrder", Constants.NBT.TAG_STRING)
            for (j in 0 until order.tagCount()) line.stationOrder.add(order.getStringTagAt(j))
            companyLines[line.lineID] = line
        }
    }

    override fun writeToNBT(nbt: NBTTagCompound) {
        val stationList = NBTTagList()
        globalStations.forEach { (name, coords) ->
            NBTTagCompound().also {
                it.setString("Name", name)
                it.setInteger("X", coords.x)
                it.setInteger("Y", coords.y)
                it.setInteger("Z", coords.z)
                stationList.appendTag(it)
            }
        }
        nbt.setTag("GlobalStations", stationList)

        val salesList = NBTTagList()
        stationSales.forEach { (name, sales) ->
            NBTTagCompound().also {
                it.setString("Name", name)
                it.setLong("Sales", sales)
                salesList.appendTag(it)
            }
        }
        nbt.setTag("StationSales", salesList)

        val detailNBTList = NBTTagList()
        stationSalesDetail.forEach { (name, bd) ->
            NBTTagCompound().also {
                it.setString("Name", name)
                it.setLong("Ticket",  bd.ticket)
                it.setLong("IC",      bd.ic)
                it.setLong("Pass",    bd.pass)
                it.setLong("Express", bd.express)
                detailNBTList.appendTag(it)
            }
        }
        nbt.setTag("StationSalesDetail", detailNBTList)

        val lineList = NBTTagList()
        companyLines.values.forEach { line ->
            NBTTagCompound().also {
                it.setString("LineID", line.lineID)
                it.setString("LineName", line.lineName)
                it.setString("CompanyName", line.companyName)
                it.setInteger("BaseFare", line.baseFare)
                it.setDouble("CostPerBlock", line.costPerBlock)
                it.setInteger("TransferFee", line.transferFee)
                it.setString("CompanyID", line.companyID)
                val order = NBTTagList()
                line.stationOrder.forEach { st -> order.appendTag(NBTTagString(st)) }
                it.setTag("StationOrder", order)
                lineList.appendTag(it)
            }
        }
        nbt.setTag("CompanyLines", lineList)

        val trainNBTList = NBTTagList()
        trainData.values.forEach { train ->
            NBTTagCompound().also { t ->
                t.setString("TrainID", train.trainID)
                t.setString("TrainName", train.trainName)
                t.setString("TrainType", train.trainType)
                t.setString("LineID", train.lineID)
                t.setInteger("ReservedFare", train.reservedFare)
                t.setInteger("UnreservedFare", train.unreservedFare)
                val stopList = NBTTagList()
                train.stopStations.forEach { st -> stopList.appendTag(NBTTagString(st)) }
                t.setTag("StopStations", stopList)
                val carList = NBTTagList()
                train.cars.forEach { car ->
                    NBTTagCompound().also { c ->
                        c.setInteger("CarNumber", car.carNumber)
                        c.setInteger("SeatCount", car.seatCount)
                        c.setString("CarClass", car.carClass)
                        carList.appendTag(c)
                    }
                }
                t.setTag("Cars", carList)
                trainNBTList.appendTag(t)
            }
        }
        nbt.setTag("TrainData", trainNBTList)

        val resvNBTList = NBTTagList()
        reservations.forEach { (key, player) ->
            NBTTagCompound().also {
                it.setString("Key", key)
                it.setString("Player", player)
                resvNBTList.appendTag(it)
            }
        }
        nbt.setTag("Reservations", resvNBTList)

        val companyNBTList = NBTTagList()
        companies.values.forEach { c ->
            NBTTagCompound().also {
                it.setString("CompanyID", c.companyID)
                it.setString("CompanyName", c.companyName)
                it.setInteger("Color", c.color)
                it.setString("ICCardName", c.icCardName)
                it.setInteger("DefaultBaseFare", c.defaultBaseFare)
                it.setDouble("DefaultCostPerBlock", c.defaultCostPerBlock)
                val memberList = NBTTagList()
                c.members.forEach { m -> memberList.appendTag(NBTTagString(m)) }
                it.setTag("Members", memberList)
                val mutualList = NBTTagList()
                c.allowedCompanies.forEach { m -> mutualList.appendTag(NBTTagString(m)) }
                it.setTag("AllowedCompanies", mutualList)
                companyNBTList.appendTag(it)
            }
        }
        nbt.setTag("Companies", companyNBTList)

        val gateLogNBTList = NBTTagList()
        gateLog.forEach { (_, entries) ->
            entries.forEach { entry ->
                NBTTagCompound().also {
                    it.setString("Station", entry.stationName)
                    it.setString("Player", entry.playerName)
                    it.setString("Action", entry.action)
                    it.setString("ItemType", entry.itemType)
                    it.setLong("Time", entry.timestamp)
                    gateLogNBTList.appendTag(it)
                }
            }
        }
        nbt.setTag("GateLog", gateLogNBTList)
        nbt.setString("TimetableFilePath", timetableFilePath)
    }
}
