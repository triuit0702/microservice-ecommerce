CREATE TABLE user_db.role_permissions (
    role_id BIGINT,
    permission_id BIGINT,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
);
INSERT INTO user_db.permissions (id, name) VALUES (1, 'USER_VIEW'); -- VIEW
INSERT INTO user_db.permissions (id, name) VALUES (2, 'USER_CREATE'); -- CREATE
INSERT INTO user_db.permissions (id, name) VALUES (3, 'USER_UPDATE'); -- UPDATE
INSERT INTO user_db.permissions (id, name) VALUES (4, 'USER_DELETE'); -- delete


-- ADMIN có toàn quyền user
INSERT INTO user_db.role_permissions (role_id, permission_id) VALUES(1, 1),(1, 2), (1, 3), (1, 4);

-- EMPLOYEE chỉ được xem, tạo mới, chỉnh sửa
INSERT INTO user_db.role_permissions (role_id, permission_id) VALUES (2, 1),(2, 5);



-- chay từng dòng sql thi mới được