package com.tw.shopping.main.dto;

import java.io.Serializable;

import lombok.Data;

// 專門給前端次分類顯示數量所設計的(UI用)
@Data
public class CategoryWithCountDto implements Serializable{
    private static final long serialVersionUID = 1L;
    private String code;
    private String cname;
    private Long count;

    // 這個建構式是給 Repository 查詢用的
    public CategoryWithCountDto(String code, String cname, Long count){
        this.code = code;
        this.cname = cname;
        this.count = count;
    }
}
