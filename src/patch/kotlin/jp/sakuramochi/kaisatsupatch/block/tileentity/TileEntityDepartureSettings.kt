package jp.sakuramochi.kaisatsupatch.block.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S35PacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity

/**
 * 発車標の「設定ブロック」TileEntity。
 *
 * 役割は「どの駅・路線・ダイヤの時刻表をデータ源にするか」の識別情報の保持のみ。
 * 方向・表示行数や実際の発車情報の算出・描画は、これにバインドした
 * [TileEntityDepartureBoard]（発車標ブロック）側が行う。
 */
class TileEntityDepartureSettings : TileEntity() {

    var stationName = ""
    var lineID      = ""        // 路線フィルター（空 = フィルターなし）
    var diaName     = ""        // ダイヤ（空 = すべて）
    var timeMode    = "real"    // "real" = 現実時刻, "game" = ゲーム内時刻

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationName", stationName)
        tag.setString("LineID",      lineID)
        tag.setString("DiaName",     diaName)
        tag.setString("TimeMode",    timeMode)
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationName = tag.getString("StationName")
        lineID      = tag.getString("LineID")
        diaName     = tag.getString("DiaName")
        timeMode    = tag.getString("TimeMode").ifEmpty { "real" }
    }

    override fun getDescriptionPacket(): Packet {
        val tag = NBTTagCompound()
        writeToNBT(tag)
        return S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, tag)
    }

    override fun onDataPacket(mgr: NetworkManager, pkt: S35PacketUpdateTileEntity) {
        readFromNBT(pkt.func_148857_g())
    }
}
