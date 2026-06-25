package jp.sakuramochi.kaisatsupatch.client

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.sakuramochi.kaisatsupatch.item.ItemCustomICCard
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.Tessellator
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11

/**
 * 会社ごとにテクスチャを切り替えるICカードレンダラー。
 *
 * テクスチャ探索順:
 *   1. assets/kaizpatch/textures/items/ic_card_<companyID>.png  (リソースパック / mod jar 内)
 *   2. なければデフォルト ic_card_default.png + 会社カラートint
 */
@SideOnly(Side.CLIENT)
class RenderICCard : IItemRenderer {

    // RTM の既存ICカードテクスチャをデフォルトとして流用
    private val defaultTexture = ResourceLocation("rtm", "textures/items/icCard.png")
    private val textureCache = mutableMapOf<String, ResourceLocation?>()

    override fun handleRenderType(item: ItemStack, type: ItemRenderType) =
        type == ItemRenderType.INVENTORY ||
        type == ItemRenderType.EQUIPPED  ||
        type == ItemRenderType.EQUIPPED_FIRST_PERSON ||
        type == ItemRenderType.ENTITY

    override fun shouldUseRenderHelper(type: ItemRenderType, item: ItemStack, helper: ItemRendererHelper) =
        helper == ItemRendererHelper.INVENTORY_BLOCK

    override fun renderItem(type: ItemRenderType, item: ItemStack, vararg data: Any) {
        val companyID = ItemCustomICCard.getCompanyID(item)
        val companyColor = ItemCustomICCard.getCompanyColor(item)

        val texture = resolveTexture(companyID)

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        when (type) {
            ItemRenderType.INVENTORY -> renderFlat(texture, companyColor, companyID.isEmpty())
            ItemRenderType.ENTITY    -> {
                GL11.glTranslatef(-0.5f, -0.25f, 0f)
                renderFlat(texture, companyColor, companyID.isEmpty())
            }
            ItemRenderType.EQUIPPED, ItemRenderType.EQUIPPED_FIRST_PERSON -> {
                GL11.glTranslatef(0.5f, 0.5f, 0f)
                renderFlat(texture, companyColor, companyID.isEmpty())
            }
            else -> {}
        }

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }

    private fun resolveTexture(companyID: String): ResourceLocation {
        if (companyID.isEmpty()) return defaultTexture
        return textureCache.getOrPut(companyID) {
            // リソースパック内の assets/kaizpatch/textures/items/ic_card_<id>.png を探す
            val loc = ResourceLocation("kaizpatch", "textures/items/ic_card_${companyID}.png")
            val exists = try {
                Minecraft.getMinecraft().resourceManager.getResource(loc); true
            } catch (_: Exception) { false }
            if (exists) loc else null
        } ?: defaultTexture
    }

    private fun renderFlat(texture: ResourceLocation, color: Int, applyTint: Boolean) {
        Minecraft.getMinecraft().renderEngine.bindTexture(texture)

        if (applyTint || texture == defaultTexture) {
            val r = ((color shr 16) and 0xFF) / 255f
            val g = ((color shr 8)  and 0xFF) / 255f
            val b = (color           and 0xFF) / 255f
            GL11.glColor4f(r, g, b, 1f)
        } else {
            GL11.glColor4f(1f, 1f, 1f, 1f)
        }

        val t = Tessellator.instance
        t.startDrawingQuads()
        t.addVertexWithUV(0.0, 1.0, 0.0, 0.0, 1.0)
        t.addVertexWithUV(1.0, 1.0, 0.0, 1.0, 1.0)
        t.addVertexWithUV(1.0, 0.0, 0.0, 1.0, 0.0)
        t.addVertexWithUV(0.0, 0.0, 0.0, 0.0, 0.0)
        t.draw()

        GL11.glColor4f(1f, 1f, 1f, 1f)
    }
}
