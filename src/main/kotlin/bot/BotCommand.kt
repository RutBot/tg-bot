package com.example.bot

interface BotCommand {
    val args: List<Any?>

    fun execute()
}