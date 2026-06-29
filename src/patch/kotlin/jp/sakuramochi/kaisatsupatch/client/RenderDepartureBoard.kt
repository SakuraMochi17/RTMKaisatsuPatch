package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import org.lwjgl.opengl.GL11
import java.time.LocalTime

/**
 * 発車標（表示体）の描画。バインドした設定ブロックの発車情報を、この板の
 * 静的情報（路線名・方面・番線・路線カラー帯）と合わせてフォントで描画する。
 *
 * 注: 現状は板面へのフォント描画のみ。HI03 モデル(.mqo)を枠として読み込む
 *     描画は後続ステップで追加予定。
 */
@SideOnly(Side.CLIENT)
class RenderDepartureBoard : TileEntitySpecialRenderer() {

    private val SCALE = 0.007f

    override fun renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, partialTick: Float) {
        val tile = te as? TileEntityDepartureBoard ?: return

        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

        val meta = te.worldObj?.getBlockMetadata(te.xCoord, te.yCoord, te.zCoord) ?: 0
        val angle = when (meta and 3) {
            0 -> 0f; 1 -> 90f; 2 -> 180f; 3 -> 270f; else -> 0f
        }
        GL11.glRotatef(angle, 0f, 1f, 0f)
        GL11.glTranslatef(0f, 0f, 0.505f)

        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glScalef(SCALE, -SCALE, SCALE)

        val fr = Minecraft.getMinecraft().fontRenderer
        val color = 0xFF000000.toInt() or (tile.lineColorHex and 0xFFFFFF)

        // 現在時刻
        val time = if (tile.timeMode() == "game") {
            val gm = (((te.worldObj?.worldTime ?: 0L) % 24000L) * 1440L / 24000L + 360L).toInt() % 1440
            "%02d:%02d".format(gm / 60, gm % 60)
        } else {
            val now = LocalTime.now(); "%02d:%02d".format(now.hour, now.minute)
        }

        // ── ヘッダー: 路線カラー帯 + 路線名 + 番線 + 現在時刻 ──────────
        drawColorBar(-70, -64, -62, -44, color)
        val titleText = tile.headerTitle().ifEmpty { "発車標" }
        val platText  = if (tile.platform.isNotEmpty()) " ${tile.platform}番線" else ""
        fr.drawString("$titleText$platText", -58, -62, 0xFFFFFF, false)
        fr.drawString(time, 40, -62, 0xFFFF55, false)

        // 方面
        if (tile.headerDirection.isNotEmpty()) {
            fr.drawString(tile.headerDirection, -58, -50, 0xFFFFFF, false)
        }

        // ── 発車情報行 ──────────────────────────────────────────────
        val rows = tile.departures()
        if (rows.isEmpty()) {
            val msg = if (tile.isBound) "発車情報なし" else "未バインド (設定ブロックを紐付け)"
            fr.drawString(msg, -(fr.getStringWidth(msg)) / 2, -30, 0x888888, false)
        } else {
            rows.take(5).forEachIndexed { i, row ->
                val rowY = -34 + i * 18
                fr.drawString(row.time,                -68, rowY, 0x55FFFF, false)
                fr.drawString(row.destination.take(6), -28, rowY, 0xFFFFFF, false)
                val typeStr = row.typeName.take(3)
                fr.drawString(typeStr, 55 - fr.getStringWidth(typeStr), rowY, 0xFFFF55, false)
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_LIGHTING)
        GL11.glPopMatrix()
    }

    /** 単色の矩形（路線カラー帯）を描画 */
    private fun drawColorBar(x1: Int, y1: Int, x2: Int, y2: Int, argb: Int) {
        val a = (argb ushr 24 and 0xFF) / 255f
        val r = (argb ushr 16 and 0xFF) / 255f
        val g = (argb ushr 8 and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glColor4f(r, g, b, a)
        val t = Tessellator.instance
        t.startDrawingQuads()
        t.addVertex(x1.toDouble(), y2.toDouble(), 0.0)
        t.addVertex(x2.toDouble(), y2.toDouble(), 0.0)
        t.addVertex(x2.toDouble(), y1.toDouble(), 0.0)
        t.addVertex(x1.toDouble(), y1.toDouble(), 0.0)
        t.draw()
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }
}
