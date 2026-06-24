package jp.sakuramochi.kaisatsupatch.core

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.world.World
import net.minecraft.world.WorldSavedData
import net.minecraftforge.common.util.Constants

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

    data class StationCoords(val x: Int, val y: Int, val z: Int)

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
        // Phase3で乗換料金として使用予定（現在は常に0）
        var transferFee: Int = 0
    )

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
                transferFee = l.getInteger("TransferFee")
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
    }
}
