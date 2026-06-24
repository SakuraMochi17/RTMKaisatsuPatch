package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper
import cpw.mods.fml.relauncher.Side

object KaizPatchNetwork {

    lateinit var CHANNEL: SimpleNetworkWrapper
        private set

    fun init() {
        CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("KaizPatch")
        var id = 0
        // C→S
        CHANNEL.registerMessage(PacketTurnstileConfig.Handler::class.java,    PacketTurnstileConfig::class.java,    id++, Side.SERVER)
        CHANNEL.registerMessage(PacketStationUpdate.Handler::class.java,       PacketStationUpdate::class.java,       id++, Side.SERVER)
        CHANNEL.registerMessage(PacketLineUpdate.Handler::class.java,          PacketLineUpdate::class.java,          id++, Side.SERVER)
        CHANNEL.registerMessage(PacketPurchaseTicket.Handler::class.java,      PacketPurchaseTicket::class.java,      id++, Side.SERVER)
        CHANNEL.registerMessage(PacketResetSales.Handler::class.java,          PacketResetSales::class.java,          id++, Side.SERVER)
        // S→C
        CHANNEL.registerMessage(PacketOpenStationGui.Handler::class.java,      PacketOpenStationGui::class.java,      id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketOpenLineGui.Handler::class.java,         PacketOpenLineGui::class.java,         id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketOpenTicketVendor.Handler::class.java,    PacketOpenTicketVendor::class.java,    id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketOpenTurnstileConfig.Handler::class.java, PacketOpenTurnstileConfig::class.java, id++, Side.CLIENT)
        // 券売機設置駅設定
        CHANNEL.registerMessage(PacketOpenVendorConfig.Handler::class.java,    PacketOpenVendorConfig::class.java,    id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketVendorStationSave.Handler::class.java,   PacketVendorStationSave::class.java,   id,   Side.SERVER)
    }
}
