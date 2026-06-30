# Setup Guide

**Language:** [日本語](../setup.md) | English | [中文](../zh/setup.md) | [한국어](../ko/setup.md)

This guide walks you through building a railway gate system from scratch using RTM Kaisatsu Patch.

---

## Prerequisites

- RTM trains, tracks, and station platforms are already placed
- You have operator (OP) permissions or are in Creative mode

---

## Step 1: Register a Station

### Place a Station Manager Block

1. Take a **Station Manager Block** from the creative tab "Kaisatsu Patch" and place it inside the station
2. Hold the **Settings Tool**, right-click the Station Manager Block to open its GUI
3. Enter the station name and press "Register"

> Station names must be unique across the entire system. Do not use the same name for multiple stations.

### Verify

Use `/kaisatsu station list` to confirm the station appears in the list.

---

## Step 2: Create a Line

### Place a Line Manager Block

1. Place a **Line Manager Block** and right-click it with the Settings Tool
2. Enter the "Line Name" and "Line ID" (alphanumeric), then press "New"
3. In the station order section, add the stations in the correct order
4. Set the base fare and distance rate in the fare section

### Fare Example

| Setting | Example |
|---|---|
| Base fare (up to 3 km) | 160 yen |
| Rate per km | 15 yen |

> Station distances are calculated automatically from the coordinates of each Station Manager Block.

---

## Step 3: Create a Company (Optional)

Grouping multiple lines under one company enables IC card interoperability.

1. Right-click the Line Manager Block with the Settings Tool → press "Company Setup" on the top screen
2. Press "+ New" and fill in the company name, abbreviation, and IC card color
3. Link the lines you want to manage under this company

See [Company Management System](../en/company.md) for details.

---

## Step 4: Place a Ticket Gate

1. Place a **Ticket Gate** at the gate entrance
2. Right-click with the Settings Tool to open the GUI
3. Configure the following settings:

| Setting | Description |
|---|---|
| Station | The station this gate belongs to |
| Mode | Entry / Exit / Both |
| Supported Company | Blank = all companies; specify to restrict to one company |
| Pass-through Message | Message shown in chat when a player passes (optional) |

> Gates form a passage with adjacent blocks. Place multiple gates side by side to create a wider barrier.

---

## Step 5: Place a Ticket Vendor

1. Place a **Ticket Vending Machine** outside the gate (the boarding area)
2. Right-click with the Settings Tool → set "Departure Station" to the station where this machine is located

The vendor will now automatically show all reachable destinations.

---

## Step 6: Test the System

1. Switch to Survival mode and right-click the Ticket Vendor
2. Select a destination and buy a ticket, or charge an IC card
3. Hold the purchased ticket or IC card and walk through the Ticket Gate

If you see an error message, see [FAQ](../en/faq.md).

---

## Optional Setup

### Departure Board

If timetable data exists, a **Departure Settings Block** (data source) and a **Departure Board** (display) can show real-time departure information. Bind them together with the Settings Tool.  
See [Block Reference › Departure Board](../en/blocks.md#departure-board).

### Fare Adjustment Machine

Handles excess-fare collection when a player rides beyond the paid destination.  
See [Block Reference › Fare Adjustment Machine](../en/blocks.md#fare-adjustment-machine).

### Unstaffed Stations (Boarding Certificate)

For stations without a gate attendant, use the **Boarding Certificate Machine**.  
See [Block Reference › Boarding Certificate Machine](../en/blocks.md#boarding-certificate-machine).
