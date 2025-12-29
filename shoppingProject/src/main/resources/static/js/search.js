// ==========================================
// 1. 全域設定與變數
// ==========================================
let searchState = {
    // 紀錄目前的搜尋狀態
    mainCategory: '', // 大分類 
    subCategory: '', // 小分類
    maxPrice: null, // 最大金額
    minPrice: null, // 最小金額
    keyword: '', // 搜索欄關鍵字
    page: 0, // 追蹤當前頁碼 (從 0 開始)
    size: 12, // 預設每頁顯示筆數 12    
    sort: 'default' // 排序
};

let totalPages = 0; // 從後端取得總頁數
const API_BASE = "/api"; // 後端 API 位置

/**
 * 防抖函式 (Debounce)
 * 作用：當函式被連續呼叫時，只會在最後一次呼叫結束後的 delay 毫秒執行一次。
 * @param {Function} func 要執行的函式
 * @param {number} delay 延遲時間 (毫秒)
 */
function debounce(func, delay) {
    let timer;
    return function (...args) {
        // 如果計時器還在跑，代表使用者又打字了，趕快清除舊的，重新計時
        if (timer) clearTimeout(timer);

        const context = this;
        timer = setTimeout(() => {
            func.apply(context, args);
        }, delay);
    };
}
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

// ==========================================
// 2. 頁面初始化 (Entry Point)
// ==========================================
$(document).ready(function () {
    // 從 URL 讀取並初始化 searchState
    initStateFromURL();

    // 介面初始化 確保價格拉桿使用 searchState 的值
    initPriceSlider()

    // 載入上方大分類的導覽列的函式
    loadMainNavigation();

    // 介面初始化：如果有大分類，就載入對應側邊欄
    if (searchState.mainCategory) {
        loadSidebar(searchState.mainCategory);
        // 這裡可以設一個暫時的標題，之後可優化成顯示中文名稱
        $("#category-title").text(searchState.mainCategory.toUpperCase());
    }

    // 預設載入第一次商品列表
    loadProducts();

    // 綁定所有按鈕與互動事件
    bindEvents();

    // 左側選單收合動畫 (原有的程式碼)
    $("#collapse").on("click", function () {
        $("#filter").toggleClass("active");
        $(this).find("i").toggleClass("bi-arrow-bar-right bi-arrow-bar-left");
        $("#collapse").toggleClass("active");
    });
});

/**
 * 從 URL 讀取參數到 searchState
 */
function initStateFromURL() {
    const urlParams = new URLSearchParams(window.location.search);

    // 把網址上的值填入我們的狀態物件，如果沒有就維持預設值
    searchState.mainCategory = urlParams.get('mainCategory') || searchState.mainCategory;
    searchState.subCategory = urlParams.get('subCategory') || searchState.subCategory;
    searchState.maxPrice = urlParams.get('maxPrice') ? parseInt(urlParams.get('maxPrice')) : searchState.maxPrice;
    searchState.minPrice = urlParams.get('minPrice') ? parseInt(urlParams.get('minPrice')) : searchState.minPrice;
    searchState.keyword = urlParams.get("keyword") || searchState.keyword;
    searchState.page = urlParams.get('page') ? parseInt(urlParams.get('page')) : searchState.page;
    searchState.size = urlParams.get('size') ? parseInt(urlParams.get('size')) : searchState.size;
    searchState.sort = urlParams.get('sort') || searchState.sort;

    console.log("URL初始化狀態:", searchState);
}

/*
 * 將 searchState 寫回 URL (不刷新頁面)
 * 實現 URL 同步的關鍵
 */
function updateUrl() {
    const params = new URLSearchParams();

    // 只有當值非空、非預設值時，才寫入 URL (保持網址乾淨)
    if (searchState.mainCategory) params.set('mainCategory', searchState.mainCategory);
    if (searchState.subCategory) params.set('subCategory', searchState.subCategory);
    if (searchState.maxPrice) params.set('maxPrice', searchState.maxPrice);
    if (searchState.minPrice) params.set('minPrice', searchState.minPrice);
    if (searchState.keyword) params.set('keyword', searchState.keyword);
    if (searchState.page) params.set('page', searchState.page);
    if (searchState.sort) params.set('sort', searchState.sort);

    const newUrl = `${window.location.pathname}?${params.toString()}`;

    // 使用 pushState 更新瀏覽器網址列
    window.history.pushState(searchState, '', newUrl);

    // 重新載入商品
    loadProducts();
}

/*
 * 動態麵包屑導覽更新函式
 */
function updateBreadcrumb() {
    const $breadcrumb = $("#dynamic-breadcrumb");
    let html = '<li class="breadcrumb-item"><a href="/search.html" class="text-decoration-none">首頁</a></li>';

    // 1. 處理主分類 (Main Category)
    if (searchState.mainCategory) {
        // 嘗試從上方導覽列 (DOM) 抓取對應的中文名稱
        let $mainElem = $(`#main-nav-list a[data-code="${searchState.mainCategory}"]`);
        let mainName = $mainElem.text().trim();

        // 如果抓不到 (可能導覽列還沒載入)，暫時顯示代碼
        if (!mainName) mainName = searchState.mainCategory.toUpperCase();

        // 如果還有子分類，那主分類應該要變成「可點擊的連結」
        if (searchState.subCategory) {
            html += `<li class="breadcrumb-item"><a href="#" onclick="onMainCategoryClick('${searchState.mainCategory}'); return false;">${mainName}</a></li>`;
        } else {
            // 如果沒有子分類，主分類就是當前頁面 (文字不可點)
            html += `<li class="breadcrumb-item active" aria-current="page">${mainName}</li>`;
        }
    } else {
        // 如果沒有選主分類
        if (!searchState.keyword) {
            html += `<li class="breadcrumb-item active" aria-current="page">所有商品</li>`;
        }
    }

    // 2. 處理子分類 (Sub Category)
    if (searchState.subCategory) {
        // 嘗試從側邊欄 (DOM) 抓取對應的中文名稱
        let $subElem = $(`.sidebar-list a[data-value="${searchState.subCategory}"]`);

        // 複製一份元素來移除裡面的 icon 或 count，只取純文字
        let subName = $subElem.clone().children().remove().end().text().trim();

        // 如果抓不到，暫時顯示代碼
        if (!subName) subName = searchState.subCategory;

        html += `<li class="breadcrumb-item active" aria-current="page">${subName}</li>`;
    }

    // 3. 處理關鍵字搜尋
    if (searchState.keyword) {
        html += `<li class="breadcrumb-item active" aria-current="page">搜尋：${searchState.keyword}</li>`;
    }

    $breadcrumb.html(html);
}

/*
* 載入上方大分類導覽列
*/
function loadMainNavigation() {
    $.ajax({
        url: `${API_BASE}/categories/main`, // 呼叫後端 API
        method: 'GET',
        success: function (categories) {
            let html = '';
            const currentCode = searchState.mainCategory; // <-- 使用 searchState.mainCategory

            // 選項：全部商品 (讓使用者可以點回首頁或清空篩選)
            html += `
                <li class="category-item ${currentCode === '' ? 'active' : ''}">
                    <a href="#" data-code="" class="main-category-link">全部商品</a>
                </li>`;

            // 迴圈跑後端回傳的所有大分類
            categories.forEach(cat => {
                // 判斷是否為當前選中的分類 (為了加 highlight 樣式)
                let isActive = (cat.code === currentCode) ? 'active' : '';

                // 注意：用 onclick 函數和 data-code 來處理，不再使用直接的 href
                html += `
                <li class="category-item ${isActive}">
                    <a href="#" data-code="${cat.code}" class="main-category-link">${cat.cname}</a>
                </li>`;
            });

            $("#main-nav-list").html(html);

            // 綁定主分類的點擊事件 (需在生成 HTML 後進行)
            $(".main-category-link").on('click', function (e) {
                e.preventDefault();
                const newMainCat = $(this).data('code');
                onMainCategoryClick(newMainCat);
            });

            updateBreadcrumb();
        },
        error: function (err) {
            console.error("無法載入主分類導覽", err);
        }
    });
}

/*
 * 初始化價格拉桿
 */
function initPriceSlider() {
    // 確保價格從 URL 讀取到，如果沒有就用預設的最大最小值
    const toVal = searchState.maxPrice !== null ? searchState.maxPrice : 60000;
    const fromVal = searchState.minPrice !== null ? searchState.minPrice : 0;

    $("#priceRangeSlider").ionRangeSlider({
        type: "double",
        min: 0,
        max: 60000,
        from: fromVal,
        to: toVal,
        step: 100,
        prefix: "NT$",
        skin: "round",
        // 價格變動完成，更新 URL 和搜尋
        onFinish: function (data) {
            // 點擊後，更新 searchState
            searchState.minPrice = data.from;
            searchState.maxPrice = data.to;
            searchState.page = 0; // 重設頁碼
            updateUrl(); // 更新 URL 並重新搜尋
        }
    });

    // 如果從 URL 讀取了價格，需要讓排序下拉選單同步顯示
    if (searchState.sort && searchState.sort !== 'default') {
        $("#sortSelect").val(searchState.sort);
    }
}

// ==========================================
// 3. 功能函式：載入資料
// ==========================================
/*
* 載入側邊欄小分類
* API: /api/categories/main/{code}/sub
*/
function loadSidebar(subCategoryCode) {
    $.ajax({
        url: `${API_BASE}/categories/main/${subCategoryCode}/sub`,
        method: 'GET',
        success: function (subCategories) {

            console.log("後端回傳的子分類資料:", subCategories);

            // 【安全性修正】確保容器存在：避免因為之前被 empty() 刪除導致無法渲染
            // 如果找不到 sidebar-list，就重新建立它
            if ($("#category-filter .sidebar-list").length === 0) {
                // 這裡假設你的 CSS 結構是需要一個 ul.sidebar-list
                // 如果原本的結構不同，請依實際情況調整
                $("#category-filter").html('<ul class="sidebar-list"></ul>');
            }

            let html = '';
            const currentSubCode = searchState.subCategory;

            // 選項：全部 (不篩選小分類)
            let isAllActive = currentSubCode === '' ? 'checked' : '';
            html += `
                     <li>
                        <a href="#" class="sub-category-link ${isAllActive}" data-value="">
                            <i class="bi bi-arrow-down"></i>
                            所有子分類
                            <span class="count">(${subCategories.length})</span>
                        </a>
                    </li>`;

            // 選項：後端回傳的子分類
            subCategories.forEach(cat => {
                let isActive = (cat.code === currentSubCode) ? 'checked' : '';

                let countHtml = (cat.count !== undefined && cat.count !== null) ? `<span class="count">(${cat.count})</span>` : '';

                // value 帶入 code (例如 'spoon')
                html += `
                        <li>
                            <a href="#" class="sub-category-link ${isActive}" data-value="${cat.code}">
                                <i class="bi bi-arrow-right"></i>
                                ${cat.cname}
                                ${countHtml}
                            </a>
                        </li>`;
            });

            $("#category-filter .sidebar-list").html(html);

            $("#category-filter").off('click', '.sub-category-link').on('click', '.sub-category-link', onSubCategoryClick);

            updateBreadcrumb();
        },
        error: function (err) {
            console.error("載入側邊欄失敗", err);
            $("#category-filter").html('<div class="text-danger">無法載入分類</div>');
        }
    });
}
/**
* 載入商品列表 (核心搜尋功能)
* API: /api/products/search
*/
function loadProducts() {
    // 準備參數
    let requestData = {
        mainCategory: searchState.mainCategory,
        subCategory: searchState.subCategory,
        maxPrice: searchState.maxPrice,
        minPrice: searchState.minPrice,
        keyword: searchState.keyword,
        page: searchState.page,
        size: searchState.size,
        sort: searchState.sort
    };

    // 顯示 Loading 狀態
    $("#product-list").html('<div class="col-12 text-center p-5"><div class="spinner-border text-primary" role="status"></div></div>');

    // 發送 AJAX
    $.ajax({
        url: `${API_BASE}/products/search?`,
        method: 'GET',
        data: requestData,
        success: function (response) {

            console.log("後端回傳 response =", response);

            // 更新全域總頁數
            totalPages = response.page.totalPages;

            // 渲染商品
            renderProductCards(response.content);

            // 渲染分頁按鈕 (傳入: 當前頁碼, 總頁數)
            renderPagination(response.page.number, response.page.totalPages);

            // 更新麵包屑
            updateBreadcrumb();

            // 渲染已選條件標籤
            renderActiveFilters();
        },
        error: function (err) {
            console.error("搜尋商品失敗", err);
            $("#product-list").html('<div class="col-12 text-center text-danger">載入商品發生錯誤，請稍後再試。</div>');
            // 隱藏分頁
            $("#pagination-nav").hide();
        }
    });
}

/*
* 渲染 HTML：把 JSON 資料轉成商品卡片
*/

function renderProductCards(products) {
    let container = $("#product-list");
    container.empty(); // 清空 loading

    if (!products || products.length === 0) {
        container.html(`
            <div class="col-12 text-center mt-5 mb-5">
                <i class="bi bi-search display-1 text-muted"></i>
                <h4 class="text-muted mt-3">找不到符合條件的商品</h4>
                <p class="text-secondary">試試看調整關鍵字，或是參考下方的精選商品！</p>
            </div>
            
            <div class="col-12 mt-4">
                <h5 class="mb-3 border-bottom pb-2 fw-bold text-primary">
                    <div class="col-12 mt-5 mb-3">
                        <h3 class="fw-bold text-dark">為您推薦</h3>
                        <p class="text-muted small">這些熱門商品您可能會喜歡</p>
                    </div>
                </h5>
                <div id="recommend-list" class="row">
                    <div class="col-12 text-center py-3">
                        <div class="spinner-border text-secondary spinner-border-sm" role="status"></div>
                        <span class="ms-2 text-muted">載入推薦中...</span>
                    </div>
                </div>
            </div>
        `);

        loadRecommendProducts();
        return;
    }

    products.forEach(product => {
        // 處理圖片 (如果沒有圖片，顯示預設圖)
        let imgUrl = product.productimage ? product.productimage : 'https://dummyimage.com/400x400/dee2e6/6c757d.jpg&text=No+Image';

        // 判斷是否沒貨
        let isSoldOut = product.stock <= 0;

        // 準備(遮罩)與(按鈕)的 HTML
        let soldOutHtml = '';
        let buttonAttr = '';
        let buttonCountent = '';

        if (isSoldOut) {
            // 沒有貨的話

            // 產生遮罩
            soldOutHtml = `
                <div class="sold-out-overlay">
                    <span class="sold-out-text">已售完</span>
                </div>
            `;

            // 購物車按鈕 變成灰色、禁止點擊、文字改成補貨中
            buttonAttr = 'disabled style="cursor: not-allowed; opacity: 0.6; background-color: #e9ecef; border-color: #dee2e6; color: #6c757d;"';
            buttonCountent = '補貨中';
        } else {
            // 有貨的狀態

            // 沒有遮罩
            soldOutHtml = '';

            // 按鈕保持原樣
            buttonAttr = 'class="btn bg-white shadow-sm rounded-pill"';
            buttonCountent = '<i class="bi bi-cart-plus"></i> 加入購物車';
        }

        // 處理價格顯示 (千分位)
        let displayPrice = product.price ? product.price.toLocaleString() : 0;

        // 使用 escapeHtml 包住文字欄位
        let safeName = escapeHtml(product.pname);
        let safeDescription = escapeHtml(product.description)

        let cardHtml = `
                <div class="col-lg-3 col-md-3 mb-3">
                    <div class="product-box">
                        <div class="product-inner-box position-relative">

                            ${soldOutHtml}

                            <div class="icons position-absolute wishList-btn">
                               <a href="#" class="text-decoration-none text-dark" onclick="event.preventDefault();addWishListEntry(${product.productid})"><i class="bi bi-suit-heart"></i></a> 
                            </div>
                            
                            <img src="${imgUrl}" loading="lazy" alt="${safeName}" class="img-fluid" 
                                onclick="location.href='/html/product.html?id=${product.productid}'"
                                style="cursor: pointer;">
                            
                            <div class="cart-btn">
                                <button ${buttonAttr} 
                                        onclick="addToCart(1, ${product.productid}, 1); event.stopPropagation();">
                                    ${buttonCountent}
                                </button>
                            </div>
                        </div> 
                        <div class="product-info mt-3">
                            <div class="product-name d-flex justify-content-between">
                                <h3 class="text-truncate" title="${safeName}">${safeName}</h3>
                                <p>${safeDescription}</p>
                            </div>
                            <div class="product-price">
                                NT$<span>${displayPrice}</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        container.append(cardHtml);
    });

    // 重新綁定 hover 特效 (因為元素是新長出來的)
    bindHoverEffects();
}

/**
 * 載入推薦商品 (當搜尋無結果時呼叫)
 */
function loadRecommendProducts() {
    // 這裡我們重用搜尋 API，只抓 4 筆資料當作推薦
    let requestData = {
        page: 0,
        size: 4,      // 只顯示 4 筆
        sort: 'ratingDesc'
        // 不傳 keyword, mainCategory, subCategory，代表抓全站推薦
    };

    $.ajax({
        url: `${API_BASE}/products/search`,
        method: 'GET',
        data: requestData,
        success: function (response) {
            // 根據您的後端結構，資料通常在 response.content
            let recommendData = response.content ? response.content : response;

            // 渲染推薦卡片
            renderRecommendCards(recommendData);
        },
        error: function (err) {
            console.error("無法載入推薦商品", err);
            $("#recommend-list").html('<div class="col-12 text-center text-muted">暫無推薦商品</div>');
        }
    });
}

/**
 * 渲染推薦商品卡片
 */
function renderRecommendCards(products) {
    let container = $("#recommend-list");
    container.empty(); // 清掉 Loading 動畫

    if (!products || products.length === 0) {
        container.html('<div class="col-12 text-muted text-center">暫無推薦商品</div>');
        return;
    }

    products.forEach(product => {
        // 1. 處理圖片與價格 (邏輯同 renderProductCards)
        let imgUrl = product.productimage ? product.productimage : 'https://dummyimage.com/400x400/dee2e6/6c757d.jpg&text=No+Image';
        let displayPrice = product.price ? product.price.toLocaleString() : 0;

        // 使用 escapeHtml 包住文字欄位
        let safeName = escapeHtml(product.pname);
        let safeDescription = escapeHtml(product.description)

        // 2. 處理描述文字 (防止過長跑版，這裡建議截斷，若您原本沒截斷可拿掉 slice)
        // 這裡直接使用原本的 HTML 結構
        let cardHtml = `
            <div class="col-lg-3 col-md-3 mb-3">
                <div class="product-box">
                    <div class="product-inner-box position-relative">
                        <div class="icons position-absolute wishList-btn ">
                           <a href="#" class="text-decoration-none text-dark" onclick="event.preventDefault();addWishListEntry(${product.productid})"><i class="bi bi-suit-heart"></i></a>
                        </div>
                        
                        <img src="${imgUrl}" loading="lazy" alt="${safeName}" class="img-fluid" onclick="location.href='/html/product.html?id=${product.productid}'">
                        
                        <div class="cart-btn">
                            <button class="btn bg-white shadow-sm rounded-pill" onclick="addToCart(1, ${safeDescription}, 1); event.stopPropagation();">
                                <i class="bi bi-cart-plus"></i> 加入購物車
                            </button>
                        </div>
                    </div> 
                    
                    <div class="product-info mt-3">
                        <div class="product-name d-flex justify-content-between">
                            <h3>${safeName}</h3>
                            <p>${safeDescription}</p>
                        </div>
                        <div class="product-price">
                            NT$<span>${displayPrice}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;
        container.append(cardHtml);
    });

    // 3. 重要：重新綁定 Hover 特效，不然推薦商品滑鼠移上去會沒有反應
    bindHoverEffects();
}

// 分頁邏輯
function renderPagination(current, totalPages) {
    const $nav = $("#pagination-nav");
    const $pageNumbers = $("#pageNumbers");
    const $prevBtn = $("#prevPageItem");
    const $nextBtn = $("#nextPageItem");

    // 如果只有 1 頁或沒資料，不顯示分頁條
    if (totalPages <= 1) {
        $nav.hide();
        return;
    }
    $nav.show();

    // 清空舊頁碼
    $pageNumbers.empty();

    // 設定上一頁按鈕狀態 (如果當前是第0頁，就 disabled)
    if (current === 0) {
        $prevBtn.addClass("disabled");
    } else {
        $prevBtn.removeClass("disabled");
    }
    // 設定下一頁按鈕狀態
    if (current === totalPages - 1) {
        $nextBtn.addClass("disabled");
    } else {
        $nextBtn.removeClass("disabled");
    }
    // 計算要顯示哪些頁碼 (最多顯示 5 個數字)
    const maxPagesToShow = 5;
    let startPage = Math.max(0, current - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxPagesToShow - 1);

    if (endPage - startPage + 1 < maxPagesToShow && totalPages >= maxPagesToShow) {
        startPage = Math.max(0, endPage - maxPagesToShow + 1);
    }
    // 產生頁碼 HTML
    for (let i = startPage; i <= endPage; i++) {
        // i + 1 是因為顯示給使用者看要從 1 開始，但程式邏輯是 0
        let activeClass = (i === current) ? 'active' : '';
        let pageHtml = `
                    <li class="page-item ${activeClass}">
                        <a class="page-link" href="#" onclick="goToPage(${i}); return false;">${i + 1
            }</a>
                    </li>
                `;
        $pageNumbers.append(pageHtml);
    }
}

// 跳轉到指定頁面
function goToPage(pageIndex) {
    // 防止超出範圍
    if (pageIndex < 0 || pageIndex >= totalPages) return;

    searchState.page = pageIndex;

    updateUrl();

    // 回到列表頂端
    $('html, body').animate({
        scrollTop: $("#product-list").offset().top - 100
    }, 300);
}

// 上一頁 / 下一頁
function changePage(delta) {
    let newPage = searchState.page + delta;
    goToPage(newPage);
}

/* ==========================================
   4. 新增功能：已選條件標籤 (Filter Chips)
   ========================================== */

function renderActiveFilters() {
    const $container = $("#active-filters-container");
    let html = '';
    let hasFilter = false;

    // 1. 關鍵字標籤
    if (searchState.keyword) {
        hasFilter = true;
        html += `
            <span class="badge bg-white text-dark border rounded-pill py-2 px-3 me-2 mb-2 shadow-sm">
                關鍵字: ${escapeHtml(searchState.keyword)}
                <i class="bi bi-x ms-2" style="cursor:pointer;" onclick="removeFilter('keyword')"></i>
            </span>
        `;
    }

    // 2. 價格標籤 (只有當價格不是預設值時才顯示)
    // 假設最大預設是 60000 (請依你實際設定調整)
    const defaultMax = 60000; 
    const defaultMin = 0;
    
    // 判斷是否有設定價格區間
    if ((searchState.minPrice !== null && searchState.minPrice > defaultMin) || 
        (searchState.maxPrice !== null && searchState.maxPrice < defaultMax)) {
        
        hasFilter = true;
        let min = searchState.minPrice || 0;
        let max = searchState.maxPrice || defaultMax;
        
        html += `
            <span class="badge bg-white text-dark border rounded-pill py-2 px-3 me-2 mb-2 shadow-sm">
                價格: NT$${min.toLocaleString()} - NT$${max.toLocaleString()}
                <i class="bi bi-x ms-2" style="cursor:pointer;" onclick="removeFilter('price')"></i>
            </span>
        `;
    }

    $container.html(html);
}

// 移除單一篩選條件的邏輯
window.removeFilter = function(type) {
    if (type === 'keyword') {
        // 清除關鍵字
        searchState.keyword = '';
        $("#keywordInput").val(''); // 同步清空搜尋框
        $("#search-input").val(''); // 如果 Header 也有搜尋框也要清空
    } 
    else if (type === 'price') {
        // 重置價格
        searchState.minPrice = null; // 或 0
        searchState.maxPrice = null; // 或 60000
        
        // ★ 重要：必須重置 UI 上的價格拉桿，不然標籤消失了但拉桿還卡在舊位置
        let slider = $("#priceRangeSlider").data("ionRangeSlider");
        if (slider) {
            slider.reset();
        }
    } 
    else if (type === 'all') {
        // 清除全部 (類似你原本的 reset 按鈕)
        searchState.keyword = '';
        searchState.minPrice = null;
        searchState.maxPrice = null;
        
        $("#keywordInput").val('');
        let slider = $("#priceRangeSlider").data("ionRangeSlider");
        if (slider) slider.reset();
        
        // 注意：這裡我沒清除分類 (Category)，因為通常「清除全部」是清除篩選條件，
        // 而不是把使用者踢出「沙發」分類。
        // 如果你想連分類都清掉，可以加上 searchState.mainCategory = '';
    }

    // 重設頁碼並搜尋
    searchState.page = 0;
    updateUrl();
};

// ==========================================
// 5. 事件綁定 (Event Listeners)
// ==========================================
// 點擊主分類的事件處理
function onMainCategoryClick(newMainCat) {
    // 1. 更新狀態
    searchState.mainCategory = newMainCat;
    searchState.subCategory = ''; // 切換主分類通常要清空次分類
    searchState.page = 0; // 重設頁碼

    // 2. 更新 URL 並搜尋
    updateUrl();

    // 3. 重新載入上方導覽列
    loadMainNavigation();

    // 4. 重新載入介面 (側邊欄)
    if (searchState.mainCategory) {
        loadSidebar(searchState.mainCategory);
        $("#category-title").text(searchState.mainCategory.toUpperCase());
    } else {
        $("#category-title").text("所有商品");

        // 【Bug修正】: 這裡之前使用 empty() 會把 ul.sidebar-list 整個刪掉
        // 修改為: 只清空 ul 裡面的 li 內容，保留 ul 結構
        $("#category-filter .sidebar-list").empty();
    }
}

// 點擊次分類的事件處理 (專門給 delegate 用)
function onSubCategoryClick(e) {
    e.preventDefault(); // 阻止 <a> 標籤的預設行為 (跳轉)

    // 1. 更新狀態
    // 從被點擊的 <a> 標籤取得 data-value
    const newSubCat = $(this).data('value');

    // 如果點擊的是已經選中的分類，則清除篩選 (可選邏輯)
    if (searchState.subCategory === newSubCat) {
        searchState.subCategory = '';
    } else {
        searchState.subCategory = newSubCat;
    }

    searchState.page = 0; // 換分類回到第一頁

    // 2. 更新 URL 並搜尋 (關鍵：將次分類條件寫入網址)
    updateUrl();

    // 3. 重新載入側邊欄，確保 active 狀態正確更新
    loadSidebar(searchState.mainCategory);
}

// 點擊排序的事件處理
function onSortChange() {
    // 1. 更新狀態
    searchState.sort = $(this).val();
    searchState.page = 0; // 排序變了，重設頁碼

    // 2. 更新 URL 並搜尋
    updateUrl();
}

function bindEvents() {
    // 監聽「關鍵字搜索」表單
    $(document).on("input", "#search-input, #keywordInput", debounce(function (e) {
        // e.preventDefault();
        const keywordInput = $(this).val().trim();

        // console.log("🔍 偵測到打字:", keywordInput);

        // 只有當關鍵字真的改變才搜尋
        if (searchState.keyword !== keywordInput) {

            if (keywordInput === "") return

            searchState.keyword = keywordInput;
            searchState.page = 0;

            updateUrl();
        }
    }, 500));

    // 監聽「排序」選單
    $("#sortSelect").on('change', onSortChange);

    // 清除所有篩選按鈕
    // 請在 HTML 加入 <button id="reset-btn">清除篩選</button>
    $("#reset-btn").on("click", function (e) {
        e.preventDefault();

        // 1. 重置所有狀態為預設值
        searchState = {
            mainCategory: '',
            subCategory: '',
            maxPrice: null,
            minPrice: null,
            keyword: '',
            page: 0,
            size: 12,
            sort: 'default'
        };

        // 2. 重置 UI - 價格拉桿 (呼叫 ionRangeSlider 的 reset)
        let slider = $("#priceRangeSlider").data("ionRangeSlider");
        if (slider) {
            slider.reset();
        }

        // 3. 重置 UI - 搜尋框與排序
        $("#keywordInput").val("");
        $("#sortSelect").val("default");

        // 4. 重置 UI - 側邊欄與標題
        $("#category-title").text("所有商品");
        $("#category-filter .sidebar-list").empty();

        // 5. 更新 URL 並重新搜尋
        updateUrl();

        // 6. 重新載入主分類 (清除 active 樣式)
        loadMainNavigation();
    });
}

// 綁定 CSS Hover 效果
function bindHoverEffects() {
    $('.product-box').hover(
        function () {
            $(this).addClass('is-hover');
        },
        function () {
            $(this).removeClass('is-hover');
        }
    );
}

// 購物車功能
function addToCart(quantity, productid, type) {
    if (window.addToCart) {
        window.addToCart(quantity, productid, type);
    } else {
        console.warn("尚未定義全域 addToCart 函式");
    }
}

//收藏清單功能
function addWishListEntry(productid) {
    if (window.addToWishList) {
        window.addToWishList(productid);
    } else {
        console.warn("尚未定義全域 addToWishList 函式");
    }
}