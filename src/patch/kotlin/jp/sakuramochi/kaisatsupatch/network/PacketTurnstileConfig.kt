package jp.sakuramochi.kaisatsupatch.network

import cpw.mods.fml.common.network.simpleimpl.IMessage
import jp.sakuramochi.kaisatsupatch.util.readStr
import jp.sakuramochi.kaisatsupatch.util.writeStr
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler
import cpw.mods.fml.common.network.simpleimpl.MessageContext
import io.netty.buffer.ByteBuf
import jp.sakuramochi.kaisatsupatch.block.tileentity.TileEntityCustomTurnstile
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting

class PacketTurnstileConfig() : IMessage {

    var x = 0; var y = 0; var z = 0
    var stationCode = ""
    var gateMode = ""
    var openTicks = 40
    var passMessage = ""

    constructor(
        x: Int, y: Int, z: Int,
        stationCode: String, gateMode: String,
        openTicks: Int = 40, passMessage: String = ""
    ) : this() {
        this.x = x; this.y = y; this.z = z
        this.stationCode = stationCode
        this.gateMode = gateMode
        this.openTicks = openTicks
        this.passMessage = passMessage
    }

    override fun fromBytes(buf: ByteBuf) {
        x = buf.readInt(); y = buf.readInt(); z = buf.readInt()
        stationCode = buf.readStr()
        gateMode = buf.readStr()
        openTicks = buf.readInt()
        passMessage = buf.readStr()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(x); buf.writeInt(y); buf.writeInt(z)
        buf.writeStr(stationCode)
        buf.writeStr(gateMode)
        buf.writeInt(openTicks)
        buf.writeStr(passMessage)
    }

    class Handler : IMessageHandler<PacketTurnstileConfig, IMessage> {
        override fun onMessage(msg: PacketTurnstileConfig, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.playerEntity
            val world = player.worldObj
            val tile = world.getTileEntity(msg.x, msg.y, msg.z) as? TileEntityCustomTurnstile
                ?: return null

            val newMode = runCatching {
                TileEntityCustomTurnstile.GateMode.valueOf(msg.gateMode)
            }.getOrDefault(TileEntityCustomTurnstile.GateMode.ENTRY)

            tile.stationCode = msg.stationCode.ifBlank { "STATION_A" }
            tile.gateMode = newMode
            tile.openTicks = msg.openTicks.coerceIn(10, 200)
            tile.passMessage = msg.passMessage
            tile.markDirty()

            player.addChatMessage(ChatComponentText(
                "${EnumChatFormatting.GREEN}改札機を設定しました — 駅:${tile.stationCode} / モード:${newMode.displayName}"
            ))
            return null
        }
    }
}

val TileEntityCustomTurnstile.GateMode.displayName: String
    get() = when (this) {
        TileEntityCustomTurnstile.GateMode.ENTRY        -> "入場専用（全種別）"
        TileEntityCustomTurnstile.GateMode.EXIT         -> "出場専用（全種別）"
        TileEntityCustomTurnstile.GateMode.BOTH         -> "入出場兼用（全種別）"
        TileEntityCustomTurnstile.GateMode.IC_ONLY      -> "IC専用（入出場兼用）"
        TileEntityCustomTurnstile.GateMode.TICKET_ONLY  -> "切符専用（入出場兼用）"
        TileEntityCustomTurnstile.GateMode.PASS_ONLY    -> "定期専用（入出場兼用）"
    }
