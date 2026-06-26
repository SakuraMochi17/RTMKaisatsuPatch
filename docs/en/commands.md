# Admin Commands

**Language:** [日本語](../commands.md) | English | [中文](../zh/commands.md) | [한국어](../ko/commands.md)

All commands begin with `/kaisatsu`. OP permission is required.

---

## Station Management

```
/kaisatsu station list
```
Lists all registered stations.

```
/kaisatsu station delete <station-name>
```
Deletes the specified station. It is automatically removed from all line and gate configurations.

```
/kaisatsu station rename <old-name> <new-name>
```
Renames a station. All references in lines and gates are updated automatically.

---

## Line Management

```
/kaisatsu line list
```
Lists all registered lines.

```
/kaisatsu line delete <line-id>
```
Deletes the specified line.

---

## IC Card & Balance Operations

```
/kaisatsu ic balance <player-name>
```
Displays the IC card balance held by the specified player.

```
/kaisatsu ic charge <player-name> <amount>
```
Adds balance to the specified player's IC card (admin use).

```
/kaisatsu ic reset <player-name>
```
Resets the boarding status of the specified player's IC card. Use when a player is stuck in a boarded state and cannot exit.

---

## Data Management

```
/kaisatsu reload
```
Reloads world data (stations, lines, sales) from disk.

```
/kaisatsu export
```
Outputs current data to the server log in JSON format (for backup purposes).

---

## Debug

```
/kaisatsu debug fare <origin> <destination>
```
Displays the calculated fare and route for the specified section. Useful when fares seem incorrect.

```
/kaisatsu debug network
```
Displays a network graph of all registered stations, lines, and their connections in chat.

---

## Permissions

OP level 2 or higher is required to run commands. To grant specific commands to staff members, add the following nodes via a permissions plugin (e.g. LuckPerms):

| Node | Grants Access To |
|---|---|
| `rtmkaisatsu.station.list` | View station list |
| `rtmkaisatsu.ic.balance` | Check IC card balance |
| `rtmkaisatsu.ic.reset` | Reset boarding status |
