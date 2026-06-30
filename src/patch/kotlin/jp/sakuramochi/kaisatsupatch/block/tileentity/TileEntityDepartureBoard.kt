package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.sakuramochi.kaisatsupatch.core.DepartureRow
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity

/**
 * 発車標（表示体）の TileEntity。
 *
 * 役割は「この板に常時表示する静的情報（路線名・方面・番線・路線カラー）」の保持と、
 * バインドした設定ブロック [TileEntityDepartureSettings] からの発車情報の取得。
 * 発車情報の算出自体は設定ブロック側が行う。
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
    var sampleMode      = false     // サンプル表示（駅管理不要で見え方を確認するため）

    fun bindTo(x: Int, y: Int, z: Int) {
        boundX = x; boundY = y; boundZ = z
        markDirty()
        worldObj?.markBlockForUpdate(xCoord, yCoord, zCoord)
    }

    /** バインドした設定ブロック TE を取得（無ければ null） */
    fun boundSettings(): TileEntityDepartureSettings? {
        if (!isBound || worldObj == null) return null
        return worldObj.getTileEntity(boundX, boundY, boundZ) as? TileEntityDepartureSettings
    }

    /** 表示する発車情報（バインド先から取得） */
    fun departures(): List<DepartureRow> = boundSettings()?.cachedDepartures ?: emptyList()

    /** ヘッダーに出す路線名: 板の路線名 > バインド先の駅名 */
    fun headerTitle(): String {
        if (headerLine.isNotEmpty()) return headerLine
        return boundSettings()?.stationName ?: ""
    }

    fun timeMode(): String = boundSettings()?.timeMode ?: "real"

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
