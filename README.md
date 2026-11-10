# My Blog — backend

Учебный проект Яндекс Практикума (Middle Java, спринт 3): REST API для блога с постами, комментариями, тегами, лайками и изображениями.

Бэкенд реализован на **Spring Framework без Spring Boot** и развёртывается в отдельном сервлет-контейнере Apache Tomcat. React-фронтенд предоставляется вместе с заданием отдельно и не входит в этот репозиторий.

## Технологии

- Java 21
- Spring Framework 6.2 (Spring MVC, Spring JDBC, Spring Test)
- Gradle (Kotlin DSL), Gradle Wrapper 8.10
- H2 Database
- JUnit 5, Mockito, MockMvc
- WAR, Apache Tomcat 10.1 (Jakarta Servlet 6)

## Архитектура

`Controller → Service → Repository (DAO) → H2`

Основные пакеты в `src/main/java/ru/practicum/blog`:

- `config` — Java-конфигурация Spring, MVC, источника данных и инициализация `DispatcherServlet`;
- `controller` — обработчики REST-запросов;
- `service` — бизнес-логика;
- `repository` — SQL-запросы через Spring `JdbcTemplate`;
- `model` — DTO запросов и ответов.

Схема базы данных находится в `src/main/resources/schema.sql` и создаётся автоматически при инициализации приложения.

## Требования для запуска

- JDK 21 (сборка через Gradle Toolchains);
- Docker Desktop с работающим Linux Engine — для варианта запуска через контейнер;
- свободный порт **8080** для бэкенда;
- для фронтенда — свободный порт **80** и готовый архив из задания.

Отдельно устанавливать Gradle или Tomcat при запуске через Docker не требуется.

## Сборка и тесты

В корневой директории проекта выполните в PowerShell:

```powershell
.\gradlew.bat clean test
.\gradlew.bat clean build
```

В Linux/macOS соответствующие команды: `./gradlew clean test` и `./gradlew clean build`.

После сборки готовый WAR-файл находится по пути:

```text
build/libs/ROOT.war
```

Тесты (каталог `src/test/java`) включают юнит-тесты сервисного слоя и интеграционные проверки Spring MVC и DAO с тестовой H2. Для просмотра отчёта откройте `build/reports/tests/test/index.html` после запуска тестов.

## Запуск бэкенда через Docker

Команды ниже для **PowerShell из корня проекта**. После успешной сборки скачайте официальный образ Tomcat 10.1 на Java 21:

```powershell
docker pull tomcat:10.1-jdk21-temurin-noble
```

Создайте контейнер и подключите собранный WAR-файл:

```powershell
docker run -d --name my-blog-backend -p 8080:8080 -v "${PWD}\build\libs\ROOT.war:/usr/local/tomcat/webapps/ROOT.war:ro" tomcat:10.1-jdk21-temurin-noble
```

Если контейнер **уже создан** (например, после перезагрузки компьютера), не повторяйте `docker run`; запустите существующий:

```powershell
docker start my-blog-backend
```

Проверка работы:

```powershell
docker ps
docker logs --tail 100 my-blog-backend
```

Проверить API можно через браузер:

```text
http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5
```

Ожидается JSON с полями `posts`, `hasPrev`, `hasNext`, `lastPage`.

**При обновлении WAR:** остановите и удалите *только* контейнер бэкенда, пересоберите WAR и выполните `docker run` заново:

```powershell
docker stop my-blog-backend
docker rm my-blog-backend
.\gradlew.bat clean build
```

Затем повторите приведённую выше команду `docker run`. Удаление контейнера может удалить и данные H2, хранящиеся внутри него.

## Запуск фронтенда

Распакуйте отдельный архив `my-blog-front-app`, предоставленный Яндекс Практикумом. В каталоге, содержащем его `docker-compose.yaml`, выполните:

```powershell
docker compose up -d
```

Откройте фронтенд:

```text
http://localhost/
```

Браузер обращается к бэкенду по адресу `http://localhost:8080`. Исходная конфигурация фронтенда использует `network_mode: host`, поэтому для запуска на Windows может потребоваться включить **host networking** в Docker Desktop. Фронтенд поставляется отдельно и не включён в этот Git-репозиторий.

Остановить фронтенд: `docker compose down` из его каталога.

## Основные REST-эндпоинты

| Метод | Адрес | Назначение |
|---|---|---|
| GET | `/api/posts?search=&pageNumber=1&pageSize=5` | Лента постов, поиск, пагинация |
| GET | `/api/posts/{id}` | Получить пост |
| POST | `/api/posts` | Создать пост |
| PUT | `/api/posts/{id}` | Изменить пост |
| DELETE | `/api/posts/{id}` | Удалить пост |
| POST | `/api/posts/{id}/likes` | Поставить лайк |
| PUT | `/api/posts/{id}/image` | Загрузить изображение (`multipart/form-data`, поле `image`) |
| GET | `/api/posts/{id}/image` | Получить изображение |
| GET | `/api/posts/{postId}/comments` | Список комментариев |
| POST | `/api/posts/{postId}/comments` | Добавить комментарий |
| GET | `/api/posts/{postId}/comments/{commentId}` | Получить комментарий |
| PUT | `/api/posts/{postId}/comments/{commentId}` | Изменить комментарий |
| DELETE | `/api/posts/{postId}/comments/{commentId}` | Удалить комментарий |

Поиск учитывает слова названия и теги, обозначенные префиксом `#`. Для совместимости с формулировкой задания также реализован `POST /api/posts/{id}` для получения поста.

## Хранение данных

По умолчанию приложение использует файловую H2 по пути `${user.home}/my-blog-data/blog` **в той среде, где работает Tomcat**. В Docker это файловая система контейнера. Пока контейнер существует, данные в нём сохраняются при обычной остановке и повторном запуске, но **могут пропасть после удаления контейнера**. Постоянный Docker volume в базовой инструкции не настроен.

В тестах используется отдельная конфигурация H2. Для локального тестового запуска возможно переопределение JDBC URL через системное свойство JVM `blog.jdbc.url`.

## Git и сдача

Разработка выполняется в ветке `module_one_sprint_three_branch`, созданной от `main`