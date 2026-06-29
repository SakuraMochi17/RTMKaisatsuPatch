# RTM Kaisatsu Patch

**Language:** [日本語](README.ja.md) | English | [中文](README.zh.md) | [한국어](README.ko.md)

A Minecraft 1.7.10 Forge mod that adds a railway fare system — ticket gates, fares, IC cards, commuter passes, and more — to **RealTrainMod (RTM)**.

---

## Requirements

One of the following setups is required.

**Option A: RTM + NGTLib**

| Component | Version |
|---|---|
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 or later |
| [RealTrainMod (RTM)](https://www.curseforge.com/minecraft/mc-mods/realtrainmod) | 1.7.10.41 or later |
| [NGTLib](https://www.curseforge.com/minecraft/mc-mods/ngtlib) | 1.7.10.32 or later |

**Option B: KaizPatchX (Recommended)**

| Component | Version |
|---|---|
| Minecraft | 1.7.10 |
| Forge | 10.13.4.1614 or later |
| [KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX/releases/latest) | Latest |

> **[KaizPatchX](https://github.com/Kai-Z-JP/KaizPatchX/releases/latest)** is an all-in-one build that bundles RTM and NGTLib with bug fixes and enhancements. It can be used instead of the RTM + NGTLib combination.

---

## Installation

1. Download all required mods and place them in `.minecraft/mods/`.
2. Download the latest `RTMKaisatsuPatch-*.jar` from [Releases](../../releases) and place it in the same `mods/` folder.
3. Launch Minecraft (world data is generated automatically on first start).

---

## Quick Start

```
1. Place a Station Manager block → right-click with the Settings Tool → register the station name
2. Place a Line Manager block → right-click with the Settings Tool → set line name, fares, and station order
3. Place a Ticket Gate → right-click with the Settings Tool → set the station name and gate mode
4. Place a Ticket Vendor → right-click with the Settings Tool → set the departure station
5. Insert coins into the Ticket Vendor, buy a ticket or IC card, and pass through the gate
```

For detailed instructions, see the **[Setup Guide](docs/en/setup.md)**.

---

## Documentation

| Page | Content |
|---|---|
| [Setup Guide](docs/en/setup.md) | Detailed walkthrough for first-time users |
| [Block Reference](docs/en/blocks.md) | Configuration for all blocks (gates, vendors, boards, etc.) |
| [Item Reference](docs/en/items.md) | Usage for all items (tickets, IC cards, passes, etc.) |
| [Company Management](docs/en/company.md) | Company creation, line assignment, IC interoperability, member management |
| [Admin Commands](docs/en/commands.md) | Full `/kaisatsu` command reference |
| [FAQ](docs/en/faq.md) | Troubleshooting and Q&A |

---

## Features

### Items

| Item | Description |
|---|---|
| Ticket | Single-use ticket with origin and destination recorded |
| IC Card | Prepaid balance card — automatic fare deduction, boarding history, company-specific design |
| Commuter Pass | Valid for a fixed route and period (7 / 30 / 90 days). Supports renewal |
| Coupon Ticket | 10-ride book at the price of 9 |
| Day Free Pass | Unlimited rides on the day of purchase |
| Reserved Express Ticket | Records car and seat number; supports reserved and unreserved seating |
| Boarding Certificate | Issued at unstaffed stations; settled with cash at staffed gates |
| Settings Tool | Opens the configuration GUI for any block |

### Blocks

| Block | Description |
|---|---|
| Ticket Gate | Supports tickets, IC cards, and passes. Configurable entry/exit modes and pass-through message |
| Ticket Vending Machine | Sells tickets, IC charge, passes (including renewals), and coupon tickets |
| Fare Adjustment Machine | Collects additional fare for IC cards and tickets when riding beyond the paid zone |
| Departure Board | Displays departure information linked to timetables. Supports real-world and in-game time |
| Boarding Certificate Machine | Issues boarding certificates for unmanned stations |
| IC Simple Reader | Compact terminal to check IC balance and boarding status |
| Station Manager Block | Manages station name, coordinates, and sales totals |
| Line Manager Block | Manages station order, fares, and company assignment |
| Train Manager Block | Defines limited express trains, cars, and stop lists |
| Reserved Seat Vendor | Sells, reserves, and cancels limited express tickets |

---

## FAQ (Excerpt)

**Q. I get an error when trying to pass through the gate**  
→ Check that the gate has a station name set and that the station is registered on a line. See [FAQ](docs/en/faq.md).

**Q. No destinations appear in the Ticket Vendor**  
→ Add the station to a line in the Line Manager block. Destinations are generated from line data.

**Q. I see "Entry station has been deleted" when exiting with an IC card**  
→ This appears when the server restarted or the station was deleted after you entered. It resets automatically — just enter again.

**Q. Where do I renew a commuter pass?**  
→ Ticket Vendor → "Pass" tab → "Renew" sub-mode. Passes with 7 or fewer days remaining are eligible.

See [FAQ](docs/en/faq.md) for more.

---

## License

Source code may be viewed for learning and reference purposes. Redistribution, modification, and commercial use are not permitted.  
RTM and NGTLib are the intellectual property of JP-MOD.
