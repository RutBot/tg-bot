package ru.kruasanich.telegram.bot.engine.core.engine

import ru.kruasanich.telegram.bot.engine.core.model.ButtonAction
import ru.kruasanich.telegram.bot.engine.core.model.ScreenId

/**
 * Кодек действий кнопок в компактный callback-payload и обратно.
 *
 * Telegram ограничивает callback_data 64 байтами, поэтому действие кодируется
 * коротким префиксом. Кодек — чистая функция без состояния, что упрощает
 * тестирование и гарантирует симметрию encode/decode.
 *
 * Формат `Custom` с аргументами: `c:<name>:<k1>=<v1>&<k2>=<v2>`. Ключи и
 * значения не должны содержать служебных символов `:`, `&`, `=`.
 */
object ActionCodec {

    private const val NAVIGATE = "n"
    private const val BACK = "b"
    private const val HOME = "h"
    private const val CUSTOM = "c"
    private const val SEPARATOR = ":"
    private const val ARG_PAIRS = "&"
    private const val ARG_KV = "="

    /**
     * Закодировать действие кнопки в callback-payload.
     *
     * @param action действие кнопки.
     * @return строка, пригодная для callback_data.
     */
    fun encode(action: ButtonAction): String = when (action) {
        is ButtonAction.Navigate -> "$NAVIGATE$SEPARATOR${action.target.value}"
        ButtonAction.Back -> BACK
        ButtonAction.Home -> HOME
        is ButtonAction.Custom ->
            if (action.args.isEmpty()) "$CUSTOM$SEPARATOR${action.name}"
            else "$CUSTOM$SEPARATOR${action.name}$SEPARATOR${encodeArgs(action.args)}"
    }

    /**
     * Декодировать callback-payload обратно в действие.
     *
     * @param payload ранее закодированный payload.
     * @return восстановленное действие или `null`, если payload не распознан.
     */
    fun decode(payload: String): ButtonAction? {
        val parts = payload.split(SEPARATOR, limit = 3)
        return when (parts[0]) {
            NAVIGATE -> parts.getOrNull(1)?.let { ButtonAction.Navigate(ScreenId(it)) }
            BACK -> ButtonAction.Back
            HOME -> ButtonAction.Home
            CUSTOM -> parts.getOrNull(1)?.let { name ->
                ButtonAction.Custom(name, parts.getOrNull(2)?.let(::decodeArgs) ?: emptyMap())
            }
            else -> null
        }
    }

    private fun encodeArgs(args: Map<String, String>): String =
        args.entries.joinToString(ARG_PAIRS) { "${it.key}$ARG_KV${it.value}" }

    private fun decodeArgs(raw: String): Map<String, String> =
        raw.split(ARG_PAIRS)
            .mapNotNull { pair ->
                val kv = pair.split(ARG_KV, limit = 2)
                if (kv.size == 2) kv[0] to kv[1] else null
            }
            .toMap()
}
