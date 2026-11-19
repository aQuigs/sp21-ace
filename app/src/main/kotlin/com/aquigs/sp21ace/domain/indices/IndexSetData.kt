package com.aquigs.sp21ace.domain.indices

// Published numbers from the sources cited in app/src/test/resources/indices/SOURCES.md. BuiltInIndicesTest pins every one to
// the fixtures there, so change a number only with a cited source and its fixture row. † marks a debated number.
internal val INDEX_SETS: Map<BuiltInIndexSet, String> = mapOf(
    BuiltInIndexSet.SHOE_H17 to """
        play       hand   upcard  index
    """,
    BuiltInIndexSet.SHOE_S17 to """
        play       hand   upcard  index
    """,
    BuiltInIndexSet.DOUBLE_DECK to """
        play       hand   upcard  index
    """,
)
