package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.RTMItem
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityFareAdjustment
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkManager
import jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenVendorConfig
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World

class BlockFareAdjustment : BlockContainer(Material.iron) {

    init {
        setBlockName("fare_adjustment")
        setBlockTextureName("rtmkaisatsupatch:station_manager")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityFareAdjustment()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityFareAdjustment ?: return true

        // 設定ツール → 設置駅選択GUI
        if (player.currentEquippedItem?.item is ItemSettingsTool) {
            if (!world.isRemote) {
                val stations = KaisatsuNetworkData.get(world)?.globalStations?.keys?.sorted() ?: emptyList()
                KaizPatchNetwork.CHANNEL.sendTo(
                    PacketOpenVendorConfig(x, y, z, tile.stationName, stations),
                    player as EntityPlayerMP
                )
            }
            return true
        }

        if (world.isRemote) return true

        val heldStack = player.currentEquippedItem
        if (heldStack == null || heldStack.item !is ItemCustomTicket) {
            player.addChatMessage(ChatComponentText("使用済み乗車券を手に持ってください"))
            return true
        }
        if (!ItemCustomTicket.isUsed(heldStack)) {
            player.addChatMessage(ChatComponentText("この切符はまだ使用されていません（先に入場改札を通ってください）"))
            return true
        }
        if (tile.stationName == "未設定") {
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}精算機の設置駅が設定されていません（設定ツールで設定してください）"))
            return true
        }

        val from = ItemCustomTicket.getFromStation(heldStack)
        val to   = ItemCustomTicket.getToStation(heldStack)
        val current = tile.stationName

        if (to == current) {
            player.addChatMessage(ChatComponentText("この切符は ${current} まで有効です。精算不要です"))
            return true
        }

        val originalFare = KaisatsuNetworkManager.calculateFare(world, from, to)
        val newFare      = KaisatsuNetworkManager.calculateFare(world, from, current)

        if (originalFare < 0 || newFare < 0) {
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}運賃データが見つかりません（路線設定を確認してください）"))
            return true
        }

        val extraFare = newFare - originalFare

        if (extraFare <= 0) {
            ItemCustomTicket.setToStation(heldStack, current)
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.GREEN}精算しました（追加料金なし）。${current} で出場できます"))
            return true
        }

        if (!deductMoney(player, extraFare)) {
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}残高不足です（精算額: ${extraFare}円）"))
            return true
        }

        ItemCustomTicket.setToStation(heldStack, current)
        addSales(world, current, extraFare.toLong())
        player.addChatMessage(ChatComponentText(
            "${EnumChatFormatting.GREEN}乗越精算しました（${extraFare}円）。${current} で出場できます"))
        return true
    }

    private fun deductMoney(player: EntityPlayer, amount: Int): Boolean {
        if (amount <= 0) return true
        val inv = player.inventory
        var total = 0
        for (i in 0 until inv.sizeInventory) {
            val s = inv.getStackInSlot(i) ?: continue
            if (s.item != RTMItem.money) continue
            total += RTMItem.MoneyType.getPrice(s.itemDamage) * s.stackSize
        }
        if (total < amount) return false
        for (i in 0 until inv.sizeInventory) {
            val s = inv.getStackInSlot(i) ?: continue
            if (s.item != RTMItem.money) continue
            inv.setInventorySlotContents(i, null)
        }
        var change = total - amount
        for ((id, value) in InventoryCustomVendor.DENOMINATIONS) {
            val count = change / value
            if (count > 0) {
                val s = ItemStack(RTMItem.money, count, id)
                if (!inv.addItemStackToInventory(s))
                    player.dropPlayerItemWithRandomChoice(s, false)
                change -= count * value
            }
        }
        return true
    }

    private fun addSales(world: World, stationName: String, amount: Long) {
        val data = KaisatsuNetworkData.get(world) ?: return
        data.stationSales[stationName] = (data.stationSales[stationName] ?: 0L) + amount
        val bd = data.stationSalesDetail.getOrPut(stationName) { KaisatsuNetworkData.SalesBreakdown() }
        bd.ticket += amount
        data.markDirty()
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
