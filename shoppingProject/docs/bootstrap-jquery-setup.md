# Bootstrap & jQuery 使用說明文件

此文件說明專案中 Bootstrap 與 jQuery 的 **檔案位置、版本資訊、引入方式與注意事項**。

---

## 1. 檔案放置位置

專案採用 Spring Boot 預設靜態資源結構，所有前端檔案放置於：

```
src/main/resources/static/
```

### **1.1 CSS 放置位置**

```
src/main/resources/static/css/
```

### **1.2 JS 放置位置**

```
src/main/resources/static/js/
```

### **實際使用的檔案（建議）**

```
css/
 └── bootstrap.min.css

js/
 ├── bootstrap.bundle.min.js
 └── jquery-3.7.1.min.js
```

---

## 2. 版本資訊

> 若版本有變動請更新此區域

### **Bootstrap 版本**

* 使用檔案：`bootstrap.min.css` / `bootstrap.bundle.min.js`
* 來源版本：Bootstrap 5.3.5

### **jQuery 版本**

* 使用檔案：`jquery-3.7.1.min.js`
* 來源版本：jQuery 3.7.1（或依實際下載版本填寫）

---

## 3. 引入方式

### **3.1 CSS 引入**

```html
<link rel="stylesheet" href="/css/bootstrap.min.css">
```

### **3.2 JS 引入（含 jQuery）**

```html
<script src="/jquery-3.7.1.min.js"></script>
<script src="/js/bootstrap.bundle.min.js"></script>
```

### **3.3 Thymeleaf 寫法**

```html
<link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
<script th:src="@{/js/jquery.min.js}"></script>
<script th:src="@{/js/bootstrap.bundle.min.js}"></script>
```

---

## 4. 注意事項

* `bootstrap.bundle.min.js` 已包含 Popper，不需額外引入 `popper.js`。
* 請使用壓縮版（min.js / min.css），減少載入時間。
* 下載時不需放整個壓縮檔，只需使用必要檔案即可。
* 自訂 CSS 請放在 `css/style.css` 並於 Bootstrap 後載入，以便覆蓋樣式。

---
