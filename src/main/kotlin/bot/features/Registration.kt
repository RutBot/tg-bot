package bot.features

import bot.BotState
import bot.StateManager
import bot.StateRegistry
import com.github.kotlintelegrambot.dispatcher.Dispatcher
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.keyboard.KeyboardButton
import com.github.kotlintelegrambot.extensions.filters.Filter

class Registration : BotFeature {
    object RegistrationRequiredState : BotState {
        override val name = "REGISTRATION_REQUIRED"
        override val text = "Привет, тебе надо зарегестрироваться при помощи кнопки снизу. Также доступен предпросмотр функций бота."
        override val buttons = listOf(
            listOf(
                KeyboardButton("Просмотр доступных функции"),
                KeyboardButton("Регистрация"))
        )
        override val data: Map<String, Any?>? = null
    }

    object WaitingForNameState : BotState {
        override val name = "WAITING_FOR_NAME"
        override val text = "Пожалуйста, введите ваше имя:"
        override val buttons = emptyList<List<KeyboardButton>>()
        override val data: Map<String, Any?>? = null
    }

    object WaitingForSurnameState : BotState {
        override val name = "WAITING_FOR_SURNAME"
        override val text = "Отлично! Теперь введите вашу фамилию:"
        override val buttons = emptyList<List<KeyboardButton>>()
        override val data: Map<String, Any?>? = null
    }

    object WaitingForCityState : BotState {
        override val name = "WAITING_FOR_CITY"
        override val text = "И последнее, введите ваш город:"
        override val buttons = emptyList<List<KeyboardButton>>()
        override val data: Map<String, Any?>? = null
    }

    init {
        StateRegistry.register(RegistrationRequiredState)
        StateRegistry.register(WaitingForNameState)
        StateRegistry.register(WaitingForSurnameState)
        StateRegistry.register(WaitingForCityState)
    }

    override fun install(dispatcher: Dispatcher) {
        dispatcher.command("start") {
            val user = message.from!!

            val currentState = StateManager.getState(user.id)
            if (currentState != null) {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "не пишите команды текстом, используйте кнопки"
                )
                return@command
            }

            StateManager.setState(message.chat.id, user.id, RegistrationRequiredState)
        }

        dispatcher.callbackQuery("confirm_name") {
            val user = callbackQuery.from
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val name = StateManager.getLastInput(user.id) ?: return@callbackQuery
            
            StateManager.saveUserData(user.id, "name", name)
            StateManager.setState(chatId, user.id, WaitingForSurnameState)
            bot.answerCallbackQuery(callbackQuery.id, text = "Имя сохранено!")
        }

        dispatcher.callbackQuery("confirm_surname") {
            val user = callbackQuery.from
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val surname = StateManager.getLastInput(user.id) ?: return@callbackQuery

            StateManager.saveUserData(user.id, "surname", surname)
            StateManager.setState(chatId, user.id, WaitingForCityState)
            bot.answerCallbackQuery(callbackQuery.id, text = "Фамилия сохранена!")
        }

        dispatcher.callbackQuery("confirm_city") {
            val user = callbackQuery.from
            val chatId = callbackQuery.message?.chat?.id ?: return@callbackQuery
            val city = StateManager.getLastInput(user.id) ?: return@callbackQuery

            StateManager.saveUserData(user.id, "city", city)
            StateManager.finishRegistration(user.id)
            StateManager.setState(chatId, user.id, MainMenu.MainMenuState)
            bot.answerCallbackQuery(callbackQuery.id, text = "Регистрация завершена!")
        }

        dispatcher.message(Filter.Text) {
            if (message.text == "/start") return@message

            val user = message.from ?: return@message
            val state = StateManager.getState(user.id)

            if (state == null) {
                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = "Чтобы начать, напишите /start"
                )
                return@message
            }

            when (state) {
                RegistrationRequiredState -> {
                    when (message.text) {
                        "Просмотр доступных функции" -> {
                            StateManager.setState(message.chat.id, user.id, MainMenu.DemoState)
                        }
                        "Регистрация" -> {
                            StateManager.setState(message.chat.id, user.id, WaitingForNameState)
                        }
                    }
                }
                MainMenu.DemoState -> {
                    if (message.text == "Назад ⬅️") {
                        StateManager.setState(message.chat.id, user.id, RegistrationRequiredState)
                    }
                }
                WaitingForNameState -> {
                    val input = message.text ?: return@message
                    StateManager.updateLastInput(user.id, input)
                    StateManager.sendConfirmation(
                        message.chat.id,
                        user.id,
                        "Вы ввели имя: $input. Если верно, нажмите подтвердить, если нет — введите еще раз.",
                        "confirm_name"
                    )
                }
                WaitingForSurnameState -> {
                    val input = message.text ?: return@message
                    StateManager.updateLastInput(user.id, input)
                    StateManager.sendConfirmation(
                        message.chat.id,
                        user.id,
                        "Вы ввели фамилию: $input. Если верно, нажмите подтвердить, если нет — введите еще раз.",
                        "confirm_surname"
                    )
                }
                WaitingForCityState -> {
                    val input = message.text ?: return@message
                    StateManager.updateLastInput(user.id, input)
                    StateManager.sendConfirmation(
                        message.chat.id,
                        user.id,
                        "Вы ввели город: $input. Если верно, нажмите подтвердить, если нет — введите еще раз.",
                        "confirm_city"
                    )
                }
            }
        }
    }
}
