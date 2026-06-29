package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureSettings
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenDepartureSettings
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

/**
 * 発車標の「設定ブロック」。Settings Tool で右クリックすると表示テンプレートの
 * 設定 GUI を開く。実際の表示は、この設定ブロックにバインドした発車標ブロックが行う。
 */
class BlockDepartureSettings : BlockContainer(Material.iron) {

    init {
        (this as net.minecraft.block.Block).setBlockName("departure_settings")
        (this as net.minecraft.block.Block).setBlockTextureName("rtmkaisatsupatch:departure_settings")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityDepartureSettings()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (world.isRemote) return true
        val tile = world.getTileEntity(x, y, z) as? TileEntityDepartureSettings ?: return true
        val mp = player as? EntityPlayerMP ?: return true
        val data = KaisatsuNetworkData.get(world) ?: return true

        // Settings Tool 以外は何もしない（将来: バインド状況の確認など）
        if (player.currentEquippedItem?.item !is ItemSettingsTool) return true

        val dias     = data.timetable?.diaNames ?: emptyList()
        val stations = data.globalStations.keys.sorted()
        val lines    = data.companyLines.values.sortedBy { it.lineID }.map { it.lineID to it.lineName }

        KaizPatchNetwork.CHANNEL.sendTo(PacketOpenDepartureSettings().also { pkt ->
            pkt.x = x; pkt.y = y; pkt.z = z
            pkt.stationName       = tile.stationName
            pkt.lineID            = tile.lineID
            pkt.diaName           = tile.diaName
            pkt.direction         = tile.direction
            pkt.displayRows       = tile.displayRows
            pkt.title             = tile.title
            pkt.timeMode          = tile.timeMode
            pkt.lineColorHex      = tile.lineColorHex
            pkt.availableDias     = dias
            pkt.availableStations = stations
            pkt.availableLines    = lines
        }, mp)
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
