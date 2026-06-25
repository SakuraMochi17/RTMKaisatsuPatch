package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenDepartureBoard
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MathHelper
import net.minecraft.world.World

class BlockDepartureBoard : BlockContainer(Material.iron) {

    init {
        setBlockName("departure_board")
        setBlockTextureName("rtmkaisatsupatch:line_manager")  // 流用テクスチャ（後で専用テクスチャに差替可）
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityDepartureBoard()

    /** 設置時に向きを決める */
    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, placer: EntityLivingBase, stack: ItemStack) {
        val facing = (MathHelper.floor_double(placer.rotationYaw * 4.0 / 360.0 + 0.5) and 3 + 2) % 4
        world.setBlockMetadataWithNotify(x, y, z, facing, 2)
    }

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (world.isRemote) return true
        val tile = world.getTileEntity(x, y, z) as? TileEntityDepartureBoard ?: return true
        val mp = player as? EntityPlayerMP ?: return true
        val data = KaisatsuNetworkData.get(world) ?: return true

        if (player.currentEquippedItem?.item is ItemSettingsTool) {
            // 設定GUI
            val dias      = data.timetable?.diaNames ?: emptyList()
            val stations  = data.globalStations.keys.sorted()
            KaizPatchNetwork.CHANNEL.sendTo(PacketOpenDepartureBoard().also { pkt ->
                pkt.x = x; pkt.y = y; pkt.z = z
                pkt.isConfigMode       = true
                pkt.stationName        = tile.stationName
                pkt.diaName            = tile.diaName
                pkt.direction          = tile.direction
                pkt.displayRows        = tile.displayRows
                pkt.title              = tile.title
                pkt.availableDias      = dias
                pkt.availableStations  = stations
            }, mp)
        } else {
            // 表示GUI（発車情報をサーバーで計算して送る）
            val now = java.time.LocalTime.now()
            val nowMin = now.hour * 60 + now.minute
            val rows = data.timetable
                ?.getNextDepartures(tile.stationName, tile.diaName, tile.direction, nowMin, 10)
                ?: emptyList()
            KaizPatchNetwork.CHANNEL.sendTo(PacketOpenDepartureBoard().also { pkt ->
                pkt.x = x; pkt.y = y; pkt.z = z
                pkt.isConfigMode  = false
                pkt.stationName   = tile.stationName
                pkt.diaName       = tile.diaName
                pkt.direction     = tile.direction
                pkt.currentTime   = "%02d:%02d".format(now.hour, now.minute)
                pkt.departures    = rows
            }, mp)
        }
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
