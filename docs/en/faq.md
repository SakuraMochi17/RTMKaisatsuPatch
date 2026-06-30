# FAQ

**Language:** [日本語](../faq.md) | English | [中文](../zh/faq.md) | [한국어](../ko/faq.md)

---

## Installation & Startup

### The game crashes on startup after adding the MOD

**Cause:** RTM or NGTLib may be missing or have an incompatible version.

**Fix:**
1. Open `.minecraft/logs/fml-client-latest.log` and check the error message
2. Confirm that RTM and NGTLib are in the `mods/` folder
3. Verify that the version combination matches the [Requirements in README](../../README.md#requirements)

---

## Gate Issues

### "This ticket is not valid here"

- Check that the ticket's origin and destination match the gate's station
- Enter through an entry gate and exit through an exit gate (check the gate's mode setting)

### "Insufficient IC balance" (IC Card)

Your IC card balance is too low. Charge it at a Ticket Vending Machine and try again.

### "Your commuter pass has expired"

The pass has passed its expiry date. Renew it via Ticket Vending Machine → "Pass" → "Renew" sub-mode (valid only if 7 or fewer days remain). If it has already expired, a new pass must be purchased.

### "This section is outside your commuter pass zone"

The section you are trying to use is not covered by your commuter pass. Only stations within the specified zone are accessible.

### Walking through the gate does nothing

- Confirm that a station name is set on the gate (right-click with the Settings Tool)
- Make sure you are holding a valid fare medium in your hand

---

## Fare Errors

### No destinations appear in the Ticket Vending Machine

Confirm that this station has been added to a line in the Line Manager Block. Destinations are generated automatically from line data.

### The fare seems wrong (too high or too low)

Run `/kaisatsu debug fare <origin> <destination>` to see the route and calculation details. When travelling via multiple lines, a transfer surcharge may be added.

---

## IC Card Issues

### "Entry station has been deleted"

This message appears when the server restarted or the station was deleted after you entered. It resets automatically — just enter again. If it keeps happening, ask an admin to run `/kaisatsu ic reset <player-name>`.

### The gate does not accept my IC card

If the gate has a "Supported Company" set, only IC cards from that company will work. Check that the company that issued your card matches the gate's supported company.

### My balance did not update after charging

Close and reopen your inventory and check again. It can take a few seconds to sync with the server.

---

## Commuter Pass Issues

### Cannot renew my commuter pass

Renewal is available only when the pass has **7 or fewer days** remaining. Wait until it is closer to expiry and try again.

### Cannot board at an intermediate station with my commuter pass

If the intermediate station is part of the pass's zone, you should be able to board there. Check that the station is correctly added to the line in the Line Manager Block.

---

## Departure Board Issues

### Nothing is displayed on the Departure Board

- Make sure the board is **bound to a settings block** (hold the Settings Tool and sneak-right-click the settings block, then the board)
- Check that the bound **settings block has a station and timetable** set
- If no departures are scheduled for the current time, "No departure information" is displayed
- To check the appearance only, turn on **Sample Mode** in the board's GUI

### Departure times are off

Check that the "Time Mode" on the Departure Board is set to "Real-world time". If set to "In-game time", Minecraft world time is used instead.

---

## Multi-World / Server Issues

### Managing separate lines in multiple worlds

Each world holds its own independent dataset. Switching worlds automatically uses the corresponding dataset.

### Backing up data

Use `/kaisatsu export` to dump data to the server log, or copy `data/rtmkaisatsu_network.dat` from the world folder.

---

## Other

### How do I buy tickets without money in Creative mode?

In Creative mode, you can take tickets and IC cards directly from the creative inventory. However, items taken this way lack proper NBT data and will not work at gates. Purchase them through the Ticket Vending Machine or use admin commands.

### I found a bug in the MOD

Please report it on [GitHub Issues](https://github.com/SakuraMochi17/RTMKaisatsuPatch/issues). Include the following:
- Versions of Minecraft, Forge, RTM, NGTLib, and this MOD
- Steps to reproduce the bug
- Log file (`fml-latest.log`)
