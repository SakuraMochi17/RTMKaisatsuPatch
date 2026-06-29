package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureSettings
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenDepartureBoard
import jp.sakuramochi.kaisatsupatch.util.rememberedCoords
import jp.sakuramochi.kaisatsupatch.util.sendError
import jp.sakuramochi.kaisatsupatch.util.sendSuccess
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MathHelper
import net.minecraft.world.World

/**
 * 発車標（表示体）ブロック。
 *
 * - Settings Tool + 通常右クリック … 路線名/方面/番線/路線カラーの設定 GUI を開く
 * - Settings Tool + スニーク右クリック … 直前に記憶した設定ブロックへバインドする
 */
class BlockDepartureBoard : BlockContainer(Material.iron) {

    init {
        (this as net.minecraft.block.Block).setBlockName("departure_board")
        (this as net.minecraft.block.Block).setBlockTextureName("rtmkaisatsupatch:departure_board")
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
        val held = player.currentEquippedItem
        if (held?.item !is ItemSettingsTool) return true

        // スニーク + Settings Tool: 記憶した設定ブロックへバインド
        if (player.isSneaking) {
            val coords = held.rememberedCoords()
            if (coords == null) {
                mp.sendError("先に設定ブロックをスニーク右クリックで記憶してください")
                return true
            }
            val target = world.getTileEntity(coords.x, coords.y, coords.z)
            if (target !is TileEntityDepartureSettings) {
                mp.sendError("記憶した位置に設定ブロックが見つかりません")
                return true
            }
            tile.bindTo(coords.x, coords.y, coords.z)
            mp.sendSuccess("設定ブロック (${coords.x}, ${coords.y}, ${coords.z}) にバインドしました")
            return true
        }

        // 通常右クリック: 表示情報の設定 GUI
        val boundInfo = tile.boundSettings()?.let { s -> s.title.ifEmpty { s.stationName }.ifEmpty { "(駅未設定)" } }
            ?: "未バインド"
        KaizPatchNetwork.CHANNEL.sendTo(PacketOpenDepartureBoard().also { pkt ->
            pkt.x = x; pkt.y = y; pkt.z = z
            pkt.headerLine      = tile.headerLine
            pkt.headerDirection = tile.headerDirection
            pkt.platform        = tile.platform
            pkt.lineColorHex    = tile.lineColorHex
            pkt.boundInfo       = boundInfo
        }, mp)
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
