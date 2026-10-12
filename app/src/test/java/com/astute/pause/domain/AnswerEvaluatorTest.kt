package com.astute.pause.domain

import com.astute.pause.data.local.SeedData
import com.astute.pause.data.local.entity.AnswerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {
    private fun q(id: Long) = SeedData.questions.first { it.id == id }

    @Test fun yes_no_uses_warning_answers() {
        assertTrue(AnswerEvaluator.isWarning(q(5), "yes"))   // already own something
        assertFalse(AnswerEvaluator.isWarning(q(5), "no"))
        assertTrue(AnswerEvaluator.isWarning(q(14), "no"))   // can't afford comfortably
    }

    @Test fun choice_uses_warning_answers() {
        assertTrue(AnswerEvaluator.isWarning(q(6), "Buy second-hand"))
        assertFalse(AnswerEvaluator.isWarning(q(6), "None of these"))
    }

    @Test fun text_is_never_auto_flagged() {
        assertFalse(AnswerEvaluator.isWarning(q(2), "no idea"))
    }

    @Test fun display() {
        assertEquals("Yes", AnswerEvaluator.display(AnswerType.YES_NO, "yes"))
        assertEquals("No", AnswerEvaluator.display(AnswerType.YES_NO, "no"))
        assertEquals("Borrow it", AnswerEvaluator.display(AnswerType.CHOICE, "Borrow it"))
        assertEquals("(no answer)", AnswerEvaluator.display(AnswerType.TEXT, "  "))
    }
}
