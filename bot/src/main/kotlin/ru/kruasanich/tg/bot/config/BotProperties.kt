package ru.kruasanich.tg.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Настройки бота.
 *
 * Префикс конфигурации — `bot.engine`. Токен обязателен для реального запуска;
 * при пустом токене и `auto-register=false` приложение поднимется без Telegram
 * (удобно для тестов и локального прогона графа бинов).
 *
 * @property token токен бота, полученный у BotFather.
 * @property username имя бота (без `@`).
 * @property autoRegister регистрировать ли бота в Telegram при старте.
 */
@ConfigurationProperties(prefix = "bot.engine")
data class BotProperties(
    val token: String = "",
    val username: String = "",
    val autoRegister: Boolean = true,
)
