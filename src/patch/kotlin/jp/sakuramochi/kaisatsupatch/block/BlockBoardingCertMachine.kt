package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityBoardingCertMachine
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemBoardingCertificate
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenVendorConfig
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World

class BlockBoardingCertMachine : BlockContainer(Material.iron) {

    init {
        setBlockName("boarding_cert_machine")
        setBlockTextureName("rtmkaisatsupatch:boarding_cert_machine")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityBoardingCertMachine()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityBoardingCertMachine ?: return true

        if (player.currentEquippedItem?.item is ItemSettingsTool) {
            if (!world.isRemote) {
                val stations = KaisatsuNetworkData.get(world)?.globalStations?.keys?.sorted() ?: emptyList()
                KaizPatchNetwork.CHANNEL.sendTo(
                    PacketOpenVendorConfig(x, y, z, tile.stationName, "", stations, emptyList()),
                    player as EntityPlayerMP
                )
            }
            return true
        }

        if (world.isRemote) return true

        if (tile.stationName == "未設定") {
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}発行機の設置駅が設定されていません（設定ツールで右クリック）"))
            return true
        }

        // 既に同駅の証明書を持っているか確認
        val inv = player.inventory
        for (i in 0 until inv.sizeInventory) {
            val s = inv.getStackInSlot(i) ?: continue
            if (s.item is ItemBoardingCertificate &&
                ItemBoardingCertificate.getBoardingStation(s) == tile.stationName) {
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.YELLOW}既に ${tile.stationName} の乗車駅証明書を持っています"))
                return true
            }
        }

        val cert = ItemBoardingCertificate.create(tile.stationName)
        if (!player.inventory.addItemStackToInventory(cert)) {
            player.dropPlayerItemWithRandomChoice(cert, false)
        }
        player.addChatMessage(ChatComponentText(
            "${EnumChatFormatting.GREEN}乗車駅証明書を発行しました（${tile.stationName} 乗車）"))
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
