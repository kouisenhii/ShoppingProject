/**
 * 處理密碼修改表單提交 (使用原生 JavaScript 和 Fetch API)
 */
document.addEventListener('DOMContentLoaded', function() {
    // 選擇表單元素
    const form = document.querySelector('form');
    // 如果找不到表單，則不執行後續邏輯
    if (!form) return; 
    
    // 取得所有輸入欄位
    const oldPasswdInput = document.getElementById('old_passwd');
    const newPasswdInput = document.getElementById('new_passwd');
    const confirmPasswdInput = document.getElementById('new_passwd_again');
    
    // 取得錯誤訊息顯示元素 (需要確保 HTML 中有這些 ID 或 class)
    // 由於您的 HTML 中並沒有這些錯誤顯示元素，我們在下方手動新增 DOM 元素
    const oldPasswdError = createErrorElement('oldPasswdError');
    const newPasswdError = createErrorElement('newPasswdError');
    const confirmPasswdError = createErrorElement('confirmPasswdError');
    
    // 將錯誤顯示元素插入到對應欄位下方
    oldPasswdInput.insertAdjacentElement('afterend', oldPasswdError);
    newPasswdInput.insertAdjacentElement('afterend', newPasswdError);
    confirmPasswdInput.insertAdjacentElement('afterend', confirmPasswdError);

    // 輔助函數：創建錯誤訊息元素
    function createErrorElement(id) {
        const div = document.createElement('div');
        div.id = id;
        div.className = 'text-danger small mt-1'; // 使用 Bootstrap 樣式
        return div;
    }

    // 新密碼的驗證規則 (與後端 DTO 保持一致)
    const NEW_PASSWORD_REGEX = /^(?=.*[A-Z])(?=.*[a-z])[A-Za-z0-9]{8,20}$/;
    const OLD_PASSWORD_LENGTH_MIN = 8;
    const OLD_PASSWORD_LENGTH_MAX = 16;
    const NEW_PASSWORD_LENGTH_MIN = 8;
    const NEW_PASSWORD_LENGTH_MAX = 20;

    // 輔助函數：清除所有錯誤訊息
    function clearErrors() {
        document.querySelectorAll('.text-danger').forEach(el => el.textContent = '');
    }

    // 監聽表單提交事件
    form.addEventListener('submit', async function(event) {
        // 阻止表單的預設提交行為
        event.preventDefault(); 
        
        clearErrors(); // 清除先前的錯誤訊息
        
        // 1. 取得表單欄位的值
        const oldPassword = oldPasswdInput.value.trim();
        const newPassword = newPasswdInput.value.trim();
        const confirmNewPassword = confirmPasswdInput.value.trim();
        
        let isValid = true; // 驗證標記

        // 2. 前端驗證
        
        // 2.1. 舊密碼驗證
        if (!oldPassword) {
            oldPasswdError.textContent = '舊密碼不能為空';
            isValid = false;
        } else if (oldPassword.length < OLD_PASSWORD_LENGTH_MIN || oldPassword.length > OLD_PASSWORD_LENGTH_MAX) {
            oldPasswdError.textContent = `舊密碼長度需介於 ${OLD_PASSWORD_LENGTH_MIN} 到 ${OLD_PASSWORD_LENGTH_MAX} 位之間`;
            isValid = false;
        }

        // 2.2. 新密碼格式驗證
        if (!newPassword) {
            newPasswdError.textContent = '新密碼不能為空';
            isValid = false;
        } else if (newPassword.length < NEW_PASSWORD_LENGTH_MIN || newPassword.length > NEW_PASSWORD_LENGTH_MAX) {
            newPasswdError.textContent = `新密碼長度需 ${NEW_PASSWORD_LENGTH_MIN} ~ ${NEW_PASSWORD_LENGTH_MAX} 字`;
            isValid = false;
        } else if (!NEW_PASSWORD_REGEX.test(newPassword)) {
            newPasswdError.textContent = '新密碼必須至少 1 個大寫和 1 個小寫字母，並且只能包含英文字母和數字';
            isValid = false;
        }
        
        // 2.3. 確認新密碼格式驗證
        if (!confirmNewPassword) {
            confirmPasswdError.textContent = '確認新密碼不能為空';
            isValid = false;
        } else if (confirmNewPassword.length < NEW_PASSWORD_LENGTH_MIN || confirmNewPassword.length > NEW_PASSWORD_LENGTH_MAX) {
            confirmPasswdError.textContent = `確認新密碼長度需 ${NEW_PASSWORD_LENGTH_MIN} ~ ${NEW_PASSWORD_LENGTH_MAX} 字`;
            isValid = false;
        } else if (!NEW_PASSWORD_REGEX.test(confirmNewPassword)) {
            confirmPasswdError.textContent = '確認新密碼必須至少 1 個大寫和 1 個小寫字母，並且只能包含英文字母和數字';
            isValid = false;
        }
        
        // 2.4. 兩次新密碼是否一致
        if (newPassword && confirmNewPassword && newPassword !== confirmNewPassword) {
            confirmPasswdError.textContent = '兩次輸入的新密碼不一致';
            isValid = false;
        }

        // 如果前端驗證失敗，停止執行
        if (!isValid) {
            alert('請檢查輸入的密碼格式是否正確！');
            return; 
        }

        // 3. 準備發送給後端的資料
        const requestData = {
            oldPassword: oldPassword,
            newPassword: newPassword,
            confirmNewPassword: confirmNewPassword
        };

        // 4. AJAX (Fetch API) 呼叫後端 API
        const submitButton = form.querySelector('.btn-submit');
        submitButton.disabled = true;
        submitButton.textContent = '修改中...'; // 禁用按鈕防止重複提交

        try {
            const response = await fetch('/v1/userinfos/password', {
                method: 'PATCH', // 使用 PATCH 方法
                headers: {
                    'Content-Type': 'application/json'
                    // 如果需要認證，可能還需要 'Authorization': 'Bearer <token>'
                },
                body: JSON.stringify(requestData) // 將資料轉換為 JSON 字串
            });

            // 處理 HTTP 狀態碼
            if (response.status === 204) {
                // 成功：HTTP 204 No Content
                alert('密碼修改成功！請重新登入以確保安全。');
                // 導向其他頁面
                window.location.href = './login.html'; 

            } else {
                // 失敗：例如 400 Bad Request, 401 Unauthorized, 403 Forbidden
                let errorMessage = '密碼修改失敗！請稍後再試。';
                
                // 嘗試解析錯誤訊息
                if (response.headers.get('content-type')?.includes('application/json')) {
                    const errorJson = await response.json();
                    if (errorJson.message) {
                         // 假設後端回傳格式為 { message: "..." }
                        errorMessage = errorJson.message;
                    } else if (response.status === 400) {
                        errorMessage = '輸入資料格式錯誤，請檢查所有欄位。';
                    }
                } else if (response.status === 401 || response.status === 403) {
                    errorMessage = '權限不足或未登入，請先登入。';
                }
                
                alert(errorMessage);
                console.error('密碼修改錯誤:', response.status, errorMessage);
            }

        } catch (error) {
            // 處理網路錯誤或 Fetch 請求本身的錯誤
            alert('網路連線失敗，請檢查您的網路。');
            console.error('Fetch 錯誤:', error);
        } finally {
            // 不論成功或失敗，都重新啟用按鈕
            submitButton.disabled = false;
            submitButton.textContent = '確認送出';
        }
    });
});