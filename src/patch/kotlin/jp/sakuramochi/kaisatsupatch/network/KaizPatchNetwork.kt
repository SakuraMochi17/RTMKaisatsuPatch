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
        CHANNEL.registerMessage(PacketVendorStationSave.Handler::class.java,   PacketVendorStationSave::class.java,   id++, Side.SERVER)
        // 指定席券売機・列車管理
        CHANNEL.registerMessage(PacketTrainUpdate.Handler::class.java,              PacketTrainUpdate::class.java,              id++, Side.SERVER)
        CHANNEL.registerMessage(PacketPurchaseExpressTicket.Handler::class.java,    PacketPurchaseExpressTicket::class.java,    id++, Side.SERVER)
        CHANNEL.registerMessage(PacketOpenTrainManager.Handler::class.java,         PacketOpenTrainManager::class.java,         id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketOpenReservedVendor.Handler::class.java,       PacketOpenReservedVendor::class.java,       id++, Side.CLIENT)
        // 予約キャンセル
        CHANNEL.registerMessage(PacketCancelReservation.Handler::class.java,       PacketCancelReservation::class.java,        id++, Side.SERVER)
        // 発車標
        CHANNEL.registerMessage(PacketOpenDepartureBoard.Handler::class.java,      PacketOpenDepartureBoard::class.java,       id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketDepartureBoardSave.Handler::class.java,      PacketDepartureBoardSave::class.java,       id++, Side.SERVER)
        // OuDia テンプレート出力
        CHANNEL.registerMessage(PacketExportTemplate.Handler::class.java,          PacketExportTemplate::class.java,           id++, Side.SERVER)
        // 発車標 設定ブロック
        CHANNEL.registerMessage(PacketOpenDepartureSettings.Handler::class.java,   PacketOpenDepartureSettings::class.java,    id++, Side.CLIENT)
        CHANNEL.registerMessage(PacketDepartureSettingsSave.Handler::class.java,   PacketDepartureSettingsSave::class.java,    id,   Side.SERVER)
    }
}
