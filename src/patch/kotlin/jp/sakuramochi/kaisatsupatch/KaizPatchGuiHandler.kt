package jp.sakuramochi.kaisatsupatch

import cpw.mods.fml.common.network.IGuiHandler
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.client.GuiCustomTicketVendor
import jp.sakuramochi.kaisatsupatch.gui.ContainerCustomVendor
import jp.sakuramochi.kaisatsupatch.network.FaresCache
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

class KaizPatchGuiHandler : IGuiHandler {

    override fun getServerGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        if (id == RTMKaisatsuPatchCore.GUI_VENDOR) {
            val tile = world.getTileEntity(x, y, z) as? TileEntityCustomTicketVendor ?: return null
            return ContainerCustomVendor(player.inventory, tile)
        }
        return null
    }

    override fun getClientGuiElement(id: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        if (id == RTMKaisatsuPatchCore.GUI_VENDOR) {
            val tile = world.getTileEntity(x, y, z) as? TileEntityCustomTicketVendor ?: return null
            return GuiCustomTicketVendor(player.inventory, tile, FaresCache.station, FaresCache.fares)
        }
        return null
    }
}
