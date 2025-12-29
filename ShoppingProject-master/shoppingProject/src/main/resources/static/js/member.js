document.addEventListener('DOMContentLoaded', () => {

    // =========================================================
    // 1. 全域設定與變數 (包含 SweetAlert 風格定義)
    // =========================================================

    // const API_BASE_URL = 'http://localhost:8080'; 
    const API_BASE_URL = 'https://ecooikos.com';

    // 定義與 header_footer.js 一致的 Toast 通知
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

    // 定義統一風格的彈窗 Helper
    const showSwal = (title, text, icon = 'success') => {
        Swal.fire({
            title: title,
            text: text,
            icon: icon,
            confirmButtonText: '確定',
            confirmButtonColor: '#d6a368', // 網站主題色
        });
    }

    // 快取 DOM 元素
    const $main = document.getElementById('main');
    const $personalInfoLi = document.getElementById('personalInfo');
    const $orderSearchLi = document.getElementById('orderSearch');
    const $wishListLi = document.getElementById('wishList');

    const CANCELED_STATUSES = ["CANCELLED"];

    // 【關鍵】: 用於儲存從後端載入的原始資料
    let currentUserInfo = {};
    const genderMap = { 'M': '男', 'F': '女', 'O': '其他', '': '未選擇' };

    // =========================================================
    // 2. 功能模組：個人資料 (Personal Info)
    // =========================================================

    function fetchPersonalInfo() {
        return fetch(API_BASE_URL + '/v1/userinfos', {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        })
            .then(async response => {
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    throw new Error(errorData.message || `載入個人資料 HTTP 錯誤: ${response.status}`);
                }
                return response.json();
            });
    }

    function fetchUserIcon() {
        const cacheBuster = `?t=${new Date().getTime()}`;
        const url = API_BASE_URL + '/v1/userinfos/icon' + cacheBuster;

        return fetch(url, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        })
            .then(async response => {
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({}));
                    throw new Error(errorData.message || `載入頭像失敗: ${response.status}`);
                }
                return response.json();
            });
    }

    function fileToBase64(file) {
        return new Promise((resolve, reject) => {
            if (!file || !file.type.startsWith('image/')) {
                reject(new Error("請選擇一個有效的圖片檔案。"));
                return;
            }
            const reader = new FileReader();
            reader.onload = (event) => {
                const dataUrl = event.target.result;
                const base64Content = dataUrl.split(',')[1];
                resolve(base64Content);
            };
            reader.onerror = (error) => {
                reject(new Error("檔案讀取失敗。"));
            };
            reader.readAsDataURL(file);
        });
    }

    async function uploadAvatar() {
        const fileInput = document.getElementById('avatarFile');
        const file = fileInput.files[0];

        if (!file) {
            // 使用 showSwal 替代
            showSwal('提示', '請先選擇一張圖片', 'warning');
            return;
        }

        try {
            const base64String = await fileToBase64(file);
            const requestBody = { iconBase64: base64String };

            const response = await fetch(API_BASE_URL + '/v1/userinfos/icon/new', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(requestBody)
            });

            if (response.ok) {
                // 1. 顯示成功訊息
                Toast.fire({
                    icon: 'success',
                    title: '頭像更新成功！'
                });

                // 2. 重新載入「會員中心」頁面中間的個人資訊與大頭貼
                await loadAllUserInfo();

                // 3. 【關鍵】重新載入「網站 Header (導覽列)」右上角的小頭貼
                // 呼叫 header_footer.js 定義的全域函式
                if (typeof window.reloadHeaderUser === 'function') {
                    console.log("正在更新 Header 頭像...");
                    await window.reloadHeaderUser();
                } else {
                    console.warn("找不到 reloadHeaderUser 函式，Header 頭像可能未更新");
                }

            } else {
                const errorData = await response.json().catch(() => ({}));
                showSwal('上傳失敗', errorData.message || response.statusText, 'error');
            }
        } catch (error) {
            console.error("網路請求錯誤:", error);
            showSwal('錯誤', '圖片轉換或上傳失敗', 'error');
        }
    }

    async function loadAllUserInfo() {
        showLoading();

        try {
            const [personalInfoData, iconData] = await Promise.all([
                fetchPersonalInfo(),
                fetchUserIcon()
            ]);

            currentUserInfo = {
                name: personalInfoData.name || '',
                gender: personalInfoData.gender || '',
                birthday: personalInfoData.birthday || '',
                phone: personalInfoData.phone || '',
                email: personalInfoData.email || '',
                address: personalInfoData.address || ''
            };

            currentUserId = personalInfoData.userid;
            const isThirdPartyLogin = personalInfoData.isThirdPartyLogin || false;

            const userInfoForForm = {
                name: currentUserInfo.name || '',
                birthday: currentUserInfo.birthday || '',
                gender: currentUserInfo.gender || 'M',
                genderText: genderMap[currentUserInfo.gender] || '男',
                phone: currentUserInfo.phone || '',
                address: currentUserInfo.address || ''
            };

            const iconBase64String = iconData?.avatar || iconData?.iconUrl || iconData?.icon || iconData?.iconBase64 || personalInfoData?.avatar || personalInfoData?.icon;

            let iconUrl;

            if (iconBase64String) {
                if (iconBase64String.startsWith('data:') || iconBase64String.startsWith('http')) {
                    iconUrl = iconBase64String;
                } else {
                    iconUrl = `data:image/jpeg;base64,${iconBase64String}`;
                }
            } else {
                const seedName = personalInfoData.name || "User";
                iconUrl = `https://api.dicebear.com/9.x/initials/svg?seed=${encodeURIComponent(seedName)}`;
            }

            let passwordButtonHTML;
            let passwordNoticeHTML = '';
            const passwordButtonClass = "btn-submit mt-2";

            if (isThirdPartyLogin) {
                passwordButtonHTML = `<button id="btnChangePassword" class="${passwordButtonClass} btn-disabled" disabled>修改密碼</button>`;
                passwordNoticeHTML = '<p class="text-danger mt-1">您是透過第三方登入，無法在此頁面修改密碼。</p>';
            } else {
                passwordButtonHTML = `<button id="btnChangePassword" class="${passwordButtonClass}">修改密碼</button>`;
            }

            $main.innerHTML = `
            <div class="info-block">
                <h2 class="text-center mb-4">用戶頭像</h2> 
                <div class="user-icon-wrapper mb-4">
                    <label for="avatarFile" class="user-icon-label" title="點擊選擇頭像">
                        <img id="userIconDisplay" 
                             src="${iconUrl}" 
                             alt="User Avatar">
                        <div class="icon-overlay">更換頭像</div>
                    </label>
                    <input type="file" id="avatarFile" accept="image/png, image/jpeg" style="display: none;">
                    <button id="btnUploadAvatar" class="btn-submit">確認上傳</button>
                </div>
            </div>

            <div class="info-block">
                <h2>個人資料</h2>
                <div class="row info-row"><div class="col-md-3">姓名：</div><div class="col-md-9" id="name">${currentUserInfo.name}</div></div>
                <div class="row info-row"><div class="col-md-3">性別：</div><div class="col-md-9" id="gender">${userInfoForForm.genderText}</div></div>
                <div class="row info-row"><div class="col-md-3">出生年月日：</div><div class="col-md-9" id="birthday">${currentUserInfo.birthday}</div></div>
                <div class="row info-row"><div class="col-md-3">手機號碼：</div><div class="col-md-9" id="phone">${currentUserInfo.phone}</div></div>
                <div class="row info-row"><div class="col-md-3">電子郵件：</div><div class="col-md-9" id="email">${currentUserInfo.email}</div></div>
                <div class="row info-row"><div class="col-md-3">地址：</div><div class="col-md-9" id="address">${currentUserInfo.address}</div></div>
                <div class="mt-3">
                    <button id="btnEditInfo" class="btn-submit">修改資料</button>
                </div>
            </div>
            
            <div class="info-block mt-4">
                <h2>密碼管理</h2>
                <div>
                    ${passwordButtonHTML} 
                </div>
                ${passwordNoticeHTML} 
            </div>
            `;

            document.getElementById('btnEditInfo').addEventListener('click', () => renderEditForm(userInfoForForm));

            const btnChangePassword = document.getElementById('btnChangePassword');

            if (btnChangePassword && !isThirdPartyLogin) {
                btnChangePassword.addEventListener('click', () =>
                    window.location.href = "./password.html"
                );
            }

            const btnUploadAvatar = document.getElementById('btnUploadAvatar');
            if (btnUploadAvatar) {
                btnUploadAvatar.addEventListener('click', uploadAvatar);
            }

            const avatarFileInput = document.getElementById('avatarFile');
            const userIconDisplay = document.getElementById('userIconDisplay');

            if (avatarFileInput && userIconDisplay) {
                avatarFileInput.addEventListener('change', function () {
                    const file = this.files[0];
                    if (file) {
                        const reader = new FileReader();
                        reader.onload = (event) => {
                            userIconDisplay.src = event.target.result;
                        };
                        reader.readAsDataURL(file);
                    }
                });
            }

        } catch (error) {
            console.error('載入個人資料或頭像時失敗:', error);
            hideLoading();
            showError('無法載入完整的個人資訊或頭像', error);
        }
    }

    function renderEditForm(userInfo) {
        let phoneNoPrefix = userInfo.phone;
        if (phoneNoPrefix.startsWith('09')) {
            phoneNoPrefix = phoneNoPrefix.substring(2);
        } else {
            phoneNoPrefix = userInfo.phone;
        }

        $main.innerHTML = `
            <div class="info-block">
                <h2>個人資料修改</h2>
                <form id="formEditInfo" class="row g-3">
                    <div class="col-md-6">
                        <label for="editName" class="mb-2" >姓名</label>
                        <input type="text" class="form-control" id="editName" placeholder="請輸入姓名" value="${userInfo.name}"> 
                    </div>

                    <div class="col-md-6">
                        <label class="mb-2">性別</label>
                        <div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input" type="radio" name="editGender" id="male" value="M" ${userInfo.gender == 'M' ? 'checked' : ''}>
                                <label class="form-check-label" for="male">男</label>
                            </div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input" type="radio" name="editGender" id="female" value="F" ${userInfo.gender == 'F' ? 'checked' : ''}>
                                <label class="form-check-label" for="female">女</label>
                            </div>
                            <div class="form-check form-check-inline">
                                <input class="form-check-input" type="radio" name="editGender" id="other" value="O" ${userInfo.gender == 'O' ? 'checked' : ''}>
                                <label class="form-check-label" for="other">其他</label>
                            </div>
                        </div>
                    </div>

                    <div class="col-md-6">
                        <label for="editBirthday" class="mb-2">出生年月日</label>
                        <input type="date" class="form-control" id="editBirthday" value="${userInfo.birthday}">
                    </div>

                    <div class="col-md-6">
                        <label for="editPhone" class="mb-2">手機號碼</label>
                        <div class="d-flex gap-2">
                            <input type="text" class="form-control" style="max-width: 80px;" value="09" disabled>
                            <input type="text" class="form-control" id="editPhone"  maxlength="8" pattern="[0-9]{8}" value="${phoneNoPrefix}">
                        </div>
                    </div>

                    <div class="col-12">
                        <label class="mb-2">地址</label>
                        <div class="row g-2">
                            <div class="col-12">
                                <input type="text" class="form-control" id="editAddress" placeholder="請輸入完整地址" value="${userInfo.address}">
                            </div>
                        </div>
                    </div>

                    <div class="col-12 mt-4">
                        <button type="submit" class="btn-submit">確認送出</button>
                        <button type="button" class="m-3 btn-submit" id="btnCancelEdit">取消</button>
                    </div>
                </form>
            </div>
        `;

        document.getElementById('formEditInfo').addEventListener('submit', function (e) {
            e.preventDefault();
            updatePersonalInfo();
        });

        document.getElementById('btnCancelEdit').addEventListener('click', loadAllUserInfo);
    }

    async function updatePersonalInfo() {

        const nameValue = document.getElementById('editName').value.trim();
        const genderValue = document.querySelector('input[name="editGender"]:checked')?.value;
        const birthdayValue = document.getElementById('editBirthday').value.trim();
        const phoneSuffix = document.getElementById('editPhone').value.trim();
        const addressValue = document.getElementById('editAddress').value.trim();

        const finalName = nameValue || currentUserInfo.name;
        const finalGender = genderValue || currentUserInfo.gender;
        const finalBirthday = birthdayValue || currentUserInfo.birthday;
        const finalAddress = addressValue || currentUserInfo.address;
        const finalMobile = phoneSuffix ? ('09' + phoneSuffix) : currentUserInfo.phone;

        // 驗證邏輯：使用 showSwal 替代 alert
        const chineseNameRegex = /^[\u4e00-\u9fa5]{2,50}$/;
        if (!finalName || !chineseNameRegex.test(finalName)) {
            showSwal('格式錯誤', '姓名必須填寫，長度需 2 ~ 50 個中文字，且必須皆是中文!', 'warning');
            return;
        }
        if (!finalGender) {
            showSwal('遺漏資料', '請選擇性別!', 'warning');
            return;
        }
        if (!finalBirthday) {
            showSwal('遺漏資料', '請填寫出生年月日!', 'warning');
            return;
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const birthdayDate = new Date(finalBirthday);

        if (birthdayDate.getTime() > today.getTime()) {
            showSwal('日期錯誤', '出生年月日不能選擇未來的日期！', 'warning');
            return;
        }

        const minAddressLength = 6;
        if (!finalAddress) {
            showSwal('遺漏資料', '請填寫地址!', 'warning');
            return;
        }
        if (finalAddress.length < minAddressLength) {
            showSwal('格式錯誤', '地址至少需要 ' + minAddressLength + ' 個字元!', 'warning');
            return;
        }

        if (finalMobile && finalMobile.length !== 10) {
            showSwal('格式錯誤', '手機號碼應為 10 碼數字!', 'warning');
            return;
        }

        const payload = {
            name: finalName,
            gender: finalGender,
            birthday: finalBirthday,
            phone: finalMobile,
            address: finalAddress
        };

        try {
            const response = await fetch(API_BASE_URL + '/v1/userinfos/personalinfo', {
                method: 'PATCH',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            console.log(payload);
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP 錯誤: ${response.status}`);
            }

            // 更新成功
            Toast.fire({
                icon: 'success',
                title: '資料更新成功'
            });
            loadAllUserInfo();

        } catch (error) {
            showSwal('更新失敗', error.message || '系統錯誤', 'error');
        }
    }

    // =========================================================
    // 3. 功能模組：訂單查詢 (Order Search)
    // =========================================================

    async function loadOrders(searchId = null) {
        showLoading();

        // 1. 【新增】付款方式對照表 (綠界代碼 -> 中文)
        const paymentMap = {
            'Credit_CreditCard': '信用卡',
            'ATM_TAISHIN': 'ATM (台新)',
            'ATM_ESUN': 'ATM (玉山)',
            'ATM_BOT': 'ATM (台銀)',
            'ATM_FUBON': 'ATM (富邦)',
            'ATM_CHINATRUST': 'ATM (中信)',
            'CVS_CVS': '超商代碼',
            'CVS_OK': 'OK 超商',
            'CVS_FAMILY': '全家超商',
            'CVS_HILIFE': '萊爾富',
            'CVS_IBON': '7-11 ibon',
            'BARCODE_BARCODE': '超商條碼',
            '1': '信用卡',  // 相容舊資料
            'null': '尚未付款',
            'undefined': '尚未付款'
        };



        try {
            const response = await fetch(API_BASE_URL + '/v1/orders');

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP 錯誤: ${response.status}`);
            }

            let data = await response.json();

            if (searchId) {
                data = data.filter(order => order.orderId.toString() === searchId.toString());
            }

            data.sort((a, b) => new Date(b.orderDate) - new Date(a.orderDate));

            let htmlContent = `
            <h2 class="mb-4">訂單及歷史購物查詢</h2>
            
            <div class="row mb-3">
                <div class="col-md-6 ">
                    <label for="orderSearchInput" class="form-label">訂單搜尋</label>
                    <div class="input-group">
                        <input type="text" class="form-control" id="orderSearchInput" placeholder="請輸入訂單編號" >
                        <button class="btn-submit" type="button" id="btnSearchOrder">搜尋</button>
                    </div>
                </div>
            </div>
        `;
            console.log(data);
            if (data && data.length > 0) {
                const rows = data.map(order => {
                    const orderStatus = order.orderStatus || '未知狀態';
                    const statusBadgeClass = CANCELED_STATUSES.includes(orderStatus) ? 'text-bg-danger' : 'text-bg-success';

                    const shipmentStatus = order.shipmentStatus || '尚未出貨';
                    const shipmentBadgeClass = (shipmentStatus === '已出貨' || shipmentStatus === '已送達') ? 'text-bg-info' : 'text-bg-secondary';
                    const formattedAmount = (order.totalAmount || 0).toLocaleString();


                    // 2. 【修改】解析付款方式 (加入 .toString().trim() 防止格式對不上)
                    // 如果 order.paymentMethod 是 null，就給空字串
                    const rawPayment = (order.paymentMethod || '').toString().trim();
                    // 查表，如果找不到就顯示原始代碼，或顯示 "其他"
                    const paymentDisplay = paymentMap[rawPayment] || rawPayment || '待確認';

                    return `
                    <tr class="order-row" data-order-id="${order.orderId}" style="cursor: pointer;">
                        <td>${order.orderId}</td>
                        <td>${order.orderDate}</td>
                        <td>$${formattedAmount}</td>
                        <td><span class="badge ${statusBadgeClass}">${order.orderStatusDisplay}</span></td>
                        <td>${paymentDisplay}</td>
                        <td><span class="badge ${shipmentBadgeClass}">${order.shipmentStatusDisplay || '待出貨'}</span></td>
                    </tr>
                `;
                }).join('');

                htmlContent += `
                <div class="table-responsive mt-4">
                    <table class="table table-bordered align-middle table-hover">
                        <thead>
                            <tr class="table-light">
                                <th class=" text-center" >訂單編號</th>
                                <th class=" text-center" >下單日期</th>
                                <th class=" text-center" >總金額</th>
                                <th class=" text-center" >訂單狀態</th>
                                <th class=" text-center" >付款方式</th> 
                                <th class=" text-center" >出貨狀態</th> 
                            </tr>
                        </thead>
                        <tbody>
                            ${rows}
                        </tbody>
                    </table>
                </div>
            `;
            } else {
                htmlContent += `<div class="alert alert-info text-center m-4">找不到訂單紀錄!</div>`;
            }

            $main.innerHTML = htmlContent;

            document.querySelectorAll('.order-row').forEach(row => {
                row.addEventListener('click', (e) => {
                    const orderId = e.currentTarget.dataset.orderId;
                    loadOrderDetail(orderId);
                });
            });

            document.getElementById('btnSearchOrder').addEventListener('click', () => {
                const searchVal = document.getElementById('orderSearchInput').value.trim();
                loadOrders(searchVal);
            });

        } catch (error) {
            showError('無法載入訂單列表', error);
        }
    }

    async function loadOrderDetail(orderId) {
        showLoading();

        try {
            const response = await fetch(`${API_BASE_URL}/v1/orders/${orderId}`);
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP 錯誤: ${response.status}`);
            }
            const orderData = await response.json();

            const orderStatus = orderData.orderStatus;
            const shipmentStatus = orderData.shipmentStatus;

            loadOrderItems(
                orderId,
                orderStatus,
                orderData.totalAmount,
                shipmentStatus
            );

        } catch (error) {
            showError(`無法載入訂單 #${orderId} 的基本資訊`, error);
        }
    }

    async function loadOrderItems(orderId, orderStatus, totalAmount, shipmentStatus) {
        showLoading();

        try {
            const itemsResponse = await fetch(`${API_BASE_URL}/v1/orders/${orderId}/orderitems`);

            if (!itemsResponse.ok) {
                const errorData = await itemsResponse.json();
                throw new Error(errorData.message || `HTTP 錯誤: ${itemsResponse.status}`);
            }

            const items = await itemsResponse.json();

            if (!items || items.length === 0) {
                $main.innerHTML = `
                <div class="info-block">
                    <h2>訂單商品明細</h2>
                    <div class="mb-3">
                        <button class="btn-submit" id="backToOrders">返回訂單列表</button> 
                    </div>
                    <div class="alert alert-warning mt-4">此訂單無商品明細資料</div>
                </div>
            `;
                const $backButton = document.getElementById('backToOrders');
                if ($backButton) {
                    $backButton.addEventListener('click', () => {
                        loadOrders();
                    });
                }
                return;
            }

            let actionButtons = '';
            let isCancelable = false;

            const isOrderProcessable = orderStatus === 'CREATED' || orderStatus === 'PAID' || orderStatus === 'PENDING';
            const isShipmentPending = shipmentStatus === 'PENDING_SHIPMENT';

            if (isOrderProcessable && isShipmentPending) {
                isCancelable = true;
                actionButtons = `<button class="btn-submit" id="btnCancelOrder">取消訂單</button>`;
            }

            const totalAmountToDisplay = totalAmount.toLocaleString();
            const itemsHtml = items.map(item => `
            <tr>
                <td>${item.productName}</td>
                <td>$${(item.unitPrice || 0).toLocaleString()}</td>
                <td>${item.quantity}</td>
                <td>$${(item.unitPrice * item.quantity).toLocaleString()}</td>
            </tr>
        `).join('');

            $main.innerHTML = `
            <div class="info-block">
                <div class="mb-3">
                    <button class="btn-submit" id="backToOrders">返回訂單列表</button>
                </div>
                <div class="table-responsive">
                    <table class="table table-bordered align-middle">
                        <thead>
                            <tr class="table-light text-center">
                                <th class=" text-center">商品名稱</th>
                                <th class=" text-center" >單價</th>
                                <th class=" text-center" >數量</th>
                                <th class=" text-center" >小計</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${itemsHtml} 
                            <tr> 
                                <td colspan="3" class="text-end">總計 (TWD)</td>
                                <td class="text-center">$${totalAmountToDisplay}</td>    
                            </tr>
                        </tbody>
                    </table>
                </div>
                
                <div class="mt-3 d-flex justify-content-end gap-2">
                    ${actionButtons} 
                </div>
            </div>
        `;
            const $backButton = document.getElementById('backToOrders');
            if ($backButton) {
                $backButton.addEventListener('click', () => {
                    loadOrders();
                });
            }

            if (isCancelable) {
                const $cancelButton = document.getElementById('btnCancelOrder');
                if ($cancelButton) {
                    $cancelButton.addEventListener('click', () => {
                        cancelOrder(orderId, orderStatus);
                    });
                }
            }

        } catch (error) {
            showError(`無法載入訂單 #${orderId} 的商品明細`, error);
        }
    }

    async function loadOrderAddress(orderId) {
        showLoading();

        try {
            const response = await fetch(`${API_BASE_URL}/v1/orders/${orderId}/orderAddress`);

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP 錯誤: ${response.status}`);
            }

            const data = await response.json();
            renderOrderAddressEditForm(orderId, data);

        } catch (error) {
            showError(`無法載入訂單 #${orderId} 的收件地址`, error);
        }
    }

    function renderOrderAddressEditForm(orderId, currentAddressInfo) {
        const { recipientName = '', recipientPhone = '', fullAddress = '' } = currentAddressInfo;

        $main.innerHTML = `
        <div class="info-block">
            <h2>訂單 #${orderId} 修改收件地址</h2>
            <div class="mb-3">
                <button class="btn btn-secondary" onclick="window.loadOrderDetail('${orderId}')">返回訂單明細</button>
            </div>

            <form id="formEditOrderAddress" class="row g-3 mt-3">
                <div class="col-md-6">
                    <label for="editRecipientName" class="form-label">收件人姓名</label>
                    <input type="text" class="form-control" id="editRecipientName" value="${recipientName}" required>
                </div>

                <div class="col-md-6">
                    <label for="editRecipientPhone" class="form-label">連絡電話</label>
                    <input type="text" class="form-control" id="editRecipientPhone" value="${recipientPhone}" required maxlength="10" pattern="^09[0-9]{8}$">
                    <div class="form-text">請輸入 10 碼 (09 開頭)</div>
                </div>

                <div class="col-12">
                    <label for="editFullAddress" class="form-label">完整地址</label>
                    <input type="text" class="form-control" id="editFullAddress" value="${fullAddress}" required>
                </div>

                <div class="col-12 mt-4">
                    <button type="submit" class="btn-submit">確認修改</button>
                    <button type="button" class="btn btn-secondary ms-2" onclick="window.loadOrderDetail('${orderId}')">取消</button>
                </div>
            </form>
        </div>
    `;

        document.getElementById('formEditOrderAddress').addEventListener('submit', function (e) {
            e.preventDefault();
            updateOrderAddress(orderId);
        });
    }

    async function cancelOrder(orderId, orderStatus) {

        const allowedStatuses = ['CREATED', 'PAID', 'PENDING'];

        if (!allowedStatuses.includes(orderStatus)) {
            showSwal('無法取消', `目前訂單狀態為【${orderStatus}】，無法執行取消操作。`, 'warning');
            return;
        }

        // 使用 Swal 替代 confirm
        Swal.fire({
            title: `確定要取消訂單 #${orderId} 嗎？`,
            text: "取消後將無法復原",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '確定取消',
            cancelButtonText: '保留訂單'
        }).then(async (result) => {
            if (result.isConfirmed) {
                showLoading();

                try {
                    const response = await fetch(`${API_BASE_URL}/v1/orders/${orderId}/cancelOrder`, {
                        method: 'PATCH'
                    });

                    if (!response.ok) {
                        const errorData = await response.json();
                        throw new Error(errorData.message || `HTTP 錯誤: ${response.status}`);
                    }

                    showSwal('成功', '訂單已成功取消!', 'success');
                    loadOrders();

                } catch (error) {
                    showSwal('取消失敗', error.message || '系統錯誤', 'error');
                    loadOrderDetail(orderId);
                }
            }
        });
    }

    window.loadOrderDetail = loadOrderDetail;
    window.loadOrderItems = loadOrderItems;
    window.loadOrderAddress = loadOrderAddress;
    window.cancelOrder = cancelOrder;
    window.renderOrderAddressEditForm = renderOrderAddressEditForm;

    // =========================================================
    // 4. 功能模組：收藏清單 (Wishlist)
    // =========================================================

    async function loadWishlist() {
        showLoading();

        try {
            const response = await fetch(`${API_BASE_URL}/v1/wishList`);

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.message || `HTTP 錯誤: ${response.status}`);
            }

            const data = await response.json();

            if (!data || data.length === 0) {
                $main.innerHTML = '<div class="info-block p-4 text-center"><h2>收藏清單</h2><p class="mt-4">您的收藏清單是空的。</p></div>';
                return;
            }

            const itemsHtml = data.map(item => {
                const formattedPrice = (item.price || 0).toLocaleString();
                return `
                <div class="wishlist-item d-flex align-items-center justify-content-between mb-3 p-3 border-bottom" id="wish-item-${item.productId}">
                    <div class="form-check me-3">
                        <input class="form-check-input wish-checkbox" type="checkbox" value="${item.productId}" id="check-${item.productId}">
                        <label class="form-check-label" for="check-${item.productId}"></label>
                    </div>

                    <div class="item-details me-3">
                        <img src="${item.productImage || 'https://placehold.co/80'}" alt="商品圖片" class="wishlist-img" style="width:80px; height:80px; object-fit:cover;">
                    </div>

                    <div class="flex-grow-1 me-3">
                        <h6 class="fw-bold mb-1">${item.productName}</h6>
                        <p class="text-muted mb-0 small">${item.description || ''}</p>
                        <p class="text-muted mb-0 small">${item.color || ''}</p>
                        <p class="text-muted mb-0 small">${item.specification || ''}</p>
                    </div>

                    <div class="price-and-actions d-flex align-items-center">
                        <div class="price-details me-3 text-nowrap">
                            <div class="fw-bold">$${formattedPrice}</div>
                        </div>
                        <div class="p-3 btn-delete-single" data-id="${item.productId}" style="cursor: pointer;">
                            <img src="/img/trash3.svg" alt="刪除">
                        </div>
                        <button type="button" class="btn-submit btn-add-cart" data-id="${item.productId}"> 
                            加入購物車
                        </button>
                    </div>
                </div>
            `;
            }).join('');

            $main.innerHTML = `
            <div class="info-block p-4">
                <h2>收藏清單</h2>
                <form id="wishlistForm">
                    ${itemsHtml}
                    <div class="mt-4 pt-3 border-top border-light">
                        <button type="button" class="btn-submit me-3" id="btnSelectAll">全選</button>
                        <button type="button" class="btn-submit" id="btnDeleteSelected">刪除所選</button>
                    </div>
                </form>
            </div>
        `;

            bindWishlistEvents();

        } catch (error) {
            showError('無法載入收藏清單', error);
        }
    }

    function bindWishlistEvents() {
        $main.addEventListener('click', function (e) {
            if (e.target.closest('.btn-delete-single')) {
                const id = e.target.closest('.btn-delete-single').dataset.id;
                deleteWishlistItem([id]);
            }
        });

        $main.addEventListener('click', function (e) {
            if (e.target.classList.contains('btn-add-cart')) {
                e.preventDefault();
                const id = e.target.dataset.id;
                addProductToCart(id, 1);
            }
        });

        document.getElementById('btnSelectAll')?.addEventListener('click', function () {
            const checkboxes = document.querySelectorAll('.wish-checkbox');
            const allChecked = checkboxes.length === document.querySelectorAll('.wish-checkbox:checked').length;
            checkboxes.forEach(cb => cb.checked = !allChecked);
        });

        document.getElementById('btnDeleteSelected')?.addEventListener('click', function () {
            const selectedIds = Array.from(document.querySelectorAll('.wish-checkbox:checked')).map(cb => cb.value);

            if (selectedIds.length === 0) {
                showSwal('提示', '請先勾選要刪除的商品', 'warning');
                return;
            }
            deleteWishlistItem(selectedIds);
        });
    }

    async function deleteWishlistItem(ids) {
        // 使用 Swal 替代 confirm
        Swal.fire({
            title: '確定要刪除嗎？',
            text: "刪除後將無法復原",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#d6a368',
            cancelButtonColor: '#d33',
            confirmButtonText: '確定刪除',
            cancelButtonText: '取消'
        }).then(async (result) => {
            if (result.isConfirmed) {
                showLoading();

                const deletePromises = ids.map(id => {
                    return fetch(`${API_BASE_URL}/v1/wishList/items/${id}`, {
                        method: 'DELETE'
                    });
                });

                try {
                    const responses = await Promise.all(deletePromises);

                    const failedResponse = responses.find(res => !res.ok);
                    if (failedResponse) {
                        const errorData = await failedResponse.json();
                        throw new Error(errorData.message || `刪除錯誤: ${failedResponse.status}`);
                    }

                    Toast.fire({
                        icon: 'success',
                        title: '商品已從清單移除'
                    });
                    loadWishlist();

                } catch (error) {
                    showSwal('錯誤', error.message || '刪除過程中發生錯誤', 'error');
                    loadWishlist();
                }
            }
        });
    }

    async function addProductToCart(productId) {
        if (!productId) {
            showSwal('錯誤', "商品 ID 或數量無效。", 'error');
            return;
        }
        const userId = currentUserId;
        const defaultQuantity = 1

        const payload = {
            productid: parseInt(productId),
            userid: parseInt(userId),
            quantity: defaultQuantity
        };

        try {
            const response = await fetch(API_BASE_URL + `/api/cart/add`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (response.ok) {
                // 加入購物車成功使用 Toast
                Toast.fire({
                    icon: 'success',
                    title: '已加入購物車'
                });

                // 順便更新 Header 的購物車數量 (如果函式存在)
                if (window.updateCartCount) {
                    window.updateCartCount(userId);
                }

                // 這裡選擇不刷新整個頁面，只刷新按鈕狀態或保持原樣體驗較好
                // 但如果您的業務邏輯需要，可以解開下行
                // loadWishlist(); 
            } else {
                const errorMessage = data.message || `加入購物車失敗`;
                showSwal('加入失敗', errorMessage, 'error');
            }

        } catch (error) {
            console.error("加入購物車時發生網路或系統錯誤:", error);
            showSwal('加入失敗', "操作失敗，請檢查網路連線。", 'error');
        }
    }

    // =========================================================
    // 5. 輔助工具 (Utils)
    // =========================================================

    function showLoading() {
        if ($main) {
            $main.innerHTML = `
                <div class="d-flex justify-content-center align-items-center" style="min-height: 200px;">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Loading...</span>
                    </div>
                </div>
            `;
        }
    }

    function showError(title, error) {
        console.error(error);
        const message = error.message || '伺服器無回應或發生錯誤，請稍後再試。';

        if ($main) {
            $main.innerHTML = `
                <div class="alert alert-danger m-4" role="alert">
                    <h4 class="alert-heading">${title}</h4>
                    <p>${message}</p>
                </div>
            `;
        }
    }

    // =========================================================
    // 6. 初始化事件綁定
    // =========================================================
    loadAllUserInfo();

    if ($personalInfoLi) $personalInfoLi.addEventListener('click', loadAllUserInfo);

    if ($orderSearchLi) {
        $orderSearchLi.addEventListener('click', () => loadOrders());
    }

    if ($wishListLi) $wishListLi.addEventListener('click', loadWishlist);

});

