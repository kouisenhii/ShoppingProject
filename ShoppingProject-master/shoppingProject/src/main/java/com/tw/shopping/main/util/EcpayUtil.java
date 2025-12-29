package com.tw.shopping.main.util;

import java.util.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;

public class EcpayUtil {
    
    /**
     * 計算 CheckMacValue (簽章值)
     */
    public static String generateCheckMacValue(Map<String, String> params, String hashKey, String hashIV) {
        
        // 1. 移除 CheckMacValue 參數
        params.remove("CheckMacValue"); 

        // 2. 排序參數 (依 A-Z 順序)
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        // 3. 組合字串
        StringBuilder data = new StringBuilder();
        data.append("HashKey=").append(hashKey);
        
        for (String key : keys) {
            data.append("&").append(key).append("=").append(params.get(key));
        }
        
        data.append("&HashIV=").append(hashIV);

        // 4. URL Encode (編碼處理)
        try {
            // 【核心修正點】：先轉為小寫 (.toLowerCase())，再進行取代
            // 因為 ECPay 規定編碼必須是小寫 (如 %2f)，且 Java URLEncoder 預設是大寫 (如 %2F)
            String encodedData = URLEncoder.encode(data.toString(), StandardCharsets.UTF_8.toString())
                                           .toLowerCase() // <--- 加上這一行！
                                           .replaceAll("%2d", "-")
                                           .replaceAll("%5f", "_")
                                           .replaceAll("%2e", ".")
                                           .replaceAll("%21", "!")
                                           .replaceAll("%2a", "*")
                                           .replaceAll("%28", "(")
                                           .replaceAll("%29", ")")
                                           .replaceAll("%20", "+");
            
            // 5. 進行 SHA256 雜湊運算
            String checkMacValue = DigestUtils.sha256Hex(encodedData).toUpperCase();
            
            return checkMacValue;
            
        } catch (Exception e) {
            e.printStackTrace();
            return ""; 
        }
    }
}