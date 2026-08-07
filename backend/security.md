

- Request đến gate way thì có 1 lớp filter.
- Khi các request gọi đến identify-service thì cũng có một lơp filter security.
- Khi refresh trang thì sẽ gọi service get ME . Truoc khi gọi thì nó sẽ filter security, nếu token chưa hết hạn hoac hợp lệ 
thì mới được gọi service GET ME .