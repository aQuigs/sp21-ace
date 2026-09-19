# Strategy chart sources

`StrategyChartData.kt` holds six-deck Spanish 21 charts for three rule sets. Every square was transcribed twice, independently, from the published charts and quoted excerpts below, then checked against the originals. The fixtures in this folder list each square with the sources that print its play, and `ChartCellsTest` pins every square to them.

## Reading the grids

- **Plays:** H hit, S stand, D double, P split, R surrender, RH surrender if allowed, otherwise hit.
- **Card counts:** a digit means hit instead once the hand has that many cards or more. D3 doubles a two-card hand but hits with 3 or more cards.
- **Bonus marks:** `*` means hit if any 6-7-8 bonus is possible, `'` if a suited 6-7-8 is possible (spades included), and `"` if a spaded 6-7-8 is possible. `$` means hit suited 7-7 against a dealer 7, for the Super Bonus.
- **Super Bonus:** `$` exists for the Super Bonus, a fixed $1,000 or $5,000 for a suited 7-7-7 against a dealer 7, so it assumes the table pays one on the bet. Nevada's rules of play have no Super Bonus, the New Jersey, Pennsylvania and Maryland tiers start at a $5 bet, and Wizard of Odds says the benefit shrinks as the bet grows past $5 or $25.
- **Debated squares:** `†` marks a square the sources still debate. They're listed below.
- **Double Down Rescue:** without redoubling, the charts print their table for a hard hand already doubled as Double Down Rescue, rows 12 to 17 only, and the fixtures file it under `AFTER_DOUBLE_HARD`. `.` means no rescue: stand on the doubled hand. When redoubling is allowed, the rescues are the R squares of the after-doubling tables.

## Rule sets

- `H17_REDOUBLE`, dealer hits soft 17, redoubling allowed: 6 decks · Dealer hits soft 17 · Redoubling allowed · Redouble limit: up to 3 doubles in How's chart, not stated by Wizard of Odds · Late surrender · Double on any number of cards · Double after split · Double Down Rescue · Resplit aces · Hit and double split aces · No bonus on doubled hands · Player 21 always wins
- `H17`, dealer hits soft 17, no redoubling: 6 decks · Dealer hits soft 17 · No redoubling · Late surrender · Double on any number of cards · Double after split · Double Down Rescue · Resplit aces · Hit and double split aces · No bonus on doubled hands · Player 21 always wins
- `S17`, dealer stands on soft 17: 6 decks · Dealer stands on soft 17 · No redoubling · Late surrender · Double on any number of cards · Double after split · Double Down Rescue · Resplit aces · Hit and double split aces · No bonus on doubled hands · Player 21 always wins

## Sources

- **Wizard of Odds**: [Spanish 21 strategy (Wizard of Odds)](https://wizardofodds.com/games/spanish-21/), by Michael Shackleford. Full charts: H17, S17, H17 with redoubling. The de facto standard: laminated strategy cards, Casino Player magazine, beatingbonuses, QFIT's trainer and many forum answers copy or point to it. Original analysis. Katarina Walker corrected some cells on this page in 2007, so agreeing with Walker is weaker evidence than agreeing with Stephen How.
- **Stephen How**: [Spanish 21 strategy (Discount Gambling)](https://discountgambling.net/spanish-21/), by Stephen How. Full chart: H17 with redoubling. Independent program for the redoubling game only. How says it doesn't carry suit information, and it prints no card-count digits on soft hands. How gives no reason for hitting 8-8 vs 10 or for the undefined P!, and How's common-mistakes table says hit 3-3 vs 6 where the chart splits.
- **Jeff Wu (Wizard of Odds, 2003–07)**: [Redoubling table by Jeff Wu (on Wizard of Odds, 2003–07)](https://web.archive.org/web/20030206082101/http://thewizardofodds.com/game/spanish21.html), by Jeff Wu. Former Wizard of Odds redoubling chart. Wizard of Odds' redoubling chart until at least 2006, replaced by the Wizard's own tables in 2007. Where the current chart differs, Wu's table agrees with How, and at 10 vs 7-8 and 11 vs 7-8 with Walker's 2008 errata.
- **Grochowski (Frome)**: [Basic Strategy for Spanish 21 (Casino City Times, 2002)](https://www.casinocitytimes.com/john-grochowski/article/basic-strategy-for-spanish-21-806), by John Grochowski. Full chart in prose: H17. Probably Lenny Frome's 1990s strategy via the Frank Scoblete Network, which Wizard of Odds replaced in 2001. Doesn't state its redouble, split or double-after-split rules.
- **HitOrSplit.com**: [Spanish 21 trainer charts (HitOrSplit.com, 2009)](http://www.hitorsplit.com/spanish21_game.html). Full charts: H17, S17, H17 with redoubling. Probably Katarina Walker's charts, but the site never names a source. Outside Walker's known corrections they equal the Wizard of Odds charts as captured in January 2010, so they don't count as independent of Wizard of Odds. Its 8-deck charts are the only ones found, and the app leaves 8 decks out.
- **Walker (quoted)**: [The Pro's Guide to Spanish 21 and Australian Pontoon (quoted)](https://wizardofvegas.com/forum/questions-and-answers/gambling/10322-spanish-21-basic-strategy-discrepancies-between-the-wizards-and-katarina-walkers/), by Katarina Walker. Excerpts and errata. The standard reference book, seen here only through errata and reader quotes. The 2008 errata agree with Jeff Wu's table and How at 10 vs 7-8 and 11 vs 7-8. HitOrSplit.com matches every quoted cell except a reader's paraphrase at 13 vs 5.
- **MGP analyzer (quoted)**: [MGP Blackjack Combinatorial Analyzer output (quoted on Wizard of Vegas, 2020)](https://wizardofvegas.com/forum/gambling/blackjack/35474-spanish-21-h17-redouble-basic-strategy/). Excerpt: 3 pair hands. Agrees with the charts on all three hands, but a simulation in the same post favours the other play.
- **Frome/Scoblete (quoted)**: [Armada Strategies for Spanish 21 (quoted)](https://wizardofodds.com/ask-the-wizard/spanish-21/), by Lenny Frome via Frank Scoblete. Excerpt. Rules not reported.
- **Casino Vérité (quoted)**: [Casino Vérité table (quoted)](https://wizardofodds.com/ask-the-wizard/spanish-21/), by Norm Wattenberger. Excerpt. Independence unknown; rules not reported.
- **beatingbonuses**: [Spanish 21 strategy (beatingbonuses, 8 decks, Microgaming)](https://www.beatingbonuses.com/tables_spanish.htm). Copy. A copy of an older Wizard of Odds chart. Used only to check copying, never counted.

## How disagreements were settled

Wizard of Odds' charts are the de facto standard, and Katarina Walker's The Pro's Guide to Spanish 21 and Australian Pontoon is the standard book. The Wizard says the two agree. Laminated strategy cards, Casino Player magazine, beatingbonuses, QFIT's trainer and forum answers copy or point to the Wizard's charts.

Almost none of the disagreements come from different rules. Grochowski's article carries Lenny Frome's 1990s strategy, which the Wizard replaced in 2001. In the redoubling game the Wizard rewrote Jeff Wu's table in 2007 and redrew it in 2014, changing several cells where Wu's table and Stephen How still agree. How's chart prints no card-count digits on soft hands and has a few unexplained plays.

A disputed hand counts as settled when independent analyses agree and the other side is a revision, an outdated analysis or a probable copy. Where no source settles a hand, the app makes a judgement call. † marks the hands still debated: those judgement calls, and the settled plays that contradict Wizard of Odds' current chart.

## Debated squares

The app marks these squares †, and the fixtures mark them `yes` in the debated column.

### Dealer hits soft 17, redoubling allowed

- **10 vs 7 · Hard totals: `D4`.** Three independent analyses print D4: Jeff Wu (2003), Stephen How (2009) and Walker's corrected book (2008 errata). The Wizard's chart since 2007 prints D and no published reason was found. HitOrSplit.com prints D4 but is probably a copy of Walker's charts.
- **10 vs 8 · Hard totals: `D3`.** Three independent analyses print D3: Jeff Wu (2003), Stephen How (2009) and Walker's corrected book (2008 errata). The Wizard's chart since 2007 prints D5 and no published reason was found. HitOrSplit.com prints D3 but is probably a copy of Walker's charts.
- **11 vs 7 · Hard totals: `D4`.** Three independent analyses print D4: Jeff Wu (2003), Stephen How (2009) and Walker's corrected book (2008 errata). The Wizard's chart since 2007 prints D5 and no published reason was found. HitOrSplit.com prints D4 but is probably a copy of Walker's charts.
- **11 vs 8 · Hard totals: `D4`.** Three independent analyses print D4: Jeff Wu (2003), Stephen How (2009) and Walker's corrected book (2008 errata). The Wizard's chart since 2007 prints D5 and no published reason was found. HitOrSplit.com prints D4 but is probably a copy of Walker's charts.
- **17 vs 7 · Hard totals: `S`.** Jeff Wu, Stephen How and the Wizard's own 2010 chart print S (and so does HitOrSplit.com, a copy). The S6 exception appeared in the 2014 redraw, which introduced two other cell changes that were later reverted, and no source explains it.
- **Soft 13 vs 3 · Soft totals: `D3`.** Pick: double, but hit a soft 13 of 3 or more cards. Wizard of Odds and HitOrSplit.com hit that hand, and Jeff Wu's former chart doubles it. Stephen How's chart gives no card limits on any soft hand, so it doesn't count either way.
- **Soft 14 vs 3 · Soft totals: `D3`.** Pick: double. Wizard of Odds, Jeff Wu's former chart and Walker's book (per a reader) double; Stephen How hits and calls the decision very close.
- **Soft 14 vs 4 · Soft totals: `D4`.** Pick: double, but hit a soft 14 of 4 or more cards. Wizard of Odds and HitOrSplit.com hit that hand, and Jeff Wu's former chart doubles it. Stephen How's chart gives no card limits on any soft hand, so it doesn't count either way.
- **6 vs 3 · After doubling: hard totals: `D`.** Pick: redouble, per Wizard of Odds. Following the chart you never reach this hand, because it never doubles a total below 5, but the row is there for completeness, for a player who has already made a mistake.

### Dealer stands on soft 17

- **15 vs 6 · Hard totals: `S6"`.** Pick: keep Wizard of Odds' spade 6-7-8 exception. The only source without it is HitOrSplit.com, a copy of an older version of the same chart.
- **Soft 20 vs 10 · Soft totals: `S`.** Pick: stand. A dealer showing 10 can never have soft 17, so the soft-17 rule can't change this play, yet Wizard of Odds prints the exception only on its stands-on-soft-17 chart, on a row that also covers soft 21. Walker's book (per readers) and HitOrSplit.com stand.

## Known gaps

The charts print plays, not what to do when a play isn't allowed. The trainer needs a sourced answer to each of these before it deals such hands.

- **8-8 vs A:** where the charts say surrender, they don't say what to do when surrender isn't possible, for example after a split. The only published fallback is HitOrSplit.com's generic legend: "if Surrender not possible - Hit".
- **Redoubling limit:** the after-doubling tables say when to redouble, but not what to do once no more doubles are allowed. Stephen How's chart allows up to 3 doubles; Wizard of Odds doesn't state a limit.
- **Redoubled hands:** Wizard of Odds' after-doubling table is for when "the player has already doubled", and Stephen How prices his worked example, 4-6 doubled against an 8 to a 12, as a hand doubled once, a rescue at -100% of the initial bet against -101.5% for standing. A rescue "forfeits an amount equal to his original bet" (Wizard of Odds) however many doubles are on the hand, while standing risks all of them, so the line between standing and rescuing moves once a hand is redoubled, and neither source prints separate plays for it. The trainer deals only hands doubled once.
