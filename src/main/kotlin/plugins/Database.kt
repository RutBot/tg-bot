package com.example.plugins

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationEnvironment
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ExperimentalDatabaseMigrationApi
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.io.path.createTempDirectory

fun createHikariConfig(environment: ApplicationEnvironment): HikariConfig {
    val dbConfig = environment.config.config("db")

    val host = dbConfig.property("host").getString()
    val port = dbConfig.property("port").getString()
    val dbName = dbConfig.property("name").getString()

    val username = dbConfig.property("username").getString()
    val password = dbConfig.property("password").getString()

    return HikariConfig().apply {
        jdbcUrl = "jdbc:postgresql://$host:$port/$dbName"

        this.username = username
        this.password = password

        driverClassName = "org.postgresql.Driver"

        maximumPoolSize = 10
        minimumIdle = 2
        idleTimeout = 60000
        connectionTimeout = 30000
        maxLifetime = 1800000
    }
}

@OptIn(ExperimentalDatabaseMigrationApi::class)
fun Application.initDb() {
    val hikariConfig = createHikariConfig(environment)
    val dataSource = HikariDataSource(hikariConfig)
    val migrationDirectory = createTempDirectory()


    TransactionManager.defaultDatabase = Database.connect(dataSource)
    val flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("filesystem:$migrationDirectory")
        .ignoreMigrationPatterns("*:missing")
        .outOfOrder(true)
        .validateMigrationNaming(true)
        .outputQueryResults(true)
        .load()

    flyway.repair()
    val version = "0.1"
    val fileName = "V${version}__tgbot"

    val file = transaction {
        return@transaction MigrationUtils.generateMigrationScript(
            bot.Users,

            scriptDirectory = migrationDirectory.toString(),
            scriptName = fileName,
            withLogs = false
        )
    }
    if (file.exists()) {
        if (file.exists() && file.length() > 0) {
            transaction {
                flyway.migrate()
            }
        } else {
            file.delete()
        }
    }
}
