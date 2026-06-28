# Texas Hold'em Poker

A cross-platform **Texas Hold'em Poker** game built with **Compose Desktop** (Kotlin Multiplatform Compose).  
Offline-first — no network required. Play against AI opponents right on your desktop.

## Features

- ♠️ Full **Texas Hold'em rules**: blinds, betting rounds (pre-flop → flop → turn → river), side pots for all-in situations
- 🃏 5 **AI opponents** with position-aware strategy
- 🏆 Complete **hand evaluation** (10 ranks: High Card → Royal Flush)
- 💰 **Bluff & raise** mechanics — AI bluffs occasionally
- 🎨 Dark **green felt table** UI with card fan display
- 📜 Scrollable **hand history** log
- 🔄 **New Hand** button to keep playing

## Quick Start

```bash
# Prerequisites: JDK 17+
./gradlew :desktop-app:run
```

Or to build a native installer:

```bash
./gradlew :desktop-app:packageDistributionForCurrentOS
```

The distributable will be in `desktop-app/build/compose/binaries/`.

## Project Structure

```
TexasHoldemPoker/
├── game-engine/          # Pure Kotlin — no framework dependencies
│   └── src/main/kotlin/com/leejl/poker/
│       ├── model/        # Card, Deck, Player, Action, HandRank
│       ├── engine/       # HandEvaluator, GameManager (state machine)
│       └── ai/           # AiStrategy
├── desktop-app/          # Compose Desktop UI
│   └── src/main/kotlin/com/leejl/poker/
│       ├── ui/           # GameScreen, CardView, PlayerPanel, ActionBar
│       ├── model/        # GameViewModel
│       └── Main.kt       # Entry point
└── gradle/               # Build config (Kotlin 2.1.10 + Compose 1.7.3)
```

## How to Play

1. Run `./gradlew :desktop-app:run`
2. You are the player at the **bottom-center** of the table
3. When it's your turn, click **Check / Call / Raise / Fold / All-In**
4. Use **Custom Raise** to set a specific raise amount
5. Click **New Hand** after each hand to continue

## Future Ideas

- [ ] Save/Load game state
- [ ] Adjustable number of AI players
- [ ] AI difficulty levels
- [ ] Hot-seat local multiplayer
- [ ] Android version (Compose Multiplatform)
- [ ] Tournament mode with blind escalation
- [ ] Chip animation

## Tech Stack

- **Kotlin 2.1.10** + **Compose Multiplatform 1.7.3** (Desktop)
- **Gradle 9.1.0** with version catalog
- Zero external runtime dependencies (everything is in the Kotlin stdlib + Compose)

## License

MIT
