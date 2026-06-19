package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.ngt.rtm.block.tileentity.TileEntityTurnstile
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound

class TileEntityCustomTurnstile : TileEntityTurnstile() {

    enum class GateMode { ENTRY, EXIT, BOTH }

    var stationCode: String = "STATION_A"
    var gateMode: GateMode = GateMode.ENTRY

    override fun getBlockType(): Block = jp.ngt.rtm.RTMBlock.turnstile

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationCode = tag.getString("StationCode").ifEmpty { "STATION_A" }
        gateMode = if (tag.getString("GateMode") == "EXIT") GateMode.EXIT else GateMode.ENTRY
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationCode", stationCode)
        tag.setString("GateMode", gateMode.name)
    }
}
