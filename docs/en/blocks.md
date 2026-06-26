# Block Reference

**Language:** [日本語](../blocks.md) | English | [中文](../zh/blocks.md) | [한국어](../ko/blocks.md)

---

## Ticket Gate

**Purpose:** A gate block that controls entry and exit using tickets, IC cards, commuter passes, and coupon tickets.

### Placement

Place in a single vertical column at the gate entrance. Place multiple side by side for a wider barrier.

### Configuration (right-click with Settings Tool)

| Setting | Description |
|---|---|
| Station | The station this gate belongs to. Used to look up fares against line data |
| Mode | **Entry** / **Exit** / **Both** |
| Supported Company | Blank = accept IC from all companies; specify to restrict to one |
| Pass-through Message | Message shown in chat on successful passage |
| Blacklist | Block specific players from passing |

### Accepted Media

| Media | Behavior |
|---|---|
| Ticket | Collected when passing the exit gate at the destination |
| IC Card | Entry recorded on entry; fare deducted on exit |
| Commuter Pass | Passes if within valid period and covered zone; error otherwise |
| Coupon Ticket | Consumes one ride per passage |
| Day Free Pass | Valid only on the day of purchase |
| Express Ticket | Used for entry/exit of the designated train service |
| Boarding Certificate | Proof of boarding at an unstaffed station; fare settled on exit |

---

## Ticket Vending Machine

**Purpose:** An automated machine that sells various types of fare media.

### Tabs

| Tab | Items Sold |
|---|---|
| Ticket | Single-ride tickets to the selected destination |
| IC Card | New issuance and top-up (charge) |
| Commuter Pass | New issue (7 / 30 / 90 days) and **renewal** |
| Coupon Ticket | 10-ride coupon for the selected route |
| Day Free Pass | Unlimited-ride day pass |

### Commuter Pass Renewal

- Ticket tab → "Pass" → switch to "Renew" sub-mode
- Passes in your inventory with 7 or fewer days remaining appear as renewal candidates
- Days are added from the current expiry date, so no days are wasted

### Configuration

| Setting | Description |
|---|---|
| Departure Station | The station where this machine is located |
| Supported Company | The company whose IC cards are sold here |

---

## Fare Adjustment Machine

**Purpose:** Collects additional fare when a player has ridden beyond their paid destination on an IC card or ticket.

### How to Use

1. Right-click the Fare Adjustment Machine at the exit station
2. Hold the IC card or ticket and press "Settle"
3. The shortfall is automatically deducted from your inventory
4. Pass through the regular gate with the now-settled media

---

## Departure Board

**Purpose:** Displays upcoming departure information in real time when placed on a platform or near the gate.

### Configuration (right-click with Settings Tool)

| Setting | Description |
|---|---|
| Title | Display name (blank = station name) |
| Station | The station whose departures are shown |
| Line | Filter (blank = all lines) |
| Timetable | The timetable dataset to use |
| Direction | Both / Outbound / Inbound |
| Platform | Platform number to display |
| Rows | Up to 8 rows |
| **Time Mode** | **Real-world time** or **In-game time** |

### Time Mode

- **Real-world time** — the actual clock of the machine running the server (default)
- **In-game time** — Minecraft world time (tick 0 = 6:00 AM)

### Display Columns

| Column | Content |
|---|---|
| Time | Departure time (HH:MM) |
| Destination | Terminal station name |
| Type | Local / Express / Limited Express, etc. |
| Train No. | Train identifier set in the timetable |

---

## Boarding Certificate Machine {#boarding-certificate-machine}

**Purpose:** Issues a certificate recording the boarding station and time for passengers at unstaffed stations.

### How to Use

1. Place near the gate at an unmanned station
2. Passengers right-click to receive a certificate
3. At a staffed station, hold the certificate and pass through the gate in **Entry mode**
4. The fare for the boarded section is settled automatically

### Certificate Contents

- Boarding station name
- Issue time (Minecraft world date)

> A certificate is consumed on first use. If lost, it must be re-issued.

---

## IC Simple Reader

**Purpose:** A compact terminal for checking IC card balance and boarding status only — it cannot be used to pass through a gate.

### How to Use

Right-click to display the IC card information from your inventory.

---

## Station Manager Block

**Purpose:** Manages basic station information (name, coordinates, sales totals).

### Configuration

| Setting | Description |
|---|---|
| Station Name | The identifier used by lines and gates |
| Display Name | Name shown on departure boards, etc. (optional) |

### Sales View

Check daily and cumulative sales in the GUI's "Sales" tab. Administrators can also collect the money.

---

## Line Manager Block

**Purpose:** One block represents one company. Use this block's GUI to define lines, station order, fares, and company properties (members, IC interoperability).

### GUI Pages

| Page | Content |
|---|---|
| Top | Company overview, line list, navigation buttons |
| Line Edit | Line ID, name, fares, station order |
| Company Settings | ID, name, colour, IC card name, default fares |
| Member Management | Add or remove players with management rights |
| IC Interoperability | Grant or revoke IC interoperability with other companies |

### First-time Setup

1. Right-click with the Settings Tool → a "Company Setup" button appears
2. Enter the company ID, name, etc. and press "Save" → the company is registered and the normal top screen appears
3. Add lines as needed

See [Company Management System](../en/company.md) for details.

---

## Train Manager Block

**Purpose:** Defines limited express and reserved-seat train services.

### Configuration

- Train name, train number, number of cars
- Stop list and scheduled times
- Seat count and grade per car (Reserved / Unreserved / Green Car)

---

## Reserved Seat Vendor

**Purpose:** Sells, reserves, and cancels limited express tickets.

### Tabs

| Tab | Function |
|---|---|
| Purchase | Select train, date, car, and seat to buy |
| Reservation Check | Review purchased express tickets |
| Cancel | Refund a purchased express ticket |
