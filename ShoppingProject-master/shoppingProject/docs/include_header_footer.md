
- `components/` 放置 header/footer HTML  
- `css/` 放置各自的 CSS  
- `js/` 放置 include 和元件 JS  

---

## 3. HTML 結構

在每個頁面放置占位符：

```html
<head>
    <link rel="stylesheet" href="/js/header_footer.js">
    <link rel="stylesheet" href="/css/header_footer.css">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@400;600;700&family=Montserrat:wght@400;500;600&family=Noto+Sans+TC:wght@300;400;500;700&family=Noto+Serif+TC:wght@400;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.7.1/jquery.min.js"></script>
</head>

<body>
    <div id="header"></div>

    <main>
        <!-- 頁面內容 -->
    </main>

    <div id="footer"></div>

    <script src="/js/include.js"></script>
</body>
