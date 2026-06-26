package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft

/** サーバー→クライアント：改札設定GUIを（駅リスト付きで）開く */
class PacketOpenTurnstileConfig() : IMessage {
    var x = 0; var y = 0; var z = 0
    var currentStation = ""
    var gateMode = ""
    var openTicks = 40
    var passMessage = ""
    var stationList: List<String> = emptyList()

    constructor(
        x: Int, y: Int, z: Int,
        currentStation: String, gateMode: String,
        stationList: List<String>,
        openTicks: Int = 40, passMessage: String = ""
    ) : this() {
        this.x = x; this.y = y; this.z = z
        this.currentStation = currentStation
        this.gateMode = gateMode
        this.openTicks = openTicks
        this.passMessage = passMessage
        this.stationList = stationList
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        currentStation = buf.readStr()
        gateMode = buf.readStr()
        openTicks = buf.readInt()
        passMessage = buf.readStr()
        val size = buf.readInt()
        stationList = (0 until size).map { buf.readStr() }
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(currentStation)
        buf.writeStr(gateMode)
        buf.writeInt(openTicks)
        buf.writeStr(passMessage)
        buf.writeInt(stationList.size)
        stationList.forEach { buf.writeStr(it) }
    }

    class Handler : IMessageHandler<PacketOpenTurnstileConfig, IMessage> {
        @SideOnly(Side.CLIENT)
        override fun onMessage(msg: PacketOpenTurnstileConfig, ctx: MessageContext): IMessage? {
            Minecraft.getMinecraft().displayGuiScreen(
                jp.sakuramochi.kaisatsupatch.client.GuiTurnstileConfig(
                    msg.x, msg.y, msg.z,
                    msg.currentStation, msg.gateMode, msg.stationList,
                    msg.openTicks, msg.passMessage
                )
            )
            return null
        }
    }
}
