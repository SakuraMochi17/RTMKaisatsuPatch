package jp.sakuramochi.kaisatsupatch.item

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureSettings
import jp.sakuramochi.kaisatsupatch.util.rememberCoords
import jp.sakuramochi.kaisatsupatch.util.rememberedCoords
import jp.sakuramochi.kaisatsupatch.util.sendError
import jp.sakuramochi.kaisatsupatch.util.sendSuccess
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class ItemSettingsTool : Item() {
    init {
        setUnlocalizedName("settings_tool")
        setTextureName("rtmkaisatsupatch:settings_tool")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    /**
     * スニーク＋ブロック右クリック時に呼ばれる（このとき block.onBlockActivated は
     * 呼ばれない）。発車標の設定ブロック⇄表示体のバインド操作をここで処理する。
     */
    override fun onItemUse(
        stack: ItemStack, player: EntityPlayer, world: World,
        x: Int, y: Int, z: Int, side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val te = world.getTileEntity(x, y, z)
        if (te !is TileEntityDepartureSettings && te !is TileEntityDepartureBoard) return false
        if (!player.isSneaking) return false   // 通常右クリックは各ブロックの GUI 側で処理

        if (!world.isRemote) {
            val mp = player as? EntityPlayerMP
            when (te) {
                is TileEntityDepartureSettings -> {
                    stack.rememberCoords(x, y, z)
                    mp?.sendSuccess("設定ブロックを記憶しました。発車標をスニーク右クリックでバインド")
                }
                is TileEntityDepartureBoard -> {
                    val c = stack.rememberedCoords()
                    if (c == null) {
                        mp?.sendError("先に設定ブロックをスニーク右クリックで記憶してください")
                    } else if (world.getTileEntity(c.x, c.y, c.z) !is TileEntityDepartureSettings) {
                        mp?.sendError("記憶した位置に設定ブロックが見つかりません")
                    } else {
                        te.bindTo(c.x, c.y, c.z)
                        mp?.sendSuccess("設定ブロック (${c.x}, ${c.y}, ${c.z}) にバインドしました")
                    }
                }
            }
        }
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<*>, advanced: Boolean) {
        @Suppress("UNCHECKED_CAST")
        (list as MutableList<String>).apply {
            add("§7右クリック§r: 各ブロックを設定")
            add("§7スニーク＋右クリック§r: モデル選択 / 発車標のバインド")
        }
    }
}
