package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityDepartureBoard
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.time.LocalTime

@SideOnly(Side.CLIENT)
class RenderDepartureBoard : TileEntitySpecialRenderer() {

    private val BG_TEX = ResourceLocation("rtmkaisatsupatch", "textures/blocks/departure_board_bg.png")

    // 1ブロック上でテキストを描画するスケール
    // scale=0.007: 1block ≈ 143 font units (幅方向)、8行収容
    private val SCALE = 0.007f

    override fun renderTileEntityAt(te: TileEntity, x: Double, y: Double, z: Double, partialTick: Float) {
        val tile = te as? TileEntityDepartureBoard ?: return

        GL11.glPushMatrix()
        GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

        // ブロックメタデータから向きを取得
        val meta = te.worldObj?.getBlockMetadata(te.xCoord, te.yCoord, te.zCoord) ?: 0
        val angle = when (meta and 3) {
            0 -> 0f    // 南向き (+Z 方向)
            1 -> 90f   // 西向き (-X 方向)
            2 -> 180f  // 北向き (-Z 方向)
            3 -> 270f  // 東向き (+X 方向)
            else -> 0f
        }
        GL11.glRotatef(angle, 0f, 1f, 0f)

        // ブロック前面のすぐ手前に移動
        GL11.glTranslatef(0f, 0f, 0.505f)

        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_DEPTH_TEST)  // 他のブロックに隠れても表示

        GL11.glScalef(SCALE, -SCALE, SCALE)  // Y を反転（上方向に描画するため）

        val fr = Minecraft.getMinecraft().fontRenderer
        val now  = LocalTime.now()
        val time = "%02d:%02d".format(now.hour, now.minute)
        val titleText = tile.title.ifEmpty { tile.stationName }.ifEmpty { "発車標" }
        val platText  = if (tile.platform.isNotEmpty()) " ${tile.platform}番線" else ""

        // ── タイトル行（現在時刻付き）──────────────────────
        val header = "$titleText$platText  $time"
        val hw = fr.getStringWidth(header)
        fr.drawString(header, -hw / 2, -62, 0xFFFF55, false)

        // ── 発車情報行 ──────────────────────────────────────
        val rows = tile.cachedDepartures
        if (rows.isEmpty()) {
            val msg = "発車情報なし"
            fr.drawString(msg, -(fr.getStringWidth(msg)) / 2, -40, 0x888888, false)
        } else {
            rows.take(5).forEachIndexed { i, row ->
                val rowY = -46 + i * 22
                val timeStr = row.time
                val destStr = row.destination.take(6)
                val typeStr = row.typeName.take(3)
                val line = "$timeStr  $destStr  $typeStr"
                val lw = fr.getStringWidth(line)
                // 時刻：左端
                fr.drawString(row.time, -68, rowY, 0x55FFFF, false)
                // 行き先：中央寄せ気味
                fr.drawString(destStr, -30, rowY, 0xFFFFFF, false)
                // 種別：右寄せ
                val typeX = 55 - fr.getStringWidth(typeStr)
                fr.drawString(typeStr, typeX, rowY, 0xFFFF55, false)
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_LIGHTING)

        GL11.glPopMatrix()
    }
}
