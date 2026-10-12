package com.astute.pause.data.local.entity

enum class ItemStatus { CONSIDERING, WAITING, BOUGHT, SKIPPED }

enum class AnswerPhase { INITIAL, RECHECK }

enum class AnswerType { YES_NO, CHOICE, TEXT }

enum class TierMode { SHORT, FULL }

enum class QuestionGroup(val label: String) {
    NEED_CLARITY("Need & Clarity"),
    ALTERNATIVES_TIMING("Alternatives & Timing"),
    USEFULNESS_QUALITY("Usefulness & Quality"),
    COSTS_TRADEOFF("Costs & Tradeoff"),
}
