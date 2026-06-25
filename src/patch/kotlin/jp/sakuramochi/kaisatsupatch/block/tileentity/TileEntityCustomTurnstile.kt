package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.ngt.rtm.block.tileentity.TileEntityTurnstile
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound

class TileEntityCustomTurnstile : TileEntityTurnstile() {

    enum class GateMode {
        ENTRY,        // 入場専用（全種別）
        EXIT,         // 出場専用（全種別）
        BOTH,         // 入出場兼用（全種別）
        IC_ONLY,      // IC専用（入出場兼用）
        TICKET_ONLY,  // 切符専用（入出場兼用）
        PASS_ONLY     // 定期専用（入出場兼用）
    }

    var stationCode: String = "STATION_A"
    var gateMode: GateMode = GateMode.ENTRY
    var ownerCompanyID: String = ""

    override fun getBlockType(): Block = jp.ngt.rtm.RTMBlock.turnstile

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationCode = tag.getString("StationCode").ifEmpty { "STATION_A" }
        gateMode = runCatching { GateMode.valueOf(tag.getString("GateMode")) }.getOrDefault(GateMode.ENTRY)
        ownerCompanyID = tag.getString("OwnerCompanyID")
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationCode", stationCode)
        tag.setString("GateMode", gateMode.name)
        tag.setString("OwnerCompanyID", ownerCompanyID)
    }
}
