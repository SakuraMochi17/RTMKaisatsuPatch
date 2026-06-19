package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class ItemCustomICCard : Item() {

    companion object {
        const val MAX_BALANCE = 20000
        private const val TAG_BALANCE = "Balance"
        private const val TAG_ENTRY_STATION = "EntryStation"

        fun getBalance(stack: ItemStack): Int {
            ensureTag(stack)
            return stack.tagCompound.getInteger(TAG_BALANCE)
        }

        fun setBalance(stack: ItemStack, amount: Int) {
            ensureTag(stack)
            stack.tagCompound.setInteger(TAG_BALANCE, amount.coerceIn(0, MAX_BALANCE))
        }

        fun charge(stack: ItemStack, amount: Int): Boolean {
            val current = getBalance(stack)
            if (current + amount > MAX_BALANCE) return false
            setBalance(stack, current + amount)
            return true
        }

        /** 残高から運賃を引く。残高不足なら false を返す。 */
        fun deduct(stack: ItemStack, fare: Int): Boolean {
            val current = getBalance(stack)
            if (current < fare) return false
            setBalance(stack, current - fare)
            return true
        }

        /** 入場駅コードを記録（入場時に呼ぶ）。 */
        fun setEntryStation(stack: ItemStack, stationCode: String) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_ENTRY_STATION, stationCode)
        }

        /** 記録済みの入場駅コードを取得。未入場なら空文字。 */
        fun getEntryStation(stack: ItemStack): String {
            ensureTag(stack)
            return stack.tagCompound.getString(TAG_ENTRY_STATION)
        }

        fun clearEntryStation(stack: ItemStack) {
            ensureTag(stack)
            stack.tagCompound.removeTag(TAG_ENTRY_STATION)
        }

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) {
                stack.tagCompound = NBTTagCompound()
            }
        }
    }

    init {
        unlocalizedName = "custom_ic_card"
        setTextureName("rtm:icCard")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val balance = getBalance(stack)
        return "ICカード（残高: ${balance}円）"
    }
}
