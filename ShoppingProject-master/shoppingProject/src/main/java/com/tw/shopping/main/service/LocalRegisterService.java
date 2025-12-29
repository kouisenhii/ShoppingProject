package com.tw.shopping.main.service;

import com.tw.shopping.main.entity.UserAuthProviderEntity;
import com.tw.shopping.main.entity.UserEntity;
import com.tw.shopping.main.dto.LocalUserRegisterDto;
import com.tw.shopping.main.dto.SessionUserDto;
import com.tw.shopping.main.repository.UserAuthProviderRepository;
import com.tw.shopping.main.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocalRegisterService {

    private final UserRepository userRepository;
    private final UserAuthProviderRepository providerRepository;
    private final PasswordEncoder passwordEncoder;


    /**
     * Local 註冊服務主流程
     *
     * 1. 驗證密碼一致性
     * 2. 檢查 Email 是否已存在於資料庫
     *    - 若存在且已綁定 LOCAL → 直接登入
     *    - 若存在但只有 Google / LINE → 拒絕註冊
     * 3. 若 Email 不存在 → 進入正常註冊流程 (doNormalRegister)
     *
     * @return UserinfoEntity 完整使用者資料
     */
    public UserEntity register(LocalUserRegisterDto request,
                                   HttpServletRequest req,
                                   HttpServletResponse resp) {

        String email = request.getEmail();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        // Step 1：確認密碼與確認密碼相同
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("密碼與確認密碼不一致");
        }

        // Step 2：查詢此 Email 是否存在於 UserInfo
        Optional<UserEntity> existUserOpt = userRepository.findByEmail(email);

        // ========== Case 1：Email 已存在 ==========
        if (existUserOpt.isPresent()) {

            UserEntity existUser = existUserOpt.get();

            // 查詢此 Email 所綁定的所有 provider (LOCAL / GOOGLE / LINE)
            List<UserAuthProviderEntity> providers =
                    providerRepository.findAllByProviderEmail(email);

            boolean hasLocal = providers.stream()
                    .anyMatch(p -> p.getProvider().equals("LOCAL"));

            // Case 1-1：已有 LOCAL → 視為登入
            if (hasLocal) {
                setSessionUser(req, existUser, "LOCAL", null);
                return existUser;
            }

            // Case 1-2：已有 Google / Line，但沒有 LOCAL → 不允許重複註冊
            String usedBy = providers.stream()
                    .map(UserAuthProviderEntity::getProvider)
                    .collect(Collectors.joining(" / "));

            throw new RuntimeException(
                    "此 Email 已被 " + usedBy + " 使用，請換 Email 或使用上述方式登入"
            );
        }

        // ========== Case 2：Email 不存在 → 正常註冊 ==========
        return doNormalRegister(request, req);
    }


    /**
     * 正常註冊流程：
     * 1. 驗證碼驗證（必須先寄出驗證碼）
     * 2. 建立 UserInfo
     * 3. 建立 Provider（LOCAL）
     * 4. 自動登入（寫入 SessionUser）
     */
    private UserEntity doNormalRegister(LocalUserRegisterDto request,
                                            HttpServletRequest req) {

        // 取得 session（必須是前面驗證碼流程所建立）
        HttpSession session = req.getSession(false);
        if (session == null) throw new RuntimeException("請先寄送驗證碼");

        // 驗證碼相關資訊
        String verifyEmail = (String) session.getAttribute("email_for_verify");
        String verifyCode = (String) session.getAttribute("email_code");

        // 驗證是否有寄過驗證碼
        if (verifyEmail == null || verifyCode == null)
            throw new RuntimeException("請先寄送驗證碼");

        // 驗證碼是否屬於當前 email
        if (!verifyEmail.equals(request.getEmail()))
            throw new RuntimeException("驗證碼為另一個 Email 所寄送");

        // 驗證使用者輸入的 verifyCode 是否符合
        if (!verifyCode.equals(request.getVerifyCode()))
            throw new RuntimeException("驗證碼錯誤");

        // Step：新增 UserInfo → icon 使用 DB DEFAULT
        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .verifiedAccount(true)
                .build();

        userRepository.save(user);

        // Step：新增 Provider (LOCAL)
        UserAuthProviderEntity provider = UserAuthProviderEntity.builder()
                .user(user)
                .provider("LOCAL")
                .providerUserid(user.getEmail())
                .providerEmail(user.getEmail())
                .providerName(user.getName())
                .build();

        providerRepository.save(provider);

        // Step：Local 註冊完成後 → 自動進行 Session Login
        setSessionUser(req, user, "LOCAL", null);

        // 清除驗證碼避免重複使用
        session.removeAttribute("email_for_verify");
        session.removeAttribute("email_code");

        return user;
    }


    /**
     * 建立 SessionUser 並寫入 session
     *
     * 若為 LOCAL：
     *     - 使用 DB 的 icon BLOB 轉 Base64
     *     - 若沒有 icon → 使用預設頭像
     *
     * 若為 Google / LINE：
     *     - 使用第三方給的 picture URL
     *
     * Session 儲存名稱固定為：USER
     */
    private void setSessionUser(HttpServletRequest req,
                                UserEntity user,
                                String provider,
                                String providerPicture) {

        String finalPicture;

        // LOCAL → 將 BLOB 轉成 Base64 Data URI
        if (provider.equals("LOCAL")) {

            if (user.getIcon() != null && user.getIcon().length > 0) {
                String base64 = java.util.Base64.getEncoder().encodeToString(user.getIcon());
                finalPicture = "data:image/jpeg;base64," + base64;
            } else {
                finalPicture = "/img/defaultAvatar.jpg";
            }

        } else {
            // Google / LINE → 使用 provider picture
            finalPicture = (providerPicture != null)
                    ? providerPicture
                    : "/img/defaultAvatar.jpg";
        }

        // 建立 SessionUser DTO
        SessionUserDto su = SessionUserDto.from(user, provider, finalPicture);

        // 寫入 session
        req.getSession().setAttribute("USER", su);
    }
}
