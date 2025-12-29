#  Spring Boot Dependencies 說明

本專案使用以下主要 Spring Boot 相關套件與依賴，涵蓋資料存取、Web API、資料驗證與開發效率提升模組：

---

##  主要依賴列表

| 套件名稱               | 說明                                                                 |
|------------------------|----------------------------------------------------------------------|
| **Lombok**             | 精簡 Java 程式碼，透過註解自動產生 getter/setter、建構子等樣板程式碼。 |
| **MySQL Driver**       | 提供與 MySQL 資料庫連線的 JDBC 驅動程式。                            |
| **Spring Data JDBC**   | 使用 JDBC 方式存取資料庫，適合簡單資料模型。                         |
| **Spring Data JPA**    | 使用 JPA 與 Hibernate 進行 ORM 映射，支援複雜關聯與查詢。             |
| **Spring Web**         | 建立 RESTful API，處理 HTTP 請求與回應。                             |
| **Spring Boot Validation** | 提供資料驗證功能，搭配 `@Valid`、`@NotNull` 等註解使用。         |

---