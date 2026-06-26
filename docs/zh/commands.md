# 管理员命令

**语言:** [日本語](../commands.md) | [English](../en/commands.md) | 中文 | [한국어](../ko/commands.md)

所有命令均以 `/kaisatsu` 开头，需要 OP 权限。

---

## 车站管理

```
/kaisatsu station list
```
列出所有已注册的车站。

```
/kaisatsu station delete <车站名>
```
删除指定车站。将自动从所有线路和闸机配置中移除。

```
/kaisatsu station rename <旧车站名> <新车站名>
```
重命名车站。线路和闸机中的引用将自动更新。

---

## 线路管理

```
/kaisatsu line list
```
列出所有已注册的线路。

```
/kaisatsu line delete <线路ID>
```
删除指定线路。

---

## IC 卡及余额操作

```
/kaisatsu ic balance <玩家名>
```
显示指定玩家持有的 IC 卡余额。

```
/kaisatsu ic charge <玩家名> <金额>
```
向指定玩家的 IC 卡添加余额（管理员用）。

```
/kaisatsu ic reset <玩家名>
```
重置指定玩家 IC 卡的入站状态。用于玩家入站记录残留而无法出站的情况。

---

## 数据管理

```
/kaisatsu reload
```
从磁盘重新加载世界数据（车站、线路、销售额）。

```
/kaisatsu export
```
将当前数据以 JSON 格式输出到服务器日志（用于备份）。

---

## 调试

```
/kaisatsu debug fare <出发站> <目的站>
```
显示指定区间的票价计算结果和经路。用于排查票价异常。

```
/kaisatsu debug network
```
在聊天栏显示所有已注册车站、线路及其连接状态的网络图。

---

## 权限

执行命令需要 OP 等级 2 以上。如需将特定命令开放给工作人员，请通过权限插件（如 LuckPerms）授予以下权限节点：

| 节点 | 授权内容 |
|---|---|
| `rtmkaisatsu.station.list` | 查看车站列表 |
| `rtmkaisatsu.ic.balance` | 查看 IC 卡余额 |
| `rtmkaisatsu.ic.reset` | 重置入站状态 |
