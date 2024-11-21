package edu.agh.susgame.back.domain.models

data class QuizQuestion(
    val question: String,
    val answers: List<String>,
    val correctAnswer: Int,
)