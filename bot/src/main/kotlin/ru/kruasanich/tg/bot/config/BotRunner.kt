package ru.kruasanich.tg.bot.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import ru.kruasanich.telegram.bot.engine.core.engine.ScreenEngine
import ru.kruasanich.tg.bot.telegram.EngineUpdateConsumer

/**
 * Связывает движок с транспортом и регистрирует потребителя апдейтов в Telegram
 * (telegrambots 7.x, long polling).
 *
 * После инициализации контекста в потребитель ставится обработчик (движок),
 * и, если включено [BotProperties.autoRegister], потребитель подключается к
 * Telegram long-polling через [TelegramBotsLongPollingApplication].
 *
 * @property consumer потребитель Telegram-апдейтов.
 * @property engine движок-исполнитель сценария.
 * @property properties настройки бота.
 */
class BotRunner(
    private val consumer: EngineUpdateConsumer,
    private val engine: ScreenEngine,
    private val properties: BotProperties,
) : InitializingBean, DisposableBean {

    private val log = LoggerFactory.getLogger(BotRunner::class.java)
    private var application: TelegramBotsLongPollingApplication? = null

    /** Подключить обработчик и (опционально) зарегистрировать потребителя. */
    override fun afterPropertiesSet() {
        consumer.processor = { update -> engine.handle(update) }
        if (properties.autoRegister && properties.token.isNotBlank()) {
            register()
        } else {
            log.info("Bot auto-registration disabled (token blank or auto-register=false); engine wired without transport")
        }
    }

    private fun register() {
        application = TelegramBotsLongPollingApplication().apply {
            registerBot(properties.token, consumer)
        }
        log.info("Telegram bot '{}' registered (long polling)", properties.username)
    }

    /** Корректно остановить long-polling при остановке контекста. */
    override fun destroy() {
        application?.close()
    }
}
