package com.example

import bot.TestBot
import bot.features.Registration
import com.example.plugins.initDb
import com.github.kotlintelegrambot.Bot
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import javax.sql.DataSource
import kotlin.io.path.createTempDirectory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import io.ktor.server.application.install
import io.ktor.server.application.log
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.plugin.Koin
import org.koin.dsl.module
import org.koin.logger.slf4jLogger
import kotlin.compareTo



fun Application.module() {
    val botToken = environment.config.property("tg.token").getString()
    val testBot = TestBot(botToken)
    testBot.addFeature(Registration())
    testBot.addFeature(bot.features.MainMenu())

    val botInstance = testBot.build()

    install(Koin) {
        modules(module {
            single { botInstance }
        })
    }

    initDb()
    botInstance.startPolling()
}

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

