package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityStationManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkManager
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenStationGui
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockStationManager : BlockContainer(Material.iron) {

    init {
        setBlockName("station_manager")
        setBlockTextureName("rtmkaisatsupatch:station_manager")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityStationManager()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityStationManager ?: return true

        if (player.currentEquippedItem?.item is ItemSettingsTool) {
            if (!world.isRemote) {
                val data = KaisatsuNetworkData.get(world)
                val sales = data?.stationSales?.get(tile.stationName) ?: 0L
                val fares = if (tile.stationName != "未設定")
                    KaisatsuNetworkManager.getAvailableFares(world, tile.stationName)
                else emptyList()
                KaizPatchNetwork.CHANNEL.sendTo(
                    PacketOpenStationGui(x, y, z, tile.stationName, sales, fares),
                    player as EntityPlayerMP
                )
            }
            return true
        }
        return true
    }

    override fun breakBlock(world: World, x: Int, y: Int, z: Int, block: Block, meta: Int) {
        if (!world.isRemote) {
            val tile = world.getTileEntity(x, y, z) as? TileEntityStationManager
            val stationName = tile?.stationName ?: ""
            if (stationName.isNotEmpty() && stationName != "未設定") {
                val data = KaisatsuNetworkData.get(world)
                if (data != null) {
                    data.globalStations.remove(stationName)
                    // 全路線の駅順序からも除去
                    data.companyLines.values.forEach { line ->
                        line.stationOrder.remove(stationName)
                    }
                    data.markDirty()
                }
            }
        }
        // super を後に呼ぶことで、tile entity 取得後に破壊処理が走る
        super.breakBlock(world, x, y, z, block, meta)
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
