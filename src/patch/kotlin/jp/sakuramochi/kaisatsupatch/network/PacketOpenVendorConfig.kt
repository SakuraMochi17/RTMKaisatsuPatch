package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.ByteBufUtils
import cpw.mods.fml.common.network.simpleimpl.IMessage
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft

/** S→C: 券売機の設置駅・会社選択GUIを開く */
class PacketOpenVendorConfig() : IMessage {
    var x = 0; var y = 0; var z = 0
    var currentStation = ""
    var currentCompanyID = ""
    var stationList: List<String> = emptyList()
    var companyList: List<Pair<String, String>> = emptyList() // id → 表示名

    constructor(
        x: Int, y: Int, z: Int,
        currentStation: String,
        currentCompanyID: String,
        stationList: List<String>,
        companyList: List<Pair<String, String>>
    ) : this() {
        this.x = x; this.y = y; this.z = z
        this.currentStation = currentStation
        this.currentCompanyID = currentCompanyID
        this.stationList = stationList
        this.companyList = companyList
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        ByteBufUtils.writeUTF8String(buf, currentStation)
        ByteBufUtils.writeUTF8String(buf, currentCompanyID)
        buf.writeInt(stationList.size)
        stationList.forEach { ByteBufUtils.writeUTF8String(buf, it) }
        buf.writeInt(companyList.size)
        companyList.forEach { (id, name) ->
            ByteBufUtils.writeUTF8String(buf, id)
            ByteBufUtils.writeUTF8String(buf, name)
        }
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        currentStation = ByteBufUtils.readUTF8String(buf)
        currentCompanyID = ByteBufUtils.readUTF8String(buf)
        val stSize = buf.readInt()
        stationList = (0 until stSize).map { ByteBufUtils.readUTF8String(buf) }
        val coSize = buf.readInt()
        companyList = (0 until coSize).map {
            ByteBufUtils.readUTF8String(buf) to ByteBufUtils.readUTF8String(buf)
        }
    }

    class Handler : IMessageHandler<PacketOpenVendorConfig, IMessage> {
        override fun onMessage(msg: PacketOpenVendorConfig, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiVendorStationConfig(
                    msg.x, msg.y, msg.z,
                    msg.currentStation, msg.currentCompanyID,
                    msg.stationList, msg.companyList
                )
            )
            return null
        }
    }
}
