$(document).ready(function () {
    console.log("Register script initialized with Modal");

    const $btn = $("#registerBtn");
    const $modalConfirmBtn = $("#confirm-captcha-btn"); // 彈窗內的確認按鈕
    const $recaptchaModal = new bootstrap.Modal(document.getElementById('recaptchaModal'));
    
    const $sendBtn = $("#send-code-btn");
    const $alertBox = $("#alertBox");

    const fields = {
        name: $("#name"),
        email: $("#email"),
        password: $("#password"),
        confirmPassword: $("#confirmPassword"),
        verifyCode: $("#verifyCode"),
        terms: $("#terms")
    };

    const REGEX = {
        NAME: /^[\u4e00-\u9fa5]{2,50}$/, 
        EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, 
        CODE: /^\d{6}$/, 
        PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])[A-Za-z\d]{8,20}$/ 
    };

    // --- UI 輔助函式 (保持不變) ---
    function setInvalid($input, msg) {
        $input.removeClass("is-valid").addClass("is-invalid");
        let $parent = $input.closest('.mb-3, .input-group');
        $parent.find('.valid-icon').remove();

        let $feedback = $input.siblings(".invalid-feedback");
        if ($input.parent().hasClass("input-group")) {
            $feedback = $input.parent().find(".invalid-feedback");
            if ($feedback.length === 0) $feedback = $input.parent().siblings(".invalid-feedback");
        }
        if ($input.attr("type") === "checkbox") $feedback = $input.siblings(".invalid-feedback");
        
        $feedback.text(msg).show();
    }

    function setValid($input) {
        $input.removeClass("is-invalid").addClass("is-valid");
        let $parent = $input.closest('.mb-3, .input-group');
        $parent.find(".invalid-feedback").hide();
        $parent.find('.valid-icon').remove();
        if ($input.parent().hasClass("input-group")) {
            $input.parent().append('<div class="valid-icon"></div>');
        } else {
            $input.parent().append('<div class="valid-icon"></div>');
        }
    }
    function validateInput(inputId) {
        let isValid = true;
        let msg = "";
        let $el;
        let val;

        switch (inputId) {
            case "name":
                $el = fields.name;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "姓名不能為空"; } 
                else if (!REGEX.NAME.test(val)) { isValid = false; msg = "姓名必須是 2 到 50 個中文字"; }
                break;
            case "email":
                $el = fields.email;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "Email 不能為空"; } 
                else if (!REGEX.EMAIL.test(val)) { isValid = false; msg = "Email 格式不正確"; }
                break;
            case "verifyCode":
                $el = fields.verifyCode;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "驗證碼不能為空"; } 
                else if (!REGEX.CODE.test(val)) { isValid = false; msg = "驗證碼必須是 6 位數字"; }
                break;
            case "password":
                $el = fields.password;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "密碼不能為空"; } 
                else if (!REGEX.PASSWORD.test(val)) { isValid = false; msg = "密碼需 8-20 字，含大小寫英文字母"; }
                if (fields.confirmPassword.val().trim() !== "") setTimeout(() => validateInput("confirmPassword"), 0);
                break;
            case "confirmPassword":
                $el = fields.confirmPassword;
                val = $el.val().trim();
                const pwdVal = fields.password.val().trim();
                if (!val) { isValid = false; msg = "確認密碼不能為空"; } 
                else if (!REGEX.PASSWORD.test(val)) { isValid = false; msg = "確認密碼格式錯誤"; } 
                else if (val !== pwdVal) { isValid = false; msg = "兩次密碼輸入不一致"; }
                break;
            case "terms":
                $el = fields.terms;
                if (!$el.is(":checked")) { isValid = false; msg = "請勾選同意服務條款"; }
                break;
        }
        if ($el) {
            if (isValid) setValid($el);
            else setInvalid($el, msg);
        }
        return isValid;
    }

    $("input").not("#terms").on("blur keyup", function () { validateInput(this.id); });
    $("#terms").on("change", function () { validateInput("terms"); });

    $sendBtn.on("click", async function () {
        const $email = fields.email;
        const emailVal = $email.val().trim();
        if (!validateInput("email")) return;
        $sendBtn.prop("disabled", true).text("檢查中...");
        try {
            const checkRes = await fetch(`/api/register/check-email?email=${encodeURIComponent(emailVal)}`, { method: "GET", credentials: "include" });
            if (checkRes.ok) {
                const checkData = await checkRes.json();
                if (checkData.exists) {
                    setInvalid($email, `此 Email 已經註冊過 (${checkData.providers.join(", ")})`);
                    $sendBtn.prop("disabled", false).text("寄送驗證碼");
                    return; 
                }
            }
            $sendBtn.text("寄送中...");
            const res = await fetch(`/api/register/send-code?email=${encodeURIComponent(emailVal)}`, { method: "POST", credentials: "include" });
            const msg = await res.text();
            if (!res.ok) {
                setInvalid($email, msg || "驗證碼寄送失敗");
                $sendBtn.prop("disabled", false).text("寄送驗證碼");
                return;
            }
            showAlert("驗證碼已寄出！請檢查您的信箱。", "success");
            setValid($email);
            startCountdown($sendBtn);
        } catch (e) {
            console.error(e);
            showAlert("網路錯誤，請稍後再試。", "danger");
            $sendBtn.prop("disabled", false).text("寄送驗證碼");
        }
    });

    // ==========================================
    // 1. 註冊按鈕點擊：只驗證欄位 + 顯示彈窗
    // ==========================================
    $btn.on("click", async function () {
        $alertBox.addClass("d-none");
        let isAllValid = true;
        for (let key in fields) {
            if (!validateInput(fields[key].attr("id"))) isAllValid = false;
        }

        if (!isAllValid) {
            $(".register-card").addClass("shake-animation");
            setTimeout(() => $(".register-card").removeClass("shake-animation"), 500);
            return;
        }

        // 重置並顯示彈窗
        try { grecaptcha.reset(); } catch(e) {}
        $recaptchaModal.show();
    });

    // ==========================================
    // 2. 彈窗確認按鈕：檢查 reCAPTCHA + 執行 AJAX
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
        $btn.prop("disabled", true).text("註冊中...");

        const data = {
            name: fields.name.val().trim(),
            email: fields.email.val().trim(),
            verifyCode: fields.verifyCode.val().trim(),
            password: fields.password.val().trim(),
            confirmPassword: fields.confirmPassword.val().trim(),
            recaptchaToken: recaptchaResponse
        };

        try {
            const res = await fetch("/api/register", {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(data)
            });
            const msg = await res.text();

            if (!res.ok) {
                $recaptchaModal.hide(); // 失敗關閉彈窗
                grecaptcha.reset();

                if (msg.includes("驗證碼")) setInvalid(fields.verifyCode, msg);
                else if (msg.includes("Email")) setInvalid(fields.email, msg);
                else if (msg.includes("姓名")) setInvalid(fields.name, msg);
                else if (msg.includes("密碼")) setInvalid(fields.password, msg);
                else showAlert(msg, "danger");
                
                resetBtns();
                return;
            }

            $recaptchaModal.hide();
            showAlert("註冊成功！正在為您登入...", "success");
            setTimeout(() => { window.location.href = "/index.html"; }, 1000); 

        } catch (e) {
            $recaptchaModal.hide();
            grecaptcha.reset();
            showAlert("發生網路錯誤，請稍後再試。", "danger");
            resetBtns();
        }
    });

    function showAlert(msg, type) {
        $alertBox.attr('class', `alert alert-${type}`).html(msg).removeClass("d-none");
    }

    function resetBtns() {
        $modalConfirmBtn.prop("disabled", false).text("確認並送出");
        $btn.prop("disabled", false).text("註冊");
    }

    function startCountdown($btnElement) {
        let t = 60;
        const timer = setInterval(() => {
            $btnElement.text(`${t} 秒後可重送`);
            t--;
            if (t < 0) {
                clearInterval(timer);
                $btnElement.prop("disabled", false).text("寄送驗證碼");
            }
        }, 1000);
    }
});