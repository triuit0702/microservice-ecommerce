# Access Token & Refresh Token (sơ đồ dễ hiểu)

## 1. Hai loại token là gì?

| | Access token | Refresh token |
|---|---|---|
| Dạng | JWT (ký HS256) | Chuỗi ngẫu nhiên 32 byte (không phải JWT) |
| Sống bao lâu | **15 phút** | **7 ngày** |
| Dùng để | Gọi API (chứng minh "tôi là ai") | Xin access token mới khi cái cũ hết hạn |
| Bên trong có | userId, email, username, roles, permissions | Không có gì, chỉ là chuỗi random |
| Lưu ở server? | ❌ Không lưu (tự verify bằng chữ ký) | ✅ Lưu **hash SHA-256** trong Redis |
| Cookie | `access_token`, path `/`, SameSite **Lax** | `refresh_token`, path `/api/v1/user/auth`, SameSite **Strict** |

Cả hai đều là cookie **httpOnly**: JavaScript không đọc được, browser tự gửi kèm request.

```
Access token  = vé vào cổng, hết hạn nhanh, ai cầm cũng kiểm tra được bằng chữ ký
Refresh token = phiếu đổi vé, sống lâu, chỉ identity-service đổi được (tra Redis)
```

---

## 2. Login: nhận 2 token

```
Browser
   │  POST /api/v1/user/auth/login {email, password}
   ▼
Gateway  (route /user/** không check token)
   │
   ▼
identity-service
   │
   ├─ 1. Kiểm tra email + password (BCrypt)        sai → 401
   │
   ├─ 2. JwtService tạo access token (15 phút)
   │
   ├─ 3. RefreshTokenService tạo refresh token (7 ngày)
   │        └─► Redis:  refresh:{hash}       = userId
   │                    refresh:user:{userId} = {hash, ...}
   │
   └─ 4. AuthCookieFactory ghi 2 cookie
            │
            ▼
Browser lưu cookie: access_token, refresh_token
```

---

## 3. Gọi API bình thường (access token còn hạn)

```
Browser  ── GET /api/v1/order  (tự gửi cookie access_token) ──►  Gateway
                                                                    │
                                     verify chữ ký + hạn + issuer   │
                                     (không cần gọi DB / Redis)     │
                                                                    ▼
                                         gắn X-User-Id, X-User-Email
                                                                    │
                                                                    ▼
                                                              order-service
```

> Refresh token **không** được gửi ở đây vì cookie của nó chỉ có path `/api/v1/user/auth`.

---

## 4. Access token hết hạn → tự refresh

Frontend làm việc này tự động trong `frontend/client/src/services/AxiosClient.js`, người dùng không thấy gì.

```
Browser                         Gateway / identity-service             Redis
   │                                    │                                │
   │── GET /api/v1/order ──────────────►│                                │
   │◄────────── 401 (token hết hạn) ────│                                │
   │                                    │                                │
   │  axios interceptor bắt 401         │                                │
   │                                    │                                │
   │── POST /auth/refresh ─────────────►│                                │
   │   (gửi cookie refresh_token)       │── tìm refresh:{hash} & xoá ───►│
   │                                    │◄────────── userId ─────────────│
   │                                    │── đánh dấu refresh:used:{hash}►│
   │                                    │                                │
   │                                    │  load lại user + roles từ DB   │
   │                                    │  tạo access token MỚI          │
   │                                    │  tạo refresh token MỚI ───────►│
   │◄──── Set-Cookie: 2 cookie mới ─────│                                │
   │                                    │                                │
   │── GET /api/v1/order (gửi lại) ────►│                                │
   │◄────────── 200 OK ─────────────────│                                │
```

Điểm cần nhớ:
- Mỗi lần refresh thì **cả 2 token đều đổi mới** (gọi là *rotation*). Refresh token cũ bị xoá, không dùng lại được.
- Roles được **load lại từ DB** khi refresh, nên đổi role thì tối đa 15 phút sau user mới bị ảnh hưởng.
- Nhiều request cùng bị 401 một lúc thì frontend chỉ gọi refresh **1 lần** (dùng chung `refreshPromise`), các request kia chờ.
- Frontend không refresh khi đang gọi `/auth/login`, `/auth/register`, `/auth/google`, `/auth/refresh`, `/auth/logout`, để tránh vòng lặp.

---

## 5. Refresh token cũng hết hạn / không hợp lệ

```
POST /auth/refresh
   │
   ▼
Redis không có refresh:{hash}
   │
   ▼
identity-service xoá 2 cookie + trả 401
   │
   ▼
Frontend chuyển về trang /login
```

---

## 6. Phát hiện refresh token bị đánh cắp (reuse detection)

Nếu ai đó dùng lại một refresh token **đã đổi rồi**:

```
Hacker gửi refresh token cũ
   │
   ▼
Redis: không có refresh:{hash}, nhưng CÓ refresh:used:{hash}
   │
   ▼
=> token này đã bị dùng 1 lần rồi → có người đang dùng trộm
   │
   ▼
revokeAll(userId): xoá TẤT CẢ refresh token của user đó
   │
   ▼
Cả hacker lẫn user thật đều phải login lại
```

---

## 7. Logout

```
Browser ── POST /auth/logout (cookie refresh_token) ──► identity-service
                                                           │
                                                           ├─► Redis: xoá refresh:{hash}
                                                           │
                                                           └─► xoá 2 cookie (Max-Age=0)
```

> Access token **không thu hồi được** (không lưu ở server). Nếu đã bị copy ra ngoài thì nó vẫn dùng được tới khi hết 15 phút.
> Vì vậy access token được để sống ngắn.

---

## 8. Dữ liệu trong Redis

```
refresh:{hash}          → userId           token đang dùng được       (TTL 7 ngày)
refresh:used:{hash}     → userId           token đã đổi, để bắt reuse (TTL 7 ngày)
refresh:user:{userId}   → {hash1, hash2}   các token của 1 user (nhiều thiết bị), dùng cho revokeAll
```

Redis chỉ lưu **hash**, không lưu token gốc → lộ Redis cũng không lấy được token để dùng.

---

## 9. Code nằm ở đâu

| Việc | File |
|---|---|
| Endpoint login / refresh / logout / me | `identity-service/.../controller/AuthController.java` |
| Logic login / refresh / logout | `identity-service/.../service/impl/AuthServiceImpl.java` |
| Tạo access token | `identity-service/.../security/JwtService.java` |
| Verify access token | `identity-service/.../security/JwtVerifier.java`, `api-gateway/.../security/JwtVerifier.java` |
| Refresh token + Redis | `identity-service/.../security/RefreshTokenService.java` |
| Ghi / xoá / đọc cookie | `identity-service/.../security/AuthCookieFactory.java` |
| Cấu hình thời gian sống | `identity-service/src/main/resources/application.properties` (`jwt.access-ttl`, `jwt.refresh-ttl`) |
| Frontend tự refresh khi 401 | `frontend/client/src/services/AxiosClient.js` |
