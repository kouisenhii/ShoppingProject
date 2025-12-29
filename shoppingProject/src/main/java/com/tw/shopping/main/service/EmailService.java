package com.tw.shopping.main.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 發送 Email 的通用方法
     *
     * 功能：
     * - 建立 SimpleMailMessage
     * - 設定收件人、主旨、內文
     * - 透過 JavaMailSender 寄出
     *
     * 此方法供註冊驗證碼、忘記密碼驗證碼等功能共用。
     */
    public void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    /**
     * 註冊流程 — 寄送 Email 驗證碼
     *
     * 實作步驟：
     * 1. 產生六位數驗證碼
     * 2. 將驗證碼與 email 存入 Session（email_for_verify / email_code）
     * 3. 寄送 Email（使用 send()）
     *
     * 目的：
     * - 讓 LocalRegisterService 在註冊前能透過 Session 驗證使用者是否已收取驗證碼
     * - 避免未經驗證的 email 直接註冊
     */
    public void sendRegisterVerificationCode(String email, HttpSession session) {
        String code = generateCode();

        session.setAttribute("email_for_verify", email);
        session.setAttribute("email_code", code);

        send(email, "OIKOS 註冊驗證碼", "您的驗證碼為：" + code);
    }

    /**
     * 註冊流程 — 驗證使用者輸入的驗證碼是否正確
     *
     * 驗證項目：
     * - Session 是否存在驗證碼（避免未寄送驗證碼直接驗證）
     * - Session 中保存的 email 與使用者輸入是否一致
     * - Session 中保存的 code 與使用者輸入是否一致
     *
     * 回傳：
     * - true：驗證成功
     * - false：驗證失敗（email 不符、驗證碼錯誤或 session 已失效）
     */
    public boolean validateRegisterCode(String email, String code, HttpSession session) {

        String savedEmail = (String) session.getAttribute("email_for_verify");
        String savedCode = (String) session.getAttribute("email_code");

        if (savedEmail == null || savedCode == null) return false;
        if (!savedEmail.equals(email)) return false;
        if (!savedCode.equals(code)) return false;

        return true;
    }

    /**
     * 忘記密碼 — 寄送驗證碼
     *
     * 實作步驟：
     * 1. 產生六位數驗證碼
     * 2. 存入 forget_email / forget_code 至 Session
     * 3. 寄送驗證碼至使用者 Email
     *
     * 忘記密碼流程通常為：
     *   sendVerificationCode → verifyCode → 重設密碼
     */
    public void sendVerificationCode(String email, HttpSession session) {
        String code = generateCode();

        session.setAttribute("forget_email", email);
        session.setAttribute("forget_code", code);

        send(email, "OIKOS 密碼重設驗證碼", "您的驗證碼為：" + code);
    }

    /**
     * 忘記密碼 — 驗證使用者輸入的驗證碼
     *
     * 驗證項目：
     * - Session 是否還存在（避免重新整理或 session 過期導致錯誤）
     * - 使用者 email 是否與 session 中的 forget_email 一致
     * - 使用者輸入的驗證碼是否正確
     *
     * 若驗證成功：
     * - 移除 forget_code 避免驗證碼重複使用
     */
    public boolean verifyCode(String email, String code, HttpSession session) {
        String savedEmail = (String) session.getAttribute("forget_email");
        String savedCode = (String) session.getAttribute("forget_code");

        if (savedEmail == null || savedCode == null) return false;
        if (!savedEmail.equals(email)) return false;
        if (!savedCode.equals(code)) return false;

        session.removeAttribute("forget_code");
        return true;
    }

    /**
     * 產生六位數驗證碼
     *
     * 格式：
     * - 固定六位，前面不足會補 0，例如：003421、948201、120984
     * - 使用 Random 生成 0~999999
     */
    private String generateCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
