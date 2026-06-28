package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class ItemBoardingCertificate : Item() {

    companion object {
        private const val TAG_BOARDING_STATION = "BoardingStation"

        lateinit var instance: ItemBoardingCertificate

        fun getBoardingStation(stack: ItemStack): String =
            if (stack.hasTagCompound()) stack.tagCompound.getString(TAG_BOARDING_STATION) else ""

        fun create(stationName: String): ItemStack {
            val stack = ItemStack(instance)
            val tag = NBTTagCompound()
            tag.setString(TAG_BOARDING_STATION, stationName)
            stack.tagCompound = tag
            return stack
        }
    }

    init {
        (this as net.minecraft.item.Item).setUnlocalizedName("boarding_certificate")
        setTextureName("rtm:ticket")
        maxStackSize = 1
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val station = getBoardingStation(stack)
        return if (station.isEmpty()) "乗車駅証明書" else "${station} 乗車駅証明書"
    }
}
