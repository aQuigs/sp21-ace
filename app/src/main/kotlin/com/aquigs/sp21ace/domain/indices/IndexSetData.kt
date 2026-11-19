package com.aquigs.sp21ace.domain.indices

// Published numbers from the sources cited in app/src/test/resources/indices/SOURCES.md, for a floored true count.
// BuiltInIndicesTest pins every one to the fixtures there, so change a number only with a cited source and its fixture row.
// † marks a number the sources debate.
internal val INDEX_SETS: Map<BuiltInIndexSet, String> = mapOf(
    BuiltInIndexSet.BJ_SHOE_H17 to """
        play       hand   upcard  direction  index
        INSURANCE  -      A       higher     +3
        STAND      16     10      higher     0
        STAND      15     10      higher     +4
        SPLIT      10-10  5       higher     +5
        SPLIT      10-10  6       higher     +4†
        DOUBLE     10     10      higher     +4
        STAND      12     3       higher     +2
        STAND      12     2       higher     +3
        DOUBLE     11     A       higher     -1†
        DOUBLE     9      2       higher     +1
        DOUBLE     10     A       higher     +3
        DOUBLE     9      7       higher     +3
        STAND      16     9       higher     +5†
        STAND      13     2       higher     -1
        STAND      12     4       higher     0
        STAND      12     5       higher     -2
        STAND      12     6       higher     -3
        STAND      13     3       higher     -2
        SURRENDER  14     10      higher     +3
        SURRENDER  15     10      higher     0
        SURRENDER  15     9       higher     +2
        SURRENDER  15     A       higher     -1
    """,
    BuiltInIndexSet.BJ_SHOE_S17 to """
        play       hand   upcard  direction  index
        INSURANCE  -      A       higher     +3
        STAND      16     10      higher     0
        STAND      15     10      higher     +4
        SPLIT      10-10  5       higher     +5
        SPLIT      10-10  6       higher     +4
        DOUBLE     10     10      higher     +4
        STAND      12     3       higher     +2
        STAND      12     2       higher     +3
        DOUBLE     11     A       higher     +1
        DOUBLE     9      2       higher     +1
        DOUBLE     10     A       higher     +4†
        DOUBLE     9      7       higher     +3
        STAND      16     9       higher     +5†
        STAND      13     2       higher     -1
        STAND      12     4       higher     0
        STAND      12     5       higher     -2
        STAND      12     6       higher     -1
        STAND      13     3       higher     -2
        SURRENDER  14     10      higher     +3
        SURRENDER  15     10      higher     0
        SURRENDER  15     9       higher     +2
        SURRENDER  15     A       higher     +1†
    """,
)
