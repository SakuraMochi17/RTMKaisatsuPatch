package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item

class ItemCustomTicket : Item() {
    init {
        unlocalizedName = "custom_ticket"
        // RTMの切符テクスチャを仮で流用（後で独自のものに変更可能）
        setTextureName("rtm:ticket")
        creativeTab = CreativeTabs.tabTransport // クリエイティブの「交通」タブに表示
    }
}