$(document).ready(function() {
    console.log("Forget Password script initialized with position fixes.");

    const $sendBtn = $("#send-code-btn");
    const $verifyBtn = $("#verify-btn");
    const $globalAlert = $("#global-alert");

    const fields = {
        email: $("#email"),
        code: $("#code")
    };

    const REGEX = {
        EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
        CODE: /^\d{6}$/
    };

    // =========================
    // UI 狀態切換 (修正版)
    // =========================
    function setInvalid($input, msg) {
        $input.removeClass("is-valid").addClass("is-invalid");
        
        // 【修正】強制移除父層內的綠勾
        let $parent = $input.closest('.mb-3, .input-group');
        $parent.find('.valid-icon').remove();

        // Input Group 的錯誤訊息處理
        let $feedback = $input.siblings(".invalid-feedback");
        if ($input.parent().hasClass("input-group")) {
            $feedback = $input.parent().find(".invalid-feedback");
            if ($feedback.length === 0) {
                $feedback = $input.parent().siblings(".invalid-feedback");
            }
        }
        $feedback.text(msg).show();
    }

    function setValid($input) {
        $input.removeClass("is-invalid").addClass("is-valid");
        
        let $parent = $input.closest('.mb-3, .input-group');
        $parent.find(".invalid-feedback").hide();

        // 【修正】移除舊 Icon 再新增
        $parent.find('.valid-icon').remove();

        if ($input.parent().hasClass("input-group")) {
            $input.parent().append('<div class="valid-icon"></div>');
        } else {
            $input.parent().append('<div class="valid-icon"></div>');
        }
    }

    // =========================
    // 單一欄位驗證
    // =========================
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

            case "code":
                $el = fields.code;
                val = $el.val().trim();
                if (!val) { isValid = false; msg = "驗證碼不能為空"; } 
                else if (!REGEX.CODE.test(val)) { isValid = false; msg = "驗證碼必須是 6 位數字"; }
                break;
        }

        if ($el) {
            if (isValid) setValid($el);
            else setInvalid($el, msg);
        }
        return isValid;
    }

    $("input").on("blur keyup", function() {
        validateInput(this.id);
        $globalAlert.addClass("d-none");
    });

    // 寄送驗證碼
    $sendBtn.on("click", function() {
        if (!validateInput("email")) return;
        const emailVal = fields.email.val().trim();

        $sendBtn.prop("disabled", true).text("寄送中...");
        $globalAlert.addClass("d-none");

        $.ajax({
            url: `/api/forget/send-code?email=${encodeURIComponent(emailVal)}`,
            method: "POST",
            xhrFields: { withCredentials: true },
            success: function(response) {
                $globalAlert.removeClass("d-none alert-danger").addClass("alert-success").text("驗證碼已寄出！請檢查您的信箱。");
                setValid(fields.email);
                startCountdown();
            },
            error: function(xhr) {
                const errText = xhr.responseText || "寄送失敗";
                if (errText.startsWith("Email:")) {
                    setInvalid(fields.email, errText.replace("Email:", ""));
                } else {
                    $globalAlert.removeClass("d-none alert-success").addClass("alert-danger").text(errText);
                }
                $sendBtn.prop("disabled", false).text("寄送驗證碼");
            }
        });
    });

    // 驗證
    $verifyBtn.on("click", function() {
        let isAllValid = true;
        if (!validateInput("email")) isAllValid = false;
        if (!validateInput("code")) isAllValid = false;

        if (!isAllValid) {
            $(".forget-card").addClass("shake-animation");
            setTimeout(() => $(".forget-card").removeClass("shake-animation"), 500);
            return;
        }

        const emailVal = fields.email.val().trim();
        const codeVal = fields.code.val().trim();

        $verifyBtn.prop("disabled", true).text("驗證中...");
        $globalAlert.addClass("d-none");

        $.ajax({
            url: `/api/forget/verify?email=${encodeURIComponent(emailVal)}&code=${encodeURIComponent(codeVal)}`,
            method: "POST",
            xhrFields: { withCredentials: true },
            success: function(response) {
                $verifyBtn.text("驗證成功！");
                setTimeout(() => {
                    window.location.href = "/html/reset_password.html";
                }, 800);
            },
            error: function(xhr) {
                const errText = xhr.responseText || "驗證失敗";
                if (errText.startsWith("驗證碼:")) {
                    setInvalid(fields.code, errText.replace("驗證碼:", ""));
                } else if (errText.startsWith("Email:")) {
                    setInvalid(fields.email, errText.replace("Email:", ""));
                } else {
                    $globalAlert.removeClass("d-none alert-success").addClass("alert-danger").text(errText);
                }
                $(".forget-card").addClass("shake-animation");
                setTimeout(() => $(".forget-card").removeClass("shake-animation"), 500);
                $verifyBtn.prop("disabled", false).text("下一步");
            }
        });
    });

    function startCountdown() {
        let t = 60;
        const timer = setInterval(() => {
            $sendBtn.text(`${t} 秒後可重送`);
            t--;
            if (t < 0) {
                clearInterval(timer);
                $sendBtn.prop("disabled", false).text("寄送驗證碼");
            }
        }, 1000);
    }
});