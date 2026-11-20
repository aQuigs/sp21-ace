# Index sources

`IndexSetData.kt` holds two built-in sets of blackjack indices for the Hi-Lo count: Don Schlesinger's Illustrious 18 and Fab 4, the 22 departures from basic strategy worth the most to a card counter. Both are for a six-deck shoe with late surrender: `BJ_SHOE_S17`, where the dealer stands on soft 17, and `BJ_SHOE_H17`, where the dealer hits it. The fixtures in this folder list each index with the sources that print it, and `BuiltInIndicesTest` pins every number, play, direction and debated mark to them.

## Reading the fixtures

- **An index:** make the named play at that true count or higher, otherwise play basic strategy. The true count is the Hi-Lo running count divided by the decks left to deal. Every built-in index reads "or higher" (`AT_OR_ABOVE`).
- **Rounding:** every number is for a floored true count, one always rounded down, so −1.5 counts as −2. A preset's deck estimate and true count rounding settings change no number, since no other convention has a readable table (see Known gaps).
- **status:** `agree` when at least two independent sources print the number, `conflict` when independent sources disagree, `single-source` when only Schlesinger's list prints it. The app marks every `conflict` row †.
- **sources:** each source's own value, by the IDs below, such as `WOO=+3`. `(ref)` marks a value seen for reference and never counted. Blackjack Apprenticeship writes `4+` for "at +4 and above", `-1-` for "at −1 and below", and `0+` or `0-` for "at any positive or negative running count".
- **Late surrender:** basic strategy surrenders 16 vs 10, so its stand index is the choice between hitting and standing. 15 vs 10 surrenders at 0 or higher before its stand index at +4 comes into play.

## Sources

- **BA:** Don Schlesinger, *Blackjack Attack*, 3rd edition: Table 5.1, the Illustrious 18 and Fab 4 (page 62), and Table 10.1, the simulation indices for S17 and H17 (page 213), as the [publisher's sample](https://www.lasvegasadvisor.com/shop/wp-content/uploads/2018/10/BJAttackSample.pdf) lists them. The original, not readable online, so it is read through the sources below. Floored, as a Schlesinger forum reply reports.
- **WOO:** [Wizard of Odds, Hi-Lo](https://wizardofodds.com/games/blackjack/card-counting/high-low/). A reprint of BA's Table 5.1, "republished here with permission", for a six-deck game; its simulation is six decks, S17, so it covers S17 only. States no rounding.
- **BJI:** [blackjackinfo.com, Illustrious 18](https://www.blackjackinfo.com/card-counting/illustrious-18/). A reprint: "Index values verified against Don Schlesinger's Blackjack Attack". Six decks, S17.
- **BJM:** [bjmath.com, Illustrious 18 (archived)](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/i18index.htm), by Richard Reid, 1997. Schlesinger's numbers from his September 1986 *Blackjack Forum* article, four decks, S17.
- **CND:** Casino News Daily, [Illustrious 18](https://www.casinonewsdaily.com/blackjack-guide/illustrious-18/) and [Fab 4](https://www.casinonewsdaily.com/blackjack-guide/fab-four-basic-strategy-variation/). Schlesinger's numbers split by S17 and H17, apparently from BA's Table 10.1. Its negative indices read "hit at N or lower", one off from BA's own meaning, so each is read as index N; its S17 shoe numbers then match Wizard of Odds' throughout. For H17 11 vs A it prints 0, where Blackjack Insider's copy of Table 10.1 prints −1.
- **BJINS:** [Blackjack Insider, Paul's Pointers: Illustrious 18 (Part 2)](http://www.bjinsider.com/newsletter_200_pointers.shtml). Quotes BA's Table 10.1 for six- and eight-deck shoes. Its 9 vs 7 at +4 and its pair of tens vs 6 at +5, which it calls a double, look like slips into the next column.
- **BJT:** [blackjacktrainer.fyi, deviations](https://www.blackjacktrainer.fyi/charts/deviations). Six decks, H17, with an S17 column. It calls them "the standard Hi-Lo values, from the same engine that grades you in the trainer" and names no table, but its numbers match the Schlesinger reprints' wherever those agree with each other, so it counts with that lineage.
- **CAC:** [Cacarulo's R18 (archived)](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/r18.htm), 2000. Its own simulation: "6D,S17,DOA,DAS,SPA1,SPL3,NS,Hi-Lo,floored indices". S17 only, and no surrender.
- **BJAPP:** [Blackjack Apprenticeship strategy charts](http://www.blackjackapprenticeship.com/resources/blackjack-strategy-charts/), S17 and H17, 2018. Its own derivation; deck count and rounding unstated. Counted for positive indices, where floored and truncated counts agree, and for its running-count entries `0+` and `0-`; its negative true count entries are not counted, since they depend on the rounding it doesn't state. It lists no index for 12 vs 5, 12 vs 6 or 13 vs 3, and leaves 14 vs 10 surrender blank.
- **BJS:** [blackjackstrategy.io, deviations](https://blackjackstrategy.io/strategy/deviations). An engine's break-even counts, not a rounding convention. Reference only, never counted.
- **CG:** [Casino Guardian, Illustrious 18](https://www.casinoguardian.co.uk/blackjack/blackjack-illustrious-18/). Prints 14 vs 10, 15 vs 9 and the S17 15 vs A one above every other source, with no reason given. Not counted.

## Which sources are independent

Wizard of Odds, blackjackinfo, bjmath, Casino News Daily, Blackjack Insider and blackjacktrainer.fyi print Schlesinger's numbers, from *Blackjack Attack* or his 1986 article, so they count as one lineage and one source. Cacarulo and Blackjack Apprenticeship derived their numbers independently.

With a 7, 8, 9 or 10 up, the dealer can never hold soft 17, so S17 evidence counts for H17 at those upcards. Whether the dealer hits soft 17 can't affect insurance either.

## Debated indices

The app marks these †, and the fixtures mark them `conflict`.

### Dealer stands on soft 17

- **10 vs A, double at +4.** Schlesinger's list and Blackjack Apprenticeship print +4; Cacarulo prints +3. Ships Wizard of Odds' +4, since only one independent source disagrees.
- **Hard 16 vs 9, stand at +5.** Schlesinger's list prints +5; Cacarulo and Blackjack Apprenticeship both print +4. The user ruled +5, as Wizard of Odds and Schlesinger print it, against the two independent analyses, so it stays debated.
- **15 vs A, surrender at +1.** Schlesinger's list prints +1; Blackjack Apprenticeship prints +2. Ships Wizard of Odds' +1, since only one independent source disagrees.

### Dealer hits soft 17

- **Pair of tens vs 6, split at +4.** Blackjack Apprenticeship and blackjacktrainer.fyi print +4; Blackjack Insider prints +5, a doubtful figure that also calls the play a double and matches Casino News Daily's single-deck column. Casino News Daily gives only its S17 value, +4. Ships +4.
- **11 vs A, double at −1.** Basic strategy already doubles here, so the index is where to stop doubling. Blackjack Insider's copy of Table 10.1 and blackjacktrainer.fyi print −1, as does blackjackstrategy.io for reference; Casino News Daily prints 0, and Blackjack Apprenticeship always doubles. Ships −1.
- **Hard 16 vs 9, stand at +5.** With a 9 up the dealer can't hold soft 17, so this matches S17. Casino News Daily, Blackjack Insider and blackjacktrainer.fyi print +5; Cacarulo (S17) and Blackjack Apprenticeship print +4. The user ruled +5, as for S17.

## Indices from Schlesinger's list alone

These rest on *Blackjack Attack* and its reprints only, with no independent source either way, and ship as printed, not debated.

- **Dealer stands on soft 17:** 12 vs 5 stand at −2, 12 vs 6 stand at −1, 13 vs 3 stand at −2, 14 vs 10 surrender at +3.
- **Dealer hits soft 17:** 13 vs 2 stand at −1, 12 vs 5 stand at −2, 12 vs 6 stand at −3, 13 vs 3 stand at −2, 14 vs 10 surrender at +3, 15 vs A surrender at −1. Cacarulo's S17 13 vs 2 can't carry over, since with a 2 up the dealer can hold soft 17.

## Known gaps

- **No table for a truncated or rounded true count.** A truncated count rounds toward zero, so −1.5 counts as −1. Stanford Wong's *Professional Blackjack* uses it in its 1994 and later editions, but its tables aren't readable online, and no table for a rounded count was found anywhere. Floored and truncated counts agree on positive indices, but not on zero and negative ones.
- **No source states its deck estimate,** how many decks it counts as dealt.
- **No double-deck set.** Its only source is Casino News Daily, a reproduction with a known error: its 9 vs 2 double at +1 contradicts Wizard of Odds' double-deck basic strategy, which already doubles 9 vs 2. It has no Fab 4 either.
- **The originals weren't read.** *Blackjack Attack*'s online copies are lending-only, and its publisher's sample covers chapter 1. Schlesinger's statement that his indices are floored was seen only as a search snippet of a forum thread. Blackjack Apprenticeship's deck count, rounding and method are unknown.
