package jp.sakuramochi.kaisatsupatch.block.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntityLineManager : TileEntity() {

    var companyID: String   = ""
    var companyName: String = "未設定"

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        companyID   = tag.getString("CompanyID")
        companyName = tag.getString("CompanyName").ifEmpty { "未設定" }
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("CompanyID",   companyID)
        tag.setString("CompanyName", companyName)
    }
}
