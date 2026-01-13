CREATE TABLE `user` (
    user_name VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    saved_time DATETIME,
    PRIMARY KEY (user_name)
);
