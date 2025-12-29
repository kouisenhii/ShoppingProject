// =========================================================
// 1. 全域變數和靜態資料定義
// =========================================================
let productData = []; // 用於本月主打
let pdData = [];      // 用於特色主打
let categoryData = []; // 用於產品系列
let currentFeaturedIndex = 0;
// =========================================================
// 2. 函式定義
// =========================================================

// --- 輪播邏輯函式 ---
function setupCarousel(listSelector, prevBtnSelector, nextBtnSelector, progressBarSelector) {
    const $el = $(listSelector);
    const $bar = $(progressBarSelector);

    $(prevBtnSelector).off('click').click(function () {
        $el.animate({ scrollLeft: '-=350' }, 300);
    });

    $(nextBtnSelector).off('click').click(function () {
        $el[0].scrollBy({ left: 350, behavior: 'smooth' });
    });

    function updateProgress() {
        const el = $el[0];
        if (!el) return;

        const scrollLeft = el.scrollLeft;
        const maxScroll = el.scrollWidth - el.clientWidth;

        let progress = 0;
        if (maxScroll > 0) {
            progress = (scrollLeft / maxScroll) * 100;
        }
        progress = Math.max(0, Math.min(100, progress));
        $bar.css('width', progress + '%');
    }

    $el.off('scroll').on('scroll', updateProgress);
    $(window).off('resize.carousel' + listSelector).on('resize.carousel' + listSelector, updateProgress);

    setTimeout(updateProgress, 100);
}

// --- 渲染骨架屏函式  ---
function renderSkeletons() {
    const productSkeletonHTML = Array(4).fill(0).map(() => `
            <div class="skeleton-card">
                <div class="skeleton skeleton-img"></div>
                <div class="skeleton skeleton-line-sm"></div>
                <div class="skeleton skeleton-line-md"></div>
                <div class="skeleton skeleton-line-sm" style="width: 30%"></div>
            </div>
        `).join('');
    $('#productsList').html(productSkeletonHTML);

    const seriesSkeletonHTML = Array(4).fill(0).map(() => `
            <div class="skeleton-card">
                <div class="skeleton skeleton-img"></div>
                <div class="skeleton skeleton-line-sm" style="width: 50%; margin-top: 1rem;"></div>
            </div>
        `).join('');
    $('#seriesList').html(seriesSkeletonHTML);
}


// 根據索引渲染推薦商品區塊 
function renderFeaturedProduct(index) {
    if (pdData.length === 0) {
        console.warn("推薦商品資料 (pdData) 為空。");
        return;
    }

    const safeIndex = (index % pdData.length + pdData.length) % pdData.length;
    currentFeaturedIndex = safeIndex;

    const featuredProduct = pdData[safeIndex];

    $('#featMainImg').attr('src', featuredProduct.productimage);
    $('#featBrand').text(featuredProduct.description);
    $('#featName').text(featuredProduct.pname);
    $('#featPrice').text('NT$ ' + featuredProduct.price.toLocaleString());

// 設定點擊跳轉事件
    // 假設後端回傳的物件有 productid 欄位
    const targetUrl = `/html/product.html?id=${featuredProduct.productid}`;

    // 針對「圖片」和「商品名稱」設定 CSS 手指游標，並綁定跳轉事件
    // 使用 .off('click') 是為了防止切換商品時重複綁定事件
    $('#featMainImg, #featName').css('cursor', 'pointer').off('click').on('click', function() {
        window.location.href = targetUrl;
    });

    // 3. 更新按鈕狀態 (保持原本邏輯)
    const isDisabled = pdData.length <= 1;
    $('#featPrevBtn').prop('disabled', isDisabled).toggleClass('opacity-50', isDisabled).toggleClass('hover-opacity-100', !isDisabled);
    $('#featNextBtn').prop('disabled', isDisabled).toggleClass('opacity-50', isDisabled).toggleClass('hover-opacity-100', !isDisabled);
}

// 處理產品切換 
function switchFeaturedProduct(direction) {
    let newIndex = currentFeaturedIndex + direction;
    if (newIndex >= pdData.length) {
        newIndex = 0;
    } else if (newIndex < 0) {
        newIndex = pdData.length - 1;
    }
    renderFeaturedProduct(newIndex);
}

// **【新增函式】** 根據產品名稱跳轉到輪播圖的指定產品
function jumpToFeaturedProductByName(productName) {
    if (pdData.length === 0) return;

    // 尋找目標產品在 pdData 陣列中的索引
    // 注意：這裡假設 pdData 中的 pname 與 data-product-name 的值是完全匹配的
    const targetIndex = pdData.findIndex(p => p.pname === productName);

    if (targetIndex !== -1) {
        // 如果找到，則直接渲染該索引的產品
        renderFeaturedProduct(targetIndex);
    } else {
        console.warn(`產品名稱 "${productName}" 在 pdData 中不存在。`);
    }
}

// --- 總體渲染函式 ---
function renderData() {
    // 1. 渲染本月主打產品列表 (productData)
    const $list = $('#productsList'); // 【關鍵修復】確保這行存在！
    $list.empty();

    if (productData.length > 0) {
        productData.forEach(p => {
            const productNameTitle = p.pname ? p.pname.replace(/"/g, '&quot;') : '';
            
            // 處理圖片路徑，若無圖片顯示預設圖
            const imgUrl = p.productimage || 'https://dummyimage.com/400x400/dee2e6/6c757d.jpg&text=No+Image';
            
            // 格式化價格
            const displayPrice = p.price ? p.price.toLocaleString() : 'N/A';

            // 【功能一致性修改】加入愛心與購物車按鈕 HTML
            const html = `
                    <div class="product-card">
                        <div class="product-img-wrapper">
                            
                            <a href="#" class="wishlist-btn-overlay" 
                               onclick="event.preventDefault(); addToWishList(${p.productid});">
                                <i class="bi bi-suit-heart"></i>
                            </a>

                            <a href="/html/product.html?id=${p.productid}">
                                <img src="${imgUrl}" alt="${p.pname}" class="product-img">
                            </a>
                            
                            <div class="cart-btn">
                                <button onclick="addToCart(1, ${p.productid}); event.stopPropagation();">
                                    <i class="bi bi-cart-plus"></i> 加入購物車
                                </button>
                            </div>
                        
                        </div>
                        <div class="brand-text">${p.description || 'N/A'}</div> 
                        <div class="product-name" title="${productNameTitle}">${p.pname}</div>
                        <div class="product-price">NT$ ${displayPrice}</div>
                    </div>
                `;
            $list.append(html);
        });
    } else {
        console.warn("產品列表資料為空 (productData)。");
    }

    // 2. 渲染系列 (產品分類)
    const $seriesList = $('#seriesList');
    $seriesList.empty();
    
    categoryData.forEach(c => {
        const targetUrl = `/search.html?mainCategory=${encodeURIComponent(c.code)}`;
        const html = `
                <div class="category-card" onclick="window.location.href='${targetUrl}'" style="cursor: pointer;">
                    <div class="product-img-wrapper">
                        <img src="${c.categoryimage}" alt="${c.cname}" class="product-img">
                    </div>
                    <div class="category-name">${c.cname}</div>
                </div>
            `;
        $seriesList.append(html);
    });

    // 3. 渲染推薦商品區塊資料 (pdData)
    if (pdData.length > 0) {
        // 設定背景圖片 (靜態)
        $('#featContextImg').attr('src', "https://www.ikea.com.tw/dairyfarm/tw/pageImages/page_zh_tw_17521296761538929105.webp");

        // 渲染第一筆資料
        currentFeaturedIndex = 0;
        renderFeaturedProduct(currentFeaturedIndex);

        // 綁定左右箭頭按鈕事件
        $('#featPrevBtn').off('click').on('click', () => switchFeaturedProduct(-1));
        $('#featNextBtn').off('click').on('click', () => switchFeaturedProduct(1));

        // 綁定熱點點擊事件
        $('.pulse-dot').off('mouseenter').on('mouseenter', function () {
            const productName = $(this).data('product-name');
            if (productName) {
                jumpToFeaturedProductByName(productName);
            }
        });

        // 切換可見性
        $('#featuredSkeleton').addClass('d-none');
        $('#featuredContent').removeClass('d-none');

    } else {
        console.warn("推薦商品資料 (pdData) 為空，無法渲染特色主打區塊。");
        $('#featuredSkeleton').removeClass('d-none');
        $('#featuredContent').addClass('d-none');
    }

    // 4. (已移除舊的 jQuery click 事件綁定，因為現在直接寫在 HTML onclick 屬性中)
}

// =========================================================
// 3. 異步資料載入 
// =========================================================

// 【修改】只呼叫一次整合後的 API
const fetchAllData = fetch("/home") // <--- 呼叫新的統一 API 路徑
    .then(response => {
        if (!response.ok) {
            throw new Error('網路回應錯誤');
        }
        return response.json();
    })
    .then(data => {
        console.log("所有資料載入完成。");
        console.log(data);

        // 【修改】從單一響應結構 HomeDataResponseDto 中分離出三種列表
        productData = data.mainProducts || [];      // 賦值給本月主打 (mainProducts)
        pdData = data.featuredProducts || [];       // 賦值給推薦熱點 (featuredProducts)
        categoryData = data.categories || [];       // 賦值給產品系列 (categories)

        renderData();

        // 重新設定輪播
        setupCarousel('#productsList', '#prevBtn', '#nextBtn', '#progressBar');
        setupCarousel('#seriesList', '#seriesPrevBtn', '#seriesNextBtn', '#seriesProgressBar');
    })
    .catch(error => {
        console.error("總體資料載入流程發生未預期的錯誤:", error);
    });

// =========================================================
// 4. DOM 準備就緒後執行 (初始設定)
// =========================================================
$(document).ready(function () {
    renderSkeletons();
    setupCarousel('#productsList', '#prevBtn', '#nextBtn', '#progressBar');
    setupCarousel('#seriesList', '#seriesPrevBtn', '#seriesNextBtn', '#seriesProgressBar');
});
