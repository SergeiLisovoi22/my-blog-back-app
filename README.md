# My Blog — Backend

Проектная работа четвертого спринта курса Middle Java Developer от Яндекс Практикума.

Бэкенд приложения-блога, реализованный на Spring Boot. Проект является продолжением работы третьего спринта, выполненной на Spring Framework и Gradle.

## Технологии

- Java 21
- Spring Boot 3.5.16
- Spring Web MVC
- Spring Data JDBC / Spring JDBC
- H2 Database
- Maven
- JUnit 5, Mockito, Spring Boot Test
- Embedded Apache Tomcat

## Функциональность

- Создание, получение, редактирование и удаление постов.
- Работа с комментариями (CRUD).
- Поиск постов по словам и тегам.
- Пагинация постов.
- Лайки.
- Загрузка и получение изображений.

## Требования для запуска

- JDK 21
- Доступ к Maven Central для первой загрузки зависимостей

Maven Wrapper входит в репозиторий, поэтому отдельно устанавливать Maven не требуется.

## Сборка приложения

В Windows PowerShell выполните:

```powershell
.\mvnw.cmd clean package
```

Исполняемый JAR создается в каталоге:

`target/my-blog-back-app-0.0.1-SNAPSHOT.jar`

## Запуск приложения

Через Maven:

```powershell
.\mvnw.cmd spring-boot:run
```

Или из собранного JAR:

```powershell
java -jar target/my-blog-back-app-0.0.1-SNAPSHOT.jar
```

Приложение запускается на порту `8080` со встроенным Tomcat. Внешний Tomcat не требуется.

## База данных

Используется встроенная H2 Database.

Настройки подключения находятся в `src/main/resources/application.properties`.

Структура базы данных создается автоматически при запуске приложения с помощью `schema.sql`.

База работает в памяти, поэтому данные не сохраняются после остановки приложения.

## REST API

Основной адрес: `http://localhost:8080`

Примеры:

- `GET /api/posts` — получение списка постов.
- `POST /api/posts` — создание поста.
- `GET /api/posts/{id}` — получение поста.
- `PUT /api/posts/{id}` — редактирование поста.
- `DELETE /api/posts/{id}` — удаление поста.
- `GET /api/posts/{id}/comments` — получение комментариев.
- `POST /api/posts/{id}/comments` — создание комментария.
- `PUT /api/posts/{id}/comments/{commentId}` — редактирование комментария.
- `DELETE /api/posts/{id}/comments/{commentId}` — удаление комментария.

## Тестирование

Запуск всех тестов:

```powershell
.\mvnw.cmd clean test
```

Полная сборка с тестами:

```powershell
.\mvnw.cmd clean package
```

В проекте реализованы модульные и интеграционные тесты для сервисного слоя, MVC и DAO.

Интеграционные тесты используют `@SpringBootTest`, `@AutoConfigureMockMvc`, JUnit 5 и H2. Совместимые тестовые классы используют кеширование контекста Spring.

## Фронтенд

Для проверки можно использовать предоставленный Яндекс Практикумом React-фронтенд, который запускается отдельно через Docker.

Фронтенд: `http://localhost/`

Бэкенд: `http://localhost:8080`

## Отличия от третьего спринта

- Gradle заменен на Maven.
- Spring Framework заменен на Spring Boot.
- WAR заменен на Executable JAR.
- Внешний Tomcat заменен встроенным.
- Удалена ручная конфигурация запуска Servlet-контейнера.
- Используется автоконфигурация Spring Boot для JDBC, MVC и транзакций.
- Интеграционные тесты переведены на Spring Boot Test.

История коммитов третьего спринта сохранена в Git.