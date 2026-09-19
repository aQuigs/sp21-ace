package com.aquigs.sp21ace.domain.strategy

// Settled plays from the published charts cited in app/src/test/resources/strategy/SOURCES.md. ChartCellsTest pins every
// square to the fixtures there, so change a square only with a cited source and its fixture row. † marks a debated play.
internal val CHART_GRIDS: Map<RuleSet, Map<ChartTable, String>> = mapOf(
    RuleSet.H17_REDOUBLE to mapOf(
        ChartTable.HARD to """
            hand  2     3     4     5     6     7     8     9     10    A
            4     H     H     H     H     H     H     H     H     H     H
            5     H     H     H     H     D     H     H     H     H     H
            6     H     H     H     H     D     H     H     H     H     H
            7     H     H     H     H     D     H     H     H     H     H
            8     H     H     H     D     D     H     H     H     H     H
            9     H     D4    D     D     D     H     H     H     H     H
            10    D5    D5    D     D     D     D4†   D3†   H     H     H
            11    D4    D5    D5    D5    D5    D4†   D4†   D4    D3    D3
            12    H     H     H     H     H     H     H     H     H     H
            13    H     H     H     H     S4*   H     H     H     H     H
            14    H     H     S4*   S5'   S6"   H     H     H     H     H
            15    S4*   S5'   S6    S6    S     H     H     H     H     H
            16    S6    S6    S6    S     S     H     H     H     H     RH
            17    S     S     S     S     S     S†    S6    S6    S6    RH
            18    S     S     S     S     S     S     S     S     S     S
            19    S     S     S     S     S     S     S     S     S     S
            20    S     S     S     S     S     S     S     S     S     S
            21    S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.SOFT to """
            hand  2     3     4     5     6     7     8     9     10    A
            A-2   H     D3†   D     D     D     H     H     H     H     H
            A-3   H     D3†   D4†   D     D     H     H     H     H     H
            A-4   H     H     D4    D4    D5    H     H     H     H     H
            A-5   H     H     D3    D4    D5    H     H     H     H     H
            A-6   H     H     D3    D4    D5    H     H     H     H     H
            A-7   S4    S4    D4    D5    D6    S6    S4    H     H     H
            A-8   S     S     S     S     S     S     S     S     S6    S6
            A-9   S     S     S     S     S     S     S     S     S     S
            A-10  S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.PAIRS to """
            hand  2     3     4     5     6     7     8     9     10    A
            2-2   P     P     P     P     P     P     P     H     H     H
            3-3   P     P     P     P     P     P     P     H     H     H
            4-4   H     H     H     D     D     H     H     H     H     H
            5-5   D     D     D     D     D     D     D     H     H     H
            6-6   H     H     P     P     P     H     H     H     H     H
            7-7   P     P     P     P     P     P$    H     H     H     H
            8-8   P     P     P     P     P     P     P     P     P     R
            9-9   S     P     P     P     P     S     P     P     S     S
            10-10 S     S     S     S     S     S     S     S     S     S
            A-A   P     P     P     P     P     P     P     P     P     P
        """,
        ChartTable.AFTER_DOUBLE_HARD to """
            hand  2     3     4     5     6     7     8     9     10    A
            6     S     D†    D     D     D     D     R     R     R     R
            7     D     D     D     D     D     D     R     R     R     R
            8     D     D     D     D     D     D     D     D     R     R
            9     D     D     D     D     D     D     D     D     D     D
            10    D     D     D     D     D     D     D     D     D     D
            11    D     D     D     D     D     D     D     D     D     D
            12    S     S     S     S     S     D     D     R     R     R
            13    S     S     S     S     S     S     R     R     R     R
            14    S     S     S     S     S     S     R     R     R     R
            15    S     S     S     S     S     S     R     R     R     R
            16    S     S     S     S     S     S     R     R     R     R
            17    S     S     S     S     S     S     S     S     S     R
            18    S     S     S     S     S     S     S     S     S     S
            19    S     S     S     S     S     S     S     S     S     S
            20    S     S     S     S     S     S     S     S     S     S
            21    S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.AFTER_DOUBLE_SOFT to """
            hand  2     3     4     5     6     7     8     9     10    A
            A-2   D     D     D     D     D     D     D     D     D     D
            A-3   D     D     D     D     D     D     D     D     D     D
            A-4   D     D     D     D     D     D     D     D     D     D
            A-5   D     D     D     D     D     D     D     D     D     D
            A-6   D     D     D     D     D     D     D     D     D     D
            A-7   S     S     D     D     D     S     S     S     S     S
            A-8   S     S     S     S     S     S     S     S     S     S
            A-9   S     S     S     S     S     S     S     S     S     S
            A-10  S     S     S     S     S     S     S     S     S     S
        """,
    ),
    RuleSet.H17 to mapOf(
        ChartTable.HARD to """
            hand  2     3     4     5     6     7     8     9     10    A
            4     H     H     H     H     H     H     H     H     H     H
            5     H     H     H     H     H     H     H     H     H     H
            6     H     H     H     H     H     H     H     H     H     H
            7     H     H     H     H     H     H     H     H     H     H
            8     H     H     H     H     H     H     H     H     H     H
            9     H     H     H     H     D     H     H     H     H     H
            10    D5    D5    D     D     D     D4    D3    H     H     H
            11    D4    D5    D5    D5    D5    D4    D4    D4    D3    D3
            12    H     H     H     H     H     H     H     H     H     H
            13    H     H     H     H     S4*   H     H     H     H     H
            14    H     H     S4*   S5'   S6"   H     H     H     H     H
            15    S4*   S5'   S6    S6    S     H     H     H     H     H
            16    S6    S6    S6    S     S     H     H     H     H     RH
            17    S     S     S     S     S     S     S6    S6    S6    RH
            18    S     S     S     S     S     S     S     S     S     S
            19    S     S     S     S     S     S     S     S     S     S
            20    S     S     S     S     S     S     S     S     S     S
            21    S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.SOFT to """
            hand  2     3     4     5     6     7     8     9     10    A
            A-A   H     H     H     H     H     H     H     H     H     H
            A-2   H     H     H     H     H     H     H     H     H     H
            A-3   H     H     H     H     H     H     H     H     H     H
            A-4   H     H     H     H     D4    H     H     H     H     H
            A-5   H     H     H     D3    D4    H     H     H     H     H
            A-6   H     H     D3    D4    D5    H     H     H     H     H
            A-7   S4    S4    D4    D5    D6    S6    S4    H     H     H
            A-8   S     S     S     S     S     S     S     S     S6    S6
            A-9   S     S     S     S     S     S     S     S     S     S
            A-10  S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.PAIRS to """
            hand  2     3     4     5     6     7     8     9     10    A
            2-2   P     P     P     P     P     P     P     H     H     H
            3-3   P     P     P     P     P     P     P     H     H     H
            4-4   H     H     H     H     H     H     H     H     H     H
            5-5   D     D     D     D     D     D     D     H     H     H
            6-6   H     H     P     P     P     H     H     H     H     H
            7-7   P     P     P     P     P     P$    H     H     H     H
            8-8   P     P     P     P     P     P     P     P     P     R
            9-9   S     P     P     P     P     S     P     P     S     S
            10-10 S     S     S     S     S     S     S     S     S     S
            A-A   P     P     P     P     P     P     P     P     P     P
        """,
        ChartTable.AFTER_DOUBLE_HARD to """
            hand  2     3     4     5     6     7     8     9     10    A
            12    S     S     S     S     S     S     R     R     R     R
            13    S     S     S     S     S     S     R     R     R     R
            14    S     S     S     S     S     S     R     R     R     R
            15    S     S     S     S     S     S     R     R     R     R
            16    S     S     S     S     S     S     R     R     R     R
            17    S     S     S     S     S     S     S     S     S     R
        """,
    ),
    RuleSet.S17 to mapOf(
        ChartTable.HARD to """
            hand  2     3     4     5     6     7     8     9     10    A
            4     H     H     H     H     H     H     H     H     H     H
            5     H     H     H     H     H     H     H     H     H     H
            6     H     H     H     H     H     H     H     H     H     H
            7     H     H     H     H     H     H     H     H     H     H
            8     H     H     H     H     H     H     H     H     H     H
            9     H     H     H     H     D4    H     H     H     H     H
            10    D5    D5    D     D     D     D4    D3    H     H     H
            11    D4    D5    D5    D5    D5    D4    D4    D4    D3    D3
            12    H     H     H     H     H     H     H     H     H     H
            13    H     H     H     H     H     H     H     H     H     H
            14    H     H     S4*   S5*   S4*   H     H     H     H     H
            15    S4*   S5*   S5"   S6    S6"†  H     H     H     H     H
            16    S5    S6    S6    S     S     H     H     H     H     H
            17    S     S     S     S     S     S     S6    S6    S6    RH
            18    S     S     S     S     S     S     S     S     S     S
            19    S     S     S     S     S     S     S     S     S     S
            20    S     S     S     S     S     S     S     S     S     S
            21    S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.SOFT to """
            hand  2     3     4     5     6     7     8     9     10    A
            A-A   H     H     H     H     H     H     H     H     H     H
            A-2   H     H     H     H     H     H     H     H     H     H
            A-3   H     H     H     H     H     H     H     H     H     H
            A-4   H     H     H     H     H     H     H     H     H     H
            A-5   H     H     H     H     D4    H     H     H     H     H
            A-6   H     H     D3    D4    D5    H     H     H     H     H
            A-7   S4    S4    D4    D5    D5    S6    S4    H     H     H
            A-8   S     S     S     S     S     S     S     S     S6    S
            A-9   S     S     S     S     S     S     S     S     S†    S
            A-10  S     S     S     S     S     S     S     S     S     S
        """,
        ChartTable.PAIRS to """
            hand  2     3     4     5     6     7     8     9     10    A
            2-2   P     P     P     P     P     P     P     H     H     H
            3-3   P     P     P     P     P     P     P     H     H     H
            4-4   H     H     H     H     H     H     H     H     H     H
            5-5   D     D     D     D     D     D     D     H     H     H
            6-6   H     H     P     P     P     H     H     H     H     H
            7-7   P     P     P     P     P     P$    H     H     H     H
            8-8   P     P     P     P     P     P     P     P     P     P
            9-9   S     P     P     P     P     S     P     P     S     S
            10-10 S     S     S     S     S     S     S     S     S     S
            A-A   P     P     P     P     P     P     P     P     P     P
        """,
        ChartTable.AFTER_DOUBLE_HARD to """
            hand  2     3     4     5     6     7     8     9     10    A
            12    S     S     S     S     S     S     R     R     R     R
            13    S     S     S     S     S     S     R     R     R     R
            14    S     S     S     S     S     S     R     R     R     R
            15    S     S     S     S     S     S     R     R     R     R
            16    S     S     S     S     S     S     R     R     R     R
            17    S     S     S     S     S     S     S     S     S     R
        """,
    ),
)
