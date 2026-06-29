package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.electric.MachineType
import jp.ngt.rtm.gui.GuiSelectModel
import jp.ngt.rtm.modelpack.IModelSelectorWithType
import jp.ngt.rtm.modelpack.ModelPackManager
import jp.ngt.rtm.modelpack.cfg.IConfigWithType
import jp.ngt.rtm.modelpack.modelset.ModelSetBase
import jp.ngt.rtm.modelpack.state.ResourceState
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.IIconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.IIcon
import net.minecraft.world.World

class ItemBlockCustomTicketVendor(block: Block) : ItemBlock(block) {

    companion object {
        // RTM のモデル登録 type キー（改札機・券売機等はすべて "ModelMachine"）。
        private const val RTM_MODEL_TYPE = "ModelMachine"
    }

    // ItemBlock は通常ブロック側のテクスチャを使うため、インベントリ用に
    // RTM のアイテムアイコンを明示的に登録して使う。
    private var inventoryIcon: IIcon? = null

    @SideOnly(Side.CLIENT)
    override fun registerIcons(reg: IIconRegister) {
        inventoryIcon = reg.registerIcon("rtmkaisatsupatch:ticket_vendor")
    }

    override fun getIconFromDamage(meta: Int): IIcon =
        inventoryIcon ?: super.getIconFromDamage(meta)

    // ItemBlock は既定でブロックアトラス(0)を使うが、上で登録したアイコンは
    // アイテムアトラスにあるため 1 を返してアイテムアトラスを参照させる。
    override fun getSpriteNumber(): Int = 1

    override fun onItemRightClick(itemStack: ItemStack, world: World, player: EntityPlayer): ItemStack {
        // 該当サブタイプ(Vendor)のモデルが存在する時だけ選択 GUI を開く。
        // vanilla RTM には Vendor モデルが無いため、その環境では開かない。
        if (world.isRemote && hasModelOfSubType(MachineType.Vendor.name)) {
            Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, buildSelector(itemStack)))
        }
        return itemStack
    }

    /** 指定サブタイプ(machineType)の ModelMachine モデルが 1 つ以上あるか。 */
    private fun hasModelOfSubType(subType: String): Boolean = try {
        ModelPackManager.INSTANCE.getModelList(RTM_MODEL_TYPE)
            .any { (it.config as? IConfigWithType)?.subType == subType }
    } catch (e: Throwable) {
        // RTM の版差で config API が異なる場合は従来通り開く（クラッシュ回避を優先）。
        true
    }

    private fun buildSelector(itemStack: ItemStack) = object : IModelSelectorWithType {
        // モデル登録 type キーは "ModelMachine"。Vendor は machineType(サブタイプ)。
        override fun getModelType(): String = RTM_MODEL_TYPE
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
        // vanilla RTM の IModelSelector は closeGui(String) の1引数版を要求するため両対応する。
        fun closeGui(name: String): Boolean = true
        override fun getModelSet(): ModelSetBase<*>? {
            val n = modelName; return if (n.isEmpty()) null else ModelPackManager.INSTANCE.getModelSet(modelType, n)
        }
    }
}
