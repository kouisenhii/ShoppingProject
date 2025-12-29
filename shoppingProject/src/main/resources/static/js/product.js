/*
 *  ========== API Layer ==========
 *  (負責資料請求 / server 互動)
*/
async function fetchProduct(id){
    const res = await fetch(`/api/product/${id}`);
    if(!res.ok) throw new Error("找不到商品");
    
    const data = await res.json();
    
    // 【新增】檢查是否為「防穿透的空商品」
    if (data.productid === -1) {
        throw new Error("查無此商品 (ID不存在)");
    }

    return data;
}

async function fetchReviews(productId) {
    // 呼叫剛剛寫好的 Controller API
    const res = await fetch(`/api/product/${productId}/reviews`);
    if(!res.ok) return { reviews: [] }; // 若出錯回傳空資料，避免當機
    return res.json();
}

async function fetchRelated(categoryId, excludeId){
    return fetch(`/api/product/related/${categoryId}?exclude=${excludeId}`)
            .then(res=>res.json());
}



/* ================= 全域變數 ================= */
let currentProductId = null; // 用來記錄當前商品 ID
let allReviewsCache = []; // 用來存所有的評論
let originalReviewsCache = [];
let currentSortType = 'default';
const REVIEWS_PER_PAGE = 5; // 設定一頁顯示幾筆


/*
 *  ========== UI Render Layer ==========
 *  (負責畫面渲染，不含事件控制)
 */
/**
 * HTML 轉義函式 (防止 XSS 攻擊)
 * 把 < > & " ' 轉換成 HTML Entity
 */
function escapeHtml(text) {
    if (!text) return text;
    // 如果是數字就直接回傳 (例如價格)
    if (typeof text === 'number') return text;
    
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}


/*新增：更新商品頁麵包屑導覽 */
function updateProductBreadcrumb(product) {
    const $breadcrumb = $("#dynamic-breadcrumb");

    // 1. 固定顯示：首頁
    let html = '<li class="breadcrumb-item"><a href="/search.html">首頁</a></li>';

    // 2. 判斷並顯示：主分類
    // 使用後端回傳的 mainCategoryCode 和 mainCategoryName
    if (product.mainCategoryCode) {
        html += `<li class="breadcrumb-item">
                    <a href="/search.html?mainCategory=${product.mainCategoryCode}">
                        ${product.mainCategoryName}
                    </a>
                 </li>`;
    }

    // 3. 判斷並顯示：子分類
    // 使用後端回傳的 subCategoryCode 和 subCategoryName
    if (product.subCategoryCode) {
        // 注意：連結同時帶入主分類與子分類，確保搜尋頁能正確篩選
        html += `<li class="breadcrumb-item">
                    <a href="/search.html?mainCategory=${product.mainCategoryCode}&subCategory=${product.subCategoryCode}">
                        ${product.subCategoryName}
                    </a>
                 </li>`;
    }

    // 4. 顯示：當前商品名稱 (最後一層，不可點擊)
    if (product.pname) {
        html += `<li class="breadcrumb-item active" aria-current="page">${escapeHtml(product.pname)}</li>`;
    }

    $breadcrumb.html(html);
}



function renderBasicInfo(p){
    // 改用 getElementById，確保精準抓到 HTML 裡的 ID
    const titleEl = document.getElementById("product-name");
    const subEl = document.getElementById("product-subtitle");
    const priceEl = document.getElementById("product-price");

    // 1. 安全移除骨架樣式 (加上 if 判斷，避免元素不存在導致 JS 報錯當機)
    if (titleEl) titleEl.classList.remove("skeleton");
    if (subEl) subEl.classList.remove("skeleton");
    if (priceEl) priceEl.classList.remove("skeleton");

    // 2. 填入真實資料
    if (titleEl) titleEl.textContent = p.pname;
    if (subEl) subEl.textContent = p.description;

    // 價格邏輯
    const prices = p.options.color.map(c => c.price);
    const min = Math.min(...prices), max = Math.max(...prices);
    
    if (priceEl) {
        priceEl.textContent = min === max 
            ? `NT$${min.toLocaleString()}` 
            : `NT$${min.toLocaleString()} ~ $${max.toLocaleString()}`;
    }

    document.querySelector("#shipping").innerHTML = p.shipping;
    document.querySelector("#qty").max = p.stock;
    document.querySelector("#stock").textContent = `庫存：${p.stock}`;

    renderAverageRating(p.averageRating, p.reviewCount);
}

function renderDescription(p) {
    let html = ``;

    if (p.options && p.options.color && p.options.color.length > 0) {
        html += `
            <div class="mb-4">
                <br />
                <h4 class="spec-section-title">詳細規格表</h4>
                <br />
                
                <div class="spec-container">
                    <div class="row spec-header-row m-0">
                        <div class="col-12 col-md-5 spec-header-text">顏色 / 款式</div>
                        <div class="col-12 col-md-5 spec-header-text">規格描述</div>
                        <div class="col-12 col-md-2 spec-header-text">價格</div>
                    </div>
                    `;

        p.options.color.forEach((c) => {
            html += `
            
                <div class="row spec-body-row align-items-center">
                    
                    <div class="col-12 col-md-5">
                        <span class="spec-label">${escapeHtml(c.name)}</span>
                    </div>
                    
                    <div class="col-12 col-md-5 spec-desc">
                        ${c.specification}
                    </div>
                    
                    <div class="col-12 col-md-2 mt-2 mt-md-0">
                        <span class="spec-price">NT$${c.price.toLocaleString()}</span>
                    </div>
                </div>
            `;
        });

        html += `
                </div>
            </div>
        `;
    }

    document.querySelector("#desc-list").innerHTML = html;
}


//運送及付款方式
function renderShippingPayment() {
    
    const html = `
        <div class="mb-5">
            <br />
            <h4 class="spec-section-title">運送方式</h4>
            <br />
            
            <div class="row g-3">
                <div class="col-md-4">
                    <div class="shipping-card">
                        <div class="shipping-title">貨運配送</div>
                        <div class="shipping-desc">輸入配送地址</div>
                        <div class="shipping-price">NT$150</div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="shipping-card">
                        <div class="shipping-title">門市/指定點取貨</div>
                        <div class="shipping-desc">選擇取貨門市</div>
                        <div class="shipping-price">NT$0</div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="shipping-card">
                        <div class="shipping-title">便利商店取貨</div>
                        <div class="shipping-desc">7-ELEVEN / 全家</div>
                        <div class="shipping-price">NT$65</div>
                    </div>
                </div>
            </div>
        </div>

        <div class="mb-4">
            <br />
            <h4 class="spec-section-title">付款方式</h4>
            <br />
            
            <div class="payment-list-container">
                <div class="payment-item">
                    <div class="payment-label">信用卡</div>
                    <div class="d-flex align-items-center flex-wrap">
                        <span class="card-badge bg-visa">VISA</span>
                        <span class="card-badge bg-master">Master</span>
                        <span class="card-badge bg-jcb">JCB</span>
                        <span class="text-secondary ms-2 small">支援一次付清 / 分期付款</span>
                    </div>
                </div>

                <div class="payment-item">
                    <div class="payment-label">行動支付</div>
                    <div class="text-dark">
                        <i class="bi bi-apple me-1"></i>Apple Pay / <i class="bi bi-google me-1"></i>Google Pay / 綠界 Pay
                    </div>
                </div>

                <div class="payment-item">
                    <div class="payment-label">其他方式</div>
                    <div class="text-secondary">
                        網路 ATM / ATM 虛擬帳號 / 超商代碼 / 超商條碼
                    </div>
                </div>
            </div>
        </div>
        
        <div class="text-secondary small mt-4 ps-3 border-start border-3" style="border-color: #eee !important; line-height: 1.8;">
            <p class="mb-1">※ 帳款確認後將立即處理您的訂單，您將於七個工作日內(不含週六日及國定假日)收到訂購之商品。</p>
            <p class="mb-0">※ 如本店無法接受您的訂單，將於收到您的訂單後二個工作日內通知您。</p>
        </div>
    `;

    // 填入 HTML
    const shippingTab = document.querySelector("#shipping");
    if(shippingTab) shippingTab.innerHTML = html;
}


function renderAverageRating(avg, count) {
    const container = document.getElementById("product-rating-summary");

    // 1. 處理沒有評價的情況
    if (!count || count === 0) {
        container.innerHTML = `<span class="text-secondary" style="font-size:0.9rem;">(尚無評價)</span>`;
        return;
    }

    // 2. 生成星星HTML
    let starsHtml = "";

    for (let i = 1; i <= 5; i++) {
        if (avg >= i) {
            // 全星邏輯
            starsHtml += '<i class="bi bi-star-fill"></i>';
        } else if (avg >= i - 0.5) {
            //半星邏輯
            starsHtml += '<i class="bi bi-star-half"></i>';
        } else {
            // 其他情況顯示空星
            starsHtml += '<i class="bi bi-star"></i>';
        }
    }

    // 3. 組合星星 + 分數
    container.innerHTML = `
        ${starsHtml}
        <span class="ms-1 fw-bold text-dark">${avg.toFixed(1)}</span>
        <span class="ms-1 text-secondary" style="font-size:0.9rem;">(${count} 則評論)</span>
    `;
}


function renderImages(images, options){
    const main = document.getElementById("mainImageList");
    const thumb = document.getElementById("thumbImageList");
    const generateHtml = (url, index) => {
        let overlayHtml = "";
        //檢查索引對應的顏色是否存在且庫存為0
        if(options && options.color && options.color[index]){
            const variant = options.color[index];
            if(variant.stock <= 0){
                overlayHtml = `<div class="sold-out-overlay">已售完</div>`
            }
        }
        return `
            <div class="swiper-slide">
                ${overlayHtml}
                <img src="${url}" loading="lazy">
            </div>`;
    }    


    main.innerHTML = images.main.map((url, index) =>
        generateHtml(url, index)).join("");

    thumb.innerHTML = images.thumb.map((url, index) => 
        generateHtml(url, index)).join("");

    initMainSwiper();
}




function renderColorOptions(colors){
    const box = document.querySelector("#option-color");
    box.innerHTML = colors.map(c=>
        `<input type="radio" id="color-${c.productid}" name="color" value="${c.value}">
         <label for="color-${c.productid}" data-id="${c.productid}">${c.name}</label>`
    ).join("");
}

function renderReview(reviewData){
    const wrap = document.querySelector("#review-list");

    const isSel = (val) => (val === currentSortType ? "selected" : "");

    const titleHtml = `
        <div class="mb-4 d-flex justify-content-between align-items-center">
            <div>
                <br />
                <h4 class="spec-section-title" style="margin-bottom:0;">顧客評價</h4>
            </div>
            
            <div class="me-3">
                <select class="form-select form-select-sm" onchange="sortReviews(this.value)">
                    <option value="default" ${isSel('default')}>預設排序</option>
                    <option value="high"    ${isSel('high')}>評分：由高到低</option>
                    <option value="low"     ${isSel('low')}>評分：由低到高</option>
                </select>
            </div>
        </div>
    `;

    const reviews = reviewData.reviews; 
    const currentPage = reviewData.currentPage;
    const totalPages = reviewData.totalPages;

    // 無評論處理
    if (!reviews || reviews.length === 0) {
        wrap.innerHTML = `<div class="py-5 text-center text-muted">目前尚無評分，歡迎成為第一位評論者！</div>`;
        const pagination = document.getElementById("review-pagination");
        if(pagination) pagination.innerHTML = "";
        return;
    }


    let html = titleHtml + `
        <div class="spec-container">
            
            <div class="row review-header-row m-0">
                <div class="col-3 col-md-4 spec-header-text">No.</div>
                <div class="col-6 col-md-6 spec-header-text">規格 (顏色)</div>
                <div class="col-3 col-md-2 spec-header-text">評分</div>
            </div>
    `;
    
    // 計算序號起始值
    const startIndex = (currentPage - 1) * 5;

    reviews.forEach((r, i) => {
        const absoluteIndex = startIndex + i + 1;

        // 星星邏輯
        let stars = "";
        for (let j = 1; j <= 5; j++) {
            if (r.rating >= j) stars += '<i class="bi bi-star-fill"></i>';
            else if (r.rating >= j - 0.5) stars += '<i class="bi bi-star-half"></i>';
            else stars += '<i class="bi bi-star"></i>';
        }

        const rowColorClass = (i % 2 === 0) ? 'spec-row-white' : 'spec-row-alt';

        html += `
            <div class="row spec-body-row m-0 align-items-center ${rowColorClass}">
                
                <div class="col-3 col-md-4 spec-label">
                    ${absoluteIndex}.
                </div>

                <div class="col-6 col-md-6 spec-desc">
                    ${r.color}
                </div>
                
                <div class="col-3 col-md-2 text-warning fs-6">
                    <span class="d-none d-sm-inline">${stars}</span> 
                    <span class="review-rating-number">${Number(r.rating).toFixed(1)}</span>
                    <span class="d-inline d-sm-none text-warning"><i class="bi bi-star-fill"></i></span> </div>
            </div>`;
    });

    html += `</div>`; // 閉合 container

    wrap.innerHTML = html;

    renderServerPagination(totalPages, currentPage);
}


// 產生分頁按鈕
function renderServerPagination(totalPages, currentPage) {
    const paginationWrap = document.getElementById("review-pagination");
    paginationWrap.innerHTML = "";

    if (totalPages <= 1) return;

    let btnHtml = "";

    // 上一頁
    const prevDisabled = currentPage === 1 ? "disabled" : "";
    btnHtml += `
        <li class="page-item ${prevDisabled}">
            <a class="page-link" href="#" onclick="loadReviewPage(${currentPage - 1}, true); return false;">&laquo;</a>
        </li>`;

    // 數字頁
    for (let i = 1; i <= totalPages; i++) {
        const active = i === currentPage ? "active" : "";
        btnHtml += `
            <li class="page-item ${active}">
                <a class="page-link" href="#" onclick="loadReviewPage(${i}, true); return false;">${i}</a>
            </li>`;
    }

    // 下一頁
    const nextDisabled = currentPage === totalPages ? "disabled" : "";
    btnHtml += `
        <li class="page-item ${nextDisabled}">
            <a class="page-link" href="#" onclick="loadReviewPage(${currentPage + 1}, true); return false;">&raquo;</a>
        </li>`;

    paginationWrap.innerHTML = btnHtml;
}

// 切換頁面事件
async function loadReviewPage(page, shouldScroll = false, initLoad = false) {
    if (!currentProductId) return;
    
    //如果是第一次載入，才呼叫API
    if (initLoad){
        const data = await fetchReviews(currentProductId);
        allReviewsCache = data.reviews || [];
        originalReviewsCache = [...allReviewsCache];
    }
    
    // 1.計算總頁數
    const totalPages = Math.ceil(allReviewsCache.length / REVIEWS_PER_PAGE);

    // 2.防止頁數爆掉
    if (page < 1) page = 1;
    if (page > totalPages && totalPages > 0) page = totalPages;

    // 3. 從全域陣列中「切」出這一頁要顯示的資料
    const startIndex = (page - 1) * REVIEWS_PER_PAGE;
    const endIndex = startIndex + REVIEWS_PER_PAGE;
    const currentSlice = allReviewsCache.slice(startIndex, endIndex);

    // 4. 偽造一個跟原本後端一樣的格式丟給 renderReview
    const renderData = {
        reviews: currentSlice,
        currentPage: page,
        totalPages: totalPages
    };

    //重新渲染
    renderReview(renderData);

    if(shouldScroll){
        const reviewContainer = document.getElementById("review-list");
        if (reviewContainer) {
            // 預留給 Header 的緩衝高度
            const headerOffset = 140; 
            const targetY = reviewContainer.getBoundingClientRect().top + window.scrollY - headerOffset;

            window.scrollTo({
                top: targetY,
                behavior: 'smooth' // 平滑滾動效果
            });
        }
    }

    
}

//評論排序方式
function sortReviews(type) {

    currentSortType = type;
    // 1. 根據選擇類型對 allReviewsCache 進行排序
    if (type === 'high') {
        // 分數高 -> 低
        allReviewsCache.sort((a, b) => b.rating - a.rating);
    } else if (type === 'low') {
        // 分數低 -> 高
        allReviewsCache.sort((a, b) => a.rating - b.rating);
    } else {
        // 恢復預設 (還原成原本的順序)
        allReviewsCache = [...originalReviewsCache];
    }

    // 2. 排序後，強制回到第 1 頁並重新渲染 (不捲動，不重新 fetch)
    loadReviewPage(1, false, false);
}




//推薦列表
async function renderRelatedProducts(categoryId, productId){
    const list = await fetchRelated(categoryId,productId);
    const wrap = document.getElementById("related-list");

    
    wrap.innerHTML = list.map(p => `
            <div class="swiper-slide" onclick="location.href='/html/product.html?id=${p.productid}'">
                <img src="${p.productimage}" loading="lazy">
                <div class="description">${escapeHtml(p.description)}</div>
                <div class=name-price>
                    <p class="name">${escapeHtml(p.pname)}</p>
                    <p class="price">$${p.price.toLocaleString()}</p>
                </div>
            </div>
        `
    ).join("");

    initRelatedSwiper();
}


/*
 *  ========== Event Layer ==========
 *  (顏色切換、Tab切換、數量加減)
*/
function bindColorEvents(data){
    const labels = document.querySelectorAll("#option-color label");
    const addBtn = document.getElementById("add-to-cart");
    const buyBtn = document.getElementById("buy-now");
    const stockEl = document.getElementById("stock");

    labels.forEach((label, i)=>{
        label.addEventListener("click", () => {
            const opt = data.options.color[i];

            window.selectedOption = opt;

            // UI active 標記
            labels.forEach(l=>l.classList.remove("active"));
            label.classList.add("active");

            // 切換商品資料
            document.querySelector(".product-price").textContent = `NT$${opt.price.toLocaleString()}`;
            document.getElementById("qty").max = opt.stock;
            stockEl.textContent =  `庫存 : ${opt.stock.toLocaleString()}`

            if(opt.stock <= 0) {
                addBtn.disabled = true;
                addBtn.textContent = "已售完"
                addBtn.classList.add("disabled-btn-sold-out");

                buyBtn.disabled = true;
                buyBtn.classList.add("disabled-btn-sold-out")
            }else {
                // 有庫存：恢復正常
                addBtn.disabled = false;
                addBtn.textContent = "加入購物車";
                addBtn.classList.remove("disabled-btn"); // 移除預設的 disabled 樣式
                addBtn.classList.remove("disabled-btn-sold-out"); // 移除售完樣式

                buyBtn.disabled = false;
                buyBtn.classList.remove("disabled-btn-sold-out");

                stockEl.style.color = ""; // 恢復原色
            }

            //切換主圖
            if(window.productSwiper) window.productSwiper.slideTo(i);
        });
    });
}

function bindQtyEvents(){
    const qty = document.getElementById("qty");
    const minus=document.querySelector(".qty-btn.minus");
    const plus=document.querySelector(".qty-btn.plus");

    minus.addEventListener("click",()=>{
        qty.value = Math.max(1,(parseInt(qty.value)||1)-1);
    });
    plus.addEventListener("click",()=>{
        const max=parseInt(qty.max)||99;
        qty.value = Math.min(max,(parseInt(qty.value)||1)+1);
    });
    qty.addEventListener("input",()=>{
        const max=parseInt(qty.max);
        qty.value = Math.min(max,Math.max(1,parseInt(qty.value)||1));
    });
}


function bindTabEvents(){
    const btns=document.querySelectorAll('.tab-btn');
    const panes=document.querySelectorAll('.tab-pane');
    btns.forEach(btn=>{
        btn.addEventListener("click",()=>{
            btns.forEach(b=>b.classList.remove("active"));
            panes.forEach(p=>p.classList.remove("active"));
            btn.classList.add("active");
            document.getElementById(btn.dataset.tab).classList.add("active");
        });
    });
}


/*
 *  ========== Swiper Init ==========
*/
function initMainSwiper(){
    if(window.productSwiper) window.productSwiper.destroy(true,true);
    if(window.productThumbnail) window.productThumbnail.destroy(true,true);

    window.productThumbnail = new Swiper(".productThumbSwiper",{
        slidesPerView:5,
        spaceBetween:10,
        slideToClickedSlide:true,
        watchSlidesProgress:true
    });

    window.productSwiper = new Swiper(".productMainSwiper",{
        spaceBetween:10,
        navigation:{nextEl:".swiper-button-next",prevEl:".swiper-button-prev"},
        thumbs:{swiper:window.productThumbnail}
    });
}

function initRelatedSwiper(){
    new Swiper(".relatedSwiper",{
        slidesPerView: 1.5,
        centeredSlides: false,
        spaceBetween:15,
        freeMode: true,

        navigation:{
            nextEl:".related-next",
            prevEl:".related-prev"
        },
        breakpoints:{
            992:{ slidesPerView:3, spaceBetween: 20, freeMode: false, centeredSlides:false },
            1200:{ slidesPerView:4, spaceBetween: 24, freeMode: false, centeredSlides:false }
        }
    });
}



/*
 *  Main (程式入口)
*/
document.addEventListener("DOMContentLoaded", async()=>{

    const id = new URLSearchParams(location.search).get("id");
    if(!id) return showError("找不到此商品");

    currentProductId = id;

    try{
        const data = await fetchProduct(id);

        // 把商品資料存起來，addToCart()要用
        window.productData = data;
        window.selectedOption = null;


        // 渲染
        renderBasicInfo(data);
        renderDescription(data);
        renderShippingPayment();
        renderImages(data.images, data.options);
        renderColorOptions(data.options.color);
        renderRelatedProducts(data.categoryid,data.productid);
        updateProductBreadcrumb(data);

        // 綁定行為
        bindColorEvents(data);
        bindQtyEvents();
        bindTabEvents();

        // 評論和推薦商品同時去抓，誰先回來誰先顯示
        Promise.all([
            loadReviewPage(1, false, true)
        ]).catch(err => console.warn("次要資料載入失敗", err));


        //加入購物車
        document.getElementById("add-to-cart").addEventListener("click", async () => {
            if (!window.selectedOption) {
                return showAddToCartAnimation(false, 1, "請先選擇規格");
            }

            // 使用全域變數 currentGlobalUserId (header_footer.js提供)
            // 如果 header_footer.js 沒載入成功，才用1當備案
            const userId = (typeof currentGlobalUserId !== 'undefined' && currentGlobalUserId) ? currentGlobalUserId : 1;

            const productId = window.selectedOption.productid;
            const qty = Number(document.querySelector("#qty").value);

            await addToCart(userId, productId, qty, false);

            document.querySelector("#qty").value = 1;

        });

        //立即購買
        document.getElementById("buy-now").addEventListener("click", async () => {
            if (!window.selectedOption) {
                //return alert("請先選擇規格");
                return Swal.fire({
                title: '請先選擇規格',
                text: "必須選擇一個規格",
                icon: 'warning',
            })
            }
            
            const userId = 1;
            const productId = window.selectedOption.productid;
            const qty = Number(document.querySelector("#qty").value);

            await addToCart(userId, productId, qty, true);
            console.log(userId);
        });


    }catch(e){
        console.error(e);
        showError("查無此商品或已下架");
    }
});


/*
 * Error UI
*/
function showError(msg){
    document.querySelector("main").innerHTML=`
    <div class="text-center py-5">
        <h2 class="text-danger">${msg}</h2>
        <a href="/" class="btn btn-dark mt-3">回首頁</a>
    </div>`;

    // 2. 新增：隱藏 Tab 分頁區塊
    const tabsSection = document.querySelector(".product-tabs");
    if(tabsSection) tabsSection.style.display = "none";

    // 3. 新增：隱藏推薦商品區塊 (通常商品不存在也不會有推薦)
    const relatedSection = document.querySelector(".related-products");
    if(relatedSection) relatedSection.style.display = "none";
}