package jp.sakuramochi.kaisatsupatch.item

import jp.sakuramochi.kaisatsupatch.util.initMaxStackSize
import jp.sakuramochi.kaisatsupatch.util.initName
import jp.sakuramochi.kaisatsupatch.util.initTexture
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
        initName("boarding_certificate")
        initTexture("rtm:ticket")
        initMaxStackSize(1)
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val station = getBoardingStation(stack)
        return if (station.isEmpty()) "乗車駅証明書" else "${station} 乗車駅証明書"
    }
}
