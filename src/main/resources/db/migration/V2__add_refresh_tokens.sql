CREATE TABLE refresh_token (
    `jti` VARCHAR(255) NOT NULL,
    `user_name` varchar(255) NOT NULL,
    `expiration_time` datetime(6) NOT NULL,
    `is_revoked` BIT DEFAULT 0,
    PRIMARY KEY (`jti`),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (`user_name`)
        REFERENCES `user` (`user_name`)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_uca1400_ai_ci;;

ALTER TABLE `user` ADD COLUMN role VARCHAR(50) DEFAULT "USER";