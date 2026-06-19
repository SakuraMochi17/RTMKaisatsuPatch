package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.electric.MachineType
import jp.ngt.rtm.modelpack.IModelSelectorWithType
import jp.ngt.rtm.modelpack.modelset.ModelSetBase
import jp.ngt.rtm.modelpack.state.ResourceState
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraft.client.Minecraft
import jp.ngt.rtm.gui.GuiSelectModel
import jp.ngt.rtm.modelpack.ModelPackManager

class ItemBlockCustomTurnstile(block: Block) : ItemBlock(block) {

    override fun onItemRightClick(itemStack: ItemStack, world: World, player: EntityPlayer): ItemStack {
        if (world.isRemote) {
            val selector = object : IModelSelectorWithType {
                override fun getModelType(): String = "Turnstile"

                // ★クラッシュの根本原因だった箇所。これを指定すれば絶対に落ちません
                override fun getSubType(): String = MachineType.Turnstile.name

                override fun getModelName(): String {
                    return if (itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
                        itemStack.tagCompound.getString("ModelName")
                    } else {
                        // 空文字でも、RTM側が勝手に「改札機(Turnstile)のデフォルト」を選んでくれます
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

                // ★余計な安全装置を消し、一番シンプルな形に戻します
                override fun getModelSet(): ModelSetBase<*> {
                    return ModelPackManager.INSTANCE.getModelSet(this.modelType, this.modelName)
                }
            }

            Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, selector))
        }
        return itemStack
    }
}