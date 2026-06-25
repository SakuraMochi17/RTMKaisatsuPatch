package jp.sakuramochi.kaisatsupatch.block

import net.minecraft.block.Block
import net.minecraft.item.ItemBlock

class ItemBlockDepartureBoard(block: Block) : ItemBlock(block) {
    init { setMaxStackSize(16) }
}
