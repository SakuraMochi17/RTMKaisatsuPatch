package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.ngt.rtm.electric.TileEntityTicketVendor
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound

class TileEntityCustomTicketVendor : TileEntityTicketVendor() {

    var stationName: String = "未設定"

    override fun getBlockType(): Block = jp.ngt.rtm.RTMBlock.ticketVendor

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationName = tag.getString("StationName").ifEmpty { "未設定" }
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationName", stationName)
    }
}
