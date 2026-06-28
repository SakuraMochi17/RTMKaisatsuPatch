package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.ChatComponentText
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants

class ItemCustomICCard : Item() {

    companion object {
        const val MAX_BALANCE = 20000
        private const val TAG_BALANCE        = "Balance"
        private const val TAG_ENTRY_STATION  = "EntryStation"
        private const val TAG_ENTRY_TIME     = "EntryTime"
        private const val TAG_HISTORY        = "History"
        private const val MAX_HISTORY        = 10
        const val TAG_COMPANY_ID    = "CompanyID"
        const val TAG_COMPANY_COLOR = "CompanyColor"
        const val TAG_COMPANY_NAME  = "CompanyName"

        data class HistoryEntry(val type: String, val station: String, val amount: Int, val time: Long)

        // ── 会社情報 ──────────────────────────────────────────────────

        fun initCompany(stack: ItemStack, companyID: String, companyColor: Int, companyName: String) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_COMPANY_ID, companyID)
            stack.tagCompound.setInteger(TAG_COMPANY_COLOR, companyColor)
            stack.tagCompound.setString(TAG_COMPANY_NAME, companyName)
        }

        fun getCompanyID(stack: ItemStack): String {
            ensureTag(stack); return stack.tagCompound.getString(TAG_COMPANY_ID)
        }

        fun getCompanyColor(stack: ItemStack): Int {
            ensureTag(stack)
            return if (stack.tagCompound.hasKey(TAG_COMPANY_COLOR))
                stack.tagCompound.getInteger(TAG_COMPANY_COLOR) else 0x1E90FF
        }

        fun getCompanyName(stack: ItemStack): String {
            ensureTag(stack)
            val name = stack.tagCompound.getString(TAG_COMPANY_NAME)
            return name.ifEmpty { "IC" }
        }

        // ── 残高 ─────────────────────────────────────────────────────

        fun getBalance(stack: ItemStack): Int {
            ensureTag(stack); return stack.tagCompound.getInteger(TAG_BALANCE)
        }

        fun setBalance(stack: ItemStack, amount: Int) {
            ensureTag(stack)
            stack.tagCompound.setInteger(TAG_BALANCE, amount.coerceIn(0, MAX_BALANCE))
        }

        fun charge(stack: ItemStack, amount: Int): Boolean {
            val current = getBalance(stack)
            if (current + amount > MAX_BALANCE) return false
            setBalance(stack, current + amount); return true
        }

        fun deduct(stack: ItemStack, fare: Int): Boolean {
            val current = getBalance(stack)
            if (current < fare) return false
            setBalance(stack, current - fare); return true
        }

        // ── 入場 ─────────────────────────────────────────────────────

        fun setEntryStation(stack: ItemStack, stationCode: String) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_ENTRY_STATION, stationCode)
            stack.tagCompound.setLong(TAG_ENTRY_TIME, System.currentTimeMillis())
        }

        fun getEntryTime(stack: ItemStack): Long {
            ensureTag(stack); return stack.tagCompound.getLong(TAG_ENTRY_TIME)
        }

        fun getEntryStation(stack: ItemStack): String {
            ensureTag(stack); return stack.tagCompound.getString(TAG_ENTRY_STATION)
        }

        fun clearEntryStation(stack: ItemStack) {
            ensureTag(stack); stack.tagCompound.removeTag(TAG_ENTRY_STATION)
        }

        // ── 履歴 ─────────────────────────────────────────────────────

        fun addHistory(stack: ItemStack, type: String, station: String, amount: Int) {
            ensureTag(stack)
            val existing = if (stack.tagCompound.hasKey(TAG_HISTORY))
                stack.tagCompound.getTagList(TAG_HISTORY, Constants.NBT.TAG_COMPOUND)
            else NBTTagList()
            val newEntry = NBTTagCompound().also {
                it.setString("Type", type); it.setString("Station", station)
                it.setInteger("Amount", amount); it.setLong("Time", System.currentTimeMillis())
            }
            val newList = NBTTagList()
            newList.appendTag(newEntry)
            for (i in 0 until minOf(existing.tagCount(), MAX_HISTORY - 1))
                newList.appendTag(existing.getCompoundTagAt(i))
            stack.tagCompound.setTag(TAG_HISTORY, newList)
        }

        fun getHistory(stack: ItemStack): List<HistoryEntry> {
            ensureTag(stack)
            if (!stack.tagCompound.hasKey(TAG_HISTORY)) return emptyList()
            val list = stack.tagCompound.getTagList(TAG_HISTORY, Constants.NBT.TAG_COMPOUND)
            return (0 until list.tagCount()).map { i ->
                val e = list.getCompoundTagAt(i)
                HistoryEntry(e.getString("Type"), e.getString("Station"), e.getInteger("Amount"), e.getLong("Time"))
            }
        }

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        }
    }

    init {
        (this as net.minecraft.item.Item).setUnlocalizedName("custom_ic_card")
        setTextureName("rtm:icCard")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    override fun onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack {
        if (!world.isRemote) {
            val balance = getBalance(stack)
            val entry = getEntryStation(stack)
            val history = getHistory(stack)
            val fmt = java.text.SimpleDateFormat("MM/dd HH:mm")
            player.addChatMessage(ChatComponentText("§b=== IC 履歴 ==="))
            val entryStr = if (entry.isNotEmpty()) " §e(入場中: $entry)" else ""
            player.addChatMessage(ChatComponentText("残高: §f${"%,d".format(balance)}円$entryStr"))
            if (history.isEmpty()) {
                player.addChatMessage(ChatComponentText("§7（履歴なし）"))
            } else {
                history.forEach { h ->
                    val time = fmt.format(java.util.Date(h.time))
                    val amount = when {
                        h.amount > 0 -> " §a+${"%,d".format(h.amount)}円"
                        h.amount < 0 -> " §c${"%,d".format(h.amount)}円"
                        else -> ""
                    }
                    val st = if (h.station.isNotEmpty()) " ${h.station}" else ""
                    player.addChatMessage(ChatComponentText("§7[$time] §f${h.type}${st}${amount}"))
                }
            }
        }
        return stack
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val name = getCompanyName(stack)
        return "${name}カード"
    }

    @Suppress("UNCHECKED_CAST")
    override fun addInformation(stack: ItemStack, player: EntityPlayer, tooltip: MutableList<*>, advanced: Boolean) {
        val list = tooltip as MutableList<String>
        val companyID = getCompanyID(stack)
        if (companyID.isNotEmpty()) list.add("§7発行: ${getCompanyName(stack)} [${companyID}]")
        list.add("残高: ${getBalance(stack)}円")
        val entry = getEntryStation(stack)
        if (entry.isNotEmpty()) {
            val time = getEntryTime(stack)
            val timeStr = if (time > 0L)
                " (${java.text.SimpleDateFormat("MM/dd HH:mm").format(java.util.Date(time))})"
            else ""
            list.add("入場中: $entry$timeStr")
        }
        val history = getHistory(stack)
        if (history.isNotEmpty()) {
            list.add("§7--- 最近の履歴 ---")
            history.take(5).forEach { h ->
                val timeStr = java.text.SimpleDateFormat("MM/dd HH:mm").format(java.util.Date(h.time))
                val amountStr = when {
                    h.amount > 0 -> " §a+${h.amount}円"
                    h.amount < 0 -> " §c${h.amount}円"
                    else -> ""
                }
                list.add("§7[$timeStr] §f${h.type}${if (h.station.isNotEmpty()) " ${h.station}" else ""}$amountStr")
            }
        }
    }
}
