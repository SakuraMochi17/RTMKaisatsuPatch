# 管理者コマンド

**言語:** 日本語 | [English](en/commands.md) | [中文](zh/commands.md) | [한국어](ko/commands.md)

すべてのコマンドは `/kaisatsu` から始まります。OP 権限が必要です。

---

## 駅管理

```
/kaisatsu station list
```
登録されている全駅の一覧を表示します。

```
/kaisatsu station delete <駅名>
```
指定した駅を削除します。路線・改札機の設定から自動で除外されます。

```
/kaisatsu station rename <旧駅名> <新駅名>
```
駅名を変更します。路線・改札機の参照も自動で更新されます。

---

## 路線管理

```
/kaisatsu line list
```
登録されている全路線の一覧を表示します。

```
/kaisatsu line delete <路線ID>
```
指定した路線を削除します。

---

## ICカード・残高操作

```
/kaisatsu ic balance <プレイヤー名>
```
指定プレイヤーが所持するICカードの残高を確認します。

```
/kaisatsu ic charge <プレイヤー名> <金額>
```
指定プレイヤーのICカードに残高を追加します（管理者用）。

```
/kaisatsu ic reset <プレイヤー名>
```
指定プレイヤーのICカードの入場状態をリセットします。（入場記録が残ったまま出場できない場合に使用）

---

## データ管理

```
/kaisatsu reload
```
ワールドデータ（駅・路線・売上）をディスクから再読み込みします。

```
/kaisatsu export
```
現在のデータを JSON 形式でサーバーログに出力します（バックアップ用）。

---

## デバッグ

```
/kaisatsu debug fare <発駅> <着駅>
```
指定区間の運賃計算結果と経路を表示します。運賃がおかしいときの調査に使います。

```
/kaisatsu debug network
```
登録されている駅・路線・接続状態のネットワークグラフをチャットに表示します。

---

## パーミッション

コマンドの実行には OP レベル 2 以上が必要です。特定のコマンドをスタッフに開放したい場合は、パーミッションプラグイン（LuckPerms等）で以下のノードを付与してください。

| ノード | 内容 |
|---|---|
| `rtmkaisatsu.station.list` | 駅一覧の閲覧 |
| `rtmkaisatsu.ic.balance` | ICカード残高確認 |
| `rtmkaisatsu.ic.reset` | 入場状態リセット |
