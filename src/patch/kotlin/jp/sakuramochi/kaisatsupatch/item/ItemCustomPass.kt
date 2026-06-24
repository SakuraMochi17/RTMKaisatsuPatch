package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class ItemCustomPass : Item() {

    companion object {
        private const val TAG_FROM   = "FromStation"
        private const val TAG_TO     = "ToStation"
        private const val TAG_EXPIRY = "ExpiryDay"   // ワールド時間（日数）で管理

        /** 1MCday = 24000 ticks */
        const val TICKS_PER_DAY = 24000L

        fun init(stack: ItemStack, fromStation: String, toStation: String, currentDay: Long, durationDays: Int) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_FROM, fromStation)
            stack.tagCompound.setString(TAG_TO, toStation)
            stack.tagCompound.setLong(TAG_EXPIRY, currentDay + durationDays)
        }

        fun getFromStation(stack: ItemStack): String { ensureTag(stack); return stack.tagCompound.getString(TAG_FROM) }
        fun getToStation(stack: ItemStack): String   { ensureTag(stack); return stack.tagCompound.getString(TAG_TO) }
        fun getExpiryDay(stack: ItemStack): Long     { ensureTag(stack); return stack.tagCompound.getLong(TAG_EXPIRY) }

        /** fromStation/toStation のどちらかが現在駅で、かつ期限内なら有効 */
        fun isValid(stack: ItemStack, currentStation: String, currentDay: Long): Boolean {
            ensureTag(stack)
            if (currentDay > getExpiryDay(stack)) return false
            val from = getFromStation(stack)
            val to   = getToStation(stack)
            return currentStation == from || currentStation == to
        }

        /** 乗車区間が一致するか（from↔to 双方向） */
        fun coversRoute(stack: ItemStack, stationA: String, stationB: String): Boolean {
            val from = getFromStation(stack)
            val to   = getToStation(stack)
            return (from == stationA && to == stationB) || (from == stationB && to == stationA)
        }

        fun currentDay(world: net.minecraft.world.World): Long = world.totalWorldTime / TICKS_PER_DAY

        fun remainingDays(stack: ItemStack, currentDay: Long): Long = maxOf(0L, getExpiryDay(stack) - currentDay)

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        }
    }

    init {
        unlocalizedName = "custom_pass"
        setTextureName("rtm:ticket")   // 暫定テクスチャ
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val from = getFromStation(stack).ifEmpty { "??" }
        val to   = getToStation(stack).ifEmpty { "??" }
        return "定期券 $from ⇔ $to"
    }

    @Suppress("UNCHECKED_CAST")
    override fun addInformation(stack: ItemStack, player: EntityPlayer, tooltip: MutableList<*>, advanced: Boolean) {
        val list = tooltip as MutableList<String>
        val currentDay = if (player.worldObj != null) currentDay(player.worldObj) else 0L
        val remaining  = remainingDays(stack, currentDay)
        val expiry     = getExpiryDay(stack)
        if (remaining > 0) {
            list.add("残り ${remaining} 日（Day ${expiry} まで有効）")
        } else {
            list.add("§c期限切れ")
        }
        list.add("${getFromStation(stack)} ⇔ ${getToStation(stack)}")
    }
}
