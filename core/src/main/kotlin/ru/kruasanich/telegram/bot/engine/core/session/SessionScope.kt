package ru.kruasanich.telegram.bot.engine.core.session

/**
 * Типизированное чтение атрибутов сессии.
 *
 * Хранилище сессии плоское (`Map<String, String>`), но прикладной код почти
 * всегда работает с числами и флагами. Эти расширения избавляют от повторного
 * `?.toLongOrNull()` в каждом сервисе.
 */
fun UserSession.longAttr(key: String): Long? = attribute(key)?.toLongOrNull()

/** Прочитать атрибут как [Int]. */
fun UserSession.intAttr(key: String): Int? = attribute(key)?.toIntOrNull()

/** Прочитать атрибут как [Double]. */
fun UserSession.doubleAttr(key: String): Double? = attribute(key)?.toDoubleOrNull()

/** Прочитать атрибут как [Boolean] (строго `true`/`false`). */
fun UserSession.boolAttr(key: String): Boolean? = attribute(key)?.toBooleanStrictOrNull()

/** Записать числовой атрибут. */
fun UserSession.putAttribute(key: String, value: Long) = putAttribute(key, value.toString())

/** Записать числовой атрибут. */
fun UserSession.putAttribute(key: String, value: Int) = putAttribute(key, value.toString())

/** Записать числовой атрибут. */
fun UserSession.putAttribute(key: String, value: Double) = putAttribute(key, value.toString())

/** Записать флаг. */
fun UserSession.putAttribute(key: String, value: Boolean) = putAttribute(key, value.toString())

/**
 * Открыть именованную область атрибутов сессии.
 *
 * Область добавляет префикс `<name>.` ко всем ключам, что решает две задачи:
 *  - изолирует данные разных функций мультибота (квиз, форма, мастер) друг от друга;
 *  - позволяет одной операцией [SessionScope.clear] выкинуть всё состояние потока
 *    при выходе из него (flow-scoped данные), не оставляя «мусора» в сессии.
 *
 * @param name имя области (например, `"quiz.42"` или `"form.signup"`).
 */
fun UserSession.scope(name: String): SessionScope = SessionScope(this, name)

/**
 * Именованная, типизированная область атрибутов сессии.
 *
 * @property name префикс области.
 */
class SessionScope(private val session: UserSession, val name: String) {

    private fun key(key: String): String = "$name.$key"

    /** Прочитать строковое значение области. */
    fun get(key: String): String? = session.attribute(key(key))

    /** Прочитать значение как [Long]. */
    fun long(key: String): Long? = session.longAttr(key(key))

    /** Прочитать значение как [Int]. */
    fun int(key: String): Int? = session.intAttr(key(key))

    /** Прочитать значение как [Double]. */
    fun double(key: String): Double? = session.doubleAttr(key(key))

    /** Прочитать значение как [Boolean]. */
    fun bool(key: String): Boolean? = session.boolAttr(key(key))

    /** Записать строковое значение. */
    fun put(key: String, value: String) = session.putAttribute(key(key), value)

    /** Записать значение [Long]. */
    fun put(key: String, value: Long) = session.putAttribute(key(key), value)

    /** Записать значение [Int]. */
    fun put(key: String, value: Int) = session.putAttribute(key(key), value)

    /** Записать значение [Double]. */
    fun put(key: String, value: Double) = session.putAttribute(key(key), value)

    /** Записать флаг. */
    fun put(key: String, value: Boolean) = session.putAttribute(key(key), value)

    /** Удалить значение области. */
    fun remove(key: String) {
        session.attributes.remove(key(key))
    }

    /** Очистить все атрибуты области (выход из потока). */
    fun clear() {
        session.attributes.keys.removeAll { it == name || it.startsWith("$name.") }
    }
}
