package jp.sakuramochi.kaisatsupatch

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.registry.GameRegistry
import jp.sakuramochi.kaisatsupatch.block.*
import jp.sakuramochi.kaisatsupatch.item.*
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.ngt.rtm.block.tileentity.*

// modLanguage = "kotlin" はエラーの原因になりやすいため削除します
@Mod(
    modid = RTMKaisatsuPatchCore.MODID,
    name = RTMKaisatsuPatchCore.MODNAME,
    version = RTMKaisatsuPatchCore.VERSION,
    dependencies = "required-after:RTM;required-after:NGTLib"
)
class RTMKaisatsuPatchCore { // ◀ object から class に変更しました！

    // ◀ 定数やクリエイティブタブは companion object の中に入れます
    companion object {
        const val MODID = "RTMKaisatsuPatch"
        const val MODNAME = "RTM Kaisatsu Patch"
        const val VERSION = "0.0.1"

        val logger: Logger = LogManager.getLogger(MODID)

        // --- 独自クリエイティブタブの定義 ---
        val tabKaisatsuPatch: CreativeTabs = object : CreativeTabs("tabKaisatsuPatch") {
            override fun getTabIconItem(): Item {
                // まずはエラーが起きないバニラの「鉄インゴット」でテストします
                return net.minecraft.init.Items.iron_ingot
            }
        }
    }

    // ◀ メソッド（関数）は class の直下に置きます
    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger.info("Initializing $MODNAME - PreInit")

        // companion object 内のタブを取得
        val myTab = tabKaisatsuPatch

        // アイテムやブロックのインスタンスを作成
        val itemTicket = ItemCustomTicket()
        val itemICCard = ItemCustomICCard()
        val blockTurnstile = BlockCustomTurnstile()
        val blockVendor = BlockCustomTicketVendor()

        // --- クリエイティブタブへの登録 ---
        itemTicket.creativeTab = myTab
        itemICCard.creativeTab = myTab
        // 変更: ブロックは = ではなく setCreativeTab() を使う！
        blockTurnstile.setCreativeTab(myTab)
        blockVendor.setCreativeTab(myTab)

        // 登録処理
        GameRegistry.registerItem(itemTicket, "custom_ticket")
        GameRegistry.registerItem(itemICCard, "custom_ic_card")
        GameRegistry.registerBlock(blockTurnstile, ItemBlockCustomTurnstile::class.java, "custom_turnstile")
        GameRegistry.registerBlock(blockVendor, ItemBlockCustomTicketVendor::class.java, "custom_ticket_vendor")

        // ★これを追加（TileEntityをゲームに登録）
        GameRegistry.registerTileEntity(jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile::class.java, "TileEntityCustomTurnstile")
        GameRegistry.registerTileEntity(jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTicketVendor::class.java, "TileEntityCustomTicketVendor")

        if (event.side.isClient) {
            registerRenderer()
        }
    }

    // 🌟 preInitメソッドの外（下）に、以下のメソッドを追加します
    @SideOnly(Side.CLIENT)
    private fun registerRenderer() {
        ClientRegistry.bindTileEntitySpecialRenderer(
            jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile::class.java,
            RenderMachine.INSTANCE
        )
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        logger.info("Initializing $MODNAME - Init")
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        logger.info("Initializing $MODNAME - PostInit")
    }
}