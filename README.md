# Event System — Spring Boot, Kafka, Redis

Учебная микросервисная система управления мероприятиями и уведомлениями, основанная на `event-manager`.

## Состав

| Модуль | Назначение | Порт |
|---|---|---|
| `event-manager` | Пользователи, JWT, локации, мероприятия и регистрации | `8080` |
| `event-notificator` | Получение Kafka-событий, хранение и чтение уведомлений | `8081` |
| `event-common` | Общий контракт Kafka-сообщений | — |

Инфраструктура: PostgreSQL для `event-manager` (`5432`, существующий отдельный контейнер), PostgreSQL для `event-notificator` (`5434`), Kafka (`9092`), Redis (`6379`).

## Требования

- Java 21+
- IntelliJ IDEA или Maven 3.9+
- Docker Desktop с работающим Docker Compose
- PostgreSQL для `event-manager` на `localhost:5432` (в `compose.yaml` он **не** создаётся)

## Локальный запуск

Открой корневую директорию проекта в IntelliJ и импортируй все три Maven-модуля.

```powershell
docker compose up -d
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic event-changes --partitions 1 --replication-factor 1
```

Сборка из корня:

```powershell
mvn clean install -DskipTests
```

Если Maven не установлен в Windows, используй **Maven → Lifecycle → install** в IntelliJ.

Запусти два класса в IntelliJ:

1. `parfenov.dev.eventmanager.EventManagerApplication`
2. `parfenov.dev.eventnotificator.EventNotificatorApplication`

`event-common` запускать не нужно.

## Swagger и JWT

| Сервис | Swagger | API |
|---|---|---|
| EventManager | http://localhost:8080/swagger-ui/index.html | http://localhost:8080/v3/api-docs |
| EventNotificator | http://localhost:8081/swagger-ui/index.html | http://localhost:8081/v3/api-docs |

1. В Swagger первого сервиса вызови `POST /users/auth`, получи поле `jtwToken` (название поля сохранено из исходного API).
2. Открой Swagger `event-notificator`, нажми **Authorize** и вставь **только JWT** — без `Bearer`, Swagger добавит префикс сам.
3. Вызови `GET /notifications`, `GET /notifications/unread-count`, или `POST /notifications` с телом `{"notificationIds":[1]}` (используй настоящий ID).

Чтобы получать уведомления, нужен JWT пользователя, зарегистрированного на изменённое мероприятие; токен владельца события обычно не покажет уведомления подписчика.

## Как работает система

1. `EventManager` изменяет мероприятие в PostgreSQL.
2. `EventChangePublisher` публикует `EventChangeKafkaMessage` в Kafka topic `event-changes` после коммита.
3. `EventChangeConsumer` второго сервиса получает JSON через `@KafkaListener`.
4. `NotificationService` сохраняет событие в `notification_event_payloads` и личные уведомления в `notifications`.
5. `NotificationCounterService` обновляет ключ Redis `notif:unread:<userId>`.

Redis также используется Spring Cache для чтения мероприятий и локаций. Кеш обновляется по принципу *cache-aside*: `@Cacheable` сохраняет результат при первом чтении, изменение удаляет устаревший ключ, а следующий GET загружает актуальные данные. TTL — 5 минут.

## Проверка вручную

```powershell
# Посмотреть Kafka-события (остановить Ctrl+C)
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic event-changes --from-beginning

# Посмотреть уведомления
docker compose exec notificator-db psql -U postgres -d notificator -c "SELECT id, user_id, payload_id, is_read FROM notifications;"

# Посмотреть ключи Redis
docker compose exec redis redis-cli --scan

# Получить количество непрочитанных пользователя №7
docker compose exec redis redis-cli GET notif:unread:7
```

После запуска сервисов можно выполнить базовую проверку Swagger, Kafka и Redis:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check-local.ps1
```

## Настройки и безопасность

Файлы `application.properties` используют переменные окружения: `JWT_SIGN_KEY`, `MANAGER_DB_URL`, `MANAGER_DB_USER`, `MANAGER_DB_PASSWORD`, `NOTIFICATOR_DB_URL`, `NOTIFICATOR_DB_USER`, `NOTIFICATOR_DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `REDIS_HOST`, `REDIS_PORT`. Для локального обучения у некоторых настроек есть демонстрационные значения по умолчанию. **Нельзя применять их при публичном развёртывании.**

- Оба сервиса должны использовать **одинаковый `JWT_SIGN_KEY`** (не менее 32 байт для HS256).
- Не коммитьте `.env`, реальные пароли, токены, дампы БД и файлы IntelliJ. `.gitignore` их исключает.
- `compose.yaml` поднимает Kafka/Redis и БД уведомлений; значение `POSTGRES_PASSWORD: root` — **только для локальной разработки**.
- Исходный `DefaultUserInitializer` создаёт демонстрационные учётные записи (`admin/admin`, `user/user`), если они отсутствуют. Перед публичным развёртыванием удалите или отключите такую инициализацию.
- `spring.jpa.hibernate.ddl-auto=update` оставлен для совместимости с учебной БД; для production нужны миграции (Flyway/Liquibase).
- Публикация события после commit не является гарантированной доставкой при аварийном завершении; для production обычно используют Transactional Outbox.

Проект ориентирован на **локальное обучение**, а не на production-развёртывание в открытом интернете.

## Формат даты в Kafka и уведомлениях

Новые сообщения `event-changes` отправляются с датой в ISO-8601, например:

```json
"occurredAt": "2026-11-21T18:00:27.7036779"
```

Ранее отправленные сообщения и уже сохранённые строки `notification_event_payloads.payload_json` не переписываются.
`GET /notifications` при выдаче старых уведомлений преобразует дату из массива (старый формат) в строку.
После обновления проекта перезапусти **оба** Spring Boot сервиса. Для проверки нового сообщения измени мероприятие,
а затем проверь Kafka без `--from-beginning` и Swagger уведомлений на порту 8081.
