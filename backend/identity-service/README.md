# identity-service

Service quản lý tài khoản và xác thực: đăng ký/đăng nhập (mật khẩu và Google), cấp JWT qua cookie httpOnly, quản lý user/role cho trang admin và audit log.

| Mục | Giá trị |
|---|---|
| Port | `9898` (client gọi qua gateway `9191`) |
| Database | MySQL `user_db` (mặc định `localhost:3307`) |
| Redis | refresh token + cache user (mặc định `localhost:6319`) |
| Đăng ký Eureka | `IDENTITY-SERVICE` |

## Chạy local

1. Bật hạ tầng: `cd backend/env && docker-compose -f docker-compose-dev.yml up -d` (MySQL, Redis, Kafka).
2. Chạy theo thứ tự: `service-registry` (8761) -> `api-gateway` (9191) -> `identity-service`.
3. Lần đầu MySQL chạy, `env/mysql/init/init.sql` tạo bảng và seed dữ liệu (xem mục Lưu ý).

Cấu hình trong `src/main/resources/application.properties`, đọc từ biến môi trường và có giá trị mặc định cho dev:

| Biến | Mặc định | Ghi chú |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3307/user_db` | |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | `root` / `123456` | |
| `SPRING_DATA_REDIS_HOST` / `PORT` | `localhost` / `6319` | |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | |
| `JWT_SECRET` | key dev trong file | **Phải trùng với api-gateway** |
| `JWT_ACCESS_TTL` | `15m` | |
| `JWT_REFRESH_TTL` | `7d` | |
| `JWT_COOKIE_SECURE` | `false` | Đặt `true` khi chạy HTTPS |
| `GOOGLE_CLIENT_ID` | client id dev trong file | |
| `GOOGLE_CLIENT_SECRET` | rỗng | Không commit secret |
| `GOOGLE_REDIRECT_URI` | `postmessage` | Dùng `postmessage` cho popup flow (`@react-oauth/google`) |

Secret Google: đặt biến môi trường `GOOGLE_CLIENT_SECRET`, hoặc copy `application-local.properties.example` thành `application-local.properties`. File này đã gitignore và được nạp qua `spring.config.import`.

## Gateway

- Route `identity-service` trong `api-gateway/src/main/resources/application.yml`: `Path=/api/v1/user/**` -> `lb://IDENTITY-SERVICE`. Route này không gắn filter.
- Gateway đọc cookie `access_token` và dùng cùng `JWT_SECRET` để verify token ở các route khác (`AuthenticationFilter`, `RoleBasedAccessFilter`).
- Bản thân identity-service cũng tự verify cookie bằng `JwtAuthenticationFilter`, không truy vấn DB.

## Endpoint

Tất cả nằm dưới `/api/v1/user`. Response bọc trong `ApiResponse`, trừ những endpoint trả `204`.

| Method | Path | Ai gọi | Mô tả |
|---|---|---|---|
| POST | `/auth/register` | Public | Tự đăng ký, luôn là `CUSTOMER` (201) |
| POST | `/auth/login` | Public | Body `{username, password}`, set 2 cookie, trả thông tin user |
| POST | `/auth/google` | Public | Body `{code}` (authorization code), tìm user theo email, chưa có thì tạo `CUSTOMER` |
| POST | `/auth/refresh` | Public (cần cookie `refresh_token`) | Xoay refresh token, cấp access token mới (204) |
| POST | `/auth/logout` | Public | Thu hồi refresh token, xoá cookie (204). Public để logout được cả khi access token đã hết hạn |
| GET | `/auth/me` | Đã đăng nhập | Thông tin user hiện tại (cache Redis, không có thì đọc DB) |
| GET | `/admin/users?page=0&size=10` | Quyền `user:read` | Danh sách user chưa xoá (size tối đa 100) |
| POST | `/admin/users` | Quyền `user:create` | Tạo user với `roleCodes` |
| PUT | `/admin/users/{id}` | Quyền `user:update` | Sửa name/email/roles (`roleCodes` null thì giữ role cũ) |
| DELETE | `/admin/users/{id}` | Quyền `user:delete` | Xoá mềm |
| GET | `/roles` | ADMINISTRATOR / EMPLOYEE | Danh sách role cho form admin |
| GET | `/audit-logs/recent?limit=50` | Quyền `user:read` | Audit log gần nhất (tối đa 50), trang Audit Log của admin |

Quy tắc chung trong `config/AuthConfig`: các route auth và actuator/swagger là `permitAll`, `/auth/me` cần đăng nhập, mọi route còn lại cần role `ADMINISTRATOR` hoặc `EMPLOYEE`. Quyền chi tiết kiểm bằng `@PreAuthorize` trên từng method.

## Luồng đăng nhập và token

| Cookie | Nội dung | TTL | SameSite | Path |
|---|---|---|---|---|
| `access_token` | JWT HS256 chứa `sub`=userId, username, email, roles, permissions | 15m | Lax | `/` |
| `refresh_token` | Chuỗi ngẫu nhiên (opaque), lưu Redis dạng SHA-256 hash | 7d | Strict | `/api/v1/user/auth` |

Cả hai cookie đều `httpOnly`; `secure` theo `JWT_COOKIE_SECURE`. Token không bao giờ nằm trong body.

```
login -> xác thực (password/Google) -> JWT + refresh token -> Set-Cookie x2
request -> gateway/identity verify access_token (không gọi DB)
access_token hết hạn -> 401 -> FE gọi POST /auth/refresh
refresh: rotate token cũ (xoá) -> nạp lại user + roles từ DB -> cấp cặp cookie mới
logout: xoá refresh token trong Redis + xoá cookie
```

Redis key (`security/RefreshTokenService`):
- `refresh:{hash}` -> userId: token đang dùng.
- `refresh:used:{hash}` -> userId: token đã xoay.
- `refresh:user:{userId}`: tập các hash đang dùng của user.

Reuse detection: nếu client gửi lại refresh token đã xoay, hệ thống ghi log cảnh báo, thu hồi toàn bộ refresh token của user đó và trả 401. Khi refresh lỗi, cookie bị xoá. User đã bị xoá thì refresh bị từ chối.

## Role và permission

Seed trong `env/mysql/init/init.sql`:

| Role | Permission |
|---|---|
| `ADMINISTRATOR` | Tất cả: `user:read/create/update/delete`, `product:read/create/update/delete` |
| `EMPLOYEE` | `product:read/create/update/delete` |
| `CUSTOMER` | Không có |

- Authorities được tạo bởi `Authorities.toGrantedAuthorities`: mỗi role thành `ROLE_<role>` (ví dụ `ROLE_ADMINISTRATOR`), mỗi permission giữ nguyên chuỗi (ví dụ `user:read`). Dùng `hasAuthority('user:read')`.
- Quy tắc nghiệp vụ (`service/impl/UserServiceImpl`):
  - `CUSTOMER` không được gộp với role khác, và không đổi qua lại giữa customer và staff.
  - Phải có ít nhất 1 role. Role không tồn tại trả 404.
  - Không tự xoá chính mình; không xoá hoặc gỡ role ADMINISTRATOR của admin cuối cùng; không tự gỡ role ADMINISTRATOR của mình.
  - Tên và email phải duy nhất (email được chuẩn hoá lowercase).
  - Xoá mềm: `del_flg=1`, name/email được thêm tiền tố `deleted_{id}_` để nhả ràng buộc unique.
  - Đổi role hoặc xoá user thì xoá cache user và thu hồi toàn bộ refresh token.
- Audit: method gắn `@Audit` (hiện có `USER_UPDATE`, `USER_DELETE`) ghi vào bảng `audit_logs` sau khi thành công (`audit/AuditAspect`).

## Cấu trúc package (`net.javaguides.identity_service`)

```
config/      AuthConfig (security chain, BCrypt), JwtProperties, RedisConfig
security/    JwtService (ký), JwtVerifier (verify), JwtAuthenticationFilter,
             AuthCookieFactory, RefreshTokenService, CustomUserDetails(Service),
             Authorities, JwtClaims, AuthUser
controller/  Auth, UserAdmin, Role, AuditLog
service/     AuthService, UserService, UserCacheService,
             GoogleOAuth2Service (impl/ chứa bản cài đặt)
audit/       @Audit, AuditAspect, AuditLogService
handler/     CustomAuthenticationEntryPoint (401), CustomAccessDeniedHandler (403)
exception/   GlobalExceptionHandler + exception tự định nghĩa
entity/ repository/ dto/ enums/ (ERole)
```

## Mã lỗi

Lỗi trả về dạng `ApiResponse.error(message)` (`exception/GlobalExceptionHandler`):

| Status | Khi nào |
|---|---|
| 400 | Validation lỗi, body sai, `IllegalArgumentException` (vi phạm quy tắc role, tự xoá mình, ...) |
| 401 | Sai username/password (message chung "Invalid username or password"), `AuthException` (refresh token sai/hết hạn/đã dùng, Google email chưa verify), chưa đăng nhập |
| 403 | Thiếu quyền (`AccessDeniedException`) |
| 404 | User hoặc role không tồn tại |
| 409 | Username hoặc email đã tồn tại |
| 500 | Lỗi không xác định ("Internal server error") |

## Lưu ý và hạn chế

- Đổi role có hiệu lực khi user refresh: access token cũ vẫn dùng được tối đa 15 phút, vì quyền nằm trong JWT.
- `JWT_SECRET` của identity-service và api-gateway phải giống nhau; key mặc định chỉ dùng cho dev, production phải đặt riêng.
- `spring.jpa.hibernate.ddl-auto=update`, nhưng role/permission/admin chỉ có từ `init.sql`. Script này chỉ chạy khi thư mục data MySQL còn trống. Nếu thiếu seed, tạo user sẽ lỗi "Role not found, check seed data in init.sql".
- Tài khoản admin dev seed sẵn: username `admin`, email `admin123@gmail.com` (đăng nhập bằng username, không phải email). Mật khẩu là hash BCrypt trong `init.sql`.
- `/roles` chỉ kiểm role ADMINISTRATOR/EMPLOYEE, chưa có permission riêng.
- Tài liệu thiết kế chi tiết hơn: `backend/AUTH_DESIGN.md`. Khi khác code thì code là chuẩn.
