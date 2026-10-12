# Pause: Purchase Checklist App Spec

Draft v0.1. A personal app that walks you through a smart-spending checklist before each non-essential purchase, with the amount of scrutiny scaled to the price.

## 1. Goal

Make it easy to pause before buying. The app logs when you first wanted something, asks what it costs, and runs you through only as many questions as the price deserves. It is a speed bump, not a budget tracker.

## 2. Core flow

1. **Add item.** Type what you're thinking of buying. The app stamps `first_considered_at` with the current date and time.
2. **Enter cost.** Total price, ideally including tax, shipping and add-ons.
3. **Tier is chosen automatically** from the price (see section 3).
4. **Walkthrough.** One question per screen, drawn from the checklist. Each question takes the answer type that suits it: yes/no, a choice between alternatives, or free text. Any answer can carry an optional note.
5. **Summary.** All the answers on one screen, with warning-sign answers highlighted. The app makes no recommendation; the choice is the user's.
6. **If the tier has a wait:** the item goes into a waiting list with a "decide on" date and a reminder. Otherwise the user marks it Bought or Skipped.
7. **On the reminder:** a short re-check ("Do you still want it? Are you calm about it?"), then mark it Bought or Skipped.
8. **90 days after buying:** a notification asks two questions: "Do you remember buying it?" and "Are you happy with the purchase?" The answers are kept for analytics and don't change anything else in the app.

## 3. Scrutiny tiers (all configurable)

Default thresholds, which the user can edit, add to or remove:

| Tier | Price | Behavior | Wait |
| --- | --- | --- | --- |
| Small | under $25 | Quick check: 3 key questions | none |
| Medium | $25 to $100 | Full checklist | optional (default: sleep on it, 24 hrs) |
| Large | over $100 | Full checklist, then a cool-off | 3 days |
| Huge | over $500 | Full checklist, cool-off | 7 days |
| Life changing&#32; | Over $10000 | Full checklist, cool-off | 30 days |

Per tier the user sets: price range, mode (short list / full list), and wait length (none, or a number of hours or days).

**Cool-off rules:**

- The wait runs from `first_considered_at`, not from when the questions were finished.
- The wait is advisory, not a lock: the app can't stop a real-world purchase. It shows the time remaining and a gentle reminder, and the user can mark the item Bought at any time; buying before the wait ends is logged.

## 4. Questions

- Seed with the 15 questions from the Smart Spending Checklist, grouped as Need & Clarity, Alternatives & Timing, Usefulness & Quality, and Costs & Tradeoff.
- Questions are stored as data: editable, reorderable, and each tagged with which tiers include it.
- Default "Small" tier set: *Do I actually need this?*, *Do I already own something that does this well enough?*, *Can I afford this comfortably, not just technically?*
- Default re-check set (after waiting): *Would I still want this at full price, with no sale or hype?*, *Am I calm enough to decide clearly?*, plus *What am I saying no to?*
- Note: the checklist is by Sunny Kind Journey. Fine for personal use. If this is ever shared or published, write original questions or get permission.

* Each question has an answer type: yes/no (for example, *Do I already own something that does this well enough?*), a choice between alternatives (*Could I borrow it, buy it second-hand, or get a simpler version?*), or free text (*What problem will this solve in my real life, specifically?*). The type is editable per question.

## 5. Summary

- The summary highlights warning-sign answers. Yes/no and choice questions each define which answers count; free-text answers are shown as written, and the user can flag any of them as a warning sign.
- Wording stays gentle, following the source: "Not now doesn't have to mean never."
- There is no verdict. The app doesn't recommend Buy, Wait or Skip; the decision is always the user's.

## 6. Data model

**Item**

- `id`, `name`, `notes`
- `first_considered_at` (timestamp)
- `cost` (number), `currency`
- `tier_id` (the tier at decision time)
- `status`: considering / waiting / bought / skipped
- `wait_until` (timestamp, nullable)
- `decided_at`, `final_cost` (nullable)
- `answers[]`: `question_id`, `answer`, `note`, `answered_at`, and `phase` (initial or recheck)
- `flagged` (boolean): set by the user on a free-text answer; counts as a warning sign in the summary
- `followup_due_at` (bought date plus 90 days), `remembers_it` (yes/no), `happy_with_it` (yes/no), `followup_answered_at` (nullable)

**Tier**: `id`, `min_price`, `max_price`, `mode`, `wait_hours`, `question_ids[]`

**Question**: `id`, `text`, `group`, `active`, `order`, `answer_type` (yes/no, choice or free text), `options[]` (choice only), `warning_answers[]` (yes/no and choice only)

## 7. Screens

1. **Home**: big "What are you thinking of buying?" field, plus a list of items currently waiting with countdowns.
2. **Cost entry**
3. **Walkthrough**: one question per screen, progress indicator, an answer input matched to the question type, and a note field.
4. **Summary**
5. **Waiting list**: sorted by decide-on date.
6. **Re-check**
7. **Follow-up**: the two 90-day questions.
8. **History**: bought and skipped items with answers and follow-up results, plus a running "money not spent" total for skipped items.
9. **Settings**: tiers, questions, notifications.

## 8. Notifications

- A local notification at `wait_until`: "Still want the \[item\]?"
- One optional gentle nudge a day later if it's unresolved. No streaks, no guilt.
- A local notification 90 days after an item is marked Bought, opening the two follow-up questions.

## 9. Version 1 scope

**Platform:** native Android.

**Launch speed:** the app opens quickly, straight to the item field, so no separate quick-capture shortcut is needed.

**Storage:** local-only on the device to start; no sync.

**Price entry:** the user types a single all-in price. The app does no tax, shipping or extras calculation.

**In:** add item with timestamp, price-based tiers, walkthrough, summary, waiting list with reminders, 90-day follow-up, history, editable tiers and questions.

**Out for now:** accounts or sync, budgets or bank links, sharing, price tracking, browser extension.

## 10. Open questions

None right now.
