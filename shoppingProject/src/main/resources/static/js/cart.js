/* ============================
   購物車功能邏輯 (cart.js)
   需依賴 header_footer.js 中的 SweetAlert 設定
   ============================ */

//  改用 var，防止重複宣告報錯
var loggedInUserId = null;

$(document).ready(function () {

    // ----------------------------------------
    // 【 輔助函式 】
    // ----------------------------------------

    // 數字格式化 (加入千分位逗號)
    function formatCurrency(num) {
        if (isNaN(num)) return '0';
        return parseInt(num).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }

    // ----------------------------------------
    // 【 API 呼叫與資料載入 】
    // ----------------------------------------

    async function loadCartData() {
        try {
            // 1. 先呼叫身分驗證 API
            const userResponse = await fetch("/api/user/me");

            if (!userResponse.ok) {
                // 未登入提示 (Swal)
                Swal.fire({
                    title: '請先登入',
                    text: "您必須登入會員才能查看購物車",
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#d6a368',
                    cancelButtonColor: '#d33',
                    confirmButtonText: '前往登入',
                    cancelButtonText: '再逛逛'
                }).then((result) => {
                    if (result.isConfirmed) {
                        window.location.href = "/html/login.html";
                    } else {
                        displayEmptyCart();
                    }
                });
                return; // 結束執行
            }

            const userData = await userResponse.json();
            loggedInUserId = userData.userId; // 更新全域變數
            const currentUserId = userData.userId;

            console.log("當前登入者 ID:", currentUserId);

            // 2. 組裝 API URL 並載入購物車
            const apiUrl = `/api/cart/${currentUserId}`;

            $.ajax({
                url: apiUrl,
                method: 'GET',
                dataType: 'json',
                success: function (data) {
                    if (data && data.length > 0) {
                        renderCart(data);
                    } else {
                        displayEmptyCart();
                    }
                },
                error: function (xhr, status, error) {
                    console.error("購物車載入失敗:", error);
                    displayEmptyCart(true);
                }
            });

        } catch (err) {
            console.error("無法取得使用者身分:", err);
            displayEmptyCart(true);
        }
    }

    // ----------------------------------------
    // 【 資料渲染與計算總額 】
    // ----------------------------------------

    let currentCartItems = []; // 這是函式內的變數，用 let 沒關係

    function renderCart(items) {
        currentCartItems = items;
        let totalItemsCount = 0;
        let totalPrice = 0;
        const $cartBody = $('#cart-body');
        $cartBody.empty();

        items.forEach((item, index) => {
            const subtotal = item.price * item.quantity;
            totalItemsCount += item.quantity;
            totalPrice += subtotal;

            if (index === 0 && item.address) {
                $("#user-addr").text(item.address);
            }

            const itemRow = `
                <tr data-id="${item.cartid}" data-price="${item.price}" data-index="${index}">
                    <td>
                        <div class="product-img-container">
                            <img src="${item.productimage}" alt="${item.pname}" class="img-fluid" style="max-height: 80px;">
                        </div>
                    </td>
                    <td>
                        <div class="product-info">
                            <span class="product-name">${item.pname}</span>
                            <span class="product-specs">${item.specification}</span>
                        </div>
                    </td>
                    <td class="product-price">NT$${formatCurrency(item.price)}</td>
                    <td>
                        <div class="qty-control">
                            <button type="button" class="btn-qty minus">-</button>
                            <input class="qty-input" value="${item.quantity}"> 
                            <button type="button" class="btn-qty plus">+</button>
                         </div>
                    </td>
                    <td class="item-subtotal">NT$${formatCurrency(subtotal)}</td>
                    <td>
                        <button class="btn-remove" title="移除商品">
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </td>
                </tr>
            `;
            $cartBody.append(itemRow);
        });

        updateSummary(totalItemsCount, totalPrice);
        $('.table-responsive').show();
        $('#total-section').show();
        $('#empty-message').addClass('d-none');
    }

    function updateSummary(count, total) {
        $('#total-count').text(count);
        $('#total-val').text(`NT$${formatCurrency(total)}`);
    }

    function displayEmptyCart(isError = false) {
        $('.table-responsive').hide();
        $('#total-section').hide();
        $('#empty-message').removeClass('d-none');

        if (isError) {
            $('#empty-message h4').text("載入失敗");
            $('#empty-message p').text("請檢查網路連線或稍後再試。");
        } else {
            $('#empty-message h4').text("您的購物車是空的");
            $('#empty-message p').text("去選購一些喜歡的家具吧。");
        }
    }


    // ----------------------------------------
    // 【 互動事件綁定 】
    // ----------------------------------------

    // 數量修改
    $(document).on('click', '.btn-qty.plus, .btn-qty.minus', function () {
        const $row = $(this).closest('tr');
        const $input = $row.find('.qty-input');
        let quantity = parseInt($input.val());
        const max = 99;

        if ($(this).hasClass('plus')) {
            quantity = quantity < max ? quantity + 1 : max;
        } else if ($(this).hasClass('minus')) {
            quantity = quantity > 0 ? quantity - 1 : 0;
        }

        // 當數量為 0 時，跳出 Swal 確認視窗
        if (quantity === 0) {
            Swal.fire({
                title: '確定要移除嗎？',
                text: "數量為 0 將會移除此商品",
                icon: 'warning',
                showCancelButton: true,
                confirmButtonColor: '#d6a368',
                cancelButtonColor: '#d33',
                confirmButtonText: '移除',
                cancelButtonText: '取消'
            }).then((result) => {
                if (result.isConfirmed) {
                    $row.fadeOut(300, function () {
                        $(this).remove();
                    });
                    updateItem($row, 0); // 執行刪除 API
                } else {
                    $input.val(1); // 取消則恢復為 1
                    updateItem($row, 1);
                }
            });
        } else {
            $input.val(quantity);
            updateItem($row, quantity);
        }
    });

    // 數量手動輸入
    $(document).on('change', '.qty-input', function () {
        const $row = $(this).closest('tr');
        let quantity = parseInt($(this).val());

        if (isNaN(quantity) || quantity < 1) {
            quantity = 1;
            $(this).val(1);
        }
        updateItem($row, quantity);
    });

    // 商品移除 (垃圾桶圖示)
    $(document).on('click', '.btn-remove', function () {
        const $row = $(this).closest('tr');
        const cartId = $row.data('id');

        if (!cartId) return;

        // SweetAlert 確認視窗
        Swal.fire({
            title: '確定要移除嗎？',
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '是的，移除',
            cancelButtonText: '取消'
        }).then((result) => {
            if (result.isConfirmed) {
                $row.fadeOut(300, function () {
                    $(this).remove();

                    $.ajax({
                        url: `/api/cart/${cartId}`,
                        method: 'DELETE',
                        success: function (response) {
                            // 刪除成功提示 Toast
                            Toast.fire({
                                icon: 'success',
                                title: '商品已移除'
                            });

                            const indexToDelete = currentCartItems.findIndex(item => item.cartid === cartId);
                            if (indexToDelete > -1) {
                                currentCartItems.splice(indexToDelete, 1);
                            }
                            recalculateTotal();
                        },
                        error: function (xhr) {
                            console.error(`刪除失敗:`, xhr.responseText);
                            showSwal('刪除失敗', '請重新整理頁面試試', 'error');
                            loadCartData();
                        }
                    });
                });
            }
        });
    });

    // 重新計算總額
    function recalculateTotal() {
        let newTotalCount = 0;
        let newTotalPrice = 0;

        currentCartItems.forEach(item => {
            newTotalCount += item.quantity;
            newTotalPrice += item.price * item.quantity;
        });

        updateSummary(newTotalCount, newTotalPrice);

        if (currentCartItems.length === 0) {
            displayEmptyCart();
        }
    }

    // 更新單項商品 API
    function updateItem($row, newQuantity) {
        const cartId = $row.data('id');
        const price = parseFloat($row.data('price'));
        const subtotal = price * newQuantity;

        if (!cartId) return;

        if (newQuantity > 0) {
            $row.find('.item-subtotal').text(`NT$${formatCurrency(subtotal)}`);
            const itemIndex = $row.data('index');
            if (currentCartItems[itemIndex]) {
                currentCartItems[itemIndex].quantity = newQuantity;
            }
        }

        $.ajax({
            url: `/api/cart/quantity`,
            method: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({
                "cartId": cartId,
                "quantity": newQuantity
            }),
            success: function (response) {
                console.log(`更新成功: ${newQuantity}`);
                recalculateTotal();
            },
            error: function (xhr) {
                console.error("更新失敗:", xhr.responseText);
                Toast.fire({
                    icon: 'error',
                    title: '數量更新失敗'
                });
                loadCartData();
            }
        });
        recalculateTotal();
    }

    // 運送方式切換
    $('.shipping-card').on('click', function () {
        $('.shipping-card').removeClass('active');
        $(this).addClass('active');

        const type = $(this).data('type');

        if (type === 'cvs') {
            $('#cvs-map-section').removeClass('d-none');
        } else {
            $('#cvs-map-section').addClass('d-none');
            $('#input-store-id').val('');
        }
    });

    // ----------------------------------------
    // 【 物流地圖與綠界整合 】
    // ----------------------------------------

    window.openMap = function (subType) {
        const form = $('<form>', {
            'action': '/api/ecpay/map',
            'method': 'POST'
        });
        form.append($('<input>', {
            'type': 'hidden',
            'name': 'logisticsSubType',
            'value': subType
        }));
        $('body').append(form);
        form.submit();
    };

    // 處理綠界回傳參數
    // 改用 var，防止重複宣告報錯
    var urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('storeId')) {
        const storeId = urlParams.get('storeId');
        const storeName = urlParams.get('storeName');
        const address = urlParams.get('address');
        const subType = urlParams.get('type');

        if (storeId) {
            $('.shipping-card[data-type="cvs"]').trigger('click');

            $('#input-store-id').val(storeId);
            $('#input-store-name').val(storeName);
            $('#input-store-address').val(address);
            $('#input-logistics-subtype').val(subType);

            $('#display-store-name').text(storeName);
            $('#display-store-id').text(`(${storeId})`);
            $('#display-store-addr').text(address);
            $('#store-info-display').removeClass('d-none');

            window.history.replaceState({}, document.title, window.location.pathname);
        }
    }

    // ----------------------------------------
    // 【 結帳邏輯 】
    // ----------------------------------------

    $(document).on('click', '.btn-next-step', function (e) {
        e.preventDefault();

        if (currentCartItems.length === 0) {
            showSwal('購物車是空的', '請先選購商品再結帳', 'warning');
            return;
        }

        const shippingType = $('.shipping-card.active').data('type');
        let address = "";
        let logisticsType = "HOME";
        let storeId = "";
        let storeName = "";
        let logisticsSubType = "";

        if (shippingType === 'cvs') {
            logisticsType = "CVS";
            storeId = $('#input-store-id').val();
            storeName = $('#input-store-name').val();
            address = $('#input-store-address').val();
            logisticsSubType = $('#input-logistics-subtype').val();

            if (!storeId) {
                showSwal('請選擇門市', '超商取貨必須選擇取貨門市', 'warning');
                $('html, body').animate({
                    scrollTop: $("#cvs-map-section").offset().top - 100
                }, 500);
                return;
            }
        } else {
            logisticsType = "HOME";
            address = $('#user-addr').text().trim();
            if (!address || address === "輸入配送地址" || address === "undefined") {
                showSwal('請確認地址', '您的配送地址似乎有誤', 'warning');
                return;
            }
        }

        const checkoutData = {
            userId: loggedInUserId,
            address: address,
            paymentMethod: "Credit",
            logisticsType: logisticsType,
            logisticsSubType: logisticsSubType,
            storeId: storeId,
            storeName: storeName
        };

        const $btn = $(this);
        $btn.text('處理中...').prop('disabled', true);

        // 呼叫後端建立訂單
        $.ajax({
            url: '/api/orders/checkout',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(checkoutData),
            success: function (orderResponse) {
                // 檢查後端回傳的資料是否包含有效的 orderId
                // 如果後端在庫存不足時回傳的是 200 OK，但內容包含錯誤訊息，這裡要攔截

                if (!orderResponse || !orderResponse.orderId) {
                    console.warn("訂單未成功建立，後端回傳:", orderResponse);

                    // 嘗試抓取後端回傳的錯誤訊息 (假設欄位是 message)
                    let errorMsg = orderResponse.message || '庫存不足或訂單建立失敗，請稍後再試';

                    showSwal('結帳失敗', errorMsg, 'error');

                    // 記得要把按鈕恢復，不然使用者會卡在 "處理中..."
                    $btn.text('前往結帳').prop('disabled', false);

                    return; // ⛔️ 重要：直接結束函式，不執行下面的 callEcpayCheckout
                }

                const newOrderId = orderResponse.orderId;
                callEcpayCheckout(newOrderId);
            },
            error: function (xhr) {
                console.error("結帳失敗:", xhr.responseText);
                let errorMsg = '系統發生錯誤，請稍後再試';

                // 解析後端回傳的 JSON 錯誤訊息
                try {
                    const errorJson = JSON.parse(xhr.responseText);
                    // 如果有 message 欄位，使用它
                    if (errorJson && errorJson.message) {
                        errorMsg = errorJson.message;
                    }
                } catch (e) {
                    // 解析失敗，保持預設錯誤訊息
                    if (xhr.responseText) {
                        errorMsg = xhr.responseText;
                    }
                }

                showSwal('結帳失敗', errorMsg, 'error');
                $btn.text('前往結帳').prop('disabled', false);
            }
        });
    });

    function callEcpayCheckout(orderId) {
        $.ajax({
            url: `/api/ecpay/checkout/${orderId}`,
            method: 'POST',
            success: function (params) {
                const form = $('<form action="https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5" method="POST"></form>');
                for (const key in params) {
                    form.append($(`<input type="hidden" name="${key}" value="${params[key]}">`));
                }
                $('body').append(form);
                form.submit();
            },
            error: function (err) {
                showSwal('金流連線失敗', '無法連接綠界金流', 'error');
            }
        });
    }

    loadCartData();
});