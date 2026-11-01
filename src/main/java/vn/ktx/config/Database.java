package vn.ktx.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/QuanLyKyTucXa?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh";

    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv().getOrDefault("KTX_DB_URL", DEFAULT_URL);
        String user = System.getenv().getOrDefault("KTX_DB_USER", "root");
        String password = System.getenv().getOrDefault("KTX_DB_PASSWORD", "");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("Không tìm thấy MySQL Connector/J. Hãy chạy ứng dụng bằng Maven.", exception);
        }
        java.util.Properties properties = new java.util.Properties();
        properties.setProperty("user", user);
        properties.setProperty("password", password);
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "15000");
        return DriverManager.getConnection(url, properties);
    }
}
