package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityReservedVendor
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenReservedVendor
import jp.sakuramochi.kaisatsupatch.network.PacketOpenVendorConfig
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockReservedVendor : BlockMachineBase(Material.iron) {

    init {
        (this as net.minecraft.block.Block).setBlockName("reserved_seat_vendor")
        (this as net.minecraft.block.Block).setLightOpacity(0)
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityReservedVendor()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityReservedVendor ?: return true
        val heldItem = player.currentEquippedItem

        if (heldItem?.item is ItemSettingsTool) {
            if (player.isSneaking) {
                if (world.isRemote) {
                    // モデルの選択はアイテムを手に持って空中で右クリック
                    // (ItemBlockReservedVendor#onItemRightClick) で行う。
                }
            } else {
                if (!world.isRemote) {
                    val stationList = KaisatsuNetworkData.get(world)?.globalStations?.keys?.sorted() ?: emptyList()
                    KaizPatchNetwork.CHANNEL.sendTo(
                        PacketOpenVendorConfig(x, y, z, tile.stationName, "", stationList, emptyList()),
                        player as EntityPlayerMP
                    )
                }
            }
            return true
        }

        if (!world.isRemote) {
            val data = KaisatsuNetworkData.get(world)
            val trains = buildTrainInfoList(data, tile.stationName)
            val lines = data?.companyLines?.values?.map { line ->
                PacketOpenReservedVendor.LineInfo(line.lineID, line.lineName, line.stationOrder.toList())
            } ?: emptyList()
            KaizPatchNetwork.CHANNEL.sendTo(
                PacketOpenReservedVendor(tile.stationName, trains, lines),
                player as EntityPlayerMP
            )
        }
        return true
    }

    private fun buildTrainInfoList(
        data: KaisatsuNetworkData?,
        vendorStation: String
    ): List<PacketOpenReservedVendor.TrainInfo> {
        if (data == null) return emptyList()
        return data.trainData.values
            .filter { train -> train.stopStations.contains(vendorStation) }
            .map { train ->
                val cars = train.cars.map { car ->
                    val takenSeats = data.reservations.keys
                        .filter { it.startsWith("${train.trainID}:${car.carNumber}:") }
                        .count()
                    val available = maxOf(0, car.seatCount - takenSeats)
                    PacketOpenReservedVendor.CarInfo(
                        car.carNumber, car.seatCount, car.carClass, available
                    )
                }
                PacketOpenReservedVendor.TrainInfo(
                    train.trainID, train.trainName, train.trainType, train.lineID,
                    train.stopStations, train.reservedFare, train.unreservedFare, cars
                )
            }
    }

    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase?, itemStack: ItemStack) {
        super.onBlockPlacedBy(world, x, y, z, entity, itemStack)
        val tile = world.getTileEntity(x, y, z)
        if (entity is EntityPlayer && tile is TileEntityMachineBase) {
            tile.setRotation(entity, if (entity.isSneaking) 1.0f else 15.0f, true)
        }
        if (tile is TileEntityMachineBase && itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
            tile.modelName = itemStack.tagCompound.getString("ModelName")
        }
    }

    override fun isOpaqueCube(): Boolean = false
    override fun renderAsNormalBlock(): Boolean = false
}
