package ru.kruasanich.telegram.bot.engine.core.dsl

/**
 * Маркер DSL движка. Запрещает неявный доступ к внешним receiver-ам внутри
 * вложенных блоков DSL, что предотвращает типичные ошибки описания сценария.
 */
@DslMarker
annotation class ScenarioDsl
