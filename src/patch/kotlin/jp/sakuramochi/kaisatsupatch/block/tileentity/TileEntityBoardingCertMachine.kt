package jp.sakuramochi.kaisatsupatch.block.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntityBoardingCertMachine : TileEntity() {
    var stationName = "未設定"

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("StationName", stationName)
    }

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        stationName = tag.getString("StationName").ifEmpty { "未設定" }
    }
}
