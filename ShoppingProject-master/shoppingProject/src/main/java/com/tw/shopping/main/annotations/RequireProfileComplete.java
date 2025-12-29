package com.tw.shopping.main.annotations;

import java.lang.annotation.*;

/**
 * 標記 Service 方法，強制要求使用者必須填寫完畢所有個人基本資料。
 */
@Target({ElementType.METHOD, ElementType.TYPE}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireProfileComplete {}