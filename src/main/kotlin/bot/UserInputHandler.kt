package com.example.bot

import com.github.kotlintelegrambot.dispatcher.Dispatcher

interface UserInputHandler {
    val allowedStates: List<String?>

    fun install(dispatcher: Dispatcher)

}