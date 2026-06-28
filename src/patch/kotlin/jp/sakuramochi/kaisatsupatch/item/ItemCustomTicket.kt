package jp.sakuramochi.kaisatsupatch.item

import jp.sakuramochi.kaisatsupatch.util.initName
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class ItemCustomTicket : Item() {

    companion object {
        private const val TAG_FROM = "FromStation"
        private const val TAG_TO = "ToStation"
        private const val TAG_USED = "Used"

        fun getFromStation(stack: ItemStack): String {
            ensureTag(stack)
            return stack.tagCompound.getString(TAG_FROM)
        }

        fun getToStation(stack: ItemStack): String {
            ensureTag(stack)
            return stack.tagCompound.getString(TAG_TO)
        }

        fun isUsed(stack: ItemStack): Boolean {
            ensureTag(stack)
            return stack.tagCompound.getBoolean(TAG_USED)
        }

        /** 切符を初期化する（券売機が呼ぶ）。 */
        fun initTicket(stack: ItemStack, fromStation: String, toStation: String) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_FROM, fromStation)
            stack.tagCompound.setString(TAG_TO, toStation)
            stack.tagCompound.setBoolean(TAG_USED, false)
        }

        /** 入場スタンプを押す（入場改札が呼ぶ）。 */
        fun markUsed(stack: ItemStack) {
            ensureTag(stack)
            stack.tagCompound.setBoolean(TAG_USED, true)
        }

        /** 着駅を書き換える（乗越精算機が呼ぶ）。 */
        fun setToStation(stack: ItemStack, toStation: String) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_TO, toStation)
        }

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) {
                stack.tagCompound = NBTTagCompound()
            }
        }
    }

    init {
        initName("custom_ticket")
        setTextureName("rtm:ticket")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val from = getFromStation(stack).ifEmpty { "??" }
        val to = getToStation(stack).ifEmpty { "??" }
        val suffix = if (isUsed(stack)) "【使用済】" else ""
        return "乗車券 $from → $to $suffix"
    }
}
