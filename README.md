# ZTelegram Plugin

ZTelegram - плагин интеграции Minecraft сервера с Telegram, написанный на Kotlin.

## 🌟 Особенности

- 🔄 **Двусторонняя интеграция** - сообщения отправляются в обе стороны: из игры в Telegram и из Telegram в игру
- 🎮 **Команды Minecraft в Telegram** - возможность выполнять команды прямо из Telegram
- 👥 **Система регистрации** - привязка Telegram-аккаунта к игровому аккаунту
- 🎲 **Мини-игра "Угадай слово"** - увлекательная игра с наградами для игроков
- 🔔 **Уведомления о событиях** - оповещения о входе/выходе игроков, смертях и других важных событиях сервера
- 🎨 **Поддержка MiniMessage** - красивое форматирование текста с градиентами и стилями
- ⚙️ **Гибкая настройка** - полностью настраиваемые сообщения и функционал через конфигурационные файлы

## 📋 Команды

### Команды в игре
- `/telegram` - показать ссылку на Telegram канал
- `/telegram help` - показать список команд
- `/telegram reload` - перезагрузить плагин
- `/telegram reload game` - перезагрузить только конфигурацию игры
- `/telegram unregister <player>` - удалить регистрацию игрока
- `/telegram addplayer <player>` - скрыть игрока из сообщений Telegram
- `/telegram removeplayer <player>` - показывать игрока в сообщениях Telegram
- `/telegram addchannel <1|2|3> <channelId>` - настроить ID канала
- `/telegram hidden` - показать список скрытых игроков

### Команды в Telegram
- `/online`, `/онлайн` - показать список игроков онлайн
- `/tps`, `/тпс` - показать TPS сервера
- `/restart`, `/рестарт` - перезапустить сервер
- `/gender [man/girl]`, `/пол [м/ж]` - установить свой пол
- `/player [nickname]`, `/ник [никнейм]` - информация об игроке
- `/cmd`, `/команды` - показать список всех команд
- `/game [nickname]`, `/игра [никнейм]` - сыграть в игру "Угадай слово"

## 🔧 Установка

1. Скачайте последнюю версию плагина из раздела [Releases](https://github.com/Z-oobastik-s/Ztelegram-Plugin/releases)
2. Поместите JAR-файл в папку `plugins` вашего сервера
3. Перезапустите сервер
4. Настройте плагин через файлы конфигурации в папке `plugins/ZTelegram`

## ⚙️ Конфигурация

Основная конфигурация находится в файле `config.yml`. Настройка игры "Угадай слово" - в файле `game.yml`.

### Основные настройки

```yaml
bot:
  token: "YOUR_TELEGRAM_BOT_TOKEN"  # Токен Telegram Bot API
  username: "YOUR_BOT_USERNAME"     # Имя бота (без @)

main_channel:
  enabled: true                     # Включить основной канал
  id: "YOUR_CHANNEL_ID"             # ID основного канала
```

### Настройка игры "Угадай слово"

```yaml
enabled: true                       # Включить игру
reward:
  amount: 5                         # Количество монет за победу
  commands:                         # Команды, выполняемые при победе
    - "eco give %player% 5"
```

## 🛠️ Сборка из исходников

```bash
git clone https://github.com/Z-oobastik-s/Ztelegram-Plugin.git
cd Ztelegram-Plugin
gradle shadowJar
```

## 📄 Лицензия

Проект распространяется под лицензией [MIT](LICENSE). 