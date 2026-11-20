# Index sources

`IndexSetData.kt` holds two built-in sets of blackjack indices for the Hi-Lo count: Don Schlesinger's Illustrious 18 and Fab 4, the 22 departures from basic strategy worth the most to a card counter. Both are for a six-deck shoe with late surrender: `BJ_SHOE_S17`, where the dealer stands on soft 17, and `BJ_SHOE_H17`, where the dealer hits it. The fixtures in this folder list each index with the sources that print it, and `BuiltInIndicesTest` pins every number, play, direction and debated mark to them.

## Reading the fixtures

- **An index:** make the named play at that true count or higher, otherwise play basic strategy. The true count is the Hi-Lo running count divided by the decks left to deal. Every built-in index reads "or higher" (`AT_OR_ABOVE`).
- **Rounding:** every number is for a floored true count, one always rounded down, so −1.5 counts as −2. A preset's deck estimate and true count rounding settings change no number, since no other convention has a readable table (see Known gaps).
- **status:** `agree` when at least two independent sources print the number, `conflict` when independent sources disagree, `single-source` when only Schlesinger's *Blackjack Attack* lineage prints it. The app marks every `conflict` row †.
- **sources:** each source's own value, by the IDs below, such as `WOO=+3`. `(ref)` marks a value seen for reference and never counted. Blackjack Apprenticeship writes `4+` for "at +4 and above", `-1-` for "at −1 and below", and `0+` or `0-` for "at any positive or negative running count".
- **Late surrender:** basic strategy surrenders 16 vs 10, so its stand index is the choice between hitting and standing. 15 vs 10 surrenders at 0 or higher before its stand index at +4 comes into play.

## Sources

- **BA:** Don Schlesinger, *Blackjack Attack*, 3rd edition: Table 5.1, the Illustrious 18 and Fab 4 (page 62), and Table 10.1, the simulation indices for S17 and H17 (page 213), as the [publisher's sample](https://www.lasvegasadvisor.com/shop/wp-content/uploads/2018/10/BJAttackSample.pdf) lists them. The original, not readable online, so it is read through the sources below. Floored, in Schlesinger's own forum posts as DSchles on blackjackinfo.com: "The page 213 indices in BJA3 are all floored" ([2019](https://www.blackjackinfo.com/community/threads/wong-vs-schlesinger.56195/)); "If Hi-Lo, multi-deck, standing indices (floored) for 13 v. 2 and 3 are, respectively, -1 and -2. So, 'hitting in any negative count' isn't correct." ([2016](https://www.blackjackinfo.com/community/threads/i18-v-collins-deviations.54849/)); and of 16 vs 10 and 12 vs 4, "The Hi-Lo indices are zero for both of those plays, and that means that if your floored true count is exactly zero or higher, you stand." ([2021](https://www.blackjackinfo.com/community/threads/question-about-index-numbers-at-0.56487/))
- **WOO:** [Wizard of Odds, Hi-Lo](https://wizardofodds.com/games/blackjack/card-counting/high-low/). A reprint of BA's Table 5.1, "republished here with permission", for a six-deck game; its simulation is six decks, S17, so it covers S17 only. States no rounding.
- **BJI:** [blackjackinfo.com, Illustrious 18](https://www.blackjackinfo.com/card-counting/illustrious-18/). A reprint: "Index values verified against Don Schlesinger's Blackjack Attack". Six decks, S17.
- **BJM:** [bjmath.com, Illustrious 18 (archived)](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/i18index.htm), by Richard Reid, 1997. Schlesinger's numbers from his September 1986 *Blackjack Forum* article, four decks, S17.
- **CND:** Casino News Daily, [Illustrious 18](https://www.casinonewsdaily.com/blackjack-guide/illustrious-18/) and [Fab 4](https://www.casinonewsdaily.com/blackjack-guide/fab-four-basic-strategy-variation/). Schlesinger's numbers, apparently from BA's Table 10.1, with separate S17 and H17 shoe numbers for 11 vs A, 10 vs A, 12 vs 6 and 15 vs A surrender only. Its stiff-hand stand rows (13 vs 2, 12 vs 4, 12 vs 5, 12 vs 6, 13 vs 3) read "hit at N or lower". Every other row reads "or higher", 16 vs 10 and H17 11 vs A and 15 vs A included, except 15 vs 10 surrender: "your true count is 0". Its [true count page](https://www.casinonewsdaily.com/blackjack-guide/learning-keep-true-count/) floors, and read literally that wording would stand 12 vs 4 only from +1, against Schlesinger's "floored true count is exactly zero or higher, you stand". So it is a loose paraphrase, and each of those rows is read as index N; its S17 shoe numbers then match Wizard of Odds' throughout. For H17 11 vs A it prints 0, where Blackjack Insider's copy of Table 10.1 prints −1.
- **BJINS:** [Blackjack Insider, Paul's Pointers: Illustrious 18 (Part 2)](http://www.bjinsider.com/newsletter_200_pointers.shtml). Quotes BA's Table 10.1 for six- and eight-deck shoes. Its 9 vs 7 at +4 and its pair of tens vs 6 at +5, which it calls a double, look like slips into the next column.
- **BJT:** [blackjacktrainer.fyi, deviations](https://www.blackjacktrainer.fyi/charts/deviations). Six decks, H17, with an S17 column. It calls them "the standard Hi-Lo values, from the same engine that grades you in the trainer" and names no table, but its numbers match the Schlesinger reprints' wherever those agree with each other, so it counts with that lineage.
- **CAC:** [Cacarulo's R18 (archived)](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/r18.htm), 2000. Its own simulation: "6D,S17,DOA,DAS,SPA1,SPL3,NS,Hi-Lo,floored indices". S17 only, and no surrender.
- **BJAPP:** [Blackjack Apprenticeship strategy charts](http://www.blackjackapprenticeship.com/resources/blackjack-strategy-charts/), S17 and H17, 2018. Its own derivation; deck count unstated. Its `-1-` means the play changes "at the true count and below", in the one rounding its [how-to page](http://web.archive.org/web/20241230095652/https://www.blackjackapprenticeship.com/how-to-count-cards/) shows: "If our Running Count is -7 and we have 2 decks remaining, our True Count is -3", which truncates −3.5 rather than flooring it. One example states no rule, so only its positive indices, where floored and truncated counts agree, and its running-count entries `0+` and `0-` are counted. It lists no index for 12 vs 5, 12 vs 6 or 13 vs 3, and leaves 14 vs 10 surrender blank.
- **BJS:** [blackjackstrategy.io, deviations](https://blackjackstrategy.io/strategy/deviations). An engine's break-even counts, not a rounding convention. Reference only, never counted.
- **HLC:** Don Schlesinger and Dave Brolley, *The Hi-Lo Card Counting System: A Complete Guide to Index Play* (Huntington Press, 2023), read only from the owner's photo of two of its "Nifty 50" tables, "5/6 S17 DAS LS Play-All 1–8" and "5/6 H17 DAS LS Play-All 1–8": six decks, double after split, late surrender. The photo shows S17 ranks 1–16 and H17 ranks 1–15. Of the 22, it doesn't show S17 10 vs 10, 10 vs A, 9 vs 7, 16 vs 9, 13 vs 2, 12 vs 5, 12 vs 6 and 13 vs 3, or H17 11 vs A, 13 vs 2, 13 vs 3, 12 vs 5, 12 vs 6, 15 vs A surrender, 9 vs 7, 16 vs 9 and 10 vs 10. Schlesinger's [Casino Player article](https://www.casinocenter.com/master-class-the-hi-lo-card-counting-system/) (June 2023) calls the book's indices "(floored)", and says close calls "err on the side of the more risk-averse play, delaying doubles and splits to the higher index of the two being considered, or surrendering at the lower of the two".
- **KS:** Ken Smith, founder of blackjackinfo.com, replying under [Blackjack School lesson 15](https://www.blackjackinfo.com/blackjack-school/lesson-15-advanced-course-part-3/) about six decks: "In the S17 game, the index is -1. (Stand at -1 or higher.) In the H17 game, … the 12v6 index falls all the way to -4. (Stand at -4 or higher.)" His own indices; rounding unstated.
- **CVD:** Norm Wattenberger's [CVData Index Chart Viewer](http://www.card-counting.com/cvcxonlineviewer3.htm): Hi-Lo, six decks, S17, late surrender, "generated with flooring and half-deck remaining cards estimation". Each hit/stand chart is titled with its index, such as "12 vs. 5: Hit < -1", meaning stand at −1 or higher. S17 only. Its independence from BA is uncertain: Schlesinger reportedly said BA's Table 10.1 indices were "generated by Norm", seen only as a search snippet.
- **CG:** [Casino Guardian, Illustrious 18](https://www.casinoguardian.co.uk/blackjack/blackjack-illustrious-18/). Prints 14 vs 10, 15 vs 9 and the S17 15 vs A one above every other source, with no reason given. Not counted.

## Which sources are independent

Wizard of Odds, blackjackinfo, bjmath, Casino News Daily, Blackjack Insider and blackjacktrainer.fyi print Schlesinger's numbers, from *Blackjack Attack* or his 1986 article, so they count as one lineage and one source. Cacarulo, Blackjack Apprenticeship and Ken Smith derived their numbers independently. The 2023 book is a new simulation run by Brolley, and its numbers differ from the ones the app shipped on four of the rows the photo shows, so it counts apart from that lineage. Wattenberger's charts may share a generator with BA (see CVD), so they never count toward `agree`; where they disagree, the row is still a conflict.

With a 7, 8, 9 or 10 up, the dealer can never hold soft 17, so S17 evidence counts for H17 at those upcards. Whether the dealer hits soft 17 can't affect insurance either.

## Debated indices

The app marks these †, and the fixtures mark them `conflict`. Where Wizard of Odds prints a number, that number ships, and every source against it is listed here for the owner to rule on.

### Dealer stands on soft 17

- **10 vs A, double at +4.** Schlesinger's list and Blackjack Apprenticeship print +4; Cacarulo prints +3. Ships +4.
- **Hard 16 vs 9, stand at +5.** Schlesinger's list prints +5; Cacarulo and Blackjack Apprenticeship both print +4. The user ruled +5, as Wizard of Odds and Schlesinger print it, against the two independent analyses, so it stays debated.
- **15 vs A, surrender at +1.** Schlesinger's list prints +1; the 2023 book and Blackjack Apprenticeship print +2. The book's risk-averse rule surrenders at the lower index, so its +2 is not that rule's doing. Ships Wizard of Odds' +1.
- **Pair of tens vs 6, split at +4.** Wizard of Odds, Cacarulo and Blackjack Apprenticeship print +4; the 2023 book prints +5, which fits its stated rule of delaying close splits to the higher index, and so does Blackjack Insider, a doubtful copy (see BJINS). Ships +4.
- **12 vs 2, stand at +3.** Wizard of Odds, Cacarulo and Blackjack Apprenticeship print +3; the 2023 book prints +4. Ships +3.
- **12 vs 5, stand at −2.** Schlesinger's list prints −2; Wattenberger's chart is titled "12 vs. 5: Hit < -1". Ships −2.

### Dealer hits soft 17

- **12 vs 3, stand at +1.** The 2023 book's H17 table prints +1 (its S17 table +2). +2 has two sources, Schlesinger's lineage (blackjacktrainer.fyi's H17 column, and Casino News Daily with no rule set named) and Blackjack Apprenticeship's H17 chart; +1 has one. Ships +1 all the same: Wizard of Odds prints no H17 numbers, and the book is the only H17 source that states its rounding and method, by the list's own author, while blackjacktrainer.fyi names no table or rounding and Blackjack Apprenticeship states neither its deck count nor its rounding. It shipped +2 until revision 2 (`INDEX_CORRECTIONS`).
- **12 vs 6, stand at −3.** Casino News Daily and blackjacktrainer.fyi print −3; Ken Smith prints −4 as his own index. A truncated −4 would floor lower still, so it disagrees whatever his rounding. Ships −3.
- **Pair of tens vs 6, split at +4.** The 2023 book, Blackjack Apprenticeship and blackjacktrainer.fyi print +4; Blackjack Insider prints +5, a doubtful figure that also calls the play a double and matches Casino News Daily's single-deck column. Casino News Daily gives only its S17 value, +4. Ships +4.
- **11 vs A, double at −1.** Basic strategy already doubles here, so the index is where to stop doubling. Blackjack Insider's copy of Table 10.1 and blackjacktrainer.fyi print −1, as does blackjackstrategy.io for reference; Casino News Daily prints 0, and Blackjack Apprenticeship always doubles. Ships −1.
- **Hard 16 vs 9, stand at +5.** With a 9 up the dealer can't hold soft 17, so this matches S17. Casino News Daily, Blackjack Insider and blackjacktrainer.fyi print +5; Cacarulo (S17) and Blackjack Apprenticeship print +4. The user ruled +5, as for S17.

## Indices from Schlesinger's list alone

These rest on *Blackjack Attack* and its reprints only, with no independent source either way, and ship as printed, not debated. Wattenberger's charts agree with the two S17 ones.

- **Dealer stands on soft 17:** 12 vs 6 stand at −1, 13 vs 3 stand at −2.
- **Dealer hits soft 17:** 13 vs 2 stand at −1, 12 vs 5 stand at −2, 13 vs 3 stand at −2, 15 vs A surrender at −1. S17 numbers can't carry over, since with a 2 to 6 or an ace up the dealer can hold soft 17.

## Hands of 3 or more cards

With hands of 3 or more cards on, a flashcard shows only a hand a basic strategy player could hold: every hand on the way to it is one `plainlyHits` allows, a hit whatever the rules. `PlainHitsTest` pins every square of that allow-list. Each square is a hit, or a double or surrender that falls back to a hit once past two cards, in both of [Wizard of Odds' 4-8 deck charts](https://wizardofodds.com/games/blackjack/strategy/4-decks/) ("4-8 Decks, Dealer Stands on Soft 17" and "4-8 Decks, Dealer Hits on Soft 17"). The page's text version for S17 reads:

- "Always hit hard 11 or less."
- "Stand on hard 12 against a dealer 4-6, otherwise hit."
- "Stand on hard 13-16 against a dealer 2-6, otherwise hit."
- "Always hit soft 17 or less."
- "Stand on soft 18 except hit against a dealer 9, 10, or A."

Its H17 changes ("Surrender 15, a pair of 8s, and 17 vs. dealer A", "Double 11 vs. dealer A", "Double soft 18 vs. dealer 2", "Double soft 19 vs. dealer 6") touch no square the list allows. Anything else the charts double, split or surrender against some upcard is left out of the first two cards: a hard 9 to 11, any soft hand, any pair, and a hard 15 or 16. So a hard 5 or 6 and a soft 13 to 15 always show two cards.

## Known gaps

- **No table for a truncated or rounded true count.** A truncated count rounds toward zero, so −1.5 counts as −1. Stanford Wong's *Professional Blackjack* uses it in its 1994 and later editions, but its tables aren't readable online, and no table for a rounded count was found anywhere. Sources do state the rule for a truncated count: every negative floored index goes one higher, and positive ones stay ([Pete Moss](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/pmrt.htm): "all negative indices in the scenario A table must be increased by 1"; Schlesinger on Wong: "all of Wong's negative indices got shifted one point higher"). The app doesn't apply it yet, so a preset set to truncate shows floored numbers.
- **No source of Schlesinger's numbers states its deck estimate,** how many decks it counts as dealt, and no published statement was found that the deck estimate changes index numbers. [QFIT](https://www.qfit.com/CalculatingTrueCounts.htm) says only: "For best results, you should estimate the remaining decks to the same degree of accuracy that was used during generation of the indexes that you are using."
- **No double-deck set.** Its only source is Casino News Daily, a reproduction with a known error: its 9 vs 2 double at +1 contradicts Wizard of Odds' double-deck basic strategy, which already doubles 9 vs 2. It has no Fab 4 either.
- **The originals weren't read.** *Blackjack Attack*'s online copies are lending-only, and its publisher's sample covers chapter 1. The H17 rows the 2023 book's photo doesn't show (see HLC) still rest on copies of *Blackjack Attack* until its pages are seen. The owner's spreadsheet puts H17 11 vs A at 0, 13 vs 3 at −3 and 12 vs 6 at −4, but it is not a published source, so none of those ships. Nor is it known whether a 5/6 table prints numbers simulated at 5/6 or at the 4.5/6 the article says "the index charts represent".
