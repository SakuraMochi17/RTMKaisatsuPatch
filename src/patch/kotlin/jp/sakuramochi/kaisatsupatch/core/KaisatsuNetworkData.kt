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

    data class StationCoords(val x: Int, val y: Int, val z: Int)

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
    }
}
