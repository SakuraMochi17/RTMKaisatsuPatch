package jp.sakuramochi.kaisatsupatch.block.tileentity

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity

class TileEntityTrainManager : TileEntity() {

    var trainID: String = ""

    override fun readFromNBT(tag: NBTTagCompound) {
        super.readFromNBT(tag)
        trainID = tag.getString("TrainID")
    }

    override fun writeToNBT(tag: NBTTagCompound) {
        super.writeToNBT(tag)
        tag.setString("TrainID", trainID)
    }
}
