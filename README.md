# tg-bot — крипто-бот на движке bot-engine

Telegram-бот-навигатор по криптоинструментам, переписанный на **Spring Boot** поверх
движка сценариев `bot-engine-core` (взят из `own/telegram-bot`). Бот описан декларативно
одним DSL-сценарием, а транспорт, состояние и запуск собирает тонкий Spring-слой.

## Структура (мультимодуль Gradle)

```
tg-bot/
├── core/   вендоренное ядро движка bot-engine-core (Scenario/Screen/DSL/FSM,
│           порты BotSender/SessionStore) — чистый Kotlin, без Spring/Telegram
└── bot/    Spring Boot приложение: сценарий крипто-флоу + Telegram-транспорт
            (telegrambots 7.x, long polling) + сборка графа движка
```

Граф зависимостей: `bot` → `core`.

- **Пакеты:** `core` сохраняет пакет движка `ru.kruasanich.telegram.bot.engine.core`
  (переиспользуется как есть); прикладной код `bot` — `ru.kruasanich.tg.bot`.
- **Стек:** Kotlin 2.0.21 / JDK 21 / Spring Boot 3.3.5 / Gradle 8.10.2 /
  telegrambots 7.2.0 / корутины 1.9.0.

## Флоу бота

1. `/start` — информация о проекте и работе с криптоинструментами; кнопки
   **«🔎 Подробнее»** и **«📝 Зарегистрироваться»**.
2. **Подробнее** — краткая справка про крипторынок и торги + **«Зарегистрироваться»**.
3. **Регистрация** — пошагово: **Имя → Фамилия → Возраст → Пол**, на каждом шаге
   кнопка **«Далее ➡️»**. Порядок шагов гарантируют декларативные `guard`, данные
   копятся в области сессии `reg` (валидация имени/фамилии/возраста).
4. После регистрации — **главное меню**: Крипторынок, Мой профиль, О проекте.

Весь флоу — в одном файле `bot/.../scenario/CryptoScenarioConfig.kt`.

## Сборка и запуск

Нужен JDK 21.

```bash
./gradlew build                 # компиляция + тесты обоих модулей
./gradlew :core:test            # быстрые юнит-тесты ядра (FSM)
./gradlew :bot:test             # тесты флоу крипто-бота (без Telegram)

# Запуск (нужен токен от BotFather):
TELEGRAM_BOT_TOKEN=xxx TELEGRAM_BOT_USERNAME=crypto_start_bot ./gradlew :bot:bootRun
```

Хранилище сессий — in-memory (БД не нужна). Для продакшена бин `sessionStore`
в `BotConfig` заменяется на персистентную реализацию `SessionStore`.

### Docker

```bash
docker build -t tg-bot .
docker run -e TELEGRAM_BOT_TOKEN=xxx tg-bot
```

## Конфигурация (`bot/src/main/resources/application.yml`)

| Переменная | Назначение | Дефолт |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | токен от BotFather | пусто (бот не регистрируется) |
| `TELEGRAM_BOT_USERNAME` | имя бота | `crypto_start_bot` |
| `BOT_AUTO_REGISTER` | подключать long polling при старте | `true` |

При пустом токене приложение поднимается без Telegram (удобно для тестов).
