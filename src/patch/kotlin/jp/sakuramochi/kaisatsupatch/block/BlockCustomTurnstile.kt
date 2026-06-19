package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.ngt.rtm.item.ItemTicket
import jp.sakuramochi.kaisatsupatch.FareTable
import jp.sakuramochi.kaisatsupatch.KaizPatchGuiHandler
import jp.sakuramochi.kaisatsupatch.RTMKaisatsuPatchCore
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class BlockCustomTurnstile : BlockMachineBase(Material.iron) {

    init {
        setBlockName("custom_turnstile")
        setBlockTextureName("rtmkaisatsupatch:custom_turnstile")
        setLightOpacity(0)
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity = TileEntityCustomTurnstile()

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        val tile = world.getTileEntity(x, y, z) as? TileEntityCustomTurnstile ?: return true
        val heldItem = player.currentEquippedItem

        // ---------------------------------------------------------------
        // 設定ツールを持っている場合 → 設定操作のみ
        // ---------------------------------------------------------------
        if (heldItem?.item is ItemSettingsTool) {
            if (player.isSneaking) {
                // スニーク＋設定ツール → 見た目変更（モデル選択）
                if (world.isRemote) {
                    // RTM の IModelSelector インターフェース経由でモデル選択GUIを開く
                    val selector = tile as? jp.ngt.rtm.modelpack.IModelSelector
                    if (selector != null) {
                        net.minecraft.client.Minecraft.getMinecraft()
                            .displayGuiScreen(jp.ngt.rtm.gui.GuiSelectModel(world, selector))
                    }
                }
            } else {
                // 設定ツール（スニークなし）→ 改札機設定GUI（駅コード・モード）
                player.openGui(
                    RTMKaisatsuPatchCore.instance,
                    KaizPatchGuiHandler.GUI_TURNSTILE_CONFIG,
                    world, x, y, z
                )
            }
            return true
        }

        // ---------------------------------------------------------------
        // 通常操作 → 改札通過
        // ---------------------------------------------------------------
        // RTM標準操作（レンチ回転など）を優先
        if (this.clickMachine(world, x, y, z, player)) return true

        if (heldItem == null) {
            deny(world, player, "切符またはICカードを手に持ってください")
            return true
        }

        when (val item = heldItem.item) {
            is ItemCustomICCard -> handleICCard(world, x, y, z, player, heldItem, tile)
            is ItemCustomTicket -> handleCustomTicket(world, x, y, z, player, heldItem, tile)
            is ItemTicket       -> handleRTMTicket(world, x, y, z, player, heldItem, tile, item)
            else                -> deny(world, player, "切符またはICカードを手に持ってください")
        }
        return true
    }

    // -----------------------------------------------------------------------
    // ICカード処理
    // -----------------------------------------------------------------------
    private fun handleICCard(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile
    ) {
        if (world.isRemote) return

        when (tile.gateMode) {
            TileEntityCustomTurnstile.GateMode.ENTRY -> {
                val entryStation = ItemCustomICCard.getEntryStation(stack)
                if (entryStation.isNotEmpty()) {
                    deny(world, player, "すでに入場中です（${entryStation}）。出場してください")
                    return
                }
                ItemCustomICCard.setEntryStation(stack, tile.stationCode)
                openGate(world, x, y, z, tile)
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.GREEN}【入場】${tile.stationCode}　残高: ${ItemCustomICCard.getBalance(stack)}円"
                ))
            }
            TileEntityCustomTurnstile.GateMode.EXIT -> {
                val entryStation = ItemCustomICCard.getEntryStation(stack)
                if (entryStation.isEmpty()) {
                    deny(world, player, "入場記録がありません。入場改札を先に通過してください")
                    return
                }
                val fare = FareTable.getFare(entryStation, tile.stationCode)
                if (!ItemCustomICCard.deduct(stack, fare)) {
                    deny(world, player,
                        "残高不足です（残高: ${ItemCustomICCard.getBalance(stack)}円 / 運賃: ${fare}円）")
                    return
                }
                ItemCustomICCard.clearEntryStation(stack)
                openGate(world, x, y, z, tile)
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.GREEN}【出場】${tile.stationCode}　運賃: ${fare}円　残高: ${ItemCustomICCard.getBalance(stack)}円"
                ))
            }
        }
    }

    // -----------------------------------------------------------------------
    // カスタム切符処理
    // -----------------------------------------------------------------------
    private fun handleCustomTicket(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile
    ) {
        if (world.isRemote) return

        val from = ItemCustomTicket.getFromStation(stack)
        val to   = ItemCustomTicket.getToStation(stack)

        when (tile.gateMode) {
            TileEntityCustomTurnstile.GateMode.ENTRY -> {
                if (ItemCustomTicket.isUsed(stack)) {
                    deny(world, player, "この切符はすでに使用済みです")
                    return
                }
                if (from != tile.stationCode) {
                    deny(world, player, "この切符は ${from} 発の切符です（現在の駅: ${tile.stationCode}）")
                    return
                }
                ItemCustomTicket.markUsed(stack)
                openGate(world, x, y, z, tile)
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.GREEN}【入場】${from} → ${to}"
                ))
            }
            TileEntityCustomTurnstile.GateMode.EXIT -> {
                if (!ItemCustomTicket.isUsed(stack)) {
                    deny(world, player, "入場していない切符です。入場改札を先に通過してください")
                    return
                }
                if (to != tile.stationCode) {
                    deny(world, player, "この切符の着駅は ${to} です（現在の駅: ${tile.stationCode}）")
                    return
                }
                if (!player.capabilities.isCreativeMode) stack.stackSize--
                openGate(world, x, y, z, tile)
                player.addChatMessage(ChatComponentText(
                    "${EnumChatFormatting.GREEN}【出場】${to}　ご利用ありがとうございました"
                ))
            }
        }
    }

    // -----------------------------------------------------------------------
    // RTM本家切符処理（後方互換）
    // -----------------------------------------------------------------------
    private fun handleRTMTicket(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile,
        ticketItem: ItemTicket
    ) {
        openGate(world, x, y, z, tile)
        if (!world.isRemote && ticketItem.ticketType != 2) {
            val returned = ItemTicket.consumeTicket(stack)
            if (returned != null) dropBlockAsItem(world, x, y + 1, z, returned)
        }
    }

    // -----------------------------------------------------------------------
    // ゲート制御・ユーティリティ
    // -----------------------------------------------------------------------
    private fun openGate(world: World, x: Int, y: Int, z: Int, tile: TileEntityCustomTurnstile) {
        val meta = world.getBlockMetadata(x, y, z)
        if (!isOpen(meta) && !tile.canThrough()) {
            world.setBlockMetadataWithNotify(x, y, z, meta + 4, 2)
            tile.setCount(40)
        }
    }

    private fun deny(world: World, player: EntityPlayer, reason: String) {
        if (!world.isRemote) {
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.RED}×　$reason"))
            world.playSoundAtEntity(player, "note.bass", 1.0f, 0.5f)
        }
    }

    fun isOpen(meta: Int): Boolean = meta >= 4

    override fun getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB? =
        if (isOpen(world.getBlockMetadata(x, y, z))) null
        else super.getCollisionBoundingBoxFromPool(world, x, y, z)

    override fun getBlocksMovement(world: IBlockAccess, x: Int, y: Int, z: Int): Boolean =
        isOpen(world.getBlockMetadata(x, y, z))

    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase?, itemStack: ItemStack) {
        super.onBlockPlacedBy(world, x, y, z, entity, itemStack)
        val tile = world.getTileEntity(x, y, z)
        if (tile is TileEntityMachineBase && itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
            tile.modelName = itemStack.tagCompound.getString("ModelName")
        }
    }

    override fun isOpaqueCube(): Boolean = false
    override fun renderAsNormalBlock(): Boolean = false
}
