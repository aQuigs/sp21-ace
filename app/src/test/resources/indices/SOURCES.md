# Index sources

`IndexSetData.kt` holds two built-in sets of blackjack indices for the Hi-Lo count: Don Schlesinger's Illustrious 18 and Fab 4, the 22 departures from basic strategy worth the most to a card counter. Both are for a six-deck shoe with late surrender: `BJ_SHOE_S17`, where the dealer stands on soft 17, and `BJ_SHOE_H17`, where the dealer hits it. The fixtures in this folder list each index with the sources that print it, and `BuiltInIndicesTest` pins every number, play, direction and debated mark to them.

## How far to trust each number

A primary source here is the 2023 book's photographed table for that rule set, and for S17 also Wizard of Odds' reprint of Schlesinger's classic list.

### Dealer stands on soft 17

- Seen in a primary source: all 22, in Wizard of Odds' reprint. The 2023 book's S17 table shows 14 of them and matches 11; it prints 12 vs 2, pair of tens vs 6 and 15 vs A surrender one higher.
- One lineage: 12 vs 6 and 13 vs 3, printed only by Schlesinger's two lists and Wattenberger.
- Disputed (†): 12 vs 2, pair of tens vs 6, 15 vs A surrender, 10 vs A, 9 vs 7, 16 vs 9, 12 vs 5.

### Dealer hits soft 17

- Seen in a primary source: 13 of the 22, in the 2023 book's H17 table, every one matching. Schlesinger's lists disagree on three of them: 12 vs 3, 12 vs 2 and pair of tens vs 6 (†).
- Carried over from S17, since the dealer can never hold soft 17 with a 7, 9 or 10 up: 10 vs 10, 9 vs 7 and 16 vs 9. The last two are disputed (†), as for S17.
- One lineage: 13 vs 2 and 15 vs A surrender, from Schlesinger's lists alone.
- Disputed with no primary source (†): 11 vs A, 12 vs 5, 12 vs 6, 13 vs 3. 11 vs A, 12 vs 6 and 13 vs 3 ship the owner's ruling over the number blackjacktrainer.fyi's and Tamburin's copies of Schlesinger's lists print, and for 11 vs A the published evidence leans the other way.

## Reading the fixtures

- **An index:** make the named play at that true count or higher, otherwise play basic strategy. The true count is the Hi-Lo running count divided by the decks left to deal. Every built-in index reads "or higher" (`AT_OR_ABOVE`).
- **Rounding:** every number is for a floored true count, one always rounded down, so −1.5 counts as −2 (see Schlesinger's two lists for what supports that). A preset's deck estimate and true count rounding settings change no number, since no other convention has a readable table (see Known gaps).
- **Which values count:** a value counts for a rule set only where its source labels it for that rule set or gives one value for both. `(no rule set)` marks a value whose source names none for that row. With a 7, 8, 9 or 10 up the dealer can never hold soft 17, and soft 17 can't affect insurance, so there S17 values count for H17 (`S17 evidence`). Values marked `(ref)` or `(not counted)` are listed and never counted.
- **status:**
  - `agree`: no counted value disagrees, and at least two independent sources print the number.
  - `conflict`: a counted value disagrees, including one of Schlesinger's lists against the other, Wattenberger's charts, and Wong's truncated numbers read by the published rule. The app marks every `conflict` row †.
  - `single-source`: no counted value disagrees, and only Schlesinger's lists print it, with or without Wattenberger.
- **sources:** each source's own value, by the IDs below, such as `WOO=+3`. Blackjack Apprenticeship writes `4+` for "at +4 and above", `-1-` for "at −1 and below", and `0+` or `0-` for "at any positive or negative running count".
- **Late surrender:** basic strategy surrenders 16 vs 10, so its stand index is the choice between hitting and standing. 15 vs 10 surrenders at 0 or higher before its stand index at +4 comes into play.

## Schlesinger's two lists

Don Schlesinger's *Blackjack Attack*, 3rd edition (2004), prints his numbers twice, as its [publisher's sample](https://www.lasvegasadvisor.com/shop/wp-content/uploads/2018/10/BJAttackSample.pdf) lists: "5.1 The 'Illustrious 18' and the 'Fab 4'", page 62, and "10.1 The Simulation Indices", page 213. The book itself isn't readable online, so both are read through copies.

- **The classic list,** Table 5.1. Wizard of Odds reprints it as "based on a six-deck game", and blackjackinfo, bjmath, Casino News Daily's shoe numbers and blackjacktrainer.fyi's S17 column print the same numbers. The app ships this list.
- **The simulation indices,** Table 10.1. Blackjack Insider's Paul's Pointers copies it by name, and Tamburin's table of Schlesinger's numbers matches that copy on every row both show. They differ from the classic list on five S17 rows: 12 vs 2 (+4, not +3), 12 vs 5 (−1, not −2), 9 vs 7 (+4, not +3), pair of tens vs 6 (+5, not +4) and 15 vs A surrender (+2, not +1). Tamburin prints all five, Blackjack Insider 9 vs 7 and pair of tens vs 6. Wattenberger's floored charts match on all five, and the 2023 book on the three its photo shows.

Floored, in Schlesinger's forum posts as DSchles on blackjackinfo.com: "The page 213 indices in BJA3 are all floored" ([2019](https://www.blackjackinfo.com/community/threads/wong-vs-schlesinger.56195/)) is about Table 10.1, not the list the app ships. For the classic list, what supports floored are his statements about single indices: "If Hi-Lo, multi-deck, standing indices (floored) for 13 v. 2 and 3 are, respectively, -1 and -2" ([2016](https://www.blackjackinfo.com/community/threads/i18-v-collins-deviations.54849/)), and of 16 vs 10 and 12 vs 4, "The Hi-Lo indices are zero for both of those plays, and that means that if your floored true count is exactly zero or higher, you stand" ([2021](https://www.blackjackinfo.com/community/threads/question-about-index-numbers-at-0.56487/)). Those four numbers are the same in both lists.

## Sources

Copies of Schlesinger's lists:

- **WOO:** [Wizard of Odds, Hi-Lo](https://wizardofodds.com/games/blackjack/card-counting/high-low/). Table 5.1, "republished here with permission" ("page 62, 2004 ed."). States no rounding, and no soft 17 rule for the list: its "Dealer stands on soft 17" belongs to the page's own simulation. The S17 label comes from BJI and BJM, which print the same numbers as S17.
- **BJI:** [blackjackinfo.com, Illustrious 18](https://www.blackjackinfo.com/card-counting/illustrious-18/): "Index values verified against Don Schlesinger's Blackjack Attack · Hi-Lo, six decks, dealer stands on soft 17".
- **BJM:** [bjmath.com, Illustrious 18 (archived)](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/i18index.htm), by Richard Reid, 1997: Schlesinger's "4 Deck S17" numbers from his September 1986 *Blackjack Forum* article. Four decks, not six.
- **CND:** Casino News Daily, [Illustrious 18](https://www.casinonewsdaily.com/blackjack-guide/illustrious-18/) and [Fab 4](https://www.casinonewsdaily.com/blackjack-guide/fab-four-basic-strategy-variation/). Names no table; its shoe numbers match the classic list, beside single- and double-deck columns. It labels a shoe number H17 only on 11 vs A (0), 10 vs A, 12 vs 6 and 15 vs A surrender, and S17 only on those and pair of tens vs 6; every other row says just "in shoe games". Its stiff-hand stand rows read "hit at N or lower", and 15 vs 10 surrender "your true count is 0"; read literally against its own [floored true count](https://www.casinonewsdaily.com/blackjack-guide/learning-keep-true-count/), 12 vs 4 would stand only from +1, so this is a loose paraphrase and each row is read as index N.
- **BJT:** [blackjacktrainer.fyi, deviations](https://www.blackjacktrainer.fyi/charts/deviations). Six decks, an H17 column and an S17 column, "the standard Hi-Lo values, from the same engine that grades you in the trainer". It names no table or rounding, but its numbers are the classic list's, so it counts with that lineage.
- **BJINS:** [Blackjack Insider, Paul's Pointers: Illustrious 18 (Part 2)](http://www.bjinsider.com/newsletter_200_pointers.shtml): "can be found in Schlesinger's Blackjack Attack: Playing the Pros' Way, page 213, Table 10.1", for "six- and eight-deck shoe games". Labels a rule set only on 11 vs A ("-1 (H17)/+1 (S17)") and 10 vs A ("+3 (H17)/+4 (S17)"). Calls the pair of tens splits doubles. Shows 11 of the 22; Part 1 is for paid subscribers.
- **TAM:** Henry Tamburin, [Blackjack Insider #194, "The Index Numbers for 22 plays" (archived)](http://web.archive.org/web/20160314205538/http://www.bjinsider.com/newsletter_194_bs.shtml), credited to Schlesinger: "The table contains the indices for multi-deck games with s17. Where two indices are shown, the second is for h17 game. If you want the indices for single- and double-deck games, see Table 10.1". It shows two values on 12 vs 6 (−1/−3), 11 vs A (+1/−1), 10 vs A (+4/+3) and 15 vs A surrender (+2/−1); the fixtures read each single value as holding for both rules, as that note implies (`one value for both`). States no deck count beyond "multi-deck", and no rounding.

Other sources:

- **HLC:** Don Schlesinger and Dave Brolley, *The Hi-Lo Card Counting System: A Complete Guide to Index Play* (Huntington Press, 2023), read only from the owner's photo of two of its "Nifty 50" tables, "5/6 S17 DAS LS Play-All 1–8" and "5/6 H17 DAS LS Play-All 1–8": six decks, double after split, late surrender. The photo shows S17 ranks 1–16 and H17 ranks 1–15. Of the 22, it doesn't show S17 10 vs 10, 10 vs A, 9 vs 7, 16 vs 9, 13 vs 2, 12 vs 5, 12 vs 6 and 13 vs 3, or H17 11 vs A, 13 vs 2, 13 vs 3, 12 vs 5, 12 vs 6, 15 vs A surrender, 9 vs 7, 16 vs 9 and 10 vs 10. Schlesinger's [Casino Player article](https://www.casinocenter.com/master-class-the-hi-lo-card-counting-system/) (June 2023) calls the book's indices "(floored)", and says close calls "err on the side of the more risk-averse play, delaying doubles and splits to the higher index of the two being considered, or surrendering at the lower of the two".
- **DB:** Dave "Gronbog" Brolley, the book's co-author, in the [2021 thread](https://www.blackjackinfo.com/community/threads/question-about-index-numbers-at-0.56487/) above: "You say the game is multi-deck H17. For A,7 vs 2 and 11 Vs A, Wong's index numbers to double are both zero however, … his index numbers are for truncating … The correct index number when flooring is -1 for both plays." Counted with HLC as one source.
- **WONG:** Stanford Wong's H17 numbers from *Professional Blackjack*, as a reader (21Menace) posted them on [blackjackinfo in 2010](https://www.blackjackinfo.com/community/threads/stratagy-variations-for-6-decks-h17-das-when-using-hi-opt1-or-hi-lo-counting.16899/): "I use the H17 indices in 'Professional Blackjack.' … heres what I use for H17, DAS, 6D". Edition unstated; its 13 vs 2 at 0 matches the 1994 edition, which Reid says prints 0 where the 1981 one printed −1. Wong's numbers are for a truncated count, and the fixtures list them as printed. By the published rule (see Known gaps) a positive one is the same floored and a negative one is one lower floored. For a zero Brolley says "it can get tricky", so a zero isn't counted.
- **CAC:** [Cacarulo's R18 (archived)](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/r18.htm), 2000. His own simulation: "6D,S17,DOA,DAS,SPA1,SPL3,NS,Hi-Lo,floored indices". S17 only, and no surrender (`NS`), where the app's sets have late surrender.
- **BJAPP:** [Blackjack Apprenticeship strategy charts](http://www.blackjackapprenticeship.com/resources/blackjack-strategy-charts/), S17 and H17, 2018. None of its pages read here says where its index numbers come from, so its independence is unverified. Deck count unstated. Its `-1-` means the play changes "at the true count and below", in the one rounding its [how-to page](http://web.archive.org/web/20241230095652/https://www.blackjackapprenticeship.com/how-to-count-cards/) shows: "If our Running Count is -7 and we have 2 decks remaining, our True Count is -3", which truncates −3.5 rather than flooring it. One example states no rule, so only its positive indices, where floored and truncated counts agree, and its running-count entries `0+` and `0-` are counted. It lists no index for 12 vs 5, 12 vs 6 or 13 vs 3, and leaves 14 vs 10 surrender blank.
- **KS:** Ken Smith, founder of blackjackinfo.com, replying under [Blackjack School lesson 15](https://www.blackjackinfo.com/blackjack-school/lesson-15-advanced-course-part-3/) about six decks: "In the S17 game, the index is -1. (Stand at -1 or higher.) In the H17 game, … the 12v6 index falls all the way to -4. (Stand at -4 or higher.)" His own indices; rounding unstated, so his S17 −1 isn't counted.
- **CVD:** Norm Wattenberger's [CVData Index Chart Viewer](http://www.card-counting.com/cvcxonlineviewer3.htm): "Currently only HiLo six deck indexes are provided assuming S17", late surrender, "generated with flooring and half-deck remaining cards estimation". Each chart is titled with its index: 12 vs 2 "Hit < 4", 12 vs 3 "Hit < 2", 12 vs 4 "Hit < 0", 12 vs 5 "Hit < -1", 12 vs 6 "Hit < -1", 13 vs 2 "Hit < -1", 13 vs 3 "Hit < -2", 15 vs 10 "Hit < 4", 16 vs 9 "Hit < 4", 16 vs 10 "Hit < 0", 9 vs 2 "DD >= 1", 9 vs 7 "DD >= 4", 10 vs 10 "DD >= 4", 10 vs A "DD >= 4", 11 vs A "DD >= 1", pair of tens vs 5 "Split >= 5" and vs 6 "Split >= 5", "Insure >= 3", and surrender 14 vs 10 ">= 3", 15 vs 9 ">= 2", 15 vs 10 ">= 0", 15 vs A ">= 2". "Hit < -1" means stand at −1 or higher. Schlesinger reportedly said Table 10.1 was "generated by Norm", seen only as a search snippet, so these charts never count toward `agree`.
- **BJS:** [blackjackstrategy.io, deviations](https://blackjackstrategy.io/strategy/deviations). Prints the classic list beside its engine's break-even counts to one decimal, such as +5.7 for 16 vs 9; the `(ref)` values are the engine's counts as whole numbers. Reference only.
- **CG:** [Casino Guardian, Illustrious 18](https://www.casinoguardian.co.uk/blackjack/blackjack-illustrious-18/). Prints 14 vs 10 at +4 and 15 vs 9 at +3, one above every other source, with no reason given. Reference only.

## Which sources are independent

Schlesinger's two lists and every copy of them count as one source, and the 2023 book with Brolley's statement as another: a new simulation by Brolley, whose numbers differ from the classic list on three of the S17 rows its photo shows. Cacarulo, Ken Smith and Wong are independent. Blackjack Apprenticeship counts as independent though that is unverified; S17 10 vs 10 is the one `agree` row it decides, as the only second source. Wattenberger's charts never count toward `agree`; where they disagree, the row is still a conflict.

## Debated indices

The app marks these †, and the fixtures mark them `conflict`. Where Wizard of Odds prints a number, that number ships, and every source against it is listed here for the owner to rule on.

### Dealer stands on soft 17

- **Pair of tens vs 6, split at +4.** The classic list, Cacarulo and Blackjack Apprenticeship print +4; the later list, Wattenberger and the 2023 book +5, the book's fitting its stated rule of delaying close splits to the higher index.
- **12 vs 2, stand at +3.** The classic list, Cacarulo and Blackjack Apprenticeship print +3; the later list, Wattenberger and the 2023 book +4.
- **15 vs A, surrender at +1.** The classic list prints +1; the later list, Wattenberger, the 2023 book and Blackjack Apprenticeship +2. The book's risk-averse rule surrenders at the lower index, so its +2 is not that rule's doing.
- **10 vs A, double at +4.** Both lists, Wattenberger and Blackjack Apprenticeship print +4; Cacarulo +3.
- **9 vs 7, double at +3.** The classic list, Cacarulo and Blackjack Apprenticeship print +3; the later list and Wattenberger +4.
- **Hard 16 vs 9, stand at +5.** Both lists print +5; Cacarulo, Blackjack Apprenticeship and Wattenberger +4. The user ruled +5, as Wizard of Odds and Schlesinger print it.
- **12 vs 5, stand at −2.** The classic list prints −2; the later list and Wattenberger −1.

### Dealer hits soft 17

- **12 vs 3, stand at +1.** The 2023 book's H17 table and Wong print +1 (the book's S17 table +2). blackjacktrainer.fyi's H17 column, Tamburin's single value and Blackjack Apprenticeship's H17 chart print +2. Ships +1, as the book is the only H17 source that states its rounding and method, by the list's own author. It shipped +2 until revision 2 (`INDEX_CORRECTIONS`).
- **12 vs 2, stand at +3.** The 2023 book, blackjacktrainer.fyi, Blackjack Apprenticeship and Wong print +3; Tamburin's single value +4.
- **Pair of tens vs 6, split at +4.** The 2023 book, blackjacktrainer.fyi, Blackjack Apprenticeship and Wong print +4; Tamburin's single value +5.
- **11 vs A, double at 0.** Basic strategy already doubles here, so the index is where to stop doubling. Only Casino News Daily, labeling it H17, prints 0. Blackjack Insider's and Tamburin's copies of the later list, blackjacktrainer.fyi and Brolley (floored) print −1, and Brolley says Wong's truncated 0 is −1 floored for this play, so the published evidence leans to −1. Blackjack Apprenticeship always doubles. Ships 0 because the owner ruled it, as their own spreadsheet has it, to be revisited when the 2023 book's H17 rows are seen; it shipped −1 until revision 3.
- **9 vs 7, double at +3.** With a 7 up the dealer can't hold soft 17, so it is S17's dispute: blackjacktrainer.fyi, Blackjack Apprenticeship and Wong print +3, Tamburin's single value and Wattenberger +4.
- **Hard 16 vs 9, stand at +5.** With a 9 up it matches S17. blackjacktrainer.fyi, Tamburin and Wong print +5; Cacarulo and Wattenberger (S17) and Blackjack Apprenticeship +4. The user ruled +5, as for S17.
- **12 vs 5, stand at −2.** blackjacktrainer.fyi prints −2; Tamburin's single value −1. Wong's truncated −1 is −2 floored.
- **12 vs 6, stand at −4.** Wong's truncated −3 is −4 floored. Ken Smith prints −4 as his own index with no rounding stated: −4 if he floors and −5 if he truncates, below −3 either way. Casino News Daily, blackjacktrainer.fyi and Tamburin print −3. Ships −4 because the owner ruled it, as their own spreadsheet has it, to be revisited when the 2023 book's H17 rows are seen; it shipped −3 until revision 3.
- **13 vs 3, stand at −3.** Only Wong supports it: his truncated −2 is −3 floored by the published rule. blackjacktrainer.fyi and Tamburin's single value print −2. Ships −3 because the owner ruled it, as their own spreadsheet has it, to be revisited when the 2023 book's H17 rows are seen; it shipped −2 until revision 3.

## Indices from Schlesinger's lists alone

These rest on *Blackjack Attack* and its copies only, with no independent source either way, and ship as printed, not debated.

- **Dealer stands on soft 17:** 12 vs 6 stand at −1 and 13 vs 3 stand at −2. Both lists and Wattenberger agree; Ken Smith's 12 vs 6 at −1 states no rounding, which a negative index depends on.
- **Dealer hits soft 17:** 13 vs 2 stand at −1, where Wong's truncated 0 is a zero the rule doesn't convert, and 15 vs A surrender at −1. S17 numbers can't carry over, since with a 2 or an ace up the dealer can hold soft 17.

## Hands of 3 or more cards

With hands of 3 or more cards on, a flashcard shows only a hand a basic strategy player could hold: every hand on the way to it is one `plainlyHits` allows, a hit whatever the rules. `PlainHitsTest` pins every square of that allow-list. Each square is a hit, or a double or surrender that falls back to a hit once past two cards, in both of [Wizard of Odds' 4-8 deck charts](https://wizardofodds.com/games/blackjack/strategy/4-decks/) ("4-8 Decks, Dealer Stands on Soft 17" and "4-8 Decks, Dealer Hits on Soft 17"). The page's text version for S17 reads:

- "Always hit hard 11 or less."
- "Stand on hard 12 against a dealer 4-6, otherwise hit."
- "Stand on hard 13-16 against a dealer 2-6, otherwise hit."
- "Always hit soft 17 or less."
- "Stand on soft 18 except hit against a dealer 9, 10, or A."

Its H17 changes ("Surrender 15, a pair of 8s, and 17 vs. dealer A", "Double 11 vs. dealer A", "Double soft 18 vs. dealer 2", "Double soft 19 vs. dealer 6") touch no square the list allows. Anything else the charts double, split or surrender against some upcard is left out of the first two cards: a hard 9 to 11, any soft hand, any pair, and a hard 15 or 16. So a hard 5 or 6 and a soft 13 to 15 always show two cards.

## Known gaps

- **No table for a truncated or rounded true count.** A truncated count rounds toward zero, so −1.5 counts as −1. Wong's *Professional Blackjack* uses it in its 1994 and later editions, but its tables aren't readable online (WONG is a reader's copy of the H17 one), and no table for a rounded count was found anywhere. Sources do state the rule: a truncated negative index is one higher than the floored one, and positive ones are the same ([Pete Moss](http://web.archive.org/web/2005id_/http://bjmath.com:80/bjmath/tcindex/pmrt.htm): "all negative indices in the scenario A table must be increased by 1"; Schlesinger on Wong: "all of Wong's negative indices got shifted one point higher"). The app doesn't apply it yet, so a preset set to truncate shows floored numbers.
- **No source of Schlesinger's numbers states its deck estimate,** how many decks it counts as dealt, and no published statement was found that the deck estimate changes index numbers. [QFIT](https://www.qfit.com/CalculatingTrueCounts.htm) says only: "For best results, you should estimate the remaining decks to the same degree of accuracy that was used during generation of the indexes that you are using."
- **No double-deck set.** Its only source is Casino News Daily, a reproduction with a known error: its 9 vs 2 double at +1 contradicts Wizard of Odds' double-deck basic strategy, which already doubles 9 vs 2. It has no Fab 4 either.
- **The originals weren't read.** *Blackjack Attack*'s online copies are lending-only, and its publisher's sample covers chapter 1, so neither of its tables has been seen, nor whether Table 10.1 gives one value for both rules where Tamburin shows one. The H17 rows the 2023 book's photo doesn't show (see HLC) still rest on copies until its pages are seen. The owner's spreadsheet, not a published source, puts H17 11 vs A at 0, 13 vs 3 at −3 and 12 vs 6 at −4, and those ship by the owner's ruling until then. Nor is it known whether a 5/6 table prints numbers simulated at 5/6 or at the 4.5/6 the article says "the index charts represent".
