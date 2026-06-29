package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.ngt.rtm.item.ItemTicket
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile.GateMode
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkData
import jp.sakuramochi.kaisatsupatch.core.KaisatsuNetworkManager
import jp.sakuramochi.kaisatsupatch.item.ItemBoardingCertificate
import jp.sakuramochi.kaisatsupatch.item.ItemCustomCouponTicket
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import jp.sakuramochi.kaisatsupatch.item.ItemCustomPass
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import jp.sakuramochi.kaisatsupatch.item.ItemSettingsTool
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import jp.sakuramochi.kaisatsupatch.network.PacketOpenTurnstileConfig
import jp.sakuramochi.kaisatsupatch.util.isOp
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
        (this as net.minecraft.block.Block).setBlockName("custom_turnstile")
        (this as net.minecraft.block.Block).setBlockTextureName("rtmkaisatsupatch:custom_turnstile")
        (this as net.minecraft.block.Block).setLightOpacity(0)
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
            // 会社ロックチェック（サーバーサイドのみ）
            if (!world.isRemote) {
                val ownerID = tile.ownerCompanyID
                if (ownerID.isNotEmpty()) {
                    val data = KaisatsuNetworkData.get(world)
                    val company = data?.companies?.get(ownerID)
                    val isOp = (player as? EntityPlayerMP)?.isOp() ?: false
                    if (!isOp && company != null && !company.isMember(player.gameProfile.name)) {
                        player.addChatMessage(ChatComponentText(
                            "§c[${company.companyName}] の改札です。操作権限がありません"))
                        return true
                    }
                }
            }

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
                        PacketOpenTurnstileConfig(x, y, z, tile.stationCode, tile.gateMode.name, stationList, tile.openTicks, tile.passMessage),
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

        val gm = tile.gateMode
        when (val item = heldItem.item) {
            is ItemCustomICCard -> {
                if (!gm.allowsIC) { deny(world, player, "この改札はIC専用ではありません（${gm.displayName}）"); return true }
                // 会社ロック: 相互利用チェック
                if (!world.isRemote) {
                    val ownerID = tile.ownerCompanyID
                    if (ownerID.isNotEmpty()) {
                        val cardCompanyID = ItemCustomICCard.getCompanyID(heldItem)
                        val data = KaisatsuNetworkData.get(world)
                        val ownerCompany = data?.companies?.get(ownerID)
                        val allowed = cardCompanyID.isEmpty()                          // 会社なし=汎用IC
                            || cardCompanyID == ownerID                                // 自社カード
                            || ownerCompany?.allowedCompanies?.contains(cardCompanyID) == true // 相互利用許可
                        if (!allowed) {
                            val cardName = ItemCustomICCard.getCompanyName(heldItem)
                            val ownerName = ownerCompany?.companyName ?: ownerID
                            deny(world, player, "${cardName}はこの改札（${ownerName}）では利用できません")
                            return true
                        }
                    }
                }
                handleICCard(world, x, y, z, player, heldItem, tile)
            }
            is ItemCustomCouponTicket -> {
                if (!gm.allowsTicket) { deny(world, player, "この改札は切符専用ではありません（${gm.displayName}）"); return true }
                handleCouponTicket(world, x, y, z, player, heldItem, tile)
            }
            is ItemCustomTicket -> {
                if (!gm.allowsTicket) { deny(world, player, "この改札は切符専用ではありません（${gm.displayName}）"); return true }
                handleCustomTicket(world, x, y, z, player, heldItem, tile)
            }
            is ItemCustomPass -> {
                if (!gm.allowsPass) { deny(world, player, "この改札は定期専用ではありません（${gm.displayName}）"); return true }
                handlePass(world, x, y, z, player, heldItem, tile)
            }
            is ItemTicket -> {
                if (!gm.allowsTicket) { deny(world, player, "この改札は切符専用ではありません（${gm.displayName}）"); return true }
                handleRTMTicket(world, x, y, z, player, heldItem, tile, item)
            }
            is ItemBoardingCertificate -> {
                if (gm == GateMode.ENTRY) { deny(world, player, "この改札は入場専用です。精算機または出場改札をご利用ください"); return true }
                if (!world.isRemote) handleBoardingCertificate(world, x, y, z, player, heldItem, tile)
            }
            else -> deny(world, player, "切符・ICカード・定期券を手に持ってください")
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

        val canEntry = mode != GateMode.EXIT
        val canExit  = mode != GateMode.ENTRY

        // 未入場 → 入場処理
        if (entryStation.isEmpty() && canEntry) {
            ItemCustomICCard.setEntryStation(stack, tile.stationCode)
            ItemCustomICCard.addHistory(stack, "入場", tile.stationCode, 0)
            openGate(world, x, y, z, tile)
            allow(world, player, tile)
            KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "入場", "IC")
            player.addChatMessage(ChatComponentText(
                "§a[入場] ${tile.stationCode}  残高 ${ItemCustomICCard.getBalance(stack).yen()}"
            ))
            return
        }

        // 入場済み → 出場処理
        if (entryStation.isNotEmpty() && canExit) {
            // 入場駅がネットワーク上に存在するか確認（駅削除・リネーム後のスタック防止）
            if (KaisatsuNetworkData.get(world)?.globalStations?.containsKey(entryStation) == false) {
                ItemCustomICCard.clearEntryStation(stack)
                deny(world, player, "入場駅「${entryStation}」が削除されています。入場記録をリセットしました")
                return
            }
            val fare = KaisatsuNetworkManager.calculateFare(world, entryStation, tile.stationCode, isICCard = true)
            if (fare < 0) {
                deny(world, player, KaisatsuNetworkManager.fareErrorReason(world, entryStation, tile.stationCode))
                return
            }
            if (!ItemCustomICCard.deduct(stack, fare)) {
                val balance = ItemCustomICCard.getBalance(stack)
                deny(world, player, "残高不足 ${balance.yen()} / 必要 ${fare.yen()}")
                return
            }
            ItemCustomICCard.addHistory(stack, "出場", tile.stationCode, -fare)
            ItemCustomICCard.clearEntryStation(stack)
            openGate(world, x, y, z, tile)
            allow(world, player, tile)
            KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "出場", "IC")
            player.addChatMessage(ChatComponentText(
                "§a[出場] ${tile.stationCode}  -${fare.yen()} 残高 ${ItemCustomICCard.getBalance(stack).yen()}"
            ))
            return
        }

        // 方向不一致
        if (entryStation.isNotEmpty() && !canExit) {
            deny(world, player, "この改札は入場専用です。出場改札を使ってください")
        } else if (entryStation.isEmpty() && !canEntry) {
            deny(world, player, "この改札は出場専用です。先に入場改札を通ってください")
        }
    }

    // -----------------------------------------------------------------------
    // 回数券処理
    // -----------------------------------------------------------------------
    private fun handleCouponTicket(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile
    ) {
        if (world.isRemote) return
        val mode = tile.gateMode
        val from  = ItemCustomCouponTicket.getFromStation(stack)
        val to    = ItemCustomCouponTicket.getToStation(stack)
        val uses  = ItemCustomCouponTicket.getRemainingUses(stack)

        if (uses <= 0) {
            deny(world, player, "この回数券は使い切りました")
            return
        }

        val canEntry = mode != GateMode.EXIT
        val canExit  = mode != GateMode.ENTRY

        if (!ItemCustomCouponTicket.isEntered(stack)) {
            // 入場
            if (!canEntry) { deny(world, player, "この改札は出場専用です"); return }
            if (tile.stationCode != from) {
                deny(world, player, "この回数券は ${from} からの乗車専用です（現在: ${tile.stationCode}）")
                return
            }
            ItemCustomCouponTicket.markEntry(stack, tile.stationCode)
            openGate(world, x, y, z, tile)
            allow(world, player, tile)
            KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "入場", "回数券")
            player.addChatMessage(ChatComponentText(
                "§a[入場] ${tile.stationCode}  残り ${uses} 回"))
        } else {
            // 出場
            if (!canExit) { deny(world, player, "この改札は入場専用です"); return }
            if (tile.stationCode != to) {
                deny(world, player, "この回数券の着駅は ${to} です（現在: ${tile.stationCode}）")
                return
            }
            ItemCustomCouponTicket.clearEntry(stack)
            val remaining = ItemCustomCouponTicket.consumeUse(stack)
            openGate(world, x, y, z, tile)
            allow(world, player, tile)
            KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "出場", "回数券")
            if (remaining <= 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null)
                player.addChatMessage(ChatComponentText("§a[出場] ${to}  回数券を使い切りました"))
            } else {
                player.addChatMessage(ChatComponentText(
                    "§a[出場] ${to}  残り ${remaining} 回"))
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
        val mode = tile.gateMode
        val from = ItemCustomTicket.getFromStation(stack)
        val to   = ItemCustomTicket.getToStation(stack)
        val used = ItemCustomTicket.isUsed(stack)

        val canEntry = mode != GateMode.EXIT
        val canExit  = mode != GateMode.ENTRY

        if (!used && canEntry) {
            if (from != tile.stationCode) {
                deny(world, player, "この切符は ${from} 発の切符です（現在の駅: ${tile.stationCode}）"); return
            }
            ItemCustomTicket.markUsed(stack)
            openGate(world, x, y, z, tile)
            allow(world, player, tile)
            KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "入場", "切符")
            player.addChatMessage(ChatComponentText("§a[入場] ${from} → ${to}"))
        } else if (used && canExit) {
            if (to != tile.stationCode) {
                deny(world, player, "この切符の着駅は ${to} です（現在の駅: ${tile.stationCode}）"); return
            }
            if (!player.capabilities.isCreativeMode) stack.stackSize--
            openGate(world, x, y, z, tile)
            allow(world, player, tile)
            KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "出場", "切符")
            player.addChatMessage(ChatComponentText("§a[出場] ${to}  ご乗車ありがとうございました"))
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
        allow(world, player, tile)
        KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "通過", "定期券")
        val remaining = ItemCustomPass.remainingDays(stack, currentDay)
        player.addChatMessage(ChatComponentText(
            "§a[定期] ${tile.stationCode}  残り ${remaining} 日"
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
        allow(world, player, tile)
        if (!world.isRemote) KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "通過", "RTM切符")
        if (!world.isRemote && ticketItem.ticketType != 2) {
            val returned = ItemTicket.consumeTicket(stack)
            if (returned != null) dropBlockAsItem(world, x, y + 1, z, returned)
        }
    }

    // -----------------------------------------------------------------------
    // 乗車駅証明書 出場処理
    // -----------------------------------------------------------------------
    private fun handleBoardingCertificate(
        world: World, x: Int, y: Int, z: Int,
        player: EntityPlayer, stack: ItemStack, tile: TileEntityCustomTurnstile
    ) {
        val boardingStation = ItemBoardingCertificate.getBoardingStation(stack)
        if (boardingStation.isEmpty()) {
            deny(world, player, "無効な乗車駅証明書です"); return
        }
        val fare = KaisatsuNetworkManager.calculateFare(world, boardingStation, tile.stationCode)
        if (fare < 0) {
            deny(world, player, KaisatsuNetworkManager.fareErrorReason(world, boardingStation, tile.stationCode)); return
        }
        if (!deductMoney(player, fare)) {
            deny(world, player, "残高不足です（運賃: ${fare}円）"); return
        }
        stack.stackSize--
        KaisatsuNetworkData.get(world)?.addGateLog(tile.stationCode, player.gameProfile.name, "出場", "証明書")
        openGate(world, x, y, z, tile)
        allow(world, player, tile)
        player.addChatMessage(net.minecraft.util.ChatComponentText(
            "§a[出場] ${boardingStation} → ${tile.stationCode}  ${fare.yen()}"))
    }

    private fun deductMoney(player: EntityPlayer, amount: Int): Boolean {
        if (amount <= 0) return true
        val inv = player.inventory
        var total = 0
        for (i in 0 until inv.sizeInventory) {
            val s = inv.getStackInSlot(i) ?: continue
            if (s.item != jp.ngt.rtm.RTMItem.money) continue
            total += jp.ngt.rtm.RTMItem.MoneyType.getPrice(s.itemDamage) * s.stackSize
        }
        if (total < amount) return false
        for (i in 0 until inv.sizeInventory) {
            val s = inv.getStackInSlot(i) ?: continue
            if (s.item != jp.ngt.rtm.RTMItem.money) continue
            inv.setInventorySlotContents(i, null)
        }
        var change = total - amount
        for ((id, value) in jp.sakuramochi.kaisatsupatch.gui.InventoryCustomVendor.DENOMINATIONS) {
            val count = change / value
            if (count > 0) {
                val s = net.minecraft.item.ItemStack(jp.ngt.rtm.RTMItem.money, count, id)
                if (!inv.addItemStackToInventory(s)) player.dropPlayerItemWithRandomChoice(s, false)
                change -= count * value
            }
        }
        return true
    }

    // -----------------------------------------------------------------------
    // ゲート制御・ユーティリティ
    // -----------------------------------------------------------------------
    private fun openGate(world: World, x: Int, y: Int, z: Int, tile: TileEntityCustomTurnstile) {
        val meta = world.getBlockMetadata(x, y, z)
        if (!isOpen(meta) && !tile.canThrough()) {
            world.setBlockMetadataWithNotify(x, y, z, meta + 4, 2)
            tile.setCount(tile.openTicks)
        }
    }

    private fun allow(world: World, player: EntityPlayer, tile: TileEntityCustomTurnstile? = null) {
        if (!world.isRemote) {
            world.playSoundAtEntity(player, "note.pling", 1.0f, 2.0f)
            if (tile?.passMessage?.isNotEmpty() == true) {
                player.addChatMessage(net.minecraft.util.ChatComponentText("§7${tile.passMessage}"))
            }
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

private fun Int.yen() = "%,d円".format(this)

private val TileEntityCustomTurnstile.GateMode.allowsIC: Boolean
    get() = this != TileEntityCustomTurnstile.GateMode.TICKET_ONLY && this != TileEntityCustomTurnstile.GateMode.PASS_ONLY

private val TileEntityCustomTurnstile.GateMode.allowsTicket: Boolean
    get() = this != TileEntityCustomTurnstile.GateMode.IC_ONLY && this != TileEntityCustomTurnstile.GateMode.PASS_ONLY

private val TileEntityCustomTurnstile.GateMode.allowsPass: Boolean
    get() = this != TileEntityCustomTurnstile.GateMode.IC_ONLY && this != TileEntityCustomTurnstile.GateMode.TICKET_ONLY

private val TileEntityCustomTurnstile.GateMode.displayName: String
    get() = when (this) {
        TileEntityCustomTurnstile.GateMode.ENTRY        -> "入場専用（全種別）"
        TileEntityCustomTurnstile.GateMode.EXIT         -> "出場専用（全種別）"
        TileEntityCustomTurnstile.GateMode.BOTH         -> "入出場兼用（全種別）"
        TileEntityCustomTurnstile.GateMode.IC_ONLY      -> "IC専用"
        TileEntityCustomTurnstile.GateMode.TICKET_ONLY  -> "切符専用"
        TileEntityCustomTurnstile.GateMode.PASS_ONLY    -> "定期専用"
    }
