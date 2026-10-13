package com.astute.pause.domain

import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.AnswerType
import com.astute.pause.data.local.entity.Question

object AnswerEvaluator {
    /** Text answers are never auto-flagged; the user flags them on the summary. */
    fun isWarning(question: Question, answer: String): Boolean = when (question.answerType) {
        AnswerType.TEXT -> false
        AnswerType.YES_NO, AnswerType.CHOICE -> answer in question.warningAnswers
    }

    /** The free-text follow-up to ask after this answer, or null when the chosen option has none. */
    fun followUpPrompt(question: Question, answer: String): String? =
        if (question.answerType == AnswerType.CHOICE) question.followUps[answer]?.takeIf { it.isNotBlank() } else null

    fun display(type: AnswerType, answer: String): String = when {
        type == AnswerType.YES_NO && answer == SeedData.YES -> "Yes"
        type == AnswerType.YES_NO && answer == SeedData.NO -> "No"
        answer.isBlank() -> "(no answer)"
        else -> answer
    }
}
