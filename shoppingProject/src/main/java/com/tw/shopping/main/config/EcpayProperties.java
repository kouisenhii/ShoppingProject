package com.tw.shopping.main.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "ecpay")
@Data
public class EcpayProperties {
	    
	private String merchantId;
    private String hashKey;
    private String hashIV;
    private String apiUrl;
    private String ngrokBaseUrl;
    private String returnUrlRoute;
    private String clientBackUrlRoute;
	    
	 // 取得完整的 ReturnURL (綠界回傳給後端的網址)
    public String getFullReturnUrl() {
        return ngrokBaseUrl + returnUrlRoute;
    }
	    
	    // 取得完整的 ClientBackURL (導回給使用者的網址)
    public String getFullClientBackUrl() {
        return ngrokBaseUrl + clientBackUrlRoute;
    }
}
