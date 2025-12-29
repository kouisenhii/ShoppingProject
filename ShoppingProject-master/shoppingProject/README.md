# shoppingProject

## 專案結構

```text
shoppingProject/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/tw/shopping/(name)/     # 自己在shopping後面建一個package，並將各自的檔案照下方結構放置
│   │   │       ├── config/                 # 系統層級、全域性，設定檔
│   │   │       ├── controller/             # 控制器層，處理 HTTP 請求與回應
│   │   │       ├── entity/                 # 資料模型，對應資料庫表格
│   │   │       ├── dto/                    # 資料傳輸物件
│   │   │       ├── repository/             # 資料存取層，使用 Spring Data JPA
│   │   │       ├── service/                # 商業邏輯層，處理核心邏輯
│   │   │       ├── util/                   # 工具類別，輔助功能模組
│   │   │       └── ShoppingProjectApplication.java # 主程式入口
│   │   └── resources/
│   │       ├── static/                     # 靜態資源 (HTML、CSS、JS)
│   │       └── application.properties      # Spring Boot 設定檔
│   └── test/
│       └── java/com/tw/shopping/           # 測試類別
├── pom.xml                                 # Maven 設定檔
├── mvnw
└── mvnw.cmd                                # Maven Wrapper


---

## 各 Package 說明

| Package     | 用途                        | 建議檔案                                          |
|-------------|-----------------------------|--------------------------------------------------|
| config      | 系統層級、全域性，設定檔      | -                                                |
| controller  | 控制器層，處理 API 請求與回應 | `ShoppingController.java`, `UserController.java` |
| entity      | 資料模型，對應資料庫表格      | `Product.java`, `User.java`, `Order.java`        |
| dto         | 資料傳輸物件                 | `UserResponse.java`, `ProductResponse.java`      |        
| repository  | 資料存取層，定義 JPA 介面     | `ProductRepository.java`, `UserRepository.java   |
| service     | 商業邏輯層，封裝核心功能      | `ProductService.java`, `UserService.java`        |
| util        | 工具類別，輔助功能模組        | -                                          

---

## MySQL 資料庫設定

請在 `src/main/resources/application.properties` 中加入以下設定：

```properties
# 1. 驅動程式類名
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 2. 連線 URL (已指定 shopping 資料庫，並加上連線參數)
spring.datasource.url=jdbc:mysql://113.196.223.150:3306/shopping?useSSL=false&serverTimezone=Asia/Taipei&characterEncoding=utf8

# 3. 使用者名稱
spring.datasource.username=user

# 4. 密碼 
spring.datasource.password= (傳在DC)
