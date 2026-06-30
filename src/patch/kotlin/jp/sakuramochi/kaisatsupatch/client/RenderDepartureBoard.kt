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
    // POS_X/Y/Z = 黒画面の左上。板の横幅は約 95 フォントpx。
    private val POS_X = -1.2518; private val POS_Y = 0.5679; private val POS_Z = 0.1742
    private val GLYPH_H = 0.2363   // 1 行（グリフ）の高さ[m]

    // 画面レイアウト（フォントpx 単位。0,0 = 黒画面の左上）
    private val HEADER_Y = -11    // 路線名ヘッダーの y（マイナス = 黒画面の上＝「今度の列車は」帯）
    private val ROW_Y0   = 1      // 1 行目の y
    private val ROW_STEP = 11     // 行間
    private val MAX_ROWS = 3      // 黒画面に収まる行数
    private val COL_TIME = 2      // 各列の x
    private val COL_DEST = 34
    private val COL_TYPE = 66

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
        // フォントは下方向に伸びるので Y 反転。原点(0,0)=黒画面の左上、単位=フォントpx。
        // 板の横幅は約 95px。ヘッダー(路線名)は黒画面の上の「今度の列車は」帯に出すため
        // マイナス Y（上方向）に置く。
        GL11.glScalef(s, -s, s)
        GL11.glDisable(GL11.GL_LIGHTING)   // 発光表示（深度テストは有効のまま＝裏抜け防止）

        val color = 0xFF000000.toInt() or (tile.lineColorHex and 0xFFFFFF)

        // ── ヘッダー（「今度の列車は」帯の位置）: 路線カラー帯 + 路線名 + 番線 ──
        drawBar(0f, (HEADER_Y - 1).toFloat(), 4f, (HEADER_Y + 8).toFloat(), color)
        val titleText = tile.headerTitle().ifEmpty { "発車標" }
        val platText  = if (tile.platform.isNotEmpty()) " ${tile.platform}番線" else ""
        fr.drawString("$titleText$platText", 7, HEADER_Y, 0xFFFFFF, false)
        if (tile.headerDirection.isNotEmpty())
            fr.drawString(tile.headerDirection, 7, HEADER_Y + 9, 0xCCCCCC, false)

        // ── 発車情報行（黒画面内・板幅 約95px に収める） ──
        val rows = tile.departures()
        if (!tile.isBound) {
            fr.drawString("未バインド", 4, ROW_Y0, 0x888888, false)
        } else if (rows.isEmpty()) {
            fr.drawString("発車情報なし", 4, ROW_Y0, 0x888888, false)
        } else {
            rows.take(MAX_ROWS).forEachIndexed { i, row ->
                val ry = ROW_Y0 + i * ROW_STEP
                fr.drawString(row.time,                COL_TIME, ry, 0x55FFFF, false)
                fr.drawString(row.destination.take(5), COL_DEST, ry, 0xFFFFFF, false)
                fr.drawString(row.typeName.take(2),    COL_TYPE, ry, 0xFFAA00, false)
            }
        }

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
