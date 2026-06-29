package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.ngt.rtm.electric.TileEntityTicketVendor
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound

/**
 * 指定席券売機の TileEntity。
 *
 * BlockReservedVendor は RTM の BlockMachineBase を継承しており、設置時に
 * RTM 側で TileEntity が TileEntityMachineBase へキャストされる。そのため
 * 券売機(TileEntityCustomTicketVendor)と同様に RTM の機械 TE を継承する必要がある。
 * (以前は素の TileEntity を継承していたため設置時に ClassCastException が発生していた)
 */
class TileEntityReservedVendor : TileEntityTicketVendor() {

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
