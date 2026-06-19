package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item

class ItemSettingsTool : Item() {
    init {
        unlocalizedName = "settings_tool"
        // RTMのクロウバーテクスチャを暫定利用（Phase4で独自モデルに変更予定）
        setTextureName("rtm:crowbar")
        creativeTab = CreativeTabs.tabTransport
        maxStackSize = 1
    }
}
