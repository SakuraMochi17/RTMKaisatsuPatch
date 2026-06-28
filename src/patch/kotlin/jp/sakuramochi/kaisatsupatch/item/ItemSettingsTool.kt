package jp.sakuramochi.kaisatsupatch.item

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class ItemSettingsTool : Item() {
    init {
        (this as net.minecraft.item.Item).setUnlocalizedName("settings_tool")
        setTextureName("rtmkaisatsupatch:settings_tool")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }

    @Suppress("UNCHECKED_CAST")
    override fun addInformation(stack: ItemStack, player: EntityPlayer, list: MutableList<*>, advanced: Boolean) {
        @Suppress("UNCHECKED_CAST")
        (list as MutableList<String>).apply {
            add("§7右クリック§r: 改札機・券売機・精算機などを設定")
            add("§7スニーク＋右クリック§r: モデル選択")
        }
    }
}
