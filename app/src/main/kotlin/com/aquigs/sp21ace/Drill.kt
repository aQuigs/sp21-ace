package com.aquigs.sp21ace

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.aquigs.sp21ace.data.HistoryStore
import com.aquigs.sp21ace.domain.drill.AnswerTimer
import com.aquigs.sp21ace.domain.drill.CardDealer
import com.aquigs.sp21ace.domain.drill.DrillCard
import com.aquigs.sp21ace.domain.drill.DrillRun
import com.aquigs.sp21ace.ui.flashcards.DrillActions
import java.time.Instant

/** A flashcard drill as the composition root runs it: the [run] its page shows, and what the page asks for carried out. */
internal class Drill<C : DrillCard<N>, N, A>(
    private val state: MutableState<DrillRun<C, N>>,
    private val timer: AnswerTimer<C>,
    private val dealer: CardDealer<C, N, A>,
    private val store: HistoryStore<A>,
    private val deal: (previous: C?, history: List<A>) -> C?,
    private val onAnswered: (isCorrect: Boolean) -> Unit,
) : DrillActions<C, N> {
    val run: DrillRun<C, N> get() = state.value

    // The store's own value, which a collected one can trail, or null until it loads
    private val history: List<A>? get() = store.history.value

    /** Deals afresh in place of a card the dealer could not deal, rather than grading a stale answer. */
    fun redeal() = show(run.dealtFrom(dealer) { deal(it, history.orEmpty()) })

    // Every card dealt starts its time afresh
    private fun show(next: DrillRun<C, N>) {
        if (next.card != run.card) timer.dealt(next.card)
        state.value = next
    }

    override fun answer(asked: C, answer: N) {
        val (next, recorded) = run.answer(asked, answer, dealer, timer.elapsed(asked), Instant.now(), history, deal) ?: return
        show(next)
        store.append(recorded)
        onAnswered(answer == asked.answer)
    }

    override fun shown(card: C) = timer.shown(card)

    override fun interrupted() = timer.interrupted()

    override fun time(card: C) = timer.display(card)

    override fun clearHistory() {
        store.clear()
        // The streak and the verdict's accuracy live in the run, not in the history
        state.value = run.historyCleared()
    }
}

/** A drill of [dealer]'s cards answered into [store], a change of [dealer] redealing a card it could not deal. [deal] is its deal. */
@Composable
internal fun <C : DrillCard<N>, N, A> rememberDrill(
    dealer: CardDealer<C, N, A>,
    store: HistoryStore<A>,
    clock: () -> Long,
    deal: (previous: C?, history: List<A>) -> C?,
    onAnswered: (isCorrect: Boolean) -> Unit,
): Drill<C, N, A> {
    // Not saved, so a card restored in a recreated activity, which showed before, is never timed from partway through
    val timer = remember { AnswerTimer<C>(clock) }
    val state = rememberSaveable { mutableStateOf(DrillRun(deal(null, store.history.value.orEmpty())).also { timer.dealt(it.card) }) }
    val drill = Drill(state, timer, dealer, store, deal, onAnswered)

    LaunchedEffect(dealer) { drill.redeal() }
    return drill
}
