package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityLineManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenLineGui
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockLineManager : BlockContainer(Material.iron) {

    init {
        setBlockName("line_manager")
        setBlockTextureName("rtmkaisatsupatch:line_manager")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityLineManager()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (player.currentEquippedItem?.item !is ItemSettingsTool) return true
        if (world.isRemote) return true

        val tile = world.getTileEntity(x, y, z) as? TileEntityLineManager ?: return true
        val data = KaisatsuNetworkData.get(world) ?: return true
        val mp   = player as? EntityPlayerMP ?: return true

        val co            = data.companies[tile.companyID]
        val stationNames  = data.globalStations.keys.sorted()
        val linesForComp  = data.companyLines.values
            .filter { it.companyID == tile.companyID }
            .sortedBy { it.lineID }
        val otherCompanies = data.companies.values
            .filter { it.companyID != tile.companyID }
            .sortedBy { it.companyID }
            .map { it.companyID to it.companyName }

        KaizPatchNetwork.CHANNEL.sendTo(PacketOpenLineGui().also { pkt ->
            pkt.x = x; pkt.y = y; pkt.z = z
            pkt.companyID           = tile.companyID
            pkt.companyName         = co?.companyName  ?: tile.companyName
            pkt.companyColor        = co?.color        ?: 0x1E90FF
            pkt.icCardName          = co?.icCardName   ?: ""
            pkt.defaultBaseFare     = co?.defaultBaseFare    ?: 150
            pkt.defaultCostPerBlock = co?.defaultCostPerBlock ?: 0.1
            pkt.members             = co?.members?.toList()          ?: emptyList()
            pkt.allowedCompanies    = co?.allowedCompanies?.toList() ?: emptyList()
            pkt.otherCompanies      = otherCompanies
            pkt.globalStations      = stationNames
            pkt.companyLines        = linesForComp.map { line ->
                PacketOpenLineGui.LineInfo(
                    line.lineID, line.lineName,
                    line.baseFare, line.costPerBlock,
                    line.transferFee,
                    line.stationOrder.toList()
                )
            }
        }, mp)
        return true
    }

    override fun isOpaqueCube()        = false
    override fun renderAsNormalBlock() = false
}
