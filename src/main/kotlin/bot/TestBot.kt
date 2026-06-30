package bot

import bot.features.BotFeature
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch

class TestBot(private val botToken: String) {
    private val features = mutableListOf<BotFeature>()

    fun addFeature(feature: BotFeature) {
        features.add(feature)
    }

    fun build(): Bot {
        return bot {
            token = botToken
            dispatch {
                features.forEach { it.install(this) }
            }
        }
    }
}