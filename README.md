# MedReminder

Клиентское Android-приложение для учебного проекта напоминаний о приёме лекарств.

## Что внутри
- Java 17, Android SDK 29+
- UI: XML + Material Components (без Compose)
- Локальные данные: Room (SQLite)
- Сеть: Retrofit + OkHttp + Gson
- Напоминания: AlarmManager + BroadcastReceiver + NotificationManager
- Фоновая синхронизация справочника: WorkManager

## Быстрый запуск
1. Откройте проект в Android Studio.
2. В настройках Gradle укажите JDK 17.
3. Дождитесь Sync.
4. Запустите `app` на эмуляторе/телефоне (Android 10+).

Если хотите собрать из терминала:

```bash
./gradlew assembleDebug
```

(Windows: `gradlew.bat assembleDebug`)

## Подключение к серверу
По умолчанию используется адрес для эмулятора:

`http://10.0.2.2:8080/`

Для реального телефона адрес задаётся в приложении:

`Настройки -> Адрес сервера`

Ожидаемые endpoint'ы:
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/drugs`
- `GET /api/health`

## Режим работы
- После входа и загрузки справочника приложение работает офлайн.
- Курсы, расписание, напоминания, журнал и настройки хранятся локально.
- Если сервер недоступен, показывается ошибка, локальная часть продолжает работать.
- Для разработки без backend можно создать локальный профиль: `Регистрация -> Создать локальный профиль`, затем вход через `Войти локально`.

## Напоминания
- Уведомления создаются локально через AlarmManager.
- Действия в уведомлении: `Принял`, `Пропустить`, `Перенести` (быстрый перенос на 10 минут).
- Есть тихий режим (время начала/окончания в настройках).

## Структура проекта
`app/src/main/java/com/medreminder/`
- `data/local/entity`
- `data/local/dao`
- `data/local/database`
- `data/remote/api`
- `data/remote/dto`
- `data/repository`
- `ui/auth`
- `ui/main`
- `ui/courses`
- `ui/course_edit`
- `ui/schedule`
- `ui/intake_log`
- `ui/drugs`
- `ui/settings`
- `notification`
- `util`

## Seed-данные
Демо-набор препаратов лежит в:

`app/src/main/assets/drugs_seed.json`

Используется как fallback при пустом справочнике.

## Важно
- На Android 13+ нужно разрешение на уведомления.
- Точность напоминаний зависит от системных ограничений энергосбережения устройства.
