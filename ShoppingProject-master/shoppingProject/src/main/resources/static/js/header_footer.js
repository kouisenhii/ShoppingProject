console.log("header.js loaded");



/* ============================
   SweetAlert2 全域設定與封裝
   ============================ */

// 1. 定義「吐司通知」 (Toast)：右上角滑出，3秒後自動消失，適合「加入成功」
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 1500,
    timerProgressBar: true,
    customClass: {
        popup: 'my-toast-popup'
    },
});

// 2. 定義「一般彈窗」：置中，使用者必須按確定，適合「錯誤訊息」或「重要通知」
const showSwal = (title, text, icon = 'success') => {
    Swal.fire({
        title: title,
        text: text,
        icon: icon, // success, error, warning, info, question
        confirmButtonText: '確定',
        confirmButtonColor: '#d6a368', // 使用您的網站主題色 (類似家具的顏色)
    });
}

// 全域變數儲存目前登入者 ID
let currentGlobalUserId = null;

// 更新購物車數量
function updateCartCount(userId) {
    if (!userId) {
        // 如果沒有登入用戶，直接隱藏徽章
        $('#cart-count-badge').hide().text('0');
        return;
    }

    $.ajax({
        url: `/api/cart/count/${userId}`,
        method: 'GET',
        dataType: 'json',
        success: function (count) {
            const $badge = $('#cart-count-badge');

            if (count > 0) {
                $badge.text(count);
                $badge.show();
            } else {
                $badge.text('0');
                $badge.hide();
            }
        },
        error: function (xhr, status, error) {
            console.error("更新購物車數量失敗:", status, error);
            $('#cart-count-badge').hide();
        }
    });
}

/* ==========================================
   修改：更新收藏清單數量 + 渲染下拉選單內容
   ========================================== */
async function updateWishListCount(userId) {
    // 【修正 1】如果呼叫時沒傳參數，就使用全域變數
    const targetUserId = userId || currentGlobalUserId;

    const $badge = $('#wishList-count-badge');
    // 注意：這裡不需要再抓 .wishlist-items，因為 renderWishlistDropdown 會重繪整個 #wishlist-dropdown

    // 如果沒有登入，清空並隱藏 (這裡只處理 UI 隱藏，不寫入 HTML，避免覆蓋掉原本的結構導致樣式跑掉)
    if (!targetUserId) {
        $badge.hide().text('0');
        // 可以在這裡呼叫一個渲染「未登入狀態」的函式，或保持預設
        renderWishlistEmptyState('請先登入'); 
        return;
    }

    try {
        // 1. 呼叫後端 API
        const response = await fetch(`/v1/wishList`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });

        if (!response.ok) throw new Error('無法獲取清單');

        // 2. 取得資料
        const wishListItems = await response.json();
        const totalCount = Array.isArray(wishListItems) ? wishListItems.length : 0;

        // 3. 更新紅點數量 (Badge)
        if (totalCount > 0) {
            $badge.text(totalCount).show();
        } else {
            $badge.text('0').hide();
        }

        // 4. 渲染下拉選單內容
        renderWishlistDropdown(wishListItems);

    } catch (error) {
        console.error("更新收藏清單失敗:", error);
        $badge.hide().text('0');
        renderWishlistEmptyState('載入失敗');
    }
}

/**
 * 【新增】渲染收藏清單下拉選單 HTML
 * 對應 header_footer.css 的樣式
 */
function renderWishlistDropdown(items) {
    const $dropdown = $('#wishlist-dropdown');

    // 1. 如果清單是空的
    if (!items || items.length === 0) {
        renderWishlistEmptyState('目前沒有收藏商品');
        return;
    }

    // 2. 開始建構 HTML
    // (A) 標題區
    let html = `
        <div class="wishlist-header">
            <h6>我的收藏清單 (${items.length})</h6>
        </div>
        <div class="wishlist-items-container">
    `;

    // (B) 商品列表迴圈
    // 注意：請確認您的後端回傳欄位名稱 (例如 pname, price, image)
    // 這裡預設使用常見欄位，若圖片跑不出來，請檢查這裡的欄位名稱
    items.forEach(item => {
        // 容錯處理：如果沒有圖片，顯示預設圖
        const imgUrl = item.image || item.productImage || 'https://placehold.co/60x60?text=No+Image';
        const name = item.pname || item.productName || '未命名商品';
        const price = item.price ? `NT$ ${item.price.toLocaleString()}` : '價格詳洽';
        const pid = item.productId || item.id;

        html += `
            <a href="/html/product.html?id=${pid}" class="wishlist-item-row">
                <div class="wishlist-img-box">
                    <img src="${imgUrl}" alt="${name}">
                </div>
                <div class="wishlist-info">
                    <div class="wishlist-name">${name}</div>
                    <div class="wishlist-price">${price}</div>
                </div>
            </a>
        `;
    });

    html += `</div>`; // 關閉 wishlist-items-container

    // (C) 底部按鈕區
    html += `
        <div class="wishlist-footer">
            <a href="/html/membercenter.html" class="btn-view-all">查看全部收藏</a>
        </div>
    `;

    // 3. 將 HTML 寫入頁面
    $dropdown.html(html);
}

/**
 * 渲染空狀態或錯誤狀態
 */
function renderWishlistEmptyState(message) {
    const $dropdownMenu = $('#wishlist-dropdown');
    let html = `
        <div class="wishlist-header">
            <h6>我的收藏清單</h6>
        </div>
        <div style="padding: 30px 20px; text-align: center; color: #888;">
             <i class="bi bi-heart" style="font-size: 2rem; color: #e0e0e0; display: block; margin-bottom: 10px;"></i>
            <p class="mb-0 small">${message}</p>
        </div>
        <div class="wishlist-footer">
            <a href="/html/membercenter.html" class="btn-view-all">查看全部收藏</a>
        </div>
    `;
    $dropdownMenu.html(html);
}
/**
 * 根據用戶 ID 顯示或隱藏會員 UI
 * 使用 Bootstrap Class 切換，避免 inline-style 破壞 RWD
 * @param {number|null} userId 當前登入的使用者 ID
 */
/**
 * 根據用戶 ID 顯示或隱藏會員 UI (包含手機與桌機)
 */
function updateMemberIcons(userId) {
    // Desktop Icons
    const $memberIcon = $('#member-icon'); // 會員中心圖示 (空心人像)
    const $avatarIcon = $('#avatar-icon'); // 頭像圖示 (圓形圖片)

    // Mobile Elements
    const $mobileLoginBtn = $('#mobile-login-btn');
    const $mobileUserPanel = $('#mobile-user-panel');

    // 清除舊的 inline styles
    $memberIcon.removeAttr('style');
    $avatarIcon.removeAttr('style');
    $mobileLoginBtn.removeAttr('style');
    $mobileUserPanel.removeAttr('style');

    if (userId) {
        // === 【已登入狀態】 ===

        // Desktop: 
        // 關鍵修正：加上 'd-none' 讓它在手機隱藏，加上 'd-lg-flex' 讓它在桌機顯示
        $memberIcon.addClass('d-none d-lg-flex'); 
        $avatarIcon.addClass('d-none d-lg-flex');

        // Mobile: 隱藏登入按鈕，顯示會員面板 (維持原樣)
        $mobileLoginBtn.addClass('d-none').removeClass('d-flex');
        $mobileUserPanel.removeClass('d-none').addClass('d-block');

    } else {
        // === 【未登入狀態】 ===

        // Desktop:
        // 會員中心 ICON：手機隱藏，桌機顯示 (讓桌機使用者可以點擊去登入)
        $memberIcon.addClass('d-none d-lg-flex');
        
        // 頭像 ICON：完全隱藏 (移除 d-lg-flex，保留 d-none)
        $avatarIcon.addClass('d-none').removeClass('d-lg-flex');

        // Mobile: 顯示登入按鈕，隱藏會員面板 (維持原樣)
        $mobileLoginBtn.removeClass('d-none').addClass('d-flex');
        $mobileUserPanel.addClass('d-none').removeClass('d-block');
    }
}

$(document).ready(function () {
    var $backToTop = $('#back-to-top');

    // Scroll Effect for Header and Back-to-Top
    $(window).scroll(function () {
        var scrollTop = $(this).scrollTop();

        // Header Logic
        if (scrollTop > 20) {
            $('#header-wrapper').addClass('scrolled');
        } else {
            $('#header-wrapper').removeClass('scrolled');
        }

        // Back to Top Button Logic
        if (scrollTop > 300) {
            $backToTop.addClass('show');
        } else {
            $backToTop.removeClass('show');
        }
    });

    // Back to Top Click Action
    $backToTop.click(function (e) {
        e.preventDefault();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });



    // Close search when clicking outside
    $(document).mouseup(function (e) {
        var container = $("#search-drawer");
        var button = $("#search-toggle");

        if (!container.is(e.target) && container.has(e.target).length === 0 &&
            !button.is(e.target) && button.has(e.target).length === 0) {
            if (container.is(':visible')) {
                container.slideUp(300);
            }
        }
    });
});

// 加入購物車邏輯
async function addToCart(userid, productid, quantity = 1, isBuyNow = false) {

    // 防呆檢查：如果全域變數是空的，代表沒登入
    if (!currentGlobalUserId) {
        Swal.fire({
            title: '請先登入',
            text: "您必須登入會員才能購物",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '前往登入',
            cancelButtonText: '再逛逛'
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = "/html/login.html";
            }
        });
        return; //  這裡要記得 return，不然程式會繼續往下跑
    }

    const addToCartApi = "/api/cart/add"

    const cartData = {
        userid: currentGlobalUserId,
        productid: productid,
        quantity: quantity
    }

    try {
        const response = await fetch(addToCartApi, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(cartData),
        });

        if (response.ok) {
            const result = await response.json();
            console.log('加入購物車成功:', result);

            //  這裡改用 Toast 通知
            Toast.fire({
                icon: 'success',
                title: '已加入購物車'
            });

            // 更新購物車數量
            if (typeof updateCartCount === 'function') {
                updateCartCount(currentGlobalUserId);
            } else {
                console.error("updateCartCount 函式未定義");
            }

            // === 用來判斷要不要跳轉 ===
            if (isBuyNow) {
                window.location.href = '/html/cart.html';
            }
            // 一般加入購物車不需要 else alert，因為上面已經有 Toast 了

        } else {
            // 1. 嘗試解析後端回傳的 JSON 錯誤訊息 (例如: "庫存不足...")
            let errorMsg = '無法加入購物車，請稍後再試。';
            try {
                const errorJson = await response.json();
                if (errorJson.message) {
                    errorMsg = errorJson.message;
                }
            } catch (e) {
                // 解析失敗就用預設訊息
            }

            // 2. 直接在這裡顯示 SweetAlert，並綁定「重整」邏輯
            Swal.fire({
                icon: 'error',
                title: '加入失敗',
                text: errorMsg, // 這裡會顯示後端的 "商品庫存不足"
                confirmButtonText: '重新整理頁面',
                confirmButtonColor: '#b85c38',
                allowOutsideClick: false
            }).then((result) => {
                // 3. 使用者按下確定後，強制重整
                if (result.isConfirmed) {
                    location.reload();
                }
            });

            // 4. 重要：回傳一個 "rejected" 的 Promise，讓呼叫端知道失敗了，不要繼續往下跑
            // 但因為 UI 已經在這裡處理完了，我們丟一個特殊的錯誤標記，或者單純 throw
            throw new Error("HANDLED_ERROR");
        }
    } catch (error) {
        if (error.message === "HANDLED_ERROR") return;
        console.error('加入購物車時發生錯誤:', error);
        //  這裡改用 showSwal 顯示錯誤
        showSwal('加入失敗', '無法加入購物車，請稍後再試。', 'error');
    }
}


// C. 監聽上方「搜尋框」輸入 (按下 Enter)
$("#search-input").on("keyup", function (e) {
    if (e.key === 'Enter') {
        e.preventDefault();
        const currentKeyword = $(this).val();

        // 如果使用者已經在搜尋頁 (search.html)，就不要 reload 頁面
        if (window.location.pathname.includes('/search.html') || window.location.pathname.includes('/search')) {
            // 這裡什麼都不用做，因為 search.js 的 debounce 已經會處理搜尋了
            // 只要把搜尋框收起來就好
            $("#search-drawer").slideUp(300);
            $("#search-input").blur(); // 移除焦點，收起手機鍵盤
            return;
        }

        // 如果在其他頁面 (如首頁)，才執行跳轉
        if (currentKeyword !== "") {
            window.location.href = `/search.html?keyword=${encodeURIComponent(currentKeyword)}`
        }
        $("#search-drawer").slideUp(300);
    }
});

// D. 監聽上方「搜尋圖示」點擊
$("#search-icon").parent().on("click", function (e) {
    e.preventDefault();
    const currentKeyword = $("#search-input").val();

    // 如果使用者已經在搜尋頁 (search.html)，就不要 reload 頁面
    if (window.location.pathname.includes('/search.html') || window.location.pathname.includes('/search')) {

        // 這裡什麼都不用做，因為 search.js 的 debounce 已經會處理搜尋了
        // 只要把搜尋框收起來就好
        $("#search-drawer").slideUp(300);
        $("#search-input").blur(); // 移除焦點，收起手機鍵盤
        return;
    }

    // 如果在其他頁面 (如首頁)，才執行跳轉
    if (currentKeyword !== "") {
        window.location.href = `/search.html?keyword=${encodeURIComponent(currentKeyword)}`
    }
    $("#search-drawer").slideUp(300);
});

// E.登入後跳轉會員中心
function handleMemberIconClick(e) {
    e.preventDefault(); // 阻止<a>標籤的預設行為

    // 假設 currentGlobalUserId 是一個全域變數，用於儲存登入用戶的 ID
    if (currentGlobalUserId) {
        // 1. 已登入：直接跳轉到會員中心頁面
        window.location.href = "/html/membercenter.html";
    } else {
        // 2. 未登入：彈出提示，引導至登入頁面
        Swal.fire({
            title: '請先登入',
            text: "您必須登入會員才能進入會員中心",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '前往登入',
            cancelButtonText: '取消'
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = "/login.html"; // 假設登入頁面的路徑
            }
        });
    }
}

// F.加入收藏清單
async function addToWishList(productid) {

    // 防呆檢查：如果全域變數是空的，代表沒登入
    if (!currentGlobalUserId) {
        Swal.fire({
            title: '請先登入',
            text: "您必須登入會員才能購物",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '前往登入',
            cancelButtonText: '再逛逛'
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = "/html/login.html";
            }
        });
        return;
    }

    const addToWishListApi = "/v1/wishList/items";

    // 關鍵修正：移除 userid，因為後端應從 Spring Security Context 提取
    const wishListData = {
        productId: productid
    };

    try {
        const response = await fetch(addToWishListApi, {
            method: 'POST',
            headers: {
                // 假設您的 Token 或 Session Cookie 會自動隨請求發送
                'Content-Type': 'application/json',
            },
            // 關鍵修正：body 變數名稱從 cartData 改為 wishListData
            body: JSON.stringify(wishListData),
        });

        if (response.ok) {
            const result = await response.json();
            console.log('加入收藏清單成功:', result);

            Toast.fire({
                icon: 'success',
                title: '已加入收藏清單'
            });

            // 更新收藏清單數量
            if (typeof updateWishListCount === 'function') {
                // 【修正 2】雖然我們改了 updateWishListCount 讓它自動抓 ID
                // 但明確傳入 currentGlobalUserId 是個好習慣
                updateWishListCount(currentGlobalUserId); 
            } else {
                console.error("updateWishListCount 函式未定義");
            }

        } else {
            let errorData;
            try {
                // 嘗試將錯誤響應體解析為 JSON
                errorData = await response.json();
            } catch (e) {
                // 如果不是 JSON，則取純文字
                errorData = { message: await response.text() || `HTTP 狀態碼: ${response.status}` };
            }

            // 拋出包含後端詳細訊息的錯誤
            throw new Error(errorData.message || `API 錯誤：HTTP 狀態碼 ${response.status}`);
        }
    } catch (error) {
        console.error('加入收藏清單時發生錯誤:', error);

        // 取得錯誤訊息，如果 error.message 存在，就顯示它；否則顯示預設訊息
        const displayMessage = error.message || '無法加入收藏清單，請稍後再試。';

        showSwal('加入失敗', displayMessage, 'error');
    }
}

// G.
// ==========================================================
// 收藏清單浮動區塊互動邏輯 (Wishlist Dropdown)
// ==========================================================

// A. 監聽點擊按鈕 (修正 ID 為 #wishList，注意大小寫)
$('#wishList').on('click', function (e) {
    e.preventDefault();

    // 取得浮動區塊本身
    var $dropdownMenu = $('#wishlist-dropdown');

    // 切換顯示/隱藏
    $dropdownMenu.toggleClass('show');

    // 阻止冒泡，避免觸發 document 的點擊關閉事件
    e.stopPropagation();
});

// B. 點擊畫面其他地方時關閉浮動清單
const wishlistBtn = document.getElementById('wishList'); // 注意這裡 ID 也要改對
const wishlistMenu = document.getElementById('wishlist-dropdown');

if (wishlistBtn && wishlistMenu) {
    document.addEventListener('click', function (event) {
        // 檢查點擊是否在按鈕或選單內部
        const isClickInside = wishlistBtn.contains(event.target) || wishlistMenu.contains(event.target);

        // 如果點擊在外部，且選單是開著的，就關閉它
        if (!isClickInside && wishlistMenu.classList.contains('show')) {
            wishlistMenu.classList.remove('show');
        }
    });
    
    // 防止點擊選單內部時關閉
    wishlistMenu.addEventListener('click', function (event) {
        event.stopPropagation();
    });
}
/* ============================
   jQuery 登入狀態控制（結合 updateMemberIcons）
   ============================ */

var currentProvider = "local";

$(document).ready(async function () {
    // Desktop 元素
    const $avatarIcon = $("#avatar-icon");
    const $memberIcon = $("#member-icon");
    // Mobile 元素
    const $mobileAvatar = $("#mobile-user-avatar");
    const $mobileName = $("#mobile-user-name");
    const $mobileLogoutBtn = $("#mobile-logout-btn");

    /* ============================
   修改 header_footer.js
   將讀取使用者資料的邏輯獨立出來
   ============================ */

    // 1. 定義一個全域函式，讓 member.js 可以呼叫
    window.reloadHeaderUser = async function () {
        // Desktop 元素
        const $avatarIcon = $("#avatar-icon");
        const $memberIcon = $("#member-icon");
        // Mobile 元素
        const $mobileAvatar = $("#mobile-user-avatar");
        const $mobileName = $("#mobile-user-name");
        const $mobileLogoutBtn = $("#mobile-logout-btn");

        try {
            // 加上時間戳記避免瀏覽器快取舊圖片
            const response = await fetch(`/api/user/me?t=${new Date().getTime()}`, {
                method: "GET",
                credentials: "include"
            });

            /* ========== 未登入 ========== */
            if (!response.ok) {
                console.log("未登入");
                updateMemberIcons(null);

                // 綁定點擊事件導向登入
                const redirectToLogin = function (e) {
                    e.preventDefault();
                    window.location.href = "/html/login.html";
                };
                $memberIcon.off("click").on("click", redirectToLogin);
                return;
            }

            /* ========== 已登入 ========== */
            const user = await response.json();

            if (user.role === 'ROLE_ADMIN') {
                $('#admin-btn').show();
            }

            currentProvider = user.provider?.toLowerCase() || "local";
            currentGlobalUserId = user.userId;
            // console.log(user); // 除錯用，可註解掉

            // --- 圖片處理邏輯 (與 member.js 統一) ---
            let avatarSrc;
            if (user.avatar) {
                if (user.avatar.startsWith('data:') || user.avatar.startsWith('http')) {
                    avatarSrc = user.avatar;
                } else {
                    avatarSrc = `data:image/jpeg;base64,${user.avatar}`;
                }
            } else {
                const seedName = user.name || user.username || "User";
                avatarSrc = `https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(seedName)}`;
            }

            const userNameText = user.name || '會員';

            // 更新 UI
            $avatarIcon.find("img").attr("src", avatarSrc);
            $avatarIcon.attr("title", `嗨，${userNameText}`);
            $mobileAvatar.attr("src", avatarSrc);
            $mobileName.text(userNameText);

            updateMemberIcons(user.userId);
            updateCartCount(user.userId);
            updateWishListCount();

            // 綁定登出
            const handleLogout = function (e) {
                e.preventDefault();
                confirmLogout(); // 使用已抽離的函式
            };

            $avatarIcon.off("click").on("click", handleLogout);
            $mobileLogoutBtn.off("click").on("click", handleLogout);

        } catch (err) {
            console.error("Header API error:", err);
            updateMemberIcons(null);
        }
    };

// 2. 頁面載入時，執行一次這個函式
    $(document).ready(async function () {
        
        // 核心修正：只呼叫定義好的 reloadHeaderUser，不要在這裡重寫 fetch 邏輯
        if (window.reloadHeaderUser) {
            await window.reloadHeaderUser();
        }

        // 注意：請保留原本在此處的 Scroll 與 Search 相關 UI 邏輯 (如果有的話)
        var $backToTop = $('#back-to-top');

        // Scroll Effect
        $(window).scroll(function () {
            var scrollTop = $(this).scrollTop();
            if (scrollTop > 20) {
                $('#header-wrapper').addClass('scrolled');
            } else {
                $('#header-wrapper').removeClass('scrolled');
            }
            if (scrollTop > 300) {
                $backToTop.addClass('show');
            } else {
                $backToTop.removeClass('show');
            }
        });

        // Back to Top Click
        $backToTop.click(function (e) {
            e.preventDefault();
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });

        // Search Toggle logic... (保留您原本的搜尋邏輯)
        $('#search-toggle').click(function (e) {
            e.preventDefault();
            $('#search-drawer').slideToggle(300, function () {
                if ($(this).is(':visible')) $('#search-input').focus();
            });
        });
        
        $(document).mouseup(function (e) {
            var container = $("#search-drawer");
            var button = $("#search-toggle");
            if (!container.is(e.target) && container.has(e.target).length === 0 &&
                !button.is(e.target) && button.has(e.target).length === 0) {
                if (container.is(':visible')) container.slideUp(300);
            }
        });
    });

    // ==========================================
    // 3. 把登出功能抽離出來 (這是 header.html 手機版也會用到的)
    // ==========================================

    // 登出確認視窗
    window.confirmLogout = function() {
        Swal.fire({
            title: '確定要登出嗎？',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '登出',
            cancelButtonText: '取消'
        }).then((result) => {
            if (result.isConfirmed) {
                logout();
            }
        });
    }

    // 實際執行登出
    async function logout() {
        try {
            // 確保 currentProvider 有值，預設為 local
            const provider = (typeof currentProvider !== 'undefined' && currentProvider) ? currentProvider : 'local';
            
            await fetch(`/api/logout?provider=${provider}`, {
                method: "POST",
                credentials: "include"
            });

            // 登出成功提示
            if (typeof Toast !== 'undefined') {
                Toast.fire({ icon: 'success', title: '已成功登出' });
            }

            // 延遲一點點再跳轉
            setTimeout(() => {
                window.location.href = "/html/login.html";
            }, 500);

        } catch (err) {
            console.error("登出失敗：", err);
            if (typeof showSwal !== 'undefined') {
                showSwal('登出失敗', '請稍後再試', 'error');
            }
        }
    }

    // 把登出確認視窗抽出來，讓手機跟電腦版共用
    function confirmLogout() {
        Swal.fire({
            title: '確定要登出嗎？',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '登出',
            cancelButtonText: '取消'
        }).then((result) => {
            if (result.isConfirmed) {
                logout();
            }
        });
    }

    /* ==========================================
       搜尋自動補全功能 (Autocomplete)
       ========================================== */

    /**
     * 防抖函式 (Debounce) - 避免使用者打字時頻繁呼叫 API
     * 如果你的專案其他地方也有定義，可以共用，不用重複寫
     */
    function headerDebounce(func, delay) {
        let timer;
        return function (...args) {
            if (timer) clearTimeout(timer);
            const context = this;
            timer = setTimeout(() => {
                func.apply(context, args);
            }, delay);
        };
    }

    // 綁定邏輯
    $(document).ready(function () {

        // 1. 監聽搜尋框輸入事件
        $("#search-input").on("input", headerDebounce(function () {
            const val = $(this).val().trim();
            const $list = $("#suggestion-list");

            // 如果字數太少，隱藏選單
            if (val.length < 1) {
                $list.hide().empty();
                return;
            }

            // 呼叫你的後端 API
            $.ajax({
                url: `/api/products/search`,
                method: "GET",
                data: {
                    keyword: val,
                    page: 0,
                    size: 50  // 抓多點資料用來跨過那些 (同名不同色) 的重複商品
                },
                success: function (response) {
                    const products = response.content ? response.content : response;

                    if (products && products.length > 0) {
                        let html = '';
                        const uniqueNames = new Set(); // 用來過濾重複名稱
                        let count = 0 // 用來計算實際顯示了幾個 (不同名稱) 的商品

                        products.forEach(p => {
                            // 檢查名稱是否重複，避免出現一堆一樣的商品名
                            if (!uniqueNames.has(p.pname) && count < 10) {
                                uniqueNames.add(p.pname);
                                count++;

                                // 這裡使用 escapeHtml 防止 XSS (簡單實作)
                                const safeName = p.pname.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
                                const safeDescription = p.description.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");

                                html += `
                                <li class="suggestion-item" data-val="${safeName}">
                                    <i class="bi bi-search" style="margin-right: 10px; color: #999;"></i>
                                    ${safeName} ${safeDescription}
                                </li>
                            `;
                            }
                        });

                        if (html) {
                            $list.html(html).show();
                        } else {
                            $list.hide();
                        }
                    } else {
                        $list.hide();
                    }
                },
                error: function (err) {
                    // API 錯誤時安靜失敗，不打擾使用者
                    console.error("搜尋建議 API 錯誤:", err);
                    $list.hide();
                }
            });
        }, 500)); // 延遲 500ms

        // 2. 點擊建議選項的事件
        $(document).on("click", ".suggestion-item", function () {
            const text = $(this).data("val");

            // 填入輸入框 (視覺回饋)
            $("#search-input").val(text);
            $("#suggestion-list").hide();

            // 直接跳轉到搜尋結果頁
            window.location.href = `/search.html?keyword=${encodeURIComponent(text)}`;
        });

        // 3. 點擊畫面其他地方時關閉選單 (優化體驗)
        $(document).on("click", function (e) {
            // 如果點擊的目標不是 搜尋框 也不是 建議清單
            if (!$(e.target).closest(".search-input-wrapper").length) {
                $("#suggestion-list").hide();
            }
        });


        //4.會員中心

        $(document).ready(function () {
            // 綁定 member-icon 的點擊事件
            $('#member-icon').on('click', handleMemberIconClick);

            // 注意：如果您的頭像圖標是另一個 ID (例如 #avatar-icon 或 #avatar-dropdown-container)
            // 且該圖標在未登入時會被隱藏，則無需對 #member-icon 進行額外處理。
            // 如果 #member-icon 始終顯示，則這個綁定是必要的。
        });


    })
})