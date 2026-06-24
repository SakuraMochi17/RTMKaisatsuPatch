package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.item.ItemCustomPass
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.RenderGameOverlayEvent
import org.lwjgl.opengl.GL11

@SideOnly(Side.CLIENT)
object KaizPatchClientEvents {

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.HOTBAR) return

        val mc = Minecraft.getMinecraft()
        val player = mc.thePlayer ?: return
        val world  = mc.theWorld  ?: return
        if (mc.gameSettings.showDebugInfo) return

        // インベントリから定期券を探す（残り日数が最も多いものを優先）
        val passStack = player.inventory.mainInventory
            .plus(player.inventory.armorInventory)
            .filterNotNull()
            .filter { it.item is ItemCustomPass }
            .maxByOrNull { ItemCustomPass.remainingDays(it, ItemCustomPass.currentDay(world)) }
            ?: return

        val currentDay = ItemCustomPass.currentDay(world)
        val remaining  = ItemCustomPass.remainingDays(passStack, currentDay)
        val isFreePast = ItemCustomPass.isFreePast(passStack)
        val from = ItemCustomPass.getFromStation(passStack)
        val to   = ItemCustomPass.getToStation(passStack)

        val color = when {
            remaining <= 0L -> 0xFF4444
            remaining <= 3L -> 0xFF6666
            remaining <= 7L -> 0xFFAA00
            else            -> 0x55FF55
        }

        val routeStr = if (isFreePast) "全区間" else "$from⇔$to"
        val typeStr  = if (isFreePast) "フリーパス" else "定期券"
        val label = if (remaining <= 0L)
            "$typeStr 期限切れ ($routeStr)"
        else
            "$typeStr 残り ${remaining}日 ($routeStr)"

        val sr = ScaledResolution(mc, mc.displayWidth, mc.displayHeight)

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        mc.fontRenderer.drawStringWithShadow(label, 4, sr.scaledHeight - 54, color)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glPopMatrix()
    }
}
