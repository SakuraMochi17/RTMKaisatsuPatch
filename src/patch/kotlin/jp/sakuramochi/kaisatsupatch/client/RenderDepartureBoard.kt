package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.ngt.ngtlib.renderer.model.ModelLoader
import jp.ngt.ngtlib.renderer.model.PolygonModel
import jp.ngt.ngtlib.renderer.model.VecAccuracy
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.time.LocalTime

/**
 * 発車標（表示体）の描画。HI03 様の EditableDepartureBoard のモデル(.mqo)を枠として
 * 読み込み、その画面部分にバインド先の発車情報とこの板の静的情報（路線名・方面・
 * 番線・路線カラー帯）をフォントで重ね描きする。
 *
 * モデル素材: (C) hi03 — EditableDepartureBoard（改変・再配布可。README にクレジット記載）
 */
@SideOnly(Side.CLIENT)
class RenderDepartureBoard : TileEntitySpecialRenderer() {

    private val MODEL = ResourceLocation("rtmkaisatsupatch", "models/edb_board.mqo")
    private val TEX   = ResourceLocation("rtmkaisatsupatch", "textures/edb/edb_board.png")

    // HI03 の表示原点・寸法（モデル空間 = ブロック単位）。位置調整はここで行う。
    private val POS_X = -1.2518; private val POS_Y = 0.5679; private val POS_Z = 0.1742
    private val GLYPH_H = 0.2363   // 1 行（グリフ）の高さ[m]

    private var model: PolygonModel? = null
    private var modelTried = false

    private fun model(): PolygonModel? {
        if (!modelTried) {
            modelTried = true
            model = try { ModelLoader.loadModel(MODEL, VecAccuracy.LOW) } catch (e: Throwable) { null }
        }
        return model
    }

    override fun renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, partialTick: Float) {
        val tile = te as? TileEntityDepartureBoard ?: return
        val meta = te.worldObj?.getBlockMetadata(te.xCoord, te.yCoord, te.zCoord) ?: 0
        val angle = when (meta and 3) { 0 -> 0f; 1 -> 90f; 2 -> 180f; 3 -> 270f; else -> 0f }

        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)
        GL11.glRotatef(angle, 0f, 1f, 0f)

        // ── モデル枠 ──────────────────────────────────────────────
        val m = model()
        if (m != null) {
            GL11.glPushMatrix()
            GL11.glEnable(GL12.GL_RESCALE_NORMAL)
            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glColor4f(1f, 1f, 1f, 1f)
            bindTexture(TEX)
            m.renderAll(false)
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glPopMatrix()
        }

        // ── 画面内容（文字・カラー帯） ────────────────────────────
        drawContent(tile)

        GL11.glPopMatrix()
    }

    private fun drawContent(tile: TileEntityDepartureBoard) {
        val fr = Minecraft.getMinecraft().fontRenderer
        val s = (GLYPH_H / fr.FONT_HEIGHT).toFloat()

        GL11.glPushMatrix()
        GL11.glTranslated(POS_X, POS_Y, POS_Z)
        // フォントは下方向に伸びるので Y 反転。原点(0,0)=表示左上、単位=フォントpx。
        GL11.glScalef(s, -s, s)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        val color = 0xFF000000.toInt() or (tile.lineColorHex and 0xFFFFFF)

        // 現在時刻
        val time = if (tile.timeMode() == "game") {
            val gm = (((tile.worldObj?.worldTime ?: 0L) % 24000L) * 1440L / 24000L + 360L).toInt() % 1440
            "%02d:%02d".format(gm / 60, gm % 60)
        } else {
            val now = LocalTime.now(); "%02d:%02d".format(now.hour, now.minute)
        }

        // ヘッダー: 路線カラー帯 + 路線名 + 番線 + 時刻
        drawBar(0f, 0f, 3f, 9f, color)
        val titleText = tile.headerTitle().ifEmpty { "発車標" }
        val platText  = if (tile.platform.isNotEmpty()) " ${tile.platform}番線" else ""
        fr.drawString("$titleText$platText", 5, 0, 0xFFFFFF, false)
        fr.drawString(time, 240, 0, 0xFFFF55, false)
        if (tile.headerDirection.isNotEmpty()) fr.drawString(tile.headerDirection, 5, 9, 0xCCCCCC, false)

        // 発車情報行
        val rows = tile.departures()
        if (rows.isEmpty()) {
            val msg = if (tile.isBound) "発車情報なし" else "未バインド"
            fr.drawString(msg, 5, 20, 0x888888, false)
        } else {
            rows.take(5).forEachIndexed { i, row ->
                val ry = 20 + i * 10
                fr.drawString(row.time,                5,   ry, 0x55FFFF, false)
                fr.drawString(row.destination.take(6), 45,  ry, 0xFFFFFF, false)
                fr.drawString(row.typeName.take(4),    180, ry, 0xFFAA00, false)
                if (row.trainName.isNotEmpty()) fr.drawString(row.trainName.take(6), 230, ry, 0x55FF55, false)
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_LIGHTING)
        GL11.glPopMatrix()
    }

    /** 単色矩形（路線カラー帯など）。フォントpx 空間で描画 */
    private fun drawBar(x1: Float, y1: Float, x2: Float, y2: Float, argb: Int) {
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
