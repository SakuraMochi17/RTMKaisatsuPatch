package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityBoardingCertMachine
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11

@SideOnly(Side.CLIENT)
class RenderBoardingCertMachine : TileEntitySpecialRenderer() {
    override fun renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, partialTicks: Float) {
        val tile = te as? TileEntityBoardingCertMachine ?: return
        val name = tile.stationName
        if (name == "未設定" || name.isEmpty()) return

        val fr = Minecraft.getMinecraft().fontRenderer

        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y + 1.2, z + 0.5)

        // ブロックのメタ方向に向かせる（発車標と同じパターン）
        val meta = te.worldObj?.getBlockMetadata(te.xCoord, te.yCoord, te.zCoord) ?: 0
        val angle = when (meta and 3) {
            0 -> 0f; 1 -> 90f; 2 -> 180f; else -> 270f
        }
        GL11.glRotatef(angle, 0f, 1f, 0f)
        GL11.glTranslatef(0f, 0f, 0.52f)

        val scale = 0.022f
        GL11.glScalef(-scale, -scale, scale)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        val w = fr.getStringWidth(name)
        val pad = 2
        // 半透明背景
        net.minecraft.client.renderer.Tessellator.instance.also { t ->
            t.startDrawingQuads()
            t.setColorRGBA(0, 0, 0, 140)
            t.addVertex((-w / 2.0 - pad), (-1.0 - pad), 0.0)
            t.addVertex((-w / 2.0 - pad), (9.0 + pad), 0.0)
            t.addVertex((w / 2.0 + pad).toDouble(), (9.0 + pad), 0.0)
            t.addVertex((w / 2.0 + pad).toDouble(), (-1.0 - pad), 0.0)
            t.draw()
        }
        fr.drawString(name, -w / 2, 0, 0xFFFFFF)

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_LIGHTING)
        GL11.glPopMatrix()
    }
}
