package jp.sakuramochi.kaisatsupatch.block.tileentity

import jp.ngt.rtm.block.tileentity.TileEntityTurnstile
import net.minecraft.block.Block

// ★親クラスを TileEntityMachineBase ではなく、本家の TileEntityTurnstile に変更します！
class TileEntityCustomTurnstile : TileEntityTurnstile() {

    // 🌟 魔法の裏技コード 🌟
    // RTMの描画システムに「自分はRTM本家の改札機だぞ」と錯覚させます。
    // これにより、本家のフラップ開閉アニメーションがあなたのブロックでも強制的に動くようになります！
    override fun getBlockType(): Block {
        return jp.ngt.rtm.RTMBlock.turnstile
    }

    // -------------------------------------------------------------
    // getMachineType() や NBTへの保存、count の処理などは
    // 全て親クラス（本家のTileEntityTurnstile）が自動でやってくれるため、
    // 前回書いたコードはごっそり消してしまってOKです！超スッキリします。
    // -------------------------------------------------------------
}
