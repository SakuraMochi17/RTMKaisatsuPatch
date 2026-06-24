package jp.sakuramochi.kaisatsupatch

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.ngt.rtm.block.tileentity.RenderMachine
import jp.sakuramochi.kaisatsupatch.block.*
import jp.sakuramochi.kaisatsupatch.block.tileentity.*
import jp.sakuramochi.kaisatsupatch.item.*
import jp.sakuramochi.kaisatsupatch.network.KaizPatchNetwork
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.Item
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(
    modid = RTMKaisatsuPatchCore.MODID,
    name = RTMKaisatsuPatchCore.MODNAME,
    version = RTMKaisatsuPatchCore.VERSION,
    dependencies = "required-after:RTM;required-after:NGTLib"
)
class RTMKaisatsuPatchCore {

    companion object {
        const val MODID = "RTMKaisatsuPatch"
        const val MODNAME = "RTM Kaisatsu Patch"
        const val VERSION = "0.1.0-pre1"

        val logger: Logger = LogManager.getLogger(MODID)

        val tabKaisatsuPatch: CreativeTabs = object : CreativeTabs("tabKaisatsuPatch") {
            override fun getTabIconItem(): Item = net.minecraft.init.Items.iron_ingot
        }

        @Mod.Instance(MODID)
        lateinit var instance: RTMKaisatsuPatchCore

        const val GUI_VENDOR = 1

        /** PacketPurchaseTicket から参照するためのアイテムレジストリ */
        val registeredItems: MutableMap<String, Item> = mutableMapOf()
    }

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        logger.info("Initializing $MODNAME - PreInit")

        KaizPatchNetwork.init()

        val myTab = tabKaisatsuPatch

        // アイテム
        val itemTicket       = ItemCustomTicket()
        val itemICCard       = ItemCustomICCard()
        val itemPass         = ItemCustomPass()
        val itemSettingsTool = ItemSettingsTool()

        // ブロック
        val blockTurnstile      = BlockCustomTurnstile()
        val blockVendor         = BlockCustomTicketVendor()
        val blockStationManager = BlockStationManager()
        val blockLineManager    = BlockLineManager()

        listOf(itemTicket, itemICCard, itemPass, itemSettingsTool).forEach { it.creativeTab = myTab }
        listOf(blockTurnstile, blockVendor, blockStationManager, blockLineManager).forEach { it.setCreativeTab(myTab) }

        GameRegistry.registerItem(itemTicket,       "custom_ticket")
        GameRegistry.registerItem(itemICCard,       "custom_ic_card")
        GameRegistry.registerItem(itemPass,         "custom_pass")
        GameRegistry.registerItem(itemSettingsTool, "settings_tool")

        GameRegistry.registerBlock(blockTurnstile,      ItemBlockCustomTurnstile::class.java,  "custom_turnstile")
        GameRegistry.registerBlock(blockVendor,         ItemBlockCustomTicketVendor::class.java,"custom_ticket_vendor")
        GameRegistry.registerBlock(blockStationManager, ItemBlockStationManager::class.java,   "station_manager")
        GameRegistry.registerBlock(blockLineManager,    ItemBlockLineManager::class.java,       "line_manager")

        GameRegistry.registerTileEntity(TileEntityCustomTurnstile::class.java,   "TileEntityCustomTurnstile")
        GameRegistry.registerTileEntity(TileEntityCustomTicketVendor::class.java, "TileEntityCustomTicketVendor")
        GameRegistry.registerTileEntity(TileEntityStationManager::class.java,    "TileEntityStationManager")
        GameRegistry.registerTileEntity(TileEntityLineManager::class.java,       "TileEntityLineManager")

        registeredItems["custom_ticket"] = itemTicket
        registeredItems["custom_pass"]   = itemPass

        if (event.side.isClient) {
            registerRenderer()
        }
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        logger.info("Initializing $MODNAME - Init")
        NetworkRegistry.INSTANCE.registerGuiHandler(instance, KaizPatchGuiHandler())
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        logger.info("Initializing $MODNAME - PostInit")
    }

    @SideOnly(Side.CLIENT)
    private fun registerRenderer() {
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityCustomTurnstile::class.java,
            RenderMachine.INSTANCE
        )
    }
}
