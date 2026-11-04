# My Blog — backend (Яндекс Практикум, спринт 3)

Spring Framework 6.2 (НЕ Spring Boot), Java 21, Gradle Kotlin DSL, H2, Servlet MVC, WAR.

## Требования
- JDK 21, Gradle 8.8+ (или используйте Gradle Wrapper после его генерации)
- Apache Tomcat 10.1.x с Jakarta Servlet 6.0
- Готовый фронтенд из задания на http://localhost (его файлы не изменяются)

## Сборка и тесты
```bat
gradle clean test war
```
Результат: `build/libs/ROOT.war`. Для IntelliJ IDEA выберите Gradle JVM = JDK 21.

## Запуск
1. Скопируйте `build/libs/ROOT.war` в `<TOMCAT_HOME>/webapps/ROOT.war`.
2. Запустите Tomcat на 8080 (`bin\startup.bat`).
3. Backend REST API: `http://localhost:8080/api/posts?search=&pageNumber=1&pageSize=5`.
4. Запустите исходный фронтенд через `docker compose up -d` в его каталоге, затем `http://localhost`.

H2 сохраняет данные в `${user.home}/my-blog-data/blog.mv.db`; схема автоматически инициализируется при запуске.
Для изолированных запусков можно передать JVM property `-Dblog.jdbc.url=jdbc:h2:mem:blog;DB_CLOSE_DELAY=-1`.

## REST API
- GET `/api/posts?search=&pageNumber=1&pageSize=5` — поиск/пагинация (теги `#tag`)
- GET `/api/posts/{id}`, POST `/api/posts/{id}` — получить пост (POST оставлен для совместимости с текстом ТЗ)
- POST `/api/posts`, PUT/DELETE `/api/posts/{id}` — CRUD
- POST `/api/posts/{id}/likes` — увеличить счётчик
- PUT/GET `/api/posts/{id}/image` — изображение; multipart part `image`
- GET/POST `/api/posts/{id}/comments`, GET/PUT/DELETE `/api/posts/{id}/comments/{commentId}` — комментарии

## Проверка ревьюером
Работа ведётся в отдельной ветке с микрокоммитами. Создайте PR в main и **не сливайте его до принятия ревьюером**.

## Примечания
На предоставленном фронтенде найдена потенциальная неточность: при редактировании комментария путь собирается как `/api/posts/${comment.id}/comments/${comment.id}`, а не `/api/posts/${postId}/comments/${comment.id}`. Не меняем фронтенд; это требует отдельной проверки сценария с ревьюером.
