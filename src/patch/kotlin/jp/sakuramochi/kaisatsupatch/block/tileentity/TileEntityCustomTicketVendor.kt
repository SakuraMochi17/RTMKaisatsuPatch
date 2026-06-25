package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.ngt.rtm.electric.TileEntityTicketVendor
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound

class TileEntityCustomTicketVendor : TileEntityTicketVendor() {

    var stationName: String = "未設定"
    var companyID: String = ""

    override fun getBlockType(): Block = jp.ngt.rtm.RTMBlock.ticketVendor

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationName = tag.getString("StationName").ifEmpty { "未設定" }
        companyID   = tag.getString("CompanyID")
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationName", stationName)
        tag.setString("CompanyID", companyID)
    }
}
