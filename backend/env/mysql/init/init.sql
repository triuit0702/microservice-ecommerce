CREATE DATABASE IF NOT EXISTS user_db;
CREATE DATABASE IF NOT EXISTS product_db;
CREATE DATABASE IF NOT EXISTS order_db;


use user_db;

CREATE TABLE `audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `resource` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` enum('ADMINISTRATOR','CUSTOMER','EMPLOYEE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKofx66keruapi6vyqpv6f2or37` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `role_permissions` (
  `role_id` bigint NOT NULL,
  `permission_id` bigint NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `FKegdk29eiy7mdtefy5c7eirr6e` (`permission_id`),
  CONSTRAINT `FKegdk29eiy7mdtefy5c7eirr6e` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `FKn5fotdgk8d1xvo8nav9uv3muc` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `user_credentials` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `del_flg` bit(1) NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `FKh8ciramu9cc9q3qcqiv4ue8a6` (`role_id`),
  CONSTRAINT `FKh8ciramu9cc9q3qcqiv4ue8a6` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FKqin6aw0mf2bud9ko2gb1rm15j` FOREIGN KEY (`user_id`) REFERENCES `user_credentials` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;





INSERT INTO roles (name) VALUES ('EMPLOYEE'), ('ADMINISTRATOR'), ('CUSTOMER');

INSERT INTO permissions (name)
VALUES
    ('USER_VIEW'),
    ('USER_CREATE'),
    ('USER_UPDATE'),
    ('USER_DELETE'),

    ('PRODUCT_VIEW'),
    ('PRODUCT_CREATE'),
    ('PRODUCT_UPDATE'),
    ('PRODUCT_DELETE');


-- *** Gán permission cho role
-- ADMIN CÓ TOÀN BỘ QUYỀN

 INSERT INTO role_permissions (role_id, permission_id)
 SELECT r.id, p.id
 FROM roles r
 CROSS JOIN permissions p
 WHERE r.name = 'ADMINISTRATOR';

-- EMPLOYEE → quản lý Product

 INSERT INTO role_permissions (role_id, permission_id)
 SELECT r.id, p.id
 FROM roles r
 JOIN permissions p
     ON p.name IN (
         'PRODUCT_VIEW',
         'PRODUCT_CREATE',
         'PRODUCT_UPDATE',
         'PRODUCT_DELETE'
     )
 WHERE r.name = 'EMPLOYEE';


 INSERT INTO user_credentials
 (del_flg, email, last_login_at, name, password)
 VALUES( 0, 'admin123@gmail.com', NULL, 'admin', '$2a$10$1R/n7wAqPPXx6nzPlqVGh.sHrQgCw.8KAgRyGlGBv5h2jYZ3wL0ou');

 INSERT INTO user_roles (user_id, role_id)
 SELECT
     u.id,
     r.id
 FROM user_credentials u
 JOIN roles r
     ON r.name = 'ADMINISTRATOR'
 WHERE u.email = 'admin123@gmail.com';
