package bot.features

import com.github.kotlintelegrambot.dispatcher.Dispatcher

interface BotFeature {
    fun install(dispatcher: Dispatcher)
}
