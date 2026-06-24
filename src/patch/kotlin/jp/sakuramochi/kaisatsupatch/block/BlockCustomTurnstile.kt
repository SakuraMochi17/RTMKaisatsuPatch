package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.ngt.rtm.item.ItemTicket
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile.GateMode
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkManager
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import jp.sakuramochi.kaisatsupatch.item.ItemCustomPass
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenTurnstileConfig
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.entity.player.EntityPlayerMP
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
        // 設定ツール → 設定操作
        // ---------------------------------------------------------------
        if (heldItem?.item is ItemSettingsTool) {
            if (player.isSneaking) {
                // スニーク＋設定ツール → モデル選択
                if (world.isRemote) {
                    val selector = tile as? jp.ngt.rtm.modelpack.IModelSelector
                    if (selector != null) {
                        Minecraft.getMinecraft().displayGuiScreen(jp.ngt.rtm.gui.GuiSelectModel(world, selector))
                    }
                }
            } else {
                // 設定ツール → 改札設定GUI（駅選択・モード）をパケット経由で開く
                if (!world.isRemote) {
                    val stationList = KaisatsuNetworkData.get(world)?.globalStations?.keys?.sorted() ?: emptyList()
                    KaizPatchNetwork.CHANNEL.sendTo(
                        PacketOpenTurnstileConfig(x, y, z, tile.stationCode, tile.gateMode.name, stationList),
                        player as EntityPlayerMP
                    )
                }
            }
            return true
        }

        // ---------------------------------------------------------------
        // 通常操作 → 改札通過
        // ---------------------------------------------------------------
        if (this.clickMachine(world, x, y, z, player)) return true

        if (heldItem == null) {
            deny(world, player, "切符またはICカードを手に持ってください")
            return true
        }

        when (val item = heldItem.item) {
            is ItemCustomICCard -> handleICCard(world, x, y, z, player, heldItem, tile)
            is ItemCustomTicket -> handleCustomTicket(world, x, y, z, player, heldItem, tile)
            is ItemCustomPass   -> handlePass(world, x, y, z, player, heldItem, tile)
            is ItemTicket       -> handleRTMTicket(world, x, y, z, player, heldItem, tile, item)
            else                -> deny(world, player, "切符・ICカード・定期券を手に持ってください")
        }
        return true
    }

    // -----------------------------------------------------------------------
    // ICカード処理（入場/出場/兼用 すべて対応）
    // -----------------------------------------------------------------------
    private fun handleICCard(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile
    ) {
        if (world.isRemote) return
        val mode = tile.gateMode
        val entryStation = ItemCustomICCard.getEntryStation(stack)

        val canEntry = mode == GateMode.ENTRY || mode == GateMode.BOTH
        val canExit  = mode == GateMode.EXIT  || mode == GateMode.BOTH

        // 未入場 → 入場処理
        if (entryStation.isEmpty() && canEntry) {
            ItemCustomICCard.setEntryStation(stack, tile.stationCode)
            openGate(world, x, y, z, tile)
            player.addChatMessage(ChatComponentText(
                "${EnumChatFormatting.GREEN}【入場】${tile.stationCode}　残高: ${ItemCustomICCard.getBalance(stack)}円"
            ))
            return
        }

        // 入場済み → 出場処理
        if (entryStation.isNotEmpty() && canExit) {
            val fare = KaisatsuNetworkManager.calculateFare(world, entryStation, tile.stationCode, isICCard = true)
            if (fare < 0) {
                deny(world, player, "この区間の運賃データがありません（${entryStation} → ${tile.stationCode}）")
                return
            }
            if (!ItemCustomICCard.deduct(stack, fare)) {
                val balance = ItemCustomICCard.getBalance(stack)
                deny(world, player, "残高不足です（運賃: ${fare}円 / 残高: ${balance}円 / 不足: ${fare - balance}円）")
                return
            }
            ItemCustomICCard.clearEntryStation(stack)
            openGate(world, x, y, z, tile)
            player.addChatMessage(ChatComponentText(
                "${EnumChatFormatting.GREEN}【出場】${tile.stationCode}　運賃: ${fare}円　残高: ${ItemCustomICCard.getBalance(stack)}円"
            ))
            return
        }

        // モード不一致
        if (entryStation.isNotEmpty() && !canExit) {
            deny(world, player, "この改札は入場専用です。出場改札を使ってください")
        } else if (entryStation.isEmpty() && !canEntry) {
            deny(world, player, "この改札は出場専用です。先に入場改札を通ってください")
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
        val mode = tile.gateMode
        val from = ItemCustomTicket.getFromStation(stack)
        val to   = ItemCustomTicket.getToStation(stack)
        val used = ItemCustomTicket.isUsed(stack)

        val canEntry = mode == GateMode.ENTRY || mode == GateMode.BOTH
        val canExit  = mode == GateMode.EXIT  || mode == GateMode.BOTH

        if (!used && canEntry) {
            if (from != tile.stationCode) {
                deny(world, player, "この切符は ${from} 発の切符です（現在の駅: ${tile.stationCode}）"); return
            }
            ItemCustomTicket.markUsed(stack)
            openGate(world, x, y, z, tile)
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.GREEN}【入場】${from} → ${to}"))
        } else if (used && canExit) {
            if (to != tile.stationCode) {
                deny(world, player, "この切符の着駅は ${to} です（現在の駅: ${tile.stationCode}）"); return
            }
            if (!player.capabilities.isCreativeMode) stack.stackSize--
            openGate(world, x, y, z, tile)
            player.addChatMessage(ChatComponentText("${EnumChatFormatting.GREEN}【出場】${to}　ご利用ありがとうございました"))
        } else if (used && !canExit) {
            deny(world, player, "この改札は入場専用です")
        } else if (!used && !canEntry) {
            deny(world, player, "この改札は出場専用です")
        } else {
            deny(world, player, "この切符はすでに使用済みです")
        }
    }

    // -----------------------------------------------------------------------
    // 定期券処理
    // -----------------------------------------------------------------------
    private fun handlePass(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile
    ) {
        if (world.isRemote) return
        val currentDay = ItemCustomPass.currentDay(world)
        if (!ItemCustomPass.isValid(stack, tile.stationCode, currentDay, world)) {
            val remaining = ItemCustomPass.remainingDays(stack, currentDay)
            if (remaining <= 0) {
                deny(world, player, "定期券の有効期限が切れています")
            } else {
                deny(world, player, "この定期券は ${tile.stationCode} では使用できません（区間外）")
            }
            return
        }
        openGate(world, x, y, z, tile)
        val remaining = ItemCustomPass.remainingDays(stack, currentDay)
        player.addChatMessage(ChatComponentText(
            "${EnumChatFormatting.GREEN}【定期】${tile.stationCode}　残り ${remaining} 日"
        ))
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
        if (entity is EntityPlayer && tile is TileEntityMachineBase) {
            // RTM と同じ方法: metadata に dir, setRotation で yaw を記録
            val dir = (MathHelper.floor_double(jp.ngt.ngtlib.math.NGTMath.normalizeAngle(entity.rotationYaw + 180.0) / 90.0 + 0.5) and 3).toInt()
            world.setBlockMetadataWithNotify(x, y, z, dir, 2)
            tile.setRotation(entity, 90.0f, true)
        }
        if (tile is TileEntityMachineBase && itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
            tile.modelName = itemStack.tagCompound.getString("ModelName")
        }
    }

    override fun isOpaqueCube(): Boolean = false
    override fun renderAsNormalBlock(): Boolean = false
}
