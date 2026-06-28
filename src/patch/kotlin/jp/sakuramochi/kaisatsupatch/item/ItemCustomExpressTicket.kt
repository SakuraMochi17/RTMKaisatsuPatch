package jp.sakuramochi.kaisatsupatch.item

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

class ItemCustomExpressTicket : Item() {

    companion object {
        private const val TAG_TRAIN_ID     = "TrainID"
        private const val TAG_TRAIN_NAME   = "TrainName"
        private const val TAG_FROM         = "FromStation"
        private const val TAG_TO           = "ToStation"
        private const val TAG_IS_RESERVED  = "IsReserved"
        private const val TAG_CAR_NUMBER   = "CarNumber"
        private const val TAG_SEAT_NUMBER  = "SeatNumber"
        private const val TAG_EXPRESS_FARE = "ExpressFare"

        fun init(
            stack: ItemStack,
            trainID: String,
            trainName: String,
            fromStation: String,
            toStation: String,
            isReserved: Boolean,
            carNumber: Int,
            seatNumber: Int,
            expressFare: Int
        ) {
            ensureTag(stack)
            stack.tagCompound.setString(TAG_TRAIN_ID, trainID)
            stack.tagCompound.setString(TAG_TRAIN_NAME, trainName)
            stack.tagCompound.setString(TAG_FROM, fromStation)
            stack.tagCompound.setString(TAG_TO, toStation)
            stack.tagCompound.setBoolean(TAG_IS_RESERVED, isReserved)
            stack.tagCompound.setInteger(TAG_CAR_NUMBER, carNumber)
            stack.tagCompound.setInteger(TAG_SEAT_NUMBER, seatNumber)
            stack.tagCompound.setInteger(TAG_EXPRESS_FARE, expressFare)
        }

        fun getTrainID(stack: ItemStack): String    { ensureTag(stack); return stack.tagCompound.getString(TAG_TRAIN_ID) }
        fun getTrainName(stack: ItemStack): String  { ensureTag(stack); return stack.tagCompound.getString(TAG_TRAIN_NAME) }
        fun getFromStation(stack: ItemStack): String{ ensureTag(stack); return stack.tagCompound.getString(TAG_FROM) }
        fun getToStation(stack: ItemStack): String  { ensureTag(stack); return stack.tagCompound.getString(TAG_TO) }
        fun isReserved(stack: ItemStack): Boolean   { ensureTag(stack); return stack.tagCompound.getBoolean(TAG_IS_RESERVED) }
        fun getCarNumber(stack: ItemStack): Int     { ensureTag(stack); return stack.tagCompound.getInteger(TAG_CAR_NUMBER) }
        fun getSeatNumber(stack: ItemStack): Int    { ensureTag(stack); return stack.tagCompound.getInteger(TAG_SEAT_NUMBER) }
        fun getExpressFare(stack: ItemStack): Int   { ensureTag(stack); return stack.tagCompound.getInteger(TAG_EXPRESS_FARE) }

        private fun ensureTag(stack: ItemStack) {
            if (!stack.hasTagCompound()) stack.tagCompound = NBTTagCompound()
        }
    }

    init {
        (this as net.minecraft.item.Item).setUnlocalizedName("express_ticket")
        setTextureName("rtm:ticket")
        maxStackSize = 1
    }

    override fun getItemStackDisplayName(stack: ItemStack): String {
        val name = getTrainName(stack).ifEmpty { "??" }
        val from = getFromStation(stack).ifEmpty { "??" }
        val to   = getToStation(stack).ifEmpty { "??" }
        return if (isReserved(stack)) {
            val car  = getCarNumber(stack)
            val seat = getSeatNumber(stack)
            "${name} ${car}号車${seat}番席 ${from}→${to}"
        } else {
            "${name} 自由席 ${from}→${to}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun addInformation(stack: ItemStack, player: EntityPlayer, tooltip: MutableList<*>, advanced: Boolean) {
        val lines = tooltip as MutableList<String>
        val name = getTrainName(stack).ifEmpty { "??" }
        val from = getFromStation(stack).ifEmpty { "??" }
        val to   = getToStation(stack).ifEmpty { "??" }
        val fare = getExpressFare(stack)
        lines.add("列車: $name")
        lines.add("区間: $from → $to")
        if (isReserved(stack)) {
            lines.add("${getCarNumber(stack)}号車 ${getSeatNumber(stack)}番席 (指定席)")
        } else {
            lines.add("自由席")
        }
        lines.add("特急料金: ${fare}円")
    }
}
