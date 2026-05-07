package com.hospital.registration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbInitializer {
    public static void main(String[] args) throws Exception {
        String password = args.length > 0 ? args[0] : System.getenv("DB_PASSWORD");
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("请通过 -Dexec.args 或 DB_PASSWORD 提供本机 MySQL 密码");
        }
        String sql = Files.readString(Path.of("数据库初始化脚本", "init.sql"), StandardCharsets.UTF_8);
        try (Connection connection = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false",
                "root",
                password
        ); Statement statement = connection.createStatement()) {
            for (String command : sql.split(";\\s*(\\r?\\n|$)")) {
                String trimmed = command.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        }
        System.out.println("database initialized");
    }
}
