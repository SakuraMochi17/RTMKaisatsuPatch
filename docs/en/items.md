# Item Reference

**Language:** [日本語](../items.md) | English | [中文](../zh/items.md) | [한국어](../ko/items.md)

---

## Ticket

A single-use pass valid from the purchased origin to the destination.

### How to Obtain

Buy from the "Ticket" tab of the Ticket Vending Machine.

### How to Use

- Hold the ticket and walk through the entry gate → entry stamp is applied
- Walk through the exit gate at your destination → ticket is automatically collected

### Item Info

- **Tooltip:** Shows origin, destination, and fare
- **Out-of-zone:** Cannot pass through gates at stations other than the destination

### Fare Calculation

Ticket fares are calculated in **10-yen increments (rounded up)**.

---

## IC Card

A prepaid balance card that can be used repeatedly as long as the balance is sufficient.

### How to Obtain

Issue a new card from the "IC" tab of the Ticket Vending Machine (an issuance fee may apply).

### How to Use

1. Pass through the entry gate → entry station and time are recorded
2. Pass through the exit gate → fare is deducted automatically

### Item Info

- **Tooltip:** Balance, boarding status, and recent usage history (up to 10 entries)
- Cannot pass if the balance is insufficient (top up in advance)
- IC fares are calculated in **1-yen increments (rounded up)**

### Charging

Top up from the "IC" tab of the Ticket Vending Machine. The amount is deducted from your inventory money.

### Returning

If you no longer need the card, return it via the "IC Return" option in the Ticket Vending Machine to receive a refund of the remaining balance (plus the deposit, if applicable).

---

## Commuter Pass

A pass that allows unlimited passages within the specified zone during the validity period.

### How to Obtain

Ticket Vending Machine → "Pass" tab → "New" sub-mode → select destination and duration, then purchase.

### Duration Options

| Type | Valid Period |
|---|---|
| 7-day pass | 7 days from purchase date |
| 30-day pass | 30 days from purchase date |
| 90-day pass | 90 days from purchase date |

### How to Use

Simply hold the pass and walk through the gate. Works for both entry and exit.

### Zone Coverage

- Valid not only at the origin and destination but also at **intermediate stations along the route**
- Cannot be used at stations outside the specified zone

### Renewal

When a pass has 7 or fewer days remaining, you can renew it via Ticket Vending Machine → "Pass" → "Renew" sub-mode. Days are added from the current expiry date, so no validity is wasted.

### Tooltip Colours

| Colour | Meaning |
|---|---|
| Green | 8 or more days remaining |
| Yellow | 1–7 days remaining (expiring soon) |
| Red | Expired |

---

## Coupon Ticket

A book of 10 single-ride tickets for a specified route at a discounted price.

### How to Obtain

Buy from the "Coupon" tab of the Ticket Vending Machine.

### How to Use

- One ride is consumed each time you pass through the gate
- The item is automatically deleted after all 10 rides are used

### Price

The price of 9 regular tickets (one ride free).

---

## Day Free Pass

Allows unlimited rides on all lines of the issuing company on the day of purchase.

### How to Obtain

Ticket Vending Machine → "Pass" tab → "Free Pass".

### Restrictions

- **Valid on the day of purchase only** (determined by Minecraft world date)
- Only valid on lines belonging to the issuing company

---

## Express Ticket

A ticket recording the specific train service and seat.

### How to Obtain

Purchase from the **Reserved Seat Vendor**.

### Types

| Type | Details |
|---|---|
| Reserved Seat | Car and seat number are fixed |
| Unreserved Seat | No assigned car or seat (sit in any available seat) |
| Green Car Ticket | For Green Car (first-class) cars only |

### How to Use

Hold the express ticket and pass through the gate for the designated service.

---

## Boarding Certificate

A document that certifies the boarding station and time when departing from an unmanned station.

### How to Obtain

Right-click the **Boarding Certificate Machine** to receive one automatically.

### How to Use

Hold the certificate and pass through the entry gate at a staffed station. The fare for the boarded section is settled automatically.

### Notes

- The certificate is consumed on first use
- Cannot be used at gates belonging to a different company than the one where it was issued

---

## Settings Tool

A tool for opening the configuration GUI of any block.

### How to Use

Hold the Settings Tool and **right-click** the block you want to configure.

> Without the Settings Tool you cannot open any block's configuration screen. Keep one in your inventory from the creative tab.
