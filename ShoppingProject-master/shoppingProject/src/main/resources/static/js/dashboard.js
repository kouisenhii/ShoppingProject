// --- CONSTANTS & DATA ---
let globalCategoryList = [];


// --- UTILS ---
const getCategoryName = (id) => {
    // 從全域變數中尋找
    const cat = globalCategoryList.find(c => c.categoryid === id);
    // 如果找到了回傳 cname，沒找到(或資料還沒載入)則回傳 ID
    return cat ? cat.cname : `Category-${id}`;
};
const formatMoney = (amount) => `NT$ ${amount.toLocaleString()}`;
const getStatusBadge = (status) => {
    const map = {
        0: { text: '處理中', class: 'bg-warning-subtle text-warning-emphasis' },
        1: { text: '運送中', class: 'bg-primary-subtle text-primary-emphasis' },
        2: { text: '已送達', class: 'bg-indigo-50 text-indigo-600' },
        3: { text: '已完成', class: 'bg-success-subtle text-success-emphasis' }
    };
    const s = map[status] || { text: '未知', class: 'bg-secondary-subtle text-secondary-emphasis' };
    return `<span class="badge rounded-pill ${s.class} fw-semibold">${s.text}</span>`;
};

// --- RENDER FUNCTIONS ---

function renderDashboard() {
    // 呼叫後端 API 取得真實統計數據
    $.ajax({
        url: '/api/admin/stats',
        method: 'GET',
        success: function(stats) {
            // stats 結構: { totalSales, totalOrders, totalUsers, avgOrderValue }
            
            // 1. 更新數字 (使用 formatMoney 加上千分位與幣別)
            $('#stat-sales').text(formatMoney(stats.totalSales));
            $('#stat-orders').text(stats.totalOrders.toLocaleString());
            $('#stat-users').text(stats.totalUsers.toLocaleString());
            $('#stat-avg').text(formatMoney(stats.avgOrderValue));

            // 2. 處理庫存預警 (維持原本邏輯，這裡假設 PRODUCTS 已經是透過 API 載入的)
            // 如果 PRODUCTS 還是空的，可能需要確保 renderDashboard 在 loadProducts 之後執行
            // 或者這裡單獨呼叫一次庫存檢查 API (為了簡單起見，我們先保留原本的前端過濾邏輯)
            updateStockAlert();
        },
        error: function(err) {
            console.error('無法載入營運數據', err);
            $('#stat-sales').text('Error');
        }
    });

    // Init Charts (圖表部分暫時維持原樣，之後可以再做 API 化)
    initCharts();
}

// 【新增】將庫存預警邏輯獨立出來
function updateStockAlert() {
    // 這裡使用 global 的 PRODUCTS 資料 (如果已經 API 化，請確保資料已載入)
    // 由於我們之前改寫了 renderProducts，這裡我們直接發一個請求去後端抓「庫存少於 15」的商品最準
    
    $.ajax({
        url: '/api/admin/products?size=100&sortType=STOCK_ASC', // 抓前 100 筆庫存最少的
        method: 'GET',
        success: function(data) {
            const products = data.content || [];
            // 過濾出庫存 < 15 的
            const lowStock = products.filter(p => p.stock < 15);
            
            const alertRows = lowStock.length ? lowStock.map(p => `
                <tr class="hover-bg-gray-50">
                    <td class="ps-4">
                        <div class="d-flex align-items-center">
                            <img src="${p.productimage || 'https://via.placeholder.com/40'}" class="rounded rounded-3 me-3" width="32" height="32" style="object-fit: cover;">
                            <span class="fw-medium">${p.pname}</span>
                        </div>
                    </td>
                    <td class="text-secondary small">${p.color || '-'}</td>
                    <td class="fw-medium">${formatMoney(p.price)}</td>
                    <td><span class="text-danger fw-bold">${p.stock}</span></td>
                    <td><span class="badge bg-danger-subtle text-danger rounded-pill">補貨</span></td>
                </tr>
            `).join('') : `<tr><td colspan="5" class="text-center py-4 text-secondary">目前庫存充足</td></tr>`;
            
            $('#stock-alert-body').html(alertRows);
        }
    });
}

// 全域變數：紀錄當前頁碼與每頁筆數
let currentProductPage = 0;
const pageSize = 10;

// 【修改】渲染商品列表 (支援分頁)
function renderProducts(page = 0) {
    currentProductPage = page; // 更新當前頁碼

    const term = $('#product-search').val().trim();
    const catId = $('#product-category-filter').val();
    const sort = $('#product-sort').val();

    let params = {
        sortType: sort,
        page: page,      // 傳送頁碼 (0-based)
        size: pageSize   // 傳送每頁筆數
    };

    if (term) params.keyword = term;
    if (catId !== 'ALL') params.categoryId = catId;

    $.ajax({
        url: '/api/admin/products',
        method: 'GET',
        data: params,
        success: function (data) {
            // 注意：現在 data 是一個 Page 物件
            // data.content = 商品陣列
            // data.totalPages = 總頁數
            // data.totalElements = 總筆數
            // data.number = 當前頁碼
            console.log("後端回傳資料:", data);
            const products = data.content;

            const rows = products.length ? products.map(p => `
                <tr>
                    <td class="ps-4 py-3">
                        <div class="d-flex align-items-center">
                            <img src="${p.productimage || 'https://via.placeholder.com/40'}" class="rounded rounded-3 me-3" width="40" height="40" style="object-fit: cover;">
                            <div>
                                <div class="fw-medium text-dark">${p.pname}</div>
                                <div class="small text-secondary text-truncate" style="max-width: 200px;">${p.description || '-'}</div>
                            </div>
                        </div>
                    </td>
                    <td><span class="badge bg-light text-secondary border fw-normal">${getCategoryName(p.categoryid)}</span></td>
                    <td class="fw-medium">${formatMoney(p.price)}</td>
                    <td><span class="badge rounded-pill ${p.stock > 10 ? 'bg-success-subtle text-success-emphasis' : 'bg-danger-subtle text-danger'}">${p.stock}</span></td>
                    <td class="small text-secondary">${p.color || '-'} / ${p.specification || '-'}</td>
                    <td class="text-end pe-4">
                        <button class="btn btn-link p-0 text-primary me-2"><i class="bi bi-pencil"></i></button>
                        <button class="btn btn-link p-0 text-danger" onclick="deleteProduct(${p.productid})"><i class="bi bi-x-lg"></i></button>
                    </td>
                </tr>
            `).join('') : `<tr><td colspan="6" class="text-center py-5 text-secondary">沒有符合條件的商品</td></tr>`;

            $('#product-table-body').html(rows);

            // 【呼叫】繪製分頁按鈕
            renderPagination(data);
        },
        error: function (err) {
            console.error('載入商品失敗', err);
            $('#product-table-body').html(`<tr><td colspan="6" class="text-center text-danger">載入失敗，請稍後再試</td></tr>`);
        }
    });
}

function renderPagination(data, targetFunc, infoId, ulId) {
    // 確保能讀到正確的分頁資訊物件
    const pageInfo = data.page ? data.page : data;

    const totalPages = pageInfo.totalPages;
    const curr = pageInfo.number;
    const totalElements = pageInfo.totalElements;
    const size = pageInfo.size;

    // 如果沒有資料，清空並返回
    if (!totalElements || totalElements === 0) {
        $(`#${infoId}`).text('無資料');
        $(`#${ulId}`).empty();
        return;
    }

    const startItem = curr * size + 1;
    const endItem = Math.min((curr + 1) * size, totalElements);

    // 【修正】這裡原本寫 data.page.totalPages，改成 totalElements 才是「共 X 筆」
    $(`#${infoId}`).text(`顯示第 ${startItem} - ${endItem} 筆，共 ${totalElements} 筆`);

    let html = '';

    // 上一頁
    const prevDisabled = curr === 0 ? 'disabled' : '';
    html += `<li class="page-item ${prevDisabled}">
                <button class="page-link" onclick="${targetFunc}(${curr - 1})">上一頁</button>
             </li>`;

    // 頁碼 (前2頁 + 當前 + 後2頁)
    let startPage = Math.max(0, curr - 2);
    let endPage = Math.min(totalPages - 1, curr + 2);

    for (let i = startPage; i <= endPage; i++) {
        const active = i === curr ? 'active' : '';
        html += `<li class="page-item ${active}">
                    <button class="page-link" onclick="${targetFunc}(${i})">${i + 1}</button>
                 </li>`;
    }

    // 下一頁
    const nextDisabled = curr === totalPages - 1 ? 'disabled' : '';
    html += `<li class="page-item ${nextDisabled}">
                <button class="page-link" onclick="${targetFunc}(${curr + 1})">下一頁</button>
             </li>`;

    $(`#${ulId}`).html(html);
}
let currentOrderPage = 0; // 全域變數

function renderOrders(page = 0) {
    currentOrderPage = page;
    const term = $('#order-search').val().trim();
    const status = $('#order-status-filter').val();
    const sort = $('#order-sort').val();

    let params = {
        page: page,
        size: 10,
        sortType: sort
    };

    if (term) params.keyword = term;
    if (status !== 'ALL') params.status = status;

    $.ajax({
        url: '/api/admin/orders',
        method: 'GET',
        data: params,
        success: function (data) {
            const pageInfo = data.page ? data.page : data;
            const orders = data.content || [];

            const rows = orders.length ? orders.map(o => `
                <tr>
                    <td class="ps-4 fw-medium text-dark">#${o.orderId}</td>
                    <td class="text-secondary small">${o.orderDate ? o.orderDate.replace('T', ' ').split('.')[0] : '-'}</td>
                    <td class="text-secondary small">User-${o.userId}</td>
                    <td class="fw-medium">${formatMoney(o.totalAmount)}</td>
                    <td>
                        <span class="badge rounded-pill ${o.paymentstatus === 'PAID' ? 'bg-success-subtle text-success-emphasis' : 'bg-secondary-subtle text-secondary'}">
                            ${o.paymentstatus === 'PAID' ? '已付款' : '未付款'}
                        </span>
                    </td>
                    <td>${getStatusBadge(o.orderStatus)}</td>
                    <td class="text-end pe-4">
                        <button class="btn btn-link p-0 text-secondary me-2"><i class="bi bi-printer"></i></button>
                        <button class="btn btn-link p-0 text-primary"><i class="bi bi-eye"></i></button>
                    </td>
                </tr>
            `).join('') : `<tr><td colspan="7" class="text-center py-5 text-secondary">沒有符合條件的訂單</td></tr>`;

            $('#order-table-body').html(rows);

            // 呼叫通用的分頁函式，傳入訂單專用的 ID
            renderPagination(data, 'renderOrders', 'order-page-info', 'order-pagination-ul');
        },
        error: function (err) {
            console.error('載入訂單失敗', err);
            $('#order-table-body').html(`<tr><td colspan="7" class="text-center text-danger">載入失敗</td></tr>`);
        }
    });
}
let currentUserPage = 0; // 全域變數

function renderUsers(page = 0) {
    currentUserPage = page;
    const term = $('#user-search').val().trim();
    const verified = $('#user-verified-filter').val();
    const sort = $('#user-sort').val();

    let params = {
        page: page,
        size: 10,
        sortType: sort
    };

    if (term) params.keyword = term;
    if (verified !== 'ALL') params.verified = verified;

    $.ajax({
        url: '/api/admin/users',
        method: 'GET',
        data: params,
        success: function (data) {
            console.log("會員資料:", data);

            // 處理 Page 物件結構
            const pageInfo = data.page ? data.page : data;
            const users = data.content || [];

            const rows = users.length ? users.map(u => `
                <tr>
                    <td class="ps-4 py-3">
                        <div class="d-flex align-items-center">
                            <img src="${u.icon || 'https://ui-avatars.com/api/?name=' + u.name}" class="rounded-circle border me-3" width="40" height="40">
                            <div>
                                <div class="fw-medium text-dark">${u.name}</div>
                                <div class="small text-secondary">ID: ${u.userid}</div>
                            </div>
                        </div>
                    </td>
                    <td>
                        <div class="small text-dark">${u.phone || '-'}</div>
                        <div class="small text-secondary">${u.email}</div>
                    </td>
                    <td class="small text-secondary text-truncate" style="max-width: 150px;">${u.address || '-'}</td>
                    <td class="small text-secondary">
                        <div>${u.gender === 'M' ? '男' : (u.gender === 'F' ? '女' : '未輸入')}</div>
                        <div class="text-muted" style="font-size: 0.75rem;">${u.birthday ? u.birthday.split('T')[0] : '-'}</div>
                    </td>
                    <td>
                        <span class="badge rounded-pill ${u.verifiedAccount ? 'bg-success-subtle text-success-emphasis' : 'bg-danger-subtle text-danger'} d-inline-flex align-items-center">
                            <i class="bi ${u.verifiedAccount} me-1"></i>
                            ${u.verifiedAccount ? '已驗證' : '未驗證'}
                        </span>
                    </td>
                    <td class="small text-secondary">${u.createdat ? u.createdat.replace('T', ' ').split('.')[0] : '-'}</td>
                    <td class="text-end pe-4">
                        <button class="btn btn-link p-0 text-danger" onclick="deleteUser(${u.userid})"><i class="bi bi-x-lg"></i></button>
                    </td>
                </tr>
            `).join('') : `<tr><td colspan="7" class="text-center py-5 text-secondary">沒有符合條件的會員</td></tr>`;

            $('#user-table-body').html(rows);

            // 呼叫分頁函式 (傳入會員專用的 ID)
            renderPagination(data, 'renderUsers', 'user-page-info', 'user-pagination-ul');
        },
        error: function (err) {
            console.error('載入會員失敗', err);
            $('#user-table-body').html(`<tr><td colspan="7" class="text-center text-danger">載入失敗</td></tr>`);
        }
    });
}

// 刪除會員函式
window.deleteUser = function (id) {
    if (confirm('確定要刪除此會員嗎？此動作無法復原。')) {
        $.ajax({
            url: `/api/admin/users/${id}`,
            method: 'DELETE',
            success: function () {
                renderUsers(currentUserPage);
            },
            error: function () {
                alert('刪除失敗');
            }
        });
    }
};



// --- CHART JS ---
// 全域變數
let salesChart = null;
let categoryChart = null;

// ... 全域變數宣告 ...

function initCharts() {
    // --- 1. Sales Chart (真實 API 資料) ---
    $.ajax({
        url: '/api/admin/stats/trend',
        method: 'GET',
        success: function(data) {
            // data 結構: [{ date: "11/12", total: 12500 }, { date: "12/1", total: 22000 } ...]
            console.log("銷售趨勢資料:", data);

            // 拆分 X 軸 (日期) 與 Y 軸 (金額)
            const labels = data.map(d => d.date);
            const values = data.map(d => d.total);

            const ctxSales = document.getElementById('salesChart');
            if (ctxSales) {
                if (salesChart) salesChart.destroy();

                salesChart = new Chart(ctxSales.getContext('2d'), {
                    type: 'line',
                    data: {
                        labels: labels, // 真實日期
                        datasets: [{
                            label: '銷售額',
                            data: values, // 真實金額
                            borderColor: '#4F46E5',
                            backgroundColor: 'rgba(79, 70, 229, 0.1)',
                            tension: 0.4, // 線條平滑度
                            fill: true,   // 填充背景色
                            pointBackgroundColor: '#4F46E5',
                            pointRadius: 4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        plugins: { 
                            legend: { display: false },
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        return ' NT$ ' + context.parsed.y.toLocaleString();
                                    }
                                }
                            }
                        },
                        scales: {
                            x: { grid: { display: false }, ticks: { color: '#6B7280' } },
                            y: { 
                                grid: { color: '#E5E7EB', borderDash: [4, 4] }, 
                                border: { display: false }, 
                                ticks: { color: '#6B7280' },
                                beginAtZero: true 
                            }
                        }
                    }
                });
            }
        },
        error: function(err) {
            console.error('載入銷售趨勢失敗', err);
        }
    });


    // --- 2. Category Chart (真實 API 資料) ---
    $.ajax({
        url: '/api/admin/stats/categories',
        method: 'GET',
        success: function(data) {
            // data 預期: [{categoryName: "居家家具", count: 5}, {categoryName: "臥室家具", count: 2}...]
            console.log("分類統計資料:", data); // 除錯用

            const catLabels = data.map(d => d.categoryName);
            const catData = data.map(d => d.count);

            const ctxCat = document.getElementById('categoryChart');
            if (ctxCat) {
                if (categoryChart) categoryChart.destroy();

                const catColors = ['#4F46E5', '#10B981', '#F59E0B', '#EF4444']; 

                categoryChart = new Chart(ctxCat.getContext('2d'), {
                    type: 'doughnut',
                    data: {
                        labels: catLabels,
                        datasets: [{
                            data: catData,
                            backgroundColor: catColors,
                            borderWidth: 0
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        cutout: '70%',
                        plugins: { legend: { display: false } }
                    }
                });

                // 繪製自定義圖例 (Legend)
                const total = catData.reduce((a, b) => a + b, 0);
                const legendHtml = catLabels.map((label, i) => {
                    const count = catData[i];
                    // 避免除以 0
                    const percentage = total > 0 ? Math.round((count / total) * 100) : 0;
                    return `
                        <div class="col-6 d-flex align-items-center mb-2">
                            <span style="width: 10px; height: 10px; border-radius: 50%; background: ${catColors[i % catColors.length]}; display: inline-block; margin-right: 8px;"></span>
                            <span class="text-secondary small me-auto">${label}</span>
                            <span class="fw-bold small text-dark">${percentage}%</span>
                        </div>
                    `;
                }).join('');
                
                $('#category-legend').html(legendHtml);
            }
        },
        error: function(err) {
            console.error('載入分類統計失敗', err);
        }
    });
}


// --- MAIN LOGIC ---
$(document).ready(function () {

    $('#product-search, #product-category-filter, #product-sort').off('input change').on('input change', function () {
        renderProducts(0);
    });

    // 1. 【新增】優先載入分類資料
    loadCategories();

    // Initial Render
    renderDashboard();

    // Navigation
    $('.nav-link[data-view]').click(function (e) {
        e.preventDefault();
        $('.nav-link').removeClass('active');
        $(this).addClass('active');

        const viewId = $(this).data('view');
        $('.view-section').removeClass('active');
        $(`#view-${viewId}`).addClass('active');

        // Close mobile menu
        $('.sidebar').removeClass('show');
        $('.overlay').removeClass('show');

        // Render specific view
        if (viewId === 'products') renderProducts();
        if (viewId === 'orders') renderOrders();
        if (viewId === 'users') renderUsers();
    });

    // Mobile Menu
    $('#mobileMenuBtn').click(function () {
        $('.sidebar').addClass('show');
        $('.overlay').addClass('show');
    });

    $('#sidebarOverlay').click(function () {
        $('.sidebar').removeClass('show');
        $('.overlay').removeClass('show');
    });

    // Event Listeners for Filters
    $('#product-search, #product-category-filter, #product-sort').on('input change', renderProducts);
    $('#order-search, #order-status-filter, #order-sort').off('input change').on('input change', function () {
        renderOrders(0);
    });
    $('#user-search, #user-verified-filter, #user-sort').off('input change').on('input change', function () {
        renderUsers(0); // 這裡明確傳入數字 0，而不是 Event 物件
    });
    // Add Product Save
    // 在 dashboard.js 中取代原本的 $('#saveProductBtn').click
    $('#saveProductBtn').click(function () {
        const pname = $('#new-pname').val();
        const price = $('#new-price').val();
        const imageInput = $('#new-image').val().trim();
        const finalImage = imageInput ? imageInput : `https://picsum.photos/300/300?random=${Math.random()}`;
        if (pname && price) {
            // 建構要傳給後端的 JSON 物件
            const newProduct = {
                pname: pname,
                price: parseFloat(price),
                categoryid: parseInt($('#new-category').val()),
                stock: parseInt($('#new-stock').val()) || 0,
                color: $('#new-color').val() || '',
                specification: $('#new-spec').val() || '',
                description: $('#new-desc').val() || '',
                productimage: finalImage
            };

            // 發送 POST 請求
            $.ajax({
                url: '/api/admin/products',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(newProduct),
                success: function (response) {

                    alert('商品新增成功！');

                    // 重置表單並關閉 Modal
                    $('#addProductForm')[0].reset();
                    bootstrap.Modal.getInstance(document.getElementById('addProductModal')).hide();

                    // 重新載入列表
                    renderPagination(data, 'renderProducts', 'page-info', 'pagination-ul');
                },
                error: function (xhr) {
                    alert('新增失敗：' + xhr.responseText);
                }
            });
        } else {
            alert('請輸入商品名稱與價格');
        }
    });
});

// 【通用版】監聽所有自訂 Dropdown 的點擊事件
// 只要是 class 為 dropdown-item 的連結被點擊，就會自動尋找同層級的 hidden input 並更新
$(document).on('click', '.dropdown-menu .dropdown-item', function (e) {
    e.preventDefault();

    const $item = $(this);
    const val = $item.data('val');
    const text = $item.text();

    // 找到這個選單所在的容器 (.position-relative)
    const $container = $item.closest('.position-relative');

    // 找到同容器內的按鈕 (顯示文字用)
    const $btn = $container.find('button[data-bs-toggle="dropdown"]');

    // 找到同容器內的隱藏欄位 (儲存數值用)
    const $hiddenInput = $container.find('input[type="hidden"]');

    // 更新畫面與數值
    if ($btn.length && $hiddenInput.length) {
        $btn.text(text);           // 更新按鈕文字
        $hiddenInput.val(val);     // 更新隱藏值
        $hiddenInput.trigger('change'); // 【關鍵】觸發 change 事件，讓搜尋邏輯自動執行
    }
});

// 修改後的 loadCategories
function loadCategories() {
    $.ajax({
        url: '/api/admin/categories',
        method: 'GET',
        success: function (data) {
            globalCategoryList = data;

            // 1. 處理 Filter 下拉選單 (改成 Dropdown Item 格式)
            let filterHtml = `<li><a class="dropdown-item" href="#" data-val="ALL">全部分類</a></li>`;

            data.forEach(c => {
                filterHtml += `<li><a class="dropdown-item" href="#" data-val="${c.categoryid}">${c.cname}</a></li>`;
            });

            // 填入新的 Dropdown UL
            $('#category-dropdown-list').html(filterHtml);

            // 2. 處理 Modal 新增商品的下拉選單 (這裡維持原本的 Select 沒關係，因為是原生表單)
            let modalOptions = '';
            data.forEach(c => {
                modalOptions += `<option value="${c.categoryid}">${c.cname}</option>`;
            });
            $('#new-category').html(modalOptions);

            // 3. 初始載入商品
            if (typeof renderProducts === 'function') {
                renderProducts(currentProductPage);
            }
        },
        error: function (err) {
            console.error('無法載入分類資料', err);
        }
    });
}

function renderProducts(page = 0) {
    currentProductPage = page; // 更新當前頁碼

    const term = $('#product-search').val().trim();
    const catId = $('#product-category-filter').val();
    const sort = $('#product-sort').val();

    let params = {
        sortType: sort,
        page: page,
        size: pageSize
    };

    if (term) params.keyword = term;
    if (catId !== 'ALL') params.categoryId = catId;

    $.ajax({
        url: '/api/admin/products',
        method: 'GET',
        data: params,
        success: function (data) {
            console.log("後端回傳資料:", data);
            const products = data.content;

            const rows = products.length ? products.map(p => `
                <tr>
                    <td class="ps-4 py-3">
                        <div class="d-flex align-items-center">
                            <img src="${p.productimage || 'https://via.placeholder.com/40'}" class="rounded rounded-3 me-3" width="40" height="40" style="object-fit: cover;">
                            <div>
                                <div class="fw-medium text-dark">${p.pname}</div>
                                <div class="small text-secondary text-truncate" style="max-width: 200px;">${p.description || '-'}</div>
                            </div>
                        </div>
                    </td>
                    <td><span class="badge bg-light text-secondary border fw-normal">${getCategoryName(p.categoryid)}</span></td>
                    <td class="fw-medium">${formatMoney(p.price)}</td>
                    <td><span class="badge rounded-pill ${p.stock > 10 ? 'bg-success-subtle text-success-emphasis' : 'bg-danger-subtle text-danger'}">${p.stock}</span></td>
                    <td class="small text-secondary">${p.color || '-'} / ${p.specification || '-'}</td>
                    <td class="text-end pe-4">
                        <button class="btn btn-link p-0 text-primary me-2"><i class="bi bi-pencil"></i></button>
                        <button class="btn btn-link p-0 text-danger" onclick="deleteProduct(${p.productid})"><i class="bi bi-x-lg"></i></button>
                    </td>
                </tr>
            `).join('') : `<tr><td colspan="6" class="text-center py-5 text-secondary">沒有符合條件的商品</td></tr>`;

            $('#product-table-body').html(rows);

            // 【修正】這裡必須傳入對應的 ID，否則分頁函式不知道要更新誰
            renderPagination(data, 'renderProducts', 'page-info', 'pagination-ul');
        },
        error: function (err) {
            console.error('載入商品失敗', err);
            $('#product-table-body').html(`<tr><td colspan="6" class="text-center text-danger">載入失敗，請稍後再試</td></tr>`);
        }
    });
}



window.deleteProduct = function (id) {
    if (confirm('確定要刪除此商品嗎？此動作無法復原。')) {
        $.ajax({
            url: `/api/admin/products/${id}`,
            method: 'DELETE',
            success: function () {
                // 刪除成功後重新載入
                renderProducts();
            },
            error: function () {
                alert('刪除失敗');
            }
        });
    }
};

