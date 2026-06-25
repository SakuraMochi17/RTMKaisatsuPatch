package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityLineManager
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenCompanyManager
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

        if (!world.isRemote) {
            val tile = world.getTileEntity(x, y, z) as? TileEntityLineManager ?: return true
            val data = KaisatsuNetworkData.get(world) ?: return true
            val mp = player as EntityPlayerMP

            // スニーク + SettingsTool → 会社管理GUI（全社横断）
            if (player.isSneaking) {
                val companyInfos = data.companies.values.sortedBy { it.companyID }.map { c ->
                    PacketOpenCompanyManager.CompanyInfo(
                        c.companyID, c.companyName, c.color, c.icCardName,
                        c.defaultBaseFare, c.defaultCostPerBlock,
                        c.members.toList(), c.allowedCompanies.toList()
                    )
                }
                val lineInfos = data.companyLines.values.sortedBy { it.lineID }.map { l ->
                    PacketOpenCompanyManager.LineInfo(l.lineID, l.lineName, l.companyID)
                }
                KaizPatchNetwork.CHANNEL.sendTo(PacketOpenCompanyManager(companyInfos, lineInfos), mp)
                return true
            }

            // 通常クリック → 路線管理GUI（このブロックの会社）
            val stationNames = data.globalStations.keys.sorted()
            val linesForCompany = data.companyLines.values
                .filter { it.companyName == tile.companyName }

            KaizPatchNetwork.CHANNEL.sendTo(
                PacketOpenLineGui(
                    x, y, z,
                    tile.companyName,
                    stationNames,
                    linesForCompany.map { line ->
                        PacketOpenLineGui.LineInfo(
                            line.lineID, line.lineName,
                            line.baseFare, line.costPerBlock,
                            line.transferFee,
                            line.stationOrder.toList()
                        )
                    }
                ),
                mp
            )
        }
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
