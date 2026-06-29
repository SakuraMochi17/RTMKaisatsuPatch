package jp.sakuramochi.kaisatsupatch.util

import cpw.mods.fml.common.network.ByteBufUtils
import io.netty.buffer.ByteBuf
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.util.Constants

// ── Item ─────────────────────────────────────────────────────────
//
// 本番 Minecraft では MCP 名が SRG 名（func_XXXXX_x）のままになる。
// KaizPatchX では戻り値型が void に変更されており通常呼び出しで NoSuchMethodError になる。
// 影響を受けるメソッドをすべてリフレクション経由で呼び出す。
//   setUnlocalizedName → func_77655_b
//   setTextureName     → func_111206_d
//   setCreativeTab     → func_77637_a
//   setMaxStackSize    → func_77631_c

private fun itemMethod(mcpName: String, srgName: String, paramCount: Int) =
    Item::class.java.declaredMethods
        .firstOrNull { it.name in setOf(mcpName, srgName) && it.parameterCount == paramCount }
        ?.also { it.isAccessible = true }
        ?: error("Item.$mcpName / $srgName (paramCount=$paramCount) が見つかりません")

private val methodSetName        by lazy { itemMethod("setUnlocalizedName", "func_77655_b", 1) }
private val methodSetTexture     by lazy { itemMethod("setTextureName",     "func_111206_d", 1) }
private val methodSetCreativeTab by lazy { itemMethod("setCreativeTab",     "func_77637_a",  1) }
private val methodSetMaxStack    by lazy { itemMethod("setMaxStackSize",    "func_77631_c",  1) }

fun Item.initName(name: String)               { methodSetName.invoke(this, name) }
fun Item.initTexture(texture: String)         { methodSetTexture.invoke(this, texture) }
fun Item.initCreativeTab(tab: CreativeTabs)   { methodSetCreativeTab.invoke(this, tab) }
fun Item.initMaxStackSize(size: Int)          { methodSetMaxStack.invoke(this, size) }

// ── ByteBuf ─────────────────────────────────────────────────────

fun ByteBuf.writeStr(s: String) = ByteBufUtils.writeUTF8String(this, s)
fun ByteBuf.readStr(): String   = ByteBufUtils.readUTF8String(this)

fun ByteBuf.writeCoords(x: Int, y: Int, z: Int) { writeInt(x); writeInt(y); writeInt(z) }

/** `val (cx, cy, cz) = buf.readCoords()` で分解できる */
data class BlockCoords(val x: Int, val y: Int, val z: Int)
fun ByteBuf.readCoords(): BlockCoords = BlockCoords(readInt(), readInt(), readInt())

fun ByteBuf.writeStringList(list: List<String>) {
    writeInt(list.size); list.forEach { writeStr(it) }
}
fun ByteBuf.readStringList(): List<String> = (0 until readInt()).map { readStr() }

fun <E : Enum<E>> ByteBuf.writeEnum(e: E) = writeInt(e.ordinal)
inline fun <reified E : Enum<E>> ByteBuf.readEnum(): E = enumValues<E>()[readInt()]

// ── NBT ─────────────────────────────────────────────────────────

fun NBTTagCompound.setStringList(key: String, list: Iterable<String>) {
    val tag = NBTTagList()
    list.forEach { tag.appendTag(NBTTagString(it)) }
    setTag(key, tag)
}

fun NBTTagCompound.getStringList(key: String): List<String> {
    val tag = getTagList(key, Constants.NBT.TAG_STRING)
    return (0 until tag.tagCount()).map { tag.getStringTagAt(it) }
}

// ── プレイヤー ───────────────────────────────────────────────────

fun EntityPlayerMP.isOp(): Boolean =
    mcServer.configurationManager.func_152596_g(gameProfile)

fun EntityPlayerMP.sendSuccess(msg: String) = addChatMessage(ChatComponentText("§a$msg"))
fun EntityPlayerMP.sendError(msg: String)   = addChatMessage(ChatComponentText("§c$msg"))
fun EntityPlayerMP.sendWarn(msg: String)    = addChatMessage(ChatComponentText("§e$msg"))
