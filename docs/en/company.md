# Company Management System

**Language:** [日本語](../company.md) | English | [中文](../zh/company.md) | [한국어](../ko/company.md)

The design is **one Line Manager Block = one company**. Placing the block creates a company, and all company settings (name, colour, IC name, members, IC interoperability) are managed through that block's GUI.

---

## Creating a Company

1. Place a **Line Manager Block** and right-click it with the Settings Tool
2. Press the "Company Setup" button (first time only)
3. Fill in the following fields and press "Save":

| Field | Description |
|---|---|
| Company ID | Alphanumeric identifier (e.g. `SKR`). Cannot be changed after saving |
| Company Name | Display name (e.g. Sakura Electric Railway) |
| Colour | IC card display colour (hex, e.g. `FF0000`) |
| IC Card Name | Brand name of the IC card (e.g. `SakuraIC`) |
| Default Base Fare | Base fare pre-filled when creating a new line (yen) |
| Rate | Distance surcharge pre-filled when creating a new line (yen/block) |

> The Company ID is used to identify the company in lines, IC cards, and interoperability settings. Choose carefully — it cannot be changed later.

---

## Managing Lines

Adding, editing, and deleting lines is done from the top screen of the Line Manager Block.

1. Right-click the Line Manager Block with the Settings Tool
2. Press "+ New Line" or "Edit Line →"

Lines are automatically associated with the company managed by that block.

---

## Member Management

Line Manager Block GUI → top screen → "Members" button

| Action | Description |
|---|---|
| Add | Enter a player name and press "Add" |
| Remove | Press the "Remove" button next to the player |

> Server OPs can modify the settings of any company.

---

## IC Card Interoperability

Allows a single IC card to cover fares across lines belonging to different companies.

### Setup

1. Line Manager Block GUI → press "Mutual Use"
2. Select the partner company and press "Allow"

### Conditions

- Interoperability becomes active as soon as either company grants permission
- Revoking permission disables transfers in that direction

### Fare Calculation

Fares are calculated individually for each company's segment, then summed. A transfer surcharge is added if configured for that line.

---

## Multiple Line Manager Blocks

Each block represents an independent company. You can issue IC cards through each company and link them with interoperability settings.
