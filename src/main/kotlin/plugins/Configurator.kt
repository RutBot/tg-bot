package com.example.plugins

interface Configurator {
    val dbHost: String
    val dbPort: String
    val dbName: String
    val dbUsername: String
    val dbPassword: String
    val telegramToken: String
}