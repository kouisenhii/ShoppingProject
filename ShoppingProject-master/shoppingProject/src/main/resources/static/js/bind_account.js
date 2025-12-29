// 使用 jQuery 的 $(document).ready() 確保 DOM 載入完成
$(document).ready(async function () {
    const $pendingCard = $("#pending-card");
    const $existingCard = $("#existing-card");
    const $bindText = $("#bind-text");
    const $bindBtn = $("#bind-btn");

    console.log("Bind Account script initializing...");

    // 1. 獲取待綁定及已綁定資訊
    try {
        const res = await fetch("/api/auth/pending-info", {
            method: "GET",
            credentials: "include"
        });

        if (!res.ok) {
            // 如果沒有待處理資料，通常是直接跳轉到主頁
            alert("錯誤：無可綁定資料或登入狀態錯誤");
            window.location.href = "/index.html";
            return;
        }

        const data = await res.json();
        const pending = data.pending;
        const existing = data.existingProviders;

        // 2. 動態顯示提示文字
        const existingProvidersNames = existing.map(e => e.provider).join(" / ");
        $bindText.html(
            `偵測到此 Email 已綁定「${existingProvidersNames}」帳號。` +
            `<br>是否要將「${pending.provider}」也綁定到同一個 OIKOS 帳號？`
        );

        // 3. 左邊：此次登入方式 (Pending)
        $pendingCard.html(`
            <img class="user-avatar" src="${pending.picture}" alt="${pending.name}" />
            <div class="name">${pending.name}</div>
            <div class="email text-truncate">${pending.email}</div>
            <div class="provider-badge">${pending.provider}</div>
        `);

        // 4. 右邊：已有的登入方式 (Existing) - 可以有多個
        const existingHTML = existing.map(p => `
            <div class="existing-box p-3">
                <img class="user-avatar" src="${p.picture || '/img/defaultAvatar.jpg'}" alt="${p.name}" />
                <div class="name">${p.name}</div>
                <div class="email text-truncate">${p.email}</div>
                <div class="provider-badge">${p.provider}</div>
            </div>
        `).join("");
        $existingCard.html(existingHTML);

        // 5. 綁定「確認綁定」按鈕事件
        $bindBtn.off('click').on('click', async function () {
            // 避免重複點擊
            $bindBtn.prop('disabled', true).text("處理中...");

            try {
                const res = await fetch("/api/auth/bind-provider", {
                    method: "POST",
                    credentials: "include"
                });

                if (res.ok) {
                    alert("綁定成功！您將被導向至主頁。");
                    window.location.href = "/index.html";
                } else {
                    const errorText = await res.text();
                    throw new Error("綁定失敗：" + (errorText || "系統錯誤"));
                }
            } catch (error) {
                console.error('綁定時發生錯誤:', error);
                alert(error.message);
                $bindBtn.prop('disabled', false).text("確認綁定"); // 恢復按鈕
            }
        });

    } catch (e) {
        console.error("初始化綁定頁面失敗:", e);
        // 建議保留跳轉或顯示錯誤訊息
    }
});