package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityTrainManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenTrainManager
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockTrainManager : BlockContainer(Material.iron) {

    init {
        setBlockName("train_manager")
        setBlockTextureName("rtmkaisatsupatch:station_manager")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityTrainManager()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityTrainManager ?: return true

        if (player.currentEquippedItem?.item is ItemSettingsTool) {
            if (!world.isRemote) {
                val data = KaisatsuNetworkData.get(world)
                val currentTrain = data?.trainData?.get(tile.trainID)
                val lines = data?.companyLines?.values?.map { line ->
                    PacketOpenTrainManager.LineInfo(line.lineID, line.lineName, line.stationOrder.toList())
                } ?: emptyList()
                KaizPatchNetwork.CHANNEL.sendTo(
                    PacketOpenTrainManager(x, y, z, currentTrain, lines),
                    player as EntityPlayerMP
                )
            }
            return true
        }
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
