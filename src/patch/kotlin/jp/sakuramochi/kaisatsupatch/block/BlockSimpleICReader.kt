package jp.sakuramochi.kaisatsupatch.block

import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntitySimpleICReader
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenVendorConfig
import jp.sakuramochi.kaisatsupatch.util.sendError
import jp.sakuramochi.kaisatsupatch.util.sendSuccess
import jp.sakuramochi.kaisatsupatch.util.sendWarn
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class BlockSimpleICReader : BlockContainer(Material.iron) {

    companion object {
        private const val MIN_BALANCE = 150
    }

    init {
        (this as net.minecraft.block.Block).setBlockName("simple_ic_reader")
        (this as net.minecraft.block.Block).setBlockTextureName("rtmkaisatsupatch:simple_ic_reader")
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntitySimpleICReader()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntitySimpleICReader ?: return true

        if (player.currentEquippedItem?.item is ItemSettingsTool) {
            if (!world.isRemote) {
                val stations = KaisatsuNetworkData.get(world)?.globalStations?.keys?.sorted() ?: emptyList()
                val companies = KaisatsuNetworkData.get(world)?.companies?.map { (id, co) -> id to co.companyName } ?: emptyList()
                KaizPatchNetwork.CHANNEL.sendTo(
                    PacketOpenVendorConfig(x, y, z, tile.stationName, tile.companyID, stations, companies),
                    player as EntityPlayerMP
                )
            }
            return true
        }

        if (world.isRemote) return true

        val mp = player as? EntityPlayerMP ?: return true
        val heldStack = player.currentEquippedItem

        if (heldStack == null || heldStack.item !is ItemCustomICCard) {
            mp.sendError("IC カードを手に持ってください")
            return true
        }

        if (tile.stationName == "未設定") {
            mp.sendError("リーダーの設置駅が設定されていません（設定ツールで右クリック）")
            return true
        }

        // 会社チェック（companyID が設定されている場合のみ）
        if (tile.companyID.isNotEmpty()) {
            val cardCompany = ItemCustomICCard.getCompanyID(heldStack)
            val data = KaisatsuNetworkData.get(world)
            val allowed = cardCompany.isEmpty()
                || cardCompany == tile.companyID
                || data?.companies?.get(tile.companyID)?.allowedCompanies?.contains(cardCompany) == true
            if (!allowed) {
                mp.sendError("このカードはご利用いただけません")
                return true
            }
        }

        // 既に入場済みか確認
        val entryStation = ItemCustomICCard.getEntryStation(heldStack)
        if (entryStation.isNotEmpty()) {
            mp.sendWarn("既に ${entryStation} から入場中です。出場してから再度タッチしてください")
            return true
        }

        // 最低残高チェック
        val balance = ItemCustomICCard.getBalance(heldStack)
        if (balance < MIN_BALANCE) {
            mp.sendError("残高不足です（残高: ${balance}円 / 最低 ${MIN_BALANCE}円 必要）チャージしてください")
            return true
        }

        // 入場記録（残高は引かない）
        ItemCustomICCard.setEntryStation(heldStack, tile.stationName)
        ItemCustomICCard.addHistory(heldStack, "入場", tile.stationName, 0)

        KaisatsuNetworkData.get(world)?.let { data ->
            data.addGateLog(tile.stationName, player.commandSenderName, "入場", "IC(簡易)")
        }

        mp.sendSuccess("【入場】${tile.stationName}　残高: ${balance}円")
        return true
    }

    override fun isOpaqueCube() = false
    override fun renderAsNormalBlock() = false
}
