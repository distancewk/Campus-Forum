<!-- auto-generated: code-guide:start -->
# Code guide

<!-- generated-from: sha256:5b57ff9c7b1f2038d65744e95a058c42dddf4a6026b85c00d751ffb452cc23f6 -->
<!-- generated-by: improve-java-readability -->

## Where to change common behavior

| Need | Start here | Follow to | Evidence |
| --- | --- | --- | --- |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/ai/documents"} -->
| HTTP `GET /api/admin/ai/documents` | `AdminAiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java:42](../campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java#L42) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/admin/ai/documents"} -->
| HTTP `POST /api/admin/ai/documents` | `AdminAiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java:36](../campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java#L36) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:DELETE /api/admin/ai/documents/{id}"} -->
| HTTP `DELETE /api/admin/ai/documents/{id}` | `AdminAiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java:47](../campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java#L47) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/admin/ai/documents/{id}/reindex"} -->
| HTTP `POST /api/admin/ai/documents/{id}/reindex` | `AdminAiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java:53](../campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java#L53) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/ai/moderation"} -->
| HTTP `GET /api/admin/ai/moderation` | `AdminAiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java:59](../campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java#L59) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/ai/moderation-metrics"} -->
| HTTP `GET /api/admin/ai/moderation-metrics` | `AdminAiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java:64](../campus-forum-backend/src/main/java/com/campus/ai/controller/AdminAiController.java#L64) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/admin/boards"} -->
| HTTP `POST /api/admin/boards` | `BoardController` | — | [campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java:33](../campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java#L33) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:DELETE /api/admin/boards/{id}"} -->
| HTTP `DELETE /api/admin/boards/{id}` | `BoardController` | — | [campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java:53](../campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java#L53) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/boards/{id}"} -->
| HTTP `PUT /api/admin/boards/{id}` | `BoardController` | — | [campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java:42](../campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java#L42) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/comments/pending"} -->
| HTTP `GET /api/admin/comments/pending` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:62](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L62) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/comments/{id}/audit"} -->
| HTTP `PUT /api/admin/comments/{id}/audit` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:70](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L70) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/comments/{id}/feature"} -->
| HTTP `PUT /api/admin/comments/{id}/feature` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:80](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L80) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/dashboard"} -->
| HTTP `GET /api/admin/dashboard` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:116](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L116) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/posts/pending"} -->
| HTTP `GET /api/admin/posts/pending` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:44](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L44) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:DELETE /api/admin/posts/{id}"} -->
| HTTP `DELETE /api/admin/posts/{id}` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:107](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L107) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/posts/{id}/audit"} -->
| HTTP `PUT /api/admin/posts/{id}/audit` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:52](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L52) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/posts/{id}/feature"} -->
| HTTP `PUT /api/admin/posts/{id}/feature` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:98](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L98) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/posts/{id}/pin"} -->
| HTTP `PUT /api/admin/posts/{id}/pin` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:89](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L89) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/admin/users"} -->
| HTTP `GET /api/admin/users` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:26](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L26) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/admin/users/{id}/status"} -->
| HTTP `PUT /api/admin/users/{id}/status` | `AdminController` | — | [campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java:34](../campus-forum-backend/src/main/java/com/campus/admin/controller/AdminController.java#L34) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/ai/ask"} -->
| HTTP `POST /api/ai/ask` | `AiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java:27](../campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java#L27) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/ai/sessions"} -->
| HTTP `GET /api/ai/sessions` | `AiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java:34](../campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java#L34) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/ai/sessions/{id}"} -->
| HTTP `GET /api/ai/sessions/{id}` | `AiController` | — | [campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java:40](../campus-forum-backend/src/main/java/com/campus/ai/controller/AiController.java#L40) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/forgot-password"} -->
| HTTP `POST /api/auth/forgot-password` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:101](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L101) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/login"} -->
| HTTP `POST /api/auth/login` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:57](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L57) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/logout"} -->
| HTTP `POST /api/auth/logout` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:81](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L81) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/refresh"} -->
| HTTP `POST /api/auth/refresh` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:67](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L67) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/register"} -->
| HTTP `POST /api/auth/register` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:38](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L38) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/register/verify"} -->
| HTTP `POST /api/auth/register/verify` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:47](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L47) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/reset-password"} -->
| HTTP `POST /api/auth/reset-password` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:111](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L111) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/auth/send-code"} -->
| HTTP `POST /api/auth/send-code` | `AuthController` | — | [campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java:28](../campus-forum-backend/src/main/java/com/campus/auth/controller/AuthController.java#L28) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/boards"} -->
| HTTP `GET /api/boards` | `BoardController` | — | [campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java:25](../campus-forum-backend/src/main/java/com/campus/board/controller/BoardController.java#L25) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/favorites"} -->
| HTTP `GET /api/favorites` | `InteractionController` | — | [campus-forum-backend/src/main/java/com/campus/interaction/controller/InteractionController.java:51](../campus-forum-backend/src/main/java/com/campus/interaction/controller/InteractionController.java#L51) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/favorites"} -->
| HTTP `POST /api/favorites` | `InteractionController` | — | [campus-forum-backend/src/main/java/com/campus/interaction/controller/InteractionController.java:40](../campus-forum-backend/src/main/java/com/campus/interaction/controller/InteractionController.java#L40) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/likes"} -->
| HTTP `POST /api/likes` | `InteractionController` | — | [campus-forum-backend/src/main/java/com/campus/interaction/controller/InteractionController.java:29](../campus-forum-backend/src/main/java/com/campus/interaction/controller/InteractionController.java#L29) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/messages/conversations"} -->
| HTTP `GET /api/messages/conversations` | `MessageController` | — | [campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java:27](../campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java#L27) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/messages/conversations/{userId}"} -->
| HTTP `GET /api/messages/conversations/{userId}` | `MessageController` | — | [campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java:36](../campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java#L36) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/messages/conversations/{userId}/read"} -->
| HTTP `PUT /api/messages/conversations/{userId}/read` | `MessageController` | — | [campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java:56](../campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java#L56) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/messages/unread-count"} -->
| HTTP `GET /api/messages/unread-count` | `MessageController` | — | [campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java:46](../campus-forum-backend/src/main/java/com/campus/message/controller/MessageController.java#L46) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/notifications"} -->
| HTTP `GET /api/notifications` | `NotificationController` | — | [campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java:24](../campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java#L24) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/notifications/read-all"} -->
| HTTP `POST /api/notifications/read-all` | `NotificationController` | — | [campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java:57](../campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java#L57) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/notifications/unread-count"} -->
| HTTP `GET /api/notifications/unread-count` | `NotificationController` | — | [campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java:36](../campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java#L36) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/notifications/{id}/read"} -->
| HTTP `POST /api/notifications/{id}/read` | `NotificationController` | — | [campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java:46](../campus-forum-backend/src/main/java/com/campus/notification/controller/NotificationController.java#L46) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/posts"} -->
| HTTP `GET /api/posts` | `PostController` | — | [campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java:25](../campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java#L25) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/posts"} -->
| HTTP `POST /api/posts` | `PostController` | — | [campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java:41](../campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java#L41) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/posts/upload-image"} -->
| HTTP `POST /api/posts/upload-image` | `PostController` | — | [campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java:69](../campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java#L69) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:DELETE /api/posts/{id}"} -->
| HTTP `DELETE /api/posts/{id}` | `PostController` | — | [campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java:60](../campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java#L60) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/posts/{id}"} -->
| HTTP `GET /api/posts/{id}` | `PostController` | — | [campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java:33](../campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java#L33) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/posts/{id}"} -->
| HTTP `PUT /api/posts/{id}` | `PostController` | — | [campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java:50](../campus-forum-backend/src/main/java/com/campus/post/controller/PostController.java#L50) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/posts/{postId}/comments"} -->
| HTTP `GET /api/posts/{postId}/comments` | `CommentController` | — | [campus-forum-backend/src/main/java/com/campus/comment/controller/CommentController.java:24](../campus-forum-backend/src/main/java/com/campus/comment/controller/CommentController.java#L24) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/posts/{postId}/comments"} -->
| HTTP `POST /api/posts/{postId}/comments` | `CommentController` | — | [campus-forum-backend/src/main/java/com/campus/comment/controller/CommentController.java:33](../campus-forum-backend/src/main/java/com/campus/comment/controller/CommentController.java#L33) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:DELETE /api/posts/{postId}/comments/{id}"} -->
| HTTP `DELETE /api/posts/{postId}/comments/{id}` | `CommentController` | — | [campus-forum-backend/src/main/java/com/campus/comment/controller/CommentController.java:43](../campus-forum-backend/src/main/java/com/campus/comment/controller/CommentController.java#L43) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/search"} -->
| HTTP `GET /api/search` | `SearchController` | — | [campus-forum-backend/src/main/java/com/campus/search/controller/SearchController.java:25](../campus-forum-backend/src/main/java/com/campus/search/controller/SearchController.java#L25) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/users/me"} -->
| HTTP `GET /api/users/me` | `UserController` | — | [campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java:24](../campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java#L24) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:PUT /api/users/me"} -->
| HTTP `PUT /api/users/me` | `UserController` | — | [campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java:32](../campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java#L32) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:POST /api/users/me/avatar"} -->
| HTTP `POST /api/users/me/avatar` | `UserController` | — | [campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java:41](../campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java#L41) |
<!-- evidence-claim: {"kind":"node","id":"endpoint:GET /api/users/{id}"} -->
| HTTP `GET /api/users/{id}` | `UserController` | — | [campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java:50](../campus-forum-backend/src/main/java/com/campus/user/controller/UserController.java#L50) |

## Configuration

| Key | Declared in | Consumers |
| --- | --- | --- |
<!-- evidence-claim: {"kind":"node","id":"config:campus"} -->
| `campus` | [campus-forum-backend/src/main/resources/application.yml:31](../campus-forum-backend/src/main/resources/application.yml#L31) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.admin"} -->
| `campus.admin` | [campus-forum-backend/src/main/resources/application.yml:67](../campus-forum-backend/src/main/resources/application.yml#L67) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.admin.email"} -->
| `campus.admin.email` | [campus-forum-backend/src/main/resources/application.yml:69](../campus-forum-backend/src/main/resources/application.yml#L69) | `AdminInitializer` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.admin.password"} -->
| `campus.admin.password` | [campus-forum-backend/src/main/resources/application.yml:70](../campus-forum-backend/src/main/resources/application.yml#L70) | `AdminInitializer` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.admin.student-no"} -->
| `campus.admin.student-no` | [campus-forum-backend/src/main/resources/application.yml:68](../campus-forum-backend/src/main/resources/application.yml#L68) | `AdminInitializer` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai"} -->
| `campus.ai` | [campus-forum-backend/src/main/resources/application.yml:32](../campus-forum-backend/src/main/resources/application.yml#L32) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.api-key"} -->
| `campus.ai.api-key` | [campus-forum-backend/src/main/resources/application.yml:35](../campus-forum-backend/src/main/resources/application.yml#L35) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.base-url"} -->
| `campus.ai.base-url` | [campus-forum-backend/src/main/resources/application.yml:34](../campus-forum-backend/src/main/resources/application.yml#L34) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.chat-model"} -->
| `campus.ai.chat-model` | [campus-forum-backend/src/main/resources/application.yml:36](../campus-forum-backend/src/main/resources/application.yml#L36) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.document"} -->
| `campus.ai.document` | [campus-forum-backend/src/main/resources/application.yml:55](../campus-forum-backend/src/main/resources/application.yml#L55) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.document.max-file-size"} -->
| `campus.ai.document.max-file-size` | [campus-forum-backend/src/main/resources/application.yml:56](../campus-forum-backend/src/main/resources/application.yml#L56) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.embedding-dimension"} -->
| `campus.ai.embedding-dimension` | [campus-forum-backend/src/main/resources/application.yml:38](../campus-forum-backend/src/main/resources/application.yml#L38) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.embedding-model"} -->
| `campus.ai.embedding-model` | [campus-forum-backend/src/main/resources/application.yml:37](../campus-forum-backend/src/main/resources/application.yml#L37) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.enabled"} -->
| `campus.ai.enabled` | [campus-forum-backend/src/main/resources/application.yml:33](../campus-forum-backend/src/main/resources/application.yml#L33) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation"} -->
| `campus.ai.moderation` | [campus-forum-backend/src/main/resources/application.yml:47](../campus-forum-backend/src/main/resources/application.yml#L47) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation-enabled"} -->
| `campus.ai.moderation-enabled` | [campus-forum-backend/src/main/resources/application.yml:44](../campus-forum-backend/src/main/resources/application.yml#L44) | `AiModerationService`, `AsyncModerationService`, `CommentService`, `PostService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation-high-threshold"} -->
| `campus.ai.moderation-high-threshold` | [campus-forum-backend/src/main/resources/application.yml:46](../campus-forum-backend/src/main/resources/application.yml#L46) | `AiModerationService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation-medium-threshold"} -->
| `campus.ai.moderation-medium-threshold` | [campus-forum-backend/src/main/resources/application.yml:45](../campus-forum-backend/src/main/resources/application.yml#L45) | `AiModerationService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.cache-enabled"} -->
| `campus.ai.moderation.cache-enabled` | [campus-forum-backend/src/main/resources/application.yml:49](../campus-forum-backend/src/main/resources/application.yml#L49) | `ModerationResultCache` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.cache-ttl-seconds"} -->
| `campus.ai.moderation.cache-ttl-seconds` | [campus-forum-backend/src/main/resources/application.yml:50](../campus-forum-backend/src/main/resources/application.yml#L50) | `ModerationResultCache` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.mode"} -->
| `campus.ai.moderation.mode` | [campus-forum-backend/src/main/resources/application.yml:48](../campus-forum-backend/src/main/resources/application.yml#L48) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.prompt-cache-ttl-seconds"} -->
| `campus.ai.moderation.prompt-cache-ttl-seconds` | [campus-forum-backend/src/main/resources/application.yml:54](../campus-forum-backend/src/main/resources/application.yml#L54) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.rate-limit-global"} -->
| `campus.ai.moderation.rate-limit-global` | [campus-forum-backend/src/main/resources/application.yml:52](../campus-forum-backend/src/main/resources/application.yml#L52) | `ModerationRateLimiter` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.rate-limit-per-user"} -->
| `campus.ai.moderation.rate-limit-per-user` | [campus-forum-backend/src/main/resources/application.yml:51](../campus-forum-backend/src/main/resources/application.yml#L51) | `ModerationRateLimiter` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.moderation.rate-limit-window-seconds"} -->
| `campus.ai.moderation.rate-limit-window-seconds` | [campus-forum-backend/src/main/resources/application.yml:53](../campus-forum-backend/src/main/resources/application.yml#L53) | `ModerationRateLimiter` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.qa"} -->
| `campus.ai.qa` | [campus-forum-backend/src/main/resources/application.yml:40](../campus-forum-backend/src/main/resources/application.yml#L40) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.qa.max-sources"} -->
| `campus.ai.qa.max-sources` | [campus-forum-backend/src/main/resources/application.yml:41](../campus-forum-backend/src/main/resources/application.yml#L41) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.qa.min-score"} -->
| `campus.ai.qa.min-score` | [campus-forum-backend/src/main/resources/application.yml:42](../campus-forum-backend/src/main/resources/application.yml#L42) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.qa.rate-limit-per-hour"} -->
| `campus.ai.qa.rate-limit-per-hour` | [campus-forum-backend/src/main/resources/application.yml:43](../campus-forum-backend/src/main/resources/application.yml#L43) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.ai.timeout-ms"} -->
| `campus.ai.timeout-ms` | [campus-forum-backend/src/main/resources/application.yml:39](../campus-forum-backend/src/main/resources/application.yml#L39) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.cookie"} -->
| `campus.cookie` | [campus-forum-backend/src/main/resources/application.yml:64](../campus-forum-backend/src/main/resources/application.yml#L64) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.cookie.same-site"} -->
| `campus.cookie.same-site` | [campus-forum-backend/src/main/resources/application.yml:66](../campus-forum-backend/src/main/resources/application.yml#L66) | `AuthService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.cookie.secure"} -->
| `campus.cookie.secure` | [campus-forum-backend/src/main/resources/application.yml:65](../campus-forum-backend/src/main/resources/application.yml#L65) | `AuthService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.cors"} -->
| `campus.cors` | [campus-forum-backend/src/main/resources/application.yml:59](../campus-forum-backend/src/main/resources/application.yml#L59) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.cors.allowed-origins"} -->
| `campus.cors.allowed-origins` | [campus-forum-backend/src/main/resources/application.yml:60](../campus-forum-backend/src/main/resources/application.yml#L60) | `CorsConfig`, `WebSocketConfig` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.docs"} -->
| `campus.docs` | [campus-forum-backend/src/main/resources/application.yml:57](../campus-forum-backend/src/main/resources/application.yml#L57) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.docs.enabled"} -->
| `campus.docs.enabled` | [campus-forum-backend/src/main/resources/application.yml:58](../campus-forum-backend/src/main/resources/application.yml#L58) | `SecurityConfig` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.email"} -->
| `campus.email` | [campus-forum-backend/src/main/resources/application-dev.yml:40](../campus-forum-backend/src/main/resources/application-dev.yml#L40) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.email.expire"} -->
| `campus.email.expire` | [campus-forum-backend/src/main/resources/application-dev.yml:41](../campus-forum-backend/src/main/resources/application-dev.yml#L41) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt"} -->
| `campus.jwt` | [campus-forum-backend/src/main/resources/application.yml:71](../campus-forum-backend/src/main/resources/application.yml#L71) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt.access-expiration"} -->
| `campus.jwt.access-expiration` | [campus-forum-backend/src/main/resources/application-dev.yml:24](../campus-forum-backend/src/main/resources/application-dev.yml#L24) | `JwtUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt.access-secret"} -->
| `campus.jwt.access-secret` | [campus-forum-backend/src/main/resources/application-dev.yml:23](../campus-forum-backend/src/main/resources/application-dev.yml#L23) | `JwtUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt.access-ttl"} -->
| `campus.jwt.access-ttl` | [campus-forum-backend/src/main/resources/application.yml:72](../campus-forum-backend/src/main/resources/application.yml#L72) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt.refresh-expiration"} -->
| `campus.jwt.refresh-expiration` | [campus-forum-backend/src/main/resources/application-dev.yml:26](../campus-forum-backend/src/main/resources/application-dev.yml#L26) | `AuthService`, `JwtUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt.refresh-secret"} -->
| `campus.jwt.refresh-secret` | [campus-forum-backend/src/main/resources/application-dev.yml:25](../campus-forum-backend/src/main/resources/application-dev.yml#L25) | `JwtUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.jwt.refresh-ttl"} -->
| `campus.jwt.refresh-ttl` | [campus-forum-backend/src/main/resources/application.yml:73](../campus-forum-backend/src/main/resources/application.yml#L73) | `AuthService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.otp"} -->
| `campus.otp` | [campus-forum-backend/src/main/resources/application.yml:76](../campus-forum-backend/src/main/resources/application.yml#L76) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.otp.max-attempts"} -->
| `campus.otp.max-attempts` | [campus-forum-backend/src/main/resources/application.yml:78](../campus-forum-backend/src/main/resources/application.yml#L78) | `OtpStore` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.otp.resend-seconds"} -->
| `campus.otp.resend-seconds` | [campus-forum-backend/src/main/resources/application.yml:79](../campus-forum-backend/src/main/resources/application.yml#L79) | `OtpStore` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.otp.ttl"} -->
| `campus.otp.ttl` | [campus-forum-backend/src/main/resources/application.yml:77](../campus-forum-backend/src/main/resources/application.yml#L77) | `OtpStore` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.redis"} -->
| `campus.redis` | [campus-forum-backend/src/main/resources/application.yml:74](../campus-forum-backend/src/main/resources/application.yml#L74) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.redis.rt-prefix"} -->
| `campus.redis.rt-prefix` | [campus-forum-backend/src/main/resources/application.yml:75](../campus-forum-backend/src/main/resources/application.yml#L75) | `RefreshTokenStore` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.school-email-domain"} -->
| `campus.school-email-domain` | [campus-forum-backend/src/main/resources/application.yml:63](../campus-forum-backend/src/main/resources/application.yml#L63) | `AuthService` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.upload"} -->
| `campus.upload` | [campus-forum-backend/src/main/resources/application.yml:61](../campus-forum-backend/src/main/resources/application.yml#L61) | — |
<!-- evidence-claim: {"kind":"node","id":"config:campus.upload.allowed-types"} -->
| `campus.upload.allowed-types` | [campus-forum-backend/src/main/resources/application-dev.yml:38](../campus-forum-backend/src/main/resources/application-dev.yml#L38) | `FileUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.upload.base-url"} -->
| `campus.upload.base-url` | [campus-forum-backend/src/main/resources/application.yml:62](../campus-forum-backend/src/main/resources/application.yml#L62) | `FileUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.upload.max-size"} -->
| `campus.upload.max-size` | [campus-forum-backend/src/main/resources/application-dev.yml:39](../campus-forum-backend/src/main/resources/application-dev.yml#L39) | `FileUtil` |
<!-- evidence-claim: {"kind":"node","id":"config:campus.upload.path"} -->
| `campus.upload.path` | [campus-forum-backend/src/main/resources/application-dev.yml:36](../campus-forum-backend/src/main/resources/application-dev.yml#L36) | `FileUtil`, `WebMvcConfig` |
<!-- evidence-claim: {"kind":"node","id":"config:logging"} -->
| `logging` | [campus-forum-backend/src/main/resources/application.yml:28](../campus-forum-backend/src/main/resources/application.yml#L28) | — |
<!-- evidence-claim: {"kind":"node","id":"config:logging.config"} -->
| `logging.config` | [campus-forum-backend/src/main/resources/application.yml:29](../campus-forum-backend/src/main/resources/application.yml#L29) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus"} -->
| `mybatis-plus` | [campus-forum-backend/src/main/resources/application.yml:16](../campus-forum-backend/src/main/resources/application.yml#L16) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.configuration"} -->
| `mybatis-plus.configuration` | [campus-forum-backend/src/main/resources/application.yml:18](../campus-forum-backend/src/main/resources/application.yml#L18) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.configuration.log-impl"} -->
| `mybatis-plus.configuration.log-impl` | [campus-forum-backend/src/main/resources/application.yml:20](../campus-forum-backend/src/main/resources/application.yml#L20) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.configuration.map-underscore-to-camel-case"} -->
| `mybatis-plus.configuration.map-underscore-to-camel-case` | [campus-forum-backend/src/main/resources/application.yml:19](../campus-forum-backend/src/main/resources/application.yml#L19) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.global-config"} -->
| `mybatis-plus.global-config` | [campus-forum-backend/src/main/resources/application.yml:21](../campus-forum-backend/src/main/resources/application.yml#L21) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.global-config.db-config"} -->
| `mybatis-plus.global-config.db-config` | [campus-forum-backend/src/main/resources/application.yml:22](../campus-forum-backend/src/main/resources/application.yml#L22) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.global-config.db-config.id-type"} -->
| `mybatis-plus.global-config.db-config.id-type` | [campus-forum-backend/src/main/resources/application.yml:23](../campus-forum-backend/src/main/resources/application.yml#L23) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.global-config.db-config.logic-delete-field"} -->
| `mybatis-plus.global-config.db-config.logic-delete-field` | [campus-forum-backend/src/main/resources/application.yml:24](../campus-forum-backend/src/main/resources/application.yml#L24) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.global-config.db-config.logic-delete-value"} -->
| `mybatis-plus.global-config.db-config.logic-delete-value` | [campus-forum-backend/src/main/resources/application.yml:25](../campus-forum-backend/src/main/resources/application.yml#L25) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.global-config.db-config.logic-not-delete-value"} -->
| `mybatis-plus.global-config.db-config.logic-not-delete-value` | [campus-forum-backend/src/main/resources/application.yml:26](../campus-forum-backend/src/main/resources/application.yml#L26) | — |
<!-- evidence-claim: {"kind":"node","id":"config:mybatis-plus.mapper-locations"} -->
| `mybatis-plus.mapper-locations` | [campus-forum-backend/src/main/resources/application.yml:17](../campus-forum-backend/src/main/resources/application.yml#L17) | — |
<!-- evidence-claim: {"kind":"node","id":"config:server"} -->
| `server` | [campus-forum-backend/src/main/resources/application.yml:1](../campus-forum-backend/src/main/resources/application.yml#L1) | — |
<!-- evidence-claim: {"kind":"node","id":"config:server.port"} -->
| `server.port` | [campus-forum-backend/src/main/resources/application.yml:2](../campus-forum-backend/src/main/resources/application.yml#L2) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring"} -->
| `spring` | [campus-forum-backend/src/main/resources/application.yml:4](../campus-forum-backend/src/main/resources/application.yml#L4) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.data"} -->
| `spring.data` | [campus-forum-backend/src/main/resources/application-dev.yml:7](../campus-forum-backend/src/main/resources/application-dev.yml#L7) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.data.redis"} -->
| `spring.data.redis` | [campus-forum-backend/src/main/resources/application-dev.yml:8](../campus-forum-backend/src/main/resources/application-dev.yml#L8) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.data.redis.database"} -->
| `spring.data.redis.database` | [campus-forum-backend/src/main/resources/application-dev.yml:11](../campus-forum-backend/src/main/resources/application-dev.yml#L11) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.data.redis.host"} -->
| `spring.data.redis.host` | [campus-forum-backend/src/main/resources/application-dev.yml:9](../campus-forum-backend/src/main/resources/application-dev.yml#L9) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.data.redis.password"} -->
| `spring.data.redis.password` | [campus-forum-backend/src/main/resources/application-dev.yml:12](../campus-forum-backend/src/main/resources/application-dev.yml#L12) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.data.redis.port"} -->
| `spring.data.redis.port` | [campus-forum-backend/src/main/resources/application-dev.yml:10](../campus-forum-backend/src/main/resources/application-dev.yml#L10) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.datasource"} -->
| `spring.datasource` | [campus-forum-backend/src/main/resources/application-dev.yml:2](../campus-forum-backend/src/main/resources/application-dev.yml#L2) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.datasource.driver-class-name"} -->
| `spring.datasource.driver-class-name` | [campus-forum-backend/src/main/resources/application-dev.yml:6](../campus-forum-backend/src/main/resources/application-dev.yml#L6) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.datasource.password"} -->
| `spring.datasource.password` | [campus-forum-backend/src/main/resources/application-dev.yml:5](../campus-forum-backend/src/main/resources/application-dev.yml#L5) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.datasource.url"} -->
| `spring.datasource.url` | [campus-forum-backend/src/main/resources/application-dev.yml:3](../campus-forum-backend/src/main/resources/application-dev.yml#L3) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.datasource.username"} -->
| `spring.datasource.username` | [campus-forum-backend/src/main/resources/application-dev.yml:4](../campus-forum-backend/src/main/resources/application-dev.yml#L4) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.flyway"} -->
| `spring.flyway` | [campus-forum-backend/src/main/resources/application.yml:11](../campus-forum-backend/src/main/resources/application.yml#L11) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.flyway.baseline-on-migrate"} -->
| `spring.flyway.baseline-on-migrate` | [campus-forum-backend/src/main/resources/application.yml:14](../campus-forum-backend/src/main/resources/application.yml#L14) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.flyway.enabled"} -->
| `spring.flyway.enabled` | [campus-forum-backend/src/main/resources/application.yml:12](../campus-forum-backend/src/main/resources/application.yml#L12) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.flyway.locations"} -->
| `spring.flyway.locations` | [campus-forum-backend/src/main/resources/application.yml:13](../campus-forum-backend/src/main/resources/application.yml#L13) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail"} -->
| `spring.mail` | [campus-forum-backend/src/main/resources/application-dev.yml:13](../campus-forum-backend/src/main/resources/application-dev.yml#L13) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail.host"} -->
| `spring.mail.host` | [campus-forum-backend/src/main/resources/application-dev.yml:14](../campus-forum-backend/src/main/resources/application-dev.yml#L14) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail.password"} -->
| `spring.mail.password` | [campus-forum-backend/src/main/resources/application-dev.yml:17](../campus-forum-backend/src/main/resources/application-dev.yml#L17) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail.port"} -->
| `spring.mail.port` | [campus-forum-backend/src/main/resources/application-dev.yml:15](../campus-forum-backend/src/main/resources/application-dev.yml#L15) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail.properties"} -->
| `spring.mail.properties` | [campus-forum-backend/src/main/resources/application-dev.yml:18](../campus-forum-backend/src/main/resources/application-dev.yml#L18) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail.properties.mail.smtp.ssl.enable"} -->
| `spring.mail.properties.mail.smtp.ssl.enable` | [campus-forum-backend/src/main/resources/application-dev.yml:19](../campus-forum-backend/src/main/resources/application-dev.yml#L19) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.mail.username"} -->
| `spring.mail.username` | [campus-forum-backend/src/main/resources/application-dev.yml:16](../campus-forum-backend/src/main/resources/application-dev.yml#L16) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.profiles"} -->
| `spring.profiles` | [campus-forum-backend/src/main/resources/application.yml:5](../campus-forum-backend/src/main/resources/application.yml#L5) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.profiles.active"} -->
| `spring.profiles.active` | [campus-forum-backend/src/main/resources/application.yml:6](../campus-forum-backend/src/main/resources/application.yml#L6) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.servlet"} -->
| `spring.servlet` | [campus-forum-backend/src/main/resources/application.yml:7](../campus-forum-backend/src/main/resources/application.yml#L7) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.servlet.multipart"} -->
| `spring.servlet.multipart` | [campus-forum-backend/src/main/resources/application.yml:8](../campus-forum-backend/src/main/resources/application.yml#L8) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.servlet.multipart.max-file-size"} -->
| `spring.servlet.multipart.max-file-size` | [campus-forum-backend/src/main/resources/application.yml:9](../campus-forum-backend/src/main/resources/application.yml#L9) | — |
<!-- evidence-claim: {"kind":"node","id":"config:spring.servlet.multipart.max-request-size"} -->
| `spring.servlet.multipart.max-request-size` | [campus-forum-backend/src/main/resources/application.yml:10](../campus-forum-backend/src/main/resources/application.yml#L10) | — |

## Tests and integrations

| Production code | Tests or external systems |
| --- | --- |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.client.OpenAiCompatibleClient","relation":"calls_external_system","to":"external:HTTP client"} -->
| `OpenAiCompatibleClient` | `HTTP client` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.client.OpenAiCompatibleClient","relation":"tested_by","to":"com.campus.ai.client.OpenAiCompatibleClientTest"} -->
| `OpenAiCompatibleClient` | `OpenAiCompatibleClientTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.client.RecordedRequest","relation":"tested_by","to":"com.campus.ai.client.RecordedRequest"} -->
| `com.campus.ai.client.RecordedRequest` | `com.campus.ai.client.RecordedRequest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.AiModerationService","relation":"tested_by","to":"com.campus.ai.service.AiModerationServiceTest"} -->
| `AiModerationService` | `AiModerationServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.AiQuestionAnswerService","relation":"calls_external_system","to":"external:Redis"} -->
| `AiQuestionAnswerService` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.AiQuestionAnswerService","relation":"tested_by","to":"com.campus.ai.service.AiQuestionAnswerServiceTest"} -->
| `AiQuestionAnswerService` | `AiQuestionAnswerServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.AsyncModerationService","relation":"tested_by","to":"com.campus.ai.service.AsyncModerationServiceTest"} -->
| `AsyncModerationService` | `AsyncModerationServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.CountingProvider","relation":"tested_by","to":"com.campus.ai.service.CountingProvider"} -->
| `CountingProvider` | `CountingProvider` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.DocumentTextExtractor","relation":"tested_by","to":"com.campus.ai.service.DocumentTextExtractorTest"} -->
| `DocumentTextExtractor` | `DocumentTextExtractorTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.FailingProvider","relation":"tested_by","to":"com.campus.ai.service.FailingProvider"} -->
| `FailingProvider` | `FailingProvider` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.FakeProvider","relation":"tested_by","to":"com.campus.ai.service.FakeProvider"} -->
| `FakeProvider` | `FakeProvider` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.KnowledgeIngestionService","relation":"tested_by","to":"com.campus.ai.service.KnowledgeIngestionServiceTest"} -->
| `KnowledgeIngestionService` | `KnowledgeIngestionServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.ModerationMetrics","relation":"tested_by","to":"com.campus.ai.service.ModerationMetricsTest"} -->
| `ModerationMetrics` | `ModerationMetricsTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.ModerationRateLimiter","relation":"tested_by","to":"com.campus.ai.service.ModerationRateLimiterTest"} -->
| `ModerationRateLimiter` | `ModerationRateLimiterTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.ModerationResultCache","relation":"tested_by","to":"com.campus.ai.service.ModerationResultCacheTest"} -->
| `ModerationResultCache` | `ModerationResultCacheTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.StubProvider","relation":"tested_by","to":"com.campus.ai.service.StubProvider"} -->
| `StubProvider` | `StubProvider` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.ai.service.TextChunker","relation":"tested_by","to":"com.campus.ai.service.TextChunkerTest"} -->
| `TextChunker` | `TextChunkerTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.AdminInitializer","relation":"tested_by","to":"com.campus.auth.service.AdminInitializerTest"} -->
| `AdminInitializer` | `AdminInitializerTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.AuthService","relation":"calls_external_system","to":"external:Redis"} -->
| `AuthService` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.AuthService","relation":"tested_by","to":"com.campus.auth.service.AuthServiceTest"} -->
| `AuthService` | `AuthServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.AuthServiceTest","relation":"calls_external_system","to":"external:Redis"} -->
| `AuthServiceTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.OtpBruteForceTest","relation":"calls_external_system","to":"external:Redis"} -->
| `OtpBruteForceTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.SchoolEmailTest","relation":"calls_external_system","to":"external:Redis"} -->
| `SchoolEmailTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.service.TokenVersionRevokeTest","relation":"calls_external_system","to":"external:Redis"} -->
| `TokenVersionRevokeTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.token.OtpStore","relation":"calls_external_system","to":"external:Redis"} -->
| `OtpStore` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.token.RefreshTokenStore","relation":"calls_external_system","to":"external:Redis"} -->
| `RefreshTokenStore` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.token.RefreshTokenStore","relation":"tested_by","to":"com.campus.auth.token.RefreshTokenStoreTest"} -->
| `RefreshTokenStore` | `RefreshTokenStoreTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.auth.token.RefreshTokenStoreTest","relation":"calls_external_system","to":"external:Redis"} -->
| `RefreshTokenStoreTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.board.service.BoardService","relation":"tested_by","to":"com.campus.board.service.BoardServiceTest"} -->
| `BoardService` | `BoardServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.ratelimit.DummyController","relation":"tested_by","to":"com.campus.common.ratelimit.DummyController"} -->
| `DummyController` | `DummyController` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.ratelimit.RateLimitInterceptor","relation":"calls_external_system","to":"external:Redis"} -->
| `RateLimitInterceptor` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.ratelimit.RateLimitInterceptor","relation":"tested_by","to":"com.campus.common.ratelimit.RateLimitInterceptorTest"} -->
| `RateLimitInterceptor` | `RateLimitInterceptorTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.ratelimit.RateLimitInterceptorTest","relation":"calls_external_system","to":"external:Redis"} -->
| `RateLimitInterceptorTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.security.SecurityTest","relation":"calls_external_system","to":"external:Redis"} -->
| `SecurityTest` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.util.FileUtil","relation":"tested_by","to":"com.campus.common.util.FileUtilTest"} -->
| `FileUtil` | `FileUtilTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.util.HtmlSanitizer","relation":"tested_by","to":"com.campus.common.util.HtmlSanitizerTest"} -->
| `HtmlSanitizer` | `HtmlSanitizerTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.common.util.RedisUtil","relation":"calls_external_system","to":"external:Redis"} -->
| `RedisUtil` | `Redis` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.interaction.service.FavoriteService","relation":"tested_by","to":"com.campus.interaction.service.FavoriteServiceTest"} -->
| `FavoriteService` | `FavoriteServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.interaction.service.LikeService","relation":"tested_by","to":"com.campus.interaction.service.LikeServiceTest"} -->
| `LikeService` | `LikeServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.message.service.MessageService","relation":"tested_by","to":"com.campus.message.service.MessageServiceTest"} -->
| `MessageService` | `MessageServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.search.service.SearchService","relation":"tested_by","to":"com.campus.search.service.SearchServiceTest"} -->
| `SearchService` | `SearchServiceTest` |
<!-- evidence-claim: {"kind":"relationship","from":"com.campus.user.service.UserService","relation":"tested_by","to":"com.campus.user.service.UserServiceTest"} -->
| `UserService` | `UserServiceTest` |
<!-- auto-generated: code-guide:end -->
