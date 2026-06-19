package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.electric.MachineType
import jp.ngt.rtm.modelpack.IModelSelectorWithType
import jp.ngt.rtm.modelpack.ModelPackManager
import jp.ngt.rtm.modelpack.modelset.ModelSetBase
import jp.ngt.rtm.modelpack.state.ResourceState
import jp.ngt.rtm.gui.GuiSelectModel
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class ItemBlockCustomTicketVendor(block: Block) : ItemBlock(block) {

    override fun onItemRightClick(itemStack: ItemStack, world: World, player: EntityPlayer): ItemStack {
        if (world.isRemote) {
            val selector = object : IModelSelectorWithType {
                override fun getModelType(): String = "Vendor"

                override fun getSubType(): String = MachineType.Vendor.name

                override fun getModelName(): String {
                    return if (itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
                        itemStack.tagCompound.getString("ModelName")
                    } else {
                        ""
                    }
                }

                override fun setModelName(name: String) {
                    if (!itemStack.hasTagCompound()) {
                        itemStack.tagCompound = NBTTagCompound()
                    }
                    itemStack.tagCompound.setString("ModelName", name)
                }

                override fun getResourceState(): ResourceState {
                    return ResourceState(this)
                }

                override fun getPos(): IntArray = intArrayOf(0, 0, 0)

                override fun closeGui(name: String, state: ResourceState): Boolean {
                    return true
                }

                override fun getModelSet(): ModelSetBase<*> {
                    return ModelPackManager.INSTANCE.getModelSet(this.modelType, this.modelName)
                }
            }

            Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, selector))
        }
        return itemStack
    }
}