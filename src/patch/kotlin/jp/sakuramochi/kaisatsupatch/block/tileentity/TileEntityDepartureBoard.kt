package jp.sakuramochi.kaisatsupatch.block.tileentity

import io.netty.buffer.Unpooled
import jp.sakuramochi.kaisatsupatch.core.DepartureRow
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.Constants

class TileEntityDepartureBoard : TileEntity() {

    // 設定
    var stationName  = ""
    var lineID       = ""   // 路線フィルター（空 = フィルターなし）
    var platform     = ""   // 番線表示（空 = 非表示）
    var diaName      = ""
    var direction    = "両方"
    var displayRows  = 5
    var title        = ""
    var timeMode     = "real"   // "real" = 現実時刻, "game" = ゲーム内時刻

    // サーバー計算 → クライアントに同期される発車情報
    var cachedDepartures: List<DepartureRow> = emptyList()

    private var tickCounter = 0

    override fun canUpdate() = true

    override fun updateEntity() {
        if (worldObj == null || worldObj.isRemote) return
        tickCounter++
        if (tickCounter >= 200) {   // 10秒ごとに更新
            tickCounter = 0
            recomputeDepartures()
        }
    }

    fun recomputeDepartures() {
        val data = KaisatsuNetworkData.get(worldObj) ?: return
        val tt = data.timetable ?: return
        val nowMin = if (timeMode == "game") {
            ((worldObj.worldTime % 24000L) * 1440L / 24000L + 360L).toInt() % 1440
        } else {
            val now = java.time.LocalTime.now()
            now.hour * 60 + now.minute
        }
        val lineStations = if (lineID.isNotEmpty())
            data.companyLines[lineID]?.stationOrder?.toSet() else null
        cachedDepartures = tt.getNextDepartures(stationName, diaName, direction, nowMin, displayRows, lineStations)
        worldObj?.markBlockForUpdate(xCoord, yCoord, zCoord)
    }

    // ── NBT ──────────────────────────────────────────────────────────

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationName",  stationName)
        tag.setString("LineID",       lineID)
        tag.setString("Platform",     platform)
        tag.setString("DiaName",      diaName)
        tag.setString("Direction",    direction)
        tag.setInteger("DisplayRows", displayRows)
        tag.setString("Title",        title)
        tag.setString("TimeMode",     timeMode)

        val list = NBTTagList()
        cachedDepartures.forEach { row ->
            NBTTagCompound().also {
                it.setString("Time",   row.time)
                it.setString("Dest",   row.destination)
                it.setString("Type",   row.typeName)
                it.setString("Num",    row.trainNumber)
                it.setString("Name",   row.trainName)
                list.appendTag(it)
            }
        }
        tag.setTag("Departures", list)
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationName  = tag.getString("StationName")
        lineID       = tag.getString("LineID")
        platform     = tag.getString("Platform")
        diaName      = tag.getString("DiaName")
        direction    = tag.getString("Direction").ifEmpty { "両方" }
        displayRows  = tag.getInteger("DisplayRows").let { if (it == 0) 5 else it }
        title        = tag.getString("Title")
        timeMode     = tag.getString("TimeMode").ifEmpty { "real" }

        val list = tag.getTagList("Departures", Constants.NBT.TAG_COMPOUND)
        cachedDepartures = (0 until list.tagCount()).map { i ->
            val d = list.getCompoundTagAt(i)
            DepartureRow(d.getString("Time"), d.getString("Dest"), d.getString("Type"), d.getString("Num"), d.getString("Name"))
        }
    }

    // ── クライアント同期 ──────────────────────────────────────────────

    override fun getDescriptionPacket(): Packet {
        val tag = NBTTagCompound()
        writeToNBT(tag)
        return S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag)
    }

    override fun onDataPacket(mgr: NetworkManager, pkt: S35PacketUpdateTileEntity) {
        readFromNBT(pkt.func_148857_g())
    }
}
