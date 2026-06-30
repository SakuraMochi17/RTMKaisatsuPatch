package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.sakuramochi.kaisatsupatch.core.DepartureRow
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.Constants

/**
 * 発車標（表示体）の TileEntity。
 *
 * この板に常時表示する静的情報（路線名・方面・番線・路線カラー）と、表示の絞り込み
 * （方向・表示行数）を保持する。バインドした設定ブロック [TileEntityDepartureSettings]
 * の駅・ダイヤ・時刻モードを使い、自身の方向・行数で発車情報を算出する。
 */
class TileEntityDepartureBoard : TileEntity() {

    // ── バインド先の設定ブロック座標（boundY < 0 = 未バインド） ──────
    var boundX = 0
    var boundY = -1
    var boundZ = 0

    val isBound: Boolean get() = boundY >= 0

    // ── この板固有の静的表示情報 ─────────────────────────────────────
    var headerLine      = ""        // 路線名（例: 常磐線(快速)）
    var headerDirection = ""        // 方面（例: 上野・東京・品川方面）
    var platform        = ""        // 番線
    var lineColorHex    = DEFAULT_LINE_COLOR   // 路線カラー帯（0xRRGGBB）
    var sampleMode      = false     // サンプル表示

    // ── 表示の絞り込み ───────────────────────────────────────────────
    var direction   = "両方"        // 両方 / 下り / 上り
    var displayRows  = 3

    // ── サーバー計算 → クライアント同期 ─────────────────────────────
    var cachedDepartures: List<DepartureRow> = emptyList()

    private var tickCounter = 0

    fun bindTo(x: Int, y: Int, z: Int) {
        boundX = x; boundY = y; boundZ = z
        markDirty()
        recomputeDepartures()
        worldObj?.markBlockForUpdate(xCoord, yCoord, zCoord)
    }

    fun boundSettings(): TileEntityDepartureSettings? {
        if (!isBound || worldObj == null) return null
        return worldObj.getTileEntity(boundX, boundY, boundZ) as? TileEntityDepartureSettings
    }

    fun departures(): List<DepartureRow> = cachedDepartures

    /** ヘッダーに出す路線名: 板の路線名 > バインド先の駅名 */
    fun headerTitle(): String {
        if (headerLine.isNotEmpty()) return headerLine
        return boundSettings()?.stationName ?: ""
    }

    fun timeMode(): String = boundSettings()?.timeMode ?: "real"

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
        if (worldObj == null || worldObj.isRemote) return
        val s = boundSettings()
        if (s == null) { cachedDepartures = emptyList(); return }
        val data = KaisatsuNetworkData.get(worldObj) ?: return
        val tt = data.timetable ?: run { cachedDepartures = emptyList(); return }
        val nowMin = if (s.timeMode == "game") {
            ((worldObj.worldTime % 24000L) * 1440L / 24000L + 360L).toInt() % 1440
        } else {
            val now = java.time.LocalTime.now()
            now.hour * 60 + now.minute
        }
        val lineStations = if (s.lineID.isNotEmpty())
            data.companyLines[s.lineID]?.stationOrder?.toSet() else null
        cachedDepartures = tt.getNextDepartures(s.stationName, s.diaName, direction, nowMin, displayRows, lineStations)
        worldObj?.markBlockForUpdate(xCoord, yCoord, zCoord)
    }

    // ── NBT ──────────────────────────────────────────────────────────

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setInteger("BoundX", boundX)
        tag.setInteger("BoundY", boundY)
        tag.setInteger("BoundZ", boundZ)
        tag.setString("HeaderLine",      headerLine)
        tag.setString("HeaderDirection", headerDirection)
        tag.setString("Platform",        platform)
        tag.setInteger("LineColor",      lineColorHex)
        tag.setBoolean("SampleMode",     sampleMode)
        tag.setString("Direction",       direction)
        tag.setInteger("DisplayRows",    displayRows)

        val list = NBTTagList()
        cachedDepartures.forEach { row ->
            NBTTagCompound().also {
                it.setString("Time", row.time)
                it.setString("Dest", row.destination)
                it.setString("Type", row.typeName)
                it.setString("Num",  row.trainNumber)
                it.setString("Name", row.trainName)
                list.appendTag(it)
            }
        }
        tag.setTag("Departures", list)
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        boundX = tag.getInteger("BoundX")
        boundY = if (tag.hasKey("BoundY")) tag.getInteger("BoundY") else -1
        boundZ = tag.getInteger("BoundZ")
        headerLine      = tag.getString("HeaderLine")
        headerDirection = tag.getString("HeaderDirection")
        platform        = tag.getString("Platform")
        lineColorHex    = if (tag.hasKey("LineColor")) tag.getInteger("LineColor") else DEFAULT_LINE_COLOR
        sampleMode      = tag.getBoolean("SampleMode")
        direction       = tag.getString("Direction").ifEmpty { "両方" }
        displayRows     = tag.getInteger("DisplayRows").let { if (it == 0) 3 else it }

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

    companion object {
        const val DEFAULT_LINE_COLOR = 0x1E90FF
    }
}
