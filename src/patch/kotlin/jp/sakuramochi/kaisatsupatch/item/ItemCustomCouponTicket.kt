package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class ItemCustomCouponTicket : Item() {

    companion object {
        private const val TAG_FROM  = "FromStation"
        private const val TAG_TO    = "ToStation"
        private const val TAG_USES  = "Uses"
        private const val TAG_ENTRY = "EntryStation"
        const val MAX_USES = 10

        fun initTicket(stack: ItemStack, fromStation: String, toStation: String) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_FROM, fromStation)
            stack.tagCompound.setString(TAG_TO, toStation)
            stack.tagCompound.setInteger(TAG_USES, MAX_USES)
            stack.tagCompound.setString(TAG_ENTRY, "")
        }

        fun getFromStation(stack: ItemStack): String  { ensureTag(stack); return stack.tagCompound.getString(TAG_FROM) }
        fun getToStation(stack: ItemStack): String    { ensureTag(stack); return stack.tagCompound.getString(TAG_TO) }
        fun getRemainingUses(stack: ItemStack): Int   { ensureTag(stack); return stack.tagCompound.getInteger(TAG_USES) }
        fun getEntryStation(stack: ItemStack): String { ensureTag(stack); return stack.tagCompound.getString(TAG_ENTRY) }
        fun isEntered(stack: ItemStack): Boolean = getEntryStation(stack).isNotEmpty()

        fun markEntry(stack: ItemStack, station: String) {
            ensureTag(stack); stack.tagCompound.setString(TAG_ENTRY, station)
        }

        fun clearEntry(stack: ItemStack) {
            ensureTag(stack); stack.tagCompound.setString(TAG_ENTRY, "")
        }

        /** 1回消費して残り回数を返す */
        fun consumeUse(stack: ItemStack): Int {
            ensureTag(stack)
            val remaining = maxOf(0, stack.tagCompound.getInteger(TAG_USES) - 1)
            stack.tagCompound.setInteger(TAG_USES, remaining)
            return remaining
        }

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        }
    }

    init {
        setUnlocalizedName("custom_coupon_ticket")
        setTextureName("rtm:ticket")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val from  = getFromStation(stack).ifEmpty { "??" }
        val to    = getToStation(stack).ifEmpty { "??" }
        val uses  = getRemainingUses(stack)
        val entry = if (isEntered(stack)) "【入場中】" else ""
        return "回数券 $from → $to (残${uses}回) $entry"
    }

    @Suppress("UNCHECKED_CAST")
    override fun addInformation(stack: ItemStack, player: EntityPlayer, tooltip: MutableList<*>, advanced: Boolean) {
        val list = tooltip as MutableList<String>
        list.add("残り ${getRemainingUses(stack)} / $MAX_USES 回")
        if (isEntered(stack)) list.add("入場中: ${getEntryStation(stack)}")
    }
}
