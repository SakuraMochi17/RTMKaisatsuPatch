package jp.sakuramochi.kaisatsupatch.block

import jp.ngt.rtm.block.BlockMachineBase
import jp.ngt.rtm.block.tileentity.TileEntityMachineBase
import jp.ngt.rtm.gui.GuiSelectModel
import jp.ngt.rtm.item.ItemTicket
import jp.ngt.rtm.modelpack.IModelSelector
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import jp.sakuramochi.kaisatsupatch.item.ItemCustomTicket
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

// ★親クラスを RTM の BlockMachineBase に変更しました
class BlockCustomTurnstile : BlockMachineBase(Material.iron) {

    init {
        setBlockName("custom_turnstile")
        setBlockTextureName("rtmkaisatsupatch:custom_turnstile")
        this.setLightOpacity(0) // 光のバグを防ぐためのおまじない
    }

    override fun createNewTileEntity(world: World?, meta: Int): TileEntity {
        return TileEntityCustomTurnstile()
    }

    override fun onBlockActivated(
        world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
        side: Int, hitX: Float, hitY: Float, hitZ: Float
    ): Boolean {
        // スニーク時はモデル選択画面を開く
        if (player.isSneaking) {
            if (world.isRemote) {
                val tile = world.getTileEntity(x, y, z)
                if (tile is IModelSelector) {
                    Minecraft.getMinecraft().displayGuiScreen(GuiSelectModel(world, tile))
                }
            }
            return true
        }

        // RTMの基本操作（レンチでの回転など）を優先
        if (!this.clickMachine(world, x, y, z, player)) {
            val itemStack = player.currentEquippedItem
            if (itemStack != null) {
                // RTM本家の切符、またはカスタム切符を持っているか判定
                if (itemStack.item is ItemTicket || itemStack.item is ItemCustomTicket) {
                    this.openGate(world, x, y, z, player)

                    // RTM本家の切符の消費ロジック
                    if (itemStack.item is ItemTicket) {
                        val ticket = itemStack.item as ItemTicket
                        if (ticket.ticketType != 2) { // 2はフリーパスなど
                            val returnedItem = ItemTicket.consumeTicket(itemStack)
                            if (!world.isRemote && returnedItem != null) {
                                this.dropBlockAsItem(world, x, y + 1, z, returnedItem)
                            }
                        }
                    }
                    // カスタム切符の消費ロジック
                    else if (itemStack.item is ItemCustomTicket) {
                        if (!player.capabilities.isCreativeMode) {
                            itemStack.stackSize--
                            // 今後、ここに「ICカードの残高を減らす」などの独自処理を書きます
                        }
                    }
                }
            }
        }
        return true
    }

    // ゲートを開ける処理
    fun openGate(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) {
        val meta = world.getBlockMetadata(x, y, z)
        val tile = world.getTileEntity(x, y, z) as? TileEntityCustomTurnstile ?: return

        if (!isOpen(meta) && !tile.canThrough()) {
            world.setBlockMetadataWithNotify(x, y, z, meta + 4, 2) // メタデータに+4して「開いている状態」にする
            tile.setCount(40) // 40ティック(2秒)の間開ける
        }
    }

    // ゲートが開いているかどうかの判定
    fun isOpen(meta: Int): Boolean {
        return meta >= 4
    }

    // ★重要：当たり判定の処理（開いている時は通り抜けられるようにする）
    override fun getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int): AxisAlignedBB? {
        if (this.isOpen(world.getBlockMetadata(x, y, z))) {
            return null // nullを返すと当たり判定が消え、プレイヤーがすり抜けられる
        }
        return super.getCollisionBoundingBoxFromPool(world, x, y, z)
    }

    // 村人など（AI）が通り抜けられるかどうかの判定
    override fun getBlocksMovement(world: IBlockAccess, x: Int, y: Int, z: Int): Boolean {
        return isOpen(world.getBlockMetadata(x, y, z))
    }

    // 設置時の処理（モデルの引き継ぎなど。親クラスの処理も呼ぶ）
    override fun onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase?, itemStack: ItemStack) {
        super.onBlockPlacedBy(world, x, y, z, entity, itemStack)
        val tile = world.getTileEntity(x, y, z)
        if (tile is TileEntityMachineBase) {
            if (itemStack.hasTagCompound() && itemStack.tagCompound.hasKey("ModelName")) {
                tile.modelName = itemStack.tagCompound.getString("ModelName")
            }
        }
    }

    // 3Dモデルを描画するための設定（コメントアウトを外しました）
    override fun isOpaqueCube(): Boolean = false
    override fun renderAsNormalBlock(): Boolean = false
}
