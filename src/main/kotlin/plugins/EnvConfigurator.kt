package com.example.plugins

class EnvConfigurator : Configurator {
    override val dbHost: String = System.getenv("DB_HOST") ?: "localhost"
    override val dbPort: String = System.getenv("DB_PORT") ?: "5432"
    override val dbName: String = System.getenv("DB_NAME") ?: "tgbot"
    override val dbUsername: String = System.getenv("DB_USERNAME") ?: ""
    override val dbPassword: String = System.getenv("DB_PASSWORD") ?: ""
    override val telegramToken: String = System.getenv("TELEGRAM_TOKEN") ?: ""
}