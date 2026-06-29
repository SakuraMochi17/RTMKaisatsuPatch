package jp.sakuramochi.kaisatsupatch.item

import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.util.initCreativeTab
import jp.sakuramochi.kaisatsupatch.util.initMaxStackSize
import jp.sakuramochi.kaisatsupatch.util.initName
import jp.sakuramochi.kaisatsupatch.util.initTexture
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class ItemCustomPass : Item() {

    companion object {
        private const val TAG_FROM      = "FromStation"
        private const val TAG_TO        = "ToStation"
        private const val TAG_EXPIRY    = "ExpiryDay"
        private const val TAG_FREE_PASS = "FreePast"

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
        fun isFreePast(stack: ItemStack): Boolean    { ensureTag(stack); return stack.tagCompound.getBoolean(TAG_FREE_PASS) }

        fun initDayPass(stack: ItemStack, currentDay: Long) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_FROM, "*")
            stack.tagCompound.setString(TAG_TO, "*")
            stack.tagCompound.setLong(TAG_EXPIRY, currentDay + 1)
            stack.tagCompound.setBoolean(TAG_FREE_PASS, true)
        }

        /**
         * 現在駅が定期券の有効区間内かつ期限内なら有効。
         * world を渡すと路線データを参照して途中駅も判定する。
         */
        fun isValid(stack: ItemStack, currentStation: String, currentDay: Long, world: World? = null): Boolean {
            ensureTag(stack)
            if (currentDay > getExpiryDay(stack)) return false
            if (isFreePast(stack)) return true
            val from = getFromStation(stack)
            val to   = getToStation(stack)
            if (currentStation == from || currentStation == to) return true
            if (world != null) return isIntermediateStation(world, from, to, currentStation)
            return false
        }

        /** from〜to 間の路線上に station が含まれるか判定 */
        private fun isIntermediateStation(world: World, from: String, to: String, station: String): Boolean {
            val data = KaisatsuNetworkData.get(world) ?: return false
            for (line in data.companyLines.values) {
                val order = line.stationOrder
                val fromIdx = order.indexOf(from)
                val toIdx   = order.indexOf(to)
                val stIdx   = order.indexOf(station)
                if (fromIdx < 0 || toIdx < 0 || stIdx < 0) continue
                if (stIdx in minOf(fromIdx, toIdx)..maxOf(fromIdx, toIdx)) return true
            }
            return false
        }

        fun currentDay(world: World): Long = world.totalWorldTime / TICKS_PER_DAY

        fun remainingDays(stack: ItemStack, currentDay: Long): Long = maxOf(0L, getExpiryDay(stack) - currentDay)

        fun renewExpiry(stack: ItemStack, addDays: Int, currentDay: Long) {
            ensureTag(stack)
            val baseDay = maxOf(currentDay, getExpiryDay(stack))
            stack.tagCompound.setLong(TAG_EXPIRY, baseDay + addDays)
        }

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        }
    }

    init {
        initName("custom_pass")
        initTexture("rtm:ticket")
        initCreativeTab(CreativeTabs.tabTransport)
        initMaxStackSize(1)
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        if (isFreePast(stack)) return "フリーパス（1日）"
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
        when {
            remaining > 7  -> list.add("§a残り ${remaining} 日")
            remaining in 1..7 -> list.add("§e残り ${remaining} 日（まもなく期限切れ）")
            else           -> list.add("§c期限切れ")
        }
        if (isFreePast(stack)) list.add("全区間乗り放題")
        else list.add("${getFromStation(stack)} ⇔ ${getToStation(stack)}")
    }
}
