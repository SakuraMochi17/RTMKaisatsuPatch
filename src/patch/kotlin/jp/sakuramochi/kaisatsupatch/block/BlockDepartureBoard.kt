package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
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
        // 通常右クリックのみ設定 GUI を開く（スニーク時のバインドは ItemSettingsTool 側で処理）
        if (player.currentEquippedItem?.item !is ItemSettingsTool) return true

        // 表示情報の設定 GUI
        val boundInfo = tile.boundSettings()?.let { s -> s.stationName.ifEmpty { "(駅未設定)" } }
            ?: "未バインド"
        KaizPatchNetwork.CHANNEL.sendTo(PacketOpenDepartureBoard().also { pkt ->
            pkt.x = x; pkt.y = y; pkt.z = z
            pkt.headerLine      = tile.headerLine
            pkt.headerDirection = tile.headerDirection
            pkt.platform        = tile.platform
            pkt.lineColorHex    = tile.lineColorHex
            pkt.sampleMode      = tile.sampleMode
            pkt.boundInfo       = boundInfo
        }, mp)
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
    // ブロック自体は描画しない（TESR で HI03 モデルのみ描く）。
    // これを -1 にしないとブロックのキューブも描画され MQO と二重になる。
    override fun getRenderType() = -1
}
