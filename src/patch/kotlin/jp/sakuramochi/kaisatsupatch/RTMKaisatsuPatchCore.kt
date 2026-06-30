package jp.sakuramochi.kaisatsupatch

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.event.FMLServerStartingEvent
import cpw.mods.fml.common.event.FMLServerStoppingEvent
import jp.sakuramochi.kaisatsupatch.command.CommandKaisatsuAdmin
import jp.sakuramochi.kaisatsupatch.web.KaisatsuWebServer
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import jp.ngt.rtm.block.tileentity.RenderMachine
import jp.sakuramochi.kaisatsupatch.client.KaizPatchClientEvents
import jp.sakuramochi.kaisatsupatch.block.*
import jp.sakuramochi.kaisatsupatch.block.tileentity.*
import jp.sakuramochi.kaisatsupatch.client.RenderBoardingCertMachine
import jp.sakuramochi.kaisatsupatch.client.RenderDepartureBoard
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityTrainManager
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityReservedVendor
import jp.sakuramochi.kaisatsupatch.item.*
import jp.sakuramochi.kaisatsupatch.item.ItemCustomExpressTicket
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
        const val VERSION = "0.1.0-pre2"

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
        val itemTicket              = ItemCustomTicket()
        val itemICCard              = ItemCustomICCard()
        val itemPass                = ItemCustomPass()
        val itemSettingsTool        = ItemSettingsTool()
        val itemCouponTicket        = ItemCustomCouponTicket()
        val itemBoardingCertificate = ItemBoardingCertificate()
        ItemBoardingCertificate.instance = itemBoardingCertificate

        // ブロック
        val blockTurnstile           = BlockCustomTurnstile()
        val blockVendor              = BlockCustomTicketVendor()
        val blockStationManager      = BlockStationManager()
        val blockLineManager         = BlockLineManager()
        val blockTrainManager        = BlockTrainManager()
        val blockReservedVendor      = BlockReservedVendor()
        val blockFareAdjustment      = BlockFareAdjustment()
        val blockDepartureBoard      = BlockDepartureBoard()
        val blockBoardingCertMachine = BlockBoardingCertMachine()
        val blockDepartureSettings   = BlockDepartureSettings()

        // アイテム（指定席）
        val itemExpressTicket = ItemCustomExpressTicket()

        listOf(itemTicket, itemICCard, itemPass, itemSettingsTool, itemExpressTicket, itemCouponTicket, itemBoardingCertificate).forEach { it.creativeTab = myTab }
        listOf(blockTurnstile, blockVendor, blockStationManager, blockLineManager, blockTrainManager, blockReservedVendor, blockFareAdjustment, blockDepartureBoard, blockBoardingCertMachine, blockDepartureSettings).forEach { (it as net.minecraft.block.Block).setCreativeTab(myTab) }

        // ── ブロック登録（クリエイティブタブ表示順：有人駅設備 → 無人駅設備 → 情報表示 → 管理）──
        GameRegistry.registerBlock(blockTurnstile,           ItemBlockCustomTurnstile::class.java,     "custom_turnstile")
        GameRegistry.registerBlock(blockVendor,              ItemBlockCustomTicketVendor::class.java,  "custom_ticket_vendor")
        GameRegistry.registerBlock(blockReservedVendor,      ItemBlockReservedVendor::class.java,      "reserved_seat_vendor")
        GameRegistry.registerBlock(blockFareAdjustment,      ItemBlockFareAdjustment::class.java,      "fare_adjustment")
        GameRegistry.registerBlock(blockBoardingCertMachine, ItemBlockBoardingCertMachine::class.java, "boarding_cert_machine")
        GameRegistry.registerBlock(blockDepartureBoard,      ItemBlockDepartureBoard::class.java,      "departure_board")
        GameRegistry.registerBlock(blockDepartureSettings,                                            "departure_settings")
        GameRegistry.registerBlock(blockStationManager,      ItemBlockStationManager::class.java,      "station_manager")
        GameRegistry.registerBlock(blockLineManager,         ItemBlockLineManager::class.java,         "line_manager")
        GameRegistry.registerBlock(blockTrainManager,        ItemBlockTrainManager::class.java,        "train_manager")

        // ── アイテム登録（切符類 → IC → 証明書 → ツール）──
        GameRegistry.registerItem(itemTicket,              "custom_ticket")
        GameRegistry.registerItem(itemCouponTicket,        "coupon_ticket")
        GameRegistry.registerItem(itemPass,                "custom_pass")
        GameRegistry.registerItem(itemExpressTicket,       "express_ticket")
        GameRegistry.registerItem(itemICCard,              "custom_ic_card")
        GameRegistry.registerItem(itemBoardingCertificate, "boarding_certificate")
        GameRegistry.registerItem(itemSettingsTool,        "settings_tool")

        GameRegistry.registerTileEntity(TileEntityCustomTurnstile::class.java,    "TileEntityCustomTurnstile")
        GameRegistry.registerTileEntity(TileEntityCustomTicketVendor::class.java, "TileEntityCustomTicketVendor")
        GameRegistry.registerTileEntity(TileEntityStationManager::class.java,     "TileEntityStationManager")
        GameRegistry.registerTileEntity(TileEntityLineManager::class.java,        "TileEntityLineManager")
        GameRegistry.registerTileEntity(TileEntityTrainManager::class.java,       "TileEntityTrainManager")
        GameRegistry.registerTileEntity(TileEntityReservedVendor::class.java,     "TileEntityReservedVendor")
        GameRegistry.registerTileEntity(TileEntityFareAdjustment::class.java,     "TileEntityFareAdjustment")
        GameRegistry.registerTileEntity(TileEntityDepartureBoard::class.java,     "TileEntityDepartureBoard")
        GameRegistry.registerTileEntity(TileEntityDepartureSettings::class.java,  "TileEntityDepartureSettings")
        GameRegistry.registerTileEntity(TileEntityBoardingCertMachine::class.java,"TileEntityBoardingCertMachine")

        registeredItems["custom_ticket"]        = itemTicket
        registeredItems["custom_ic_card"]       = itemICCard
        registeredItems["custom_pass"]          = itemPass
        registeredItems["express_ticket"]       = itemExpressTicket
        registeredItems["coupon_ticket"]        = itemCouponTicket
        registeredItems["boarding_certificate"] = itemBoardingCertificate

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

    @Mod.EventHandler
    fun serverStarting(event: FMLServerStartingEvent) {
        event.registerServerCommand(CommandKaisatsuAdmin())
        KaisatsuWebServer.start()
    }

    @Mod.EventHandler
    fun serverStopping(event: FMLServerStoppingEvent) {
        KaisatsuWebServer.stop()
    }

    @SideOnly(Side.CLIENT)
    private fun registerRenderer() {
        ClientRegistry.bindTileEntitySpecialRenderer(
            TileEntityCustomTurnstile::class.java,
            RenderMachine.INSTANCE
        )
        // IC カードは標準アイテムアイコン (rtm:icCard) + getColorFromItemStack で
        // 着色する方式に変更したため、カスタム IItemRenderer は登録しない。
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(KaizPatchClientEvents)
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityDepartureBoard::class.java, RenderDepartureBoard())
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBoardingCertMachine::class.java, RenderBoardingCertMachine())
    }
}
