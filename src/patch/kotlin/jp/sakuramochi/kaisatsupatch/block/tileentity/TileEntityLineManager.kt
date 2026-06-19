package jp.sakuramochi.kaisatsupatch.block.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntityLineManager : TileEntity() {

    var companyName: String = "未設定"

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        companyName = tag.getString("CompanyName").ifEmpty { "未設定" }
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("CompanyName", companyName)
    }
}
