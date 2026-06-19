package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.RTMCore
import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.ngt.rtm.modelpack.IModelSelector
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import jp.ngt.rtm.gui.GuiSelectModel
import jp.ngt.ngtlib.util.NGTUtil
import jp.ngt.rtm.RTMItem
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockCustomTicketVendor : BlockMachineBase(Material.iron) {

    init {
        setBlockName("custom_ticket_vendor")
        setLightOpacity(0)
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity {
        return TileEntityCustomTicketVendor()
    }

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        if (NGTUtil.isEquippedItem(player, RTMItem.crowbar)) {
            player.openGui(RTMCore.instance, RTMCore.guiIdChangeOffset.toInt(), world, x, y, z)
        } else if (player.isSneaking) {
            if (world.isRemote) {
                val tile = world.getTileEntity(x, y, z)
                if (tile is IModelSelector) {
                    Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, tile))
                }
            }
        } else {
            player.openGui(RTMCore.instance, RTMCore.guiIdTicketVendor.toInt(), world, x, y, z)
        }
        return true
    }

    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase?, itemStack: ItemStack) {
        super.onBlockPlacedBy(world, x, y, z, entity, itemStack)
        val tile = world.getTileEntity(x, y, z)
        if (tile is TileEntityMachineBase) {
            if (itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
                tile.modelName = itemStack.tagCompound.getString("ModelName")
            }
        }
    }

    override fun isOpaqueCube(): Boolean = false
    override fun renderAsNormalBlock(): Boolean = false
}