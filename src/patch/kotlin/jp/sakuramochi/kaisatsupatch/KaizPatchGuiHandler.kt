package jp.sakuramochi.kaisatsupatch

import cpw.mods.fml.common.network.IGuiHandler
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import jp.sakuramochi.kaisatsupatch.client.GuiTurnstileConfig
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

class KaizPatchGuiHandler : IGuiHandler {

    companion object {
        const val GUI_TURNSTILE_CONFIG = 1
    }

    // サーバー側はコンテナ不要（設定はパケットで送る）
    override fun getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? = null

    @SideOnly(Side.CLIENT)
    override fun getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        return when (id) {
            GUI_TURNSTILE_CONFIG -> {
                val tile = world.getTileEntity(x, y, z) as? TileEntityCustomTurnstile ?: return null
                GuiTurnstileConfig(tile)
            }
            else -> null
        }
    }
}
