# UI refinements · October 2026

- `ParcelDialog` is the shared popup shell for notices, global action errors, logout, address deletion/editing and companion confirmations. It provides a parcel illustration, close icon, themed surface, scrollable body and action footer. Field validation stays inline.
- Successful profile updates display a “Profile updated” popup with “Saved on this device”.
- Search clears through an accessible cross icon (“Clear search”).
- Bottom tabs no longer save/restore navigation stacks under the Home destination. Selecting Home pops back to Home and cannot restore a Purchased stack. Route transitions are immediate to prevent stale tab content during navigation. Normal activity recreation still preserves the currently open page.
- Profile → Appearance switches between light and dark. The choice is device-wide, persisted in the existing preferences table, and defaults to dark when absent. Shared semantic colors, decorative surfaces, dialogs and system-bar icons follow it.
- Default delivery advances hourly: confirmed (0h), shipped (1h), nearby hub (2h), out for delivery (3h), delivered (4h). The existing saved confirmation time anchors the schedule; reopening catches up. Instant delivery remains available.
- Tracking shows the local calendar date/time for every milestone, labels future milestones “Expected”, and displays the arrival timestamp after delivery. The map follows the saved final-leg interval.
- A day remains 24 hours in return-window calculations; the four-hour shipping duration uses a separate constant.

Each new order randomly chooses hourly progression or a same-day schedule. Late-night same-day schedules divide the remaining time into four equal steps, finishing just before local midnight; hourly schedules may cross midnight. The duration is saved once per order. Instant delivery preserves completed timestamps and stamps all remaining steps with the instant-delivery time.

No schema version change is needed: appearance is a new preference key and delivery duration is stored in the existing JSON payload. Older orders default to the four-hour schedule; already delivered/unboxed items remain delivered and owned.
