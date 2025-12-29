$(document).ready(function() {
    console.log("Reset Password script initialized with unified validation.");

    const $resetBtn = $("#reset-btn");
    const $alertBox = $("#alertBox");

    const fields = {
        newPassword: $("#new-password"),
        confirmPassword: $("#confirm-password")
    };

    // Regex 定義 (與 Register / Login 統一)
    // 密碼需 8-20 字，含大小寫英文字母
    const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])[A-Za-z\d]{8,20}$/;

    // =========================
    // UI 狀態切換函式
    // =========================
    
    function setInvalid($input, msg) {
        $input.removeClass("is-valid").addClass("is-invalid");
        // 強制移除綠勾
        $input.parent().find('.valid-icon').remove();
        // 顯示錯誤訊息
        $input.siblings(".invalid-feedback").text(msg).show();
    }

    function setValid($input) {
        $input.removeClass("is-invalid").addClass("is-valid");
        $input.siblings(".invalid-feedback").hide();

        // 移除舊 Icon
        $input.parent().find('.valid-icon').remove();
        
        // 新增打勾 Icon (CSS 已含 margin-top: -5px)
        $input.parent().append('<div class="valid-icon"></div>');
    }

    // =========================
    // 單一欄位驗證邏輯
    // =========================
    function validateInput(inputId) {
        let isValid = true;
        let msg = "";
        let $el;
        let val;

        switch (inputId) {
            case "new-password":
                $el = fields.newPassword;
                val = $el.val().trim();
                if (!val) {
                    isValid = false; msg = "新密碼不能為空";
                } else if (!PASSWORD_REGEX.test(val)) {
                    isValid = false; msg = "密碼需 8-20 字，含大小寫英文字母";
                }
                
                // 如果確認密碼已經有值，當新密碼變更時，也要觸發確認密碼的驗證
                if (fields.confirmPassword.val().trim() !== "") {
                    // 使用 setTimeout 避免遞迴調用
                    setTimeout(() => validateInput("confirm-password"), 0);
                }
                break;

            case "confirm-password":
                $el = fields.confirmPassword;
                val = $el.val().trim();
                const mainPwd = fields.newPassword.val().trim();
                
                if (!val) {
                    isValid = false; msg = "確認密碼不能為空";
                } else if (!PASSWORD_REGEX.test(val)) {
                    isValid = false; msg = "確認密碼格式錯誤";
                } else if (val !== mainPwd) {
                    isValid = false; msg = "兩次密碼輸入不一致";
                }
                break;
        }

        if ($el) {
            if (isValid) setValid($el);
            else setInvalid($el, msg);
        }
        return isValid;
    }

    // 綁定即時驗證
    $("input").on("blur keyup", function() {
        validateInput(this.id);
        $alertBox.addClass("d-none");
    });

    // =========================
    // 送出按鈕
    // =========================
    $resetBtn.on("click", function() {
        // 重置 Alert
        $alertBox.addClass("d-none");

        // 1. 全面驗證
        let isAllValid = true;
        if (!validateInput("new-password")) isAllValid = false;
        if (!validateInput("confirm-password")) isAllValid = false;

        if (!isAllValid) {
            $(".login-card").addClass("shake-animation");
            setTimeout(() => $(".login-card").removeClass("shake-animation"), 500);
            return;
        }

        const newPwd = fields.newPassword.val().trim();

        // 鎖定按鈕
        $resetBtn.prop("disabled", true).text("處理中...");

        // 2. 發送請求
        $.ajax({
            url: "/api/forget/reset-password",
            method: "POST",
            contentType: "application/json",
            xhrFields: {
                withCredentials: true
            },
            data: JSON.stringify({ newPassword: newPwd }),
            success: function(response) {
                showAlert("密碼已成功重設！正在跳轉至登入頁...", "success");
                
                // 延遲 1.5 秒跳轉
                setTimeout(() => {
                    window.location.href = "/html/login.html";
                }, 1500);
            },
            error: function(xhr, status, error) {
                console.error("重設錯誤:", error);
                const errorMsg = xhr.responseText || "重設失敗，請稍後再試";
                showAlert(errorMsg, "danger");
                
                $(".login-card").addClass("shake-animation");
                setTimeout(() => $(".login-card").removeClass("shake-animation"), 500);
                
                $resetBtn.prop("disabled", false).text("重設密碼");
            }
        });
    });

    // Alert 訊息顯示 Helper
    function showAlert(msg, type) {
        $alertBox
            .removeClass("d-none alert-success alert-danger alert-warning alert-info")
            .addClass(`alert alert-${type}`)
            .text(msg)
            .removeClass("d-none");
    }
});