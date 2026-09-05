package ru.kruasanich.tg.bot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Точка входа приложения крипто-бота.
 *
 * Всё поведение бота описано декларативно сценарием (`CryptoScenarioConfig`)
 * поверх движка `core`. Транспорт, состояние и запуск собирает
 * `BotConfig` — здесь только загрузка контекста Spring.
 */
@SpringBootApplication
class BotApplication

fun main(args: Array<String>) {
    runApplication<BotApplication>(*args)
}
