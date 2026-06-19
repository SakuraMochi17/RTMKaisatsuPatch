package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.ngt.rtm.gui.GuiSelectModel
import jp.ngt.rtm.modelpack.IModelSelector
import jp.sakuramochi.kaisatsupatch.RTMKaisatsuPatchCore
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkManager
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenTicketVendor
import jp.sakuramochi.kaisatsupatch.network.PacketOpenVendorConfig
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockCustomTicketVendor : BlockMachineBase(Material.iron) {

    init {
        setBlockName("custom_ticket_vendor")
        setLightOpacity(0)
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityCustomTicketVendor()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityCustomTicketVendor ?: return true
        val heldItem = player.currentEquippedItem

        if (heldItem?.item is ItemSettingsTool) {
            if (player.isSneaking) {
                if (world.isRemote) {
                    val selector = tile as? IModelSelector
                    if (selector != null) Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, selector))
                }
            } else {
                if (!world.isRemote) {
                    val stationList = KaisatsuNetworkData.get(world)?.globalStations?.keys?.sorted() ?: emptyList()
                    KaizPatchNetwork.CHANNEL.sendTo(
                        PacketOpenVendorConfig(x, y, z, tile.stationName, stationList),
                        player as EntityPlayerMP
                    )
                }
            }
            return true
        }

        if (!world.isRemote) {
            val fares = KaisatsuNetworkManager.getAvailableFares(world, tile.stationName, isICCard = false)
            // 1. 運賃キャッシュパケットを先に送る
            KaizPatchNetwork.CHANNEL.sendTo(PacketOpenTicketVendor(tile.stationName, fares), player as EntityPlayerMP)
            // 2. Forge 標準の openGui でコンテナを開く（S2EPacketOpenWindow が後着）
            player.openGui(RTMKaisatsuPatchCore.instance, RTMKaisatsuPatchCore.GUI_VENDOR, world, x, y, z)
        }
        return true
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
