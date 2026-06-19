package jp.sakuramochi.kaisatsupatch.gui

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor.Companion.SLOT_ICCARD
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

class ContainerCustomVendor(
    private val playerInv: InventoryPlayer,
    private val tile: TileEntityCustomTicketVendor
) : Container() {

    val vendorInv = InventoryCustomVendor()

    companion object {
        // お金スロット 2×2
        val MONEY_POSITIONS = arrayOf(
            210 to 38, 230 to 38,
            210 to 58, 230 to 58
        )
        // ICカードスロット
        const val CARD_X = 220; const val CARD_Y = 96

        // プレイヤーインベントリ
        const val INV_X = 8; const val INV_Y = 138

        // GUIサイズ
        const val GUI_WIDTH  = 284
        const val GUI_HEIGHT = 222

        // 右パネル開始X（9列インベントリ右端 + 余白）
        const val RIGHT_PANEL_X = 176
    }

    init {
        // お金スロット 0-3（2×2）
        for ((i, pos) in MONEY_POSITIONS.withIndex()) {
            val (sx, sy) = pos
            addSlotToContainer(object : Slot(vendorInv, i, sx, sy) {
                override fun isItemValid(stack: ItemStack) = vendorInv.isItemValidForSlot(i, stack)
            })
        }
        // ICカードスロット (slot index 4)
        addSlotToContainer(object : Slot(vendorInv, SLOT_ICCARD, CARD_X, CARD_Y) {
            override fun isItemValid(stack: ItemStack) = vendorInv.isItemValidForSlot(SLOT_ICCARD, stack)
        })

        // プレイヤーインベントリ 3×9
        for (row in 0..2) for (col in 0..8)
            addSlotToContainer(Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18))
        // ホットバー
        for (col in 0..8)
            addSlotToContainer(Slot(playerInv, col, INV_X + col * 18, INV_Y + 58))
    }

    override fun canInteractWith(player: EntityPlayer): Boolean {
        val world = tile.worldObj ?: return false
        return world.getTileEntity(tile.xCoord, tile.yCoord, tile.zCoord) == tile &&
               player.getDistanceSq(tile.xCoord + 0.5, tile.yCoord + 0.5, tile.zCoord + 0.5) <= 64.0
    }

    override fun onContainerClosed(player: EntityPlayer) {
        super.onContainerClosed(player)
        if (!player.worldObj.isRemote) {
            for (i in 0 until vendorInv.sizeInventory) {
                val stack = vendorInv.getStackInSlotOnClosing(i) ?: continue
                if (!player.inventory.addItemStackToInventory(stack))
                    player.dropPlayerItemWithRandomChoice(stack, false)
            }
        }
    }

    override fun transferStackInSlot(player: EntityPlayer, index: Int): ItemStack? {
        @Suppress("UNCHECKED_CAST")
        val slots = inventorySlots as List<net.minecraft.inventory.Slot?>
        val slot = slots.getOrNull(index) ?: return null
        val stack = slot.stack ?: return null
        val copy = stack.copy()

        if (index < 5) {
            // 券売機スロット → プレイヤーインベントリへ
            if (!mergeItemStack(stack, 5, slots.size, true)) return null
        } else {
            // プレイヤーインベントリ → 券売機スロットへ（お金0-3、ICカード4）
            if (!mergeItemStack(stack, 0, 5, false)) return null
        }

        if (stack.stackSize == 0) slot.putStack(null) else slot.onSlotChanged()
        return copy
    }
}
