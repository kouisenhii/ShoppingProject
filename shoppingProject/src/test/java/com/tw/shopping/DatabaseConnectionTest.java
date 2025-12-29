package com.tw.shopping;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 資料庫連線測試工具。
 * * 運行此測試將會使用 application.properties 中配置的資料庫設定。
 * 如果連線成功，測試會通過。如果連線失敗，將會拋出異常並顯示錯誤訊息。
 */
@SpringBootTest
public class DatabaseConnectionTest {

    // Spring Boot 會自動注入配置好的 JdbcTemplate 實例
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseConnectionIsSuccessful() {
        System.out.println("--- 開始執行資料庫連線測試 ---");
        
        // 1. 檢查 JdbcTemplate 是否被正確創建
        assertNotNull(jdbcTemplate, "JdbcTemplate 應已被 Spring Boot 注入。");
        System.out.println("JdbcTemplate 注入成功。");

        try {
            // 2. 執行一個簡單的 SQL 查詢來驗證連線是否生效
            // 查詢當前資料庫系統的時間，這是所有資料庫都支援的基本操作
            String sql = "SELECT 1"; 
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class);

            // 3. 驗證查詢結果
            assertNotNull(result, "查詢結果不應為 null。");
            assertTrue(result == 1, "查詢結果應為 1。");

            System.out.println(">>> 資料庫連線成功！連線到: " + getDatabaseUrl());
            System.out.println("--- 資料庫連線測試通過 ---");

        } catch (Exception e) {
            System.err.println("!!! 資料庫連線失敗 !!!");
            System.err.println("請檢查您的 application.properties 中的連線配置 (spring.datasource.url/username/password)。");
            System.err.println("失敗原因: " + e.getMessage());
            // 拋出異常以使測試失敗
            throw new RuntimeException("資料庫連線驗證失敗", e);
        }
    }

    /**
     * 嘗試從 JdbcTemplate 獲取連線 URL，用於輸出訊息。
     * 實際在生產環境中，您會直接從配置中讀取。
     */
    private String getDatabaseUrl() {
        if (jdbcTemplate.getDataSource() != null) {
            try {
                return jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            } catch (Exception ignore) {
                // 如果獲取失敗則返回一個預設值
                return "無法取得 URL，請檢查配置。";
            }
        }
        return "DataSource is null";
    }
}
