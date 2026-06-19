package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item

class ItemCustomICCard : Item() {
    init {
        unlocalizedName = "custom_ic_card"
        // RTMのICカードテクスチャを仮で流用
        setTextureName("rtm:icCard")
        creativeTab = CreativeTabs.tabTransport
    }
}