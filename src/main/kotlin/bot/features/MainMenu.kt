package bot.features

import bot.BotState
import bot.StateManager
import bot.StateRegistry
import bot.Users
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ReplyKeyboardRemove
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter

class MainMenu : BotFeature {
    object MainMenuState : BotState {
        override val name = "MAIN_MENU"
        override val text = "Добро пожаловать в главное меню!"
        override val buttons = listOf(
            listOf(KeyboardButton("Профиль 👤"), KeyboardButton("Настройки ⚙️")),
            listOf(KeyboardButton("Помощь ❓"), KeyboardButton("О проекте ℹ️")),
            listOf(KeyboardButton("Удалить аккаунт ❌"))
        )
        override val data: Map<String, Any?>? = null
    }

    object DemoState : BotState {
        override val name = "DEMO"
        override val text = "Вы находитесь в демо-режиме. Здесь вы можете увидеть доступные функции."
        override val buttons = listOf(
            listOf(KeyboardButton("Профиль 👤"), KeyboardButton("Настройки ⚙️")),
            listOf(KeyboardButton("Помощь ❓"), KeyboardButton("О проекте ℹ️")),
            listOf(KeyboardButton("Назад ⬅️"), KeyboardButton("Удалить аккаунт ❌"))
        )
        override val data: Map<String, Any?>? = null
    }

    init {
        StateRegistry.register(MainMenuState)
        StateRegistry.register(DemoState)
    }

    override fun install(dispatcher: Dispatcher) {
        dispatcher.message(Filter.Text) {
            val user = message.from ?: return@message
            val state = StateManager.getState(user.id)

            if (state == MainMenuState || state == DemoState) {
                when (message.text) {
                    "Профиль 👤" -> {
                        val userData = StateManager.getUserData(user.id)
                        val text = if (userData != null) {
                            "Ваш профиль:\n" +
                                    "Имя: ${userData[Users.name]}\n" +
                                    "Фамилия: ${userData[Users.surname]}\n" +
                                    "Город: ${userData[Users.city]}"
                        } else {
                            "Профиль не найден."
                        }
                        bot.sendMessage(
                            chatId = ChatId.fromId(message.chat.id),
                            text = text
                        )
                    }

                    "Удалить аккаунт ❌" -> {
                        StateManager.deleteAccount(user.id)
                        bot.sendMessage(
                            chatId = ChatId.fromId(message.chat.id),
                            text = "Ваш аккаунт был удален. Все данные стерты.",
                            replyMarkup = ReplyKeyboardRemove()
                        )
                    }
                }
            }
        }
    }
}
