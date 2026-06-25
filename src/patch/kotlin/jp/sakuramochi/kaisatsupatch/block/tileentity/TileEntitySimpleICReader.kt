package jp.sakuramochi.kaisatsupatch.block.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntitySimpleICReader : TileEntity() {
    var stationName = "未設定"
    var companyID   = ""   // 空 = 全社カード受付

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationName", stationName)
        tag.setString("CompanyID", companyID)
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationName = tag.getString("StationName").ifEmpty { "未設定" }
        companyID   = tag.getString("CompanyID")
    }
}
