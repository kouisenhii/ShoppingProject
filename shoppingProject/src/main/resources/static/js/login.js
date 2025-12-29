
$(document).ready(function () {
    console.log("Login script initialized with Modal");

    const $loginBtn = $("#login-btn");
    const $modalConfirmBtn = $("#confirm-captcha-btn");
    const $recaptchaModal = new bootstrap.Modal(document.getElementById('recaptchaModal'));
    const $globalAlert = $("#global-alert");
    
    const $googleBtn = $("#google-login-link");
    const $facebookBtn = $("#facebook-login-link");
    const $lineBtn = $("#line-login-link");

    // "記住我" 的 Checkbox
    const $rememberCheckbox = $("#remember");

    const fields = {
        email: $("#email"),
        password: $("#password")
    };

    const REGEX = {
        EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])[A-Za-z\d]{8,20}$/
    };

    // --- UI 輔助函式 (保持不變) ---
    function setInvalid($input, msg) {
        $input.removeClass("is-valid").addClass("is-invalid");
        let $parent = $input.closest('.mb-3, .input-group');
        $parent.find('.valid-icon').remove();
        $input.siblings(".invalid-feedback").text(msg).show();
    }

    function setValid($input) {
        $input.removeClass("is-invalid").addClass("is-valid");
        $input.siblings(".invalid-feedback").hide();
        let $parent = $input.closest('.mb-3, .input-group');
        $parent.find('.valid-icon').remove();
        $parent.append('<div class="valid-icon"></div>');
    }

    function validateInput(inputId) {
        let isValid = true;
        let msg = "";
        let $el;
        let val;

        switch (inputId) {
            case "email":
                $el = fields.email;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "Email 不能為空"; } 
                else if (!REGEX.EMAIL.test(val)) { isValid = false; msg = "Email 格式不正確"; }
                break;
            case "password":
                $el = fields.password;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "密碼不能為空"; } 
                else if (!REGEX.PASSWORD.test(val)) { isValid = false; msg = "密碼格式有誤 (需8-20字，含大小寫字母)"; }
                break;
        }

        if ($el) {
            if (isValid) setValid($el);
            else setInvalid($el, msg);
        }
        return isValid;
    }

	// ==========================================
    // --- [新增] 頁面載入時檢查 LocalStorage ---
    // ==========================================
    const savedEmail = localStorage.getItem("oikos_remember_email");
    
    if (savedEmail) {
        fields.email.val(savedEmail);            // 填入 Email
        $rememberCheckbox.prop("checked", true); // 勾選記住我
        validateInput("email");                  // 觸發驗證樣式 (顯示綠勾勾)
    } // <--- 記得這裡要加上這個括號，結束 if 區塊

    // ==========================================
    // --- [新增] 監聽 Checkbox 狀態改變 (獨立在外) ---
    // ==========================================
    $rememberCheckbox.on("change", function() {
        if (!$(this).is(":checked")) {
            // 當使用者「取消勾選」時，立即清除記憶
            localStorage.removeItem("oikos_remember_email");
            console.log("使用者取消勾選，已立即清除暫存 Email");
        }
    });
    
    // ==========================================
    // --- 原有的 Input 監聽 ---
    // ==========================================
    $("input").on("blur keyup", function () {
        validateInput(this.id);
        if (!$globalAlert.hasClass("d-none")) $globalAlert.addClass("d-none");
    });

    $googleBtn.on("click", () => window.location.href = "/oauth2/authorization/google");
    $facebookBtn.on("click", () => window.location.href = "/oauth2/authorization/facebook");
    $lineBtn.on("click", () => window.location.href = "/oauth2/authorization/line");

    // ==========================================
    // 1. 登入按鈕點擊
    // ==========================================
    $loginBtn.on("click", async function () {
        $globalAlert.addClass("d-none");

        let isAllValid = true;
        if (!validateInput("email")) isAllValid = false;
        if (!validateInput("password")) isAllValid = false;

        if (!isAllValid) {
            $(".login-card").addClass("shake-animation");
            setTimeout(() => $(".login-card").removeClass("shake-animation"), 500);
            return;
        }

        try {
            grecaptcha.reset(); 
        } catch(e) {} 
        
        $recaptchaModal.show();
    });

    // ==========================================
    // 2. 彈窗確認按鈕點擊
    // ==========================================
    $modalConfirmBtn.on("click", async function () {
        
        const recaptchaResponse = grecaptcha.getResponse();
        if (recaptchaResponse.length === 0) {
            Swal.fire({
                icon: 'warning',
                title: '驗證未通過',
                text: '請勾選「我不是機器人」',
                target: document.getElementById('recaptchaModal')
            });
            return;
        }

        $modalConfirmBtn.prop("disabled", true).text("處理中...");
        $loginBtn.prop("disabled", true).text("登入中...");

        const emailVal = fields.email.val().trim();
        const passwordVal = fields.password.val().trim();

        try {
            // Check Email
            const checkRes = await fetch(`/api/login/check-email?email=${encodeURIComponent(emailVal)}`, {
                method: "GET", credentials: "include"
            });

            if (checkRes.ok) {
                const data = await checkRes.json();
                if (!data.exists) {
                    $recaptchaModal.hide();
                    setInvalid(fields.email, "此帳號不存在，請先註冊");
                    resetBtns();
                    return;
                }
                const providers = data.providers || [];
                if (!providers.includes("LOCAL")) {
                    $recaptchaModal.hide();
                    setInvalid(fields.email, `此 Email 已綁定 ${providers.join(" / ")}，請使用下方圖示登入`);
                    resetBtns();
                    return;
                }
            }

            // Login Request
            const loginRes = await fetch("/api/login", {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ 
                    email: emailVal, 
                    password: passwordVal,
                    recaptchaToken: recaptchaResponse
                })
            });

            if (!loginRes.ok) {
                $recaptchaModal.hide();
                grecaptcha.reset();

                const errText = await loginRes.text(); 
                if (errText.startsWith("Email:")) setInvalid(fields.email, errText.replace("Email:", ""));
                else if (errText.startsWith("密碼:")) setInvalid(fields.password, errText.replace("密碼:", ""));
                else if (errText.startsWith("reCAPTCHA:")) $globalAlert.removeClass("d-none").text(errText.replace("reCAPTCHA:", ""));
                else {
                    if (errText.includes("帳號") || errText.includes("Email")) setInvalid(fields.email, errText);
                    else if (errText.includes("密碼")) setInvalid(fields.password, errText);
                    else $globalAlert.removeClass("d-none").text(errText || "登入失敗");
                }
                resetBtns();
                return;
            }

            // ==========================================
            // --- [新增] 登入成功後，處理記住我邏輯 ---
            // ==========================================
            if ($rememberCheckbox.is(":checked")) {
                localStorage.setItem("oikos_remember_email", emailVal);
            } else {
                localStorage.removeItem("oikos_remember_email");
            }

            // Success
            $recaptchaModal.hide();
            $loginBtn.text("登入成功！跳轉中...");
            window.location.href = "/index.html";

        } catch (e) {
            console.error(e);
            $recaptchaModal.hide();
            grecaptcha.reset();
            $globalAlert.removeClass("d-none").text("系統發生錯誤，請檢查網路連線");
            resetBtns();
        }
    });

    function resetBtns() {
        $modalConfirmBtn.prop("disabled", false).text("確認並送出");
        $loginBtn.prop("disabled", false).text("登入");
    }
});
