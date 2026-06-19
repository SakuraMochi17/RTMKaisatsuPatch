package jp.sakuramochi.kaisatsupatch.gui

import jp.ngt.rtm.RTMItem
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

class InventoryCustomVendor : IInventory {
    // 0-3: RTMお金（2×2）  4: ICカード
    private val slots = arrayOfNulls<ItemStack>(5)

    companion object {
        const val SLOT_ICCARD = 4
        // 大きい面額から順に並べたお釣り計算用リスト (MoneyType.id to 円)
        val DENOMINATIONS = listOf(
            8 to 10000, 7 to 5000, 6 to 1000, 5 to 500,
            4 to 100,   3 to 50,   2 to 10,   1 to 5,   0 to 1
        )
    }

    override fun getSizeInventory() = 5
    override fun getStackInSlot(i: Int): ItemStack? = slots.getOrNull(i)
    override fun getInventoryName() = "VendorSlots"
    override fun hasCustomInventoryName() = false
    override fun getInventoryStackLimit() = 64
    override fun markDirty() {}
    override fun isUseableByPlayer(player: EntityPlayer) = true
    override fun openInventory() {}
    override fun closeInventory() {}

    override fun getStackInSlotOnClosing(i: Int): ItemStack? {
        val stack = slots[i]; slots[i] = null; return stack
    }

    override fun decrStackSize(i: Int, amount: Int): ItemStack? {
        val stack = slots[i] ?: return null
        if (stack.stackSize <= amount) { slots[i] = null; return stack }
        val split = stack.splitStack(amount)
        if (stack.stackSize == 0) slots[i] = null
        return split
    }

    override fun setInventorySlotContents(i: Int, stack: ItemStack?) {
        if (i in 0..4) slots[i] = stack
    }

    override fun isItemValidForSlot(i: Int, stack: ItemStack): Boolean = when (i) {
        in 0..3 -> stack.item == RTMItem.money
        4       -> stack.item is ItemCustomICCard
        else    -> false
    }

    fun getICCardStack(): ItemStack? = slots[SLOT_ICCARD]

    /** 4スロット合計金額（円） */
    fun getMoneyYen(): Int = (0..3).sumOf { i ->
        val s = slots[i] ?: return@sumOf 0
        if (s.item != RTMItem.money) return@sumOf 0
        RTMItem.MoneyType.getPrice(s.itemDamage) * s.stackSize
    }

    /**
     * 指定金額を消費してお釣りをプレイヤーインベントリに返す。
     * 残高不足なら false を返す（スロットは変更しない）。
     */
    fun payAndChange(cost: Int, player: EntityPlayer): Boolean {
        val total = getMoneyYen()
        if (total < cost) return false

        // 全スロットのお金を回収
        for (i in 0..3) slots[i] = null

        // お釣りを計算して返却
        var change = total - cost
        for ((id, value) in DENOMINATIONS) {
            val count = change / value
            if (count > 0) {
                val stack = ItemStack(RTMItem.money, count, id)
                if (!player.inventory.addItemStackToInventory(stack)) {
                    player.dropPlayerItemWithRandomChoice(stack, false)
                }
                change -= count * value
            }
        }
        return true
    }

    fun getICBalance(): Int? {
        val card = slots[SLOT_ICCARD] ?: return null
        return ItemCustomICCard.getBalance(card)
    }
}
