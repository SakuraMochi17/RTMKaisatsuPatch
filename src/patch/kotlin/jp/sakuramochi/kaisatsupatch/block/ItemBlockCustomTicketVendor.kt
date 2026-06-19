package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.electric.MachineType
import jp.ngt.rtm.gui.GuiSelectModel
import jp.ngt.rtm.modelpack.IModelSelectorWithType
import jp.ngt.rtm.modelpack.ModelPackManager
import jp.ngt.rtm.modelpack.modelset.ModelSetBase
import jp.ngt.rtm.modelpack.state.ResourceState
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
            val modelList = ModelPackManager.INSTANCE.getModelList(MachineType.Vendor.name)
            if (modelList != null && !modelList.isEmpty()) {
                Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, buildSelector(itemStack)))
            }
        }
        return itemStack
    }

    private fun buildSelector(itemStack: ItemStack) = object : IModelSelectorWithType {
        override fun getModelType(): String = MachineType.Vendor.name
        override fun getSubType(): String  = MachineType.Vendor.name
        override fun getModelName(): String =
            if (itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName"))
                itemStack.tagCompound.getString("ModelName") else ""

        override fun setModelName(name: String) {
            if (!itemStack.hasTagCompound()) itemStack.tagCompound = NBTTagCompound()
            itemStack.tagCompound.setString("ModelName", name)
        }

        override fun getResourceState(): ResourceState = ResourceState(this)
        override fun getPos(): IntArray = intArrayOf(0, 0, 0)
        override fun closeGui(name: String, state: ResourceState): Boolean = true
        override fun getModelSet(): ModelSetBase<*>? {
            val n = modelName; return if (n.isEmpty()) null else ModelPackManager.INSTANCE.getModelSet(modelType, n)
        }
    }
}
