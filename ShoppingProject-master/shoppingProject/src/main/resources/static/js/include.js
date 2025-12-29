// 載入 Header
fetch("/components/header.html")
    .then(res => res.text())
    .then(data => {
         document.getElementById("header").innerHTML = data;

        // ⭐ Header 載入完成後再執行 header.js
        const script = document.createElement("script");
        script.src = "/js/header_footer.js";
        document.body.appendChild(script);
    })
    .catch(err => console.error("fetch header.html 失敗", err));


// 載入 Footer
fetch("/components/footer.html")
    .then(res => res.text())
    .then(data => {
        document.getElementById("footer").innerHTML = data;
    });

