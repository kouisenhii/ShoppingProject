package com.tw.shopping.main.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Integer categoryid;
	private String cname, code, categoryimage;
	private Integer parentid;
}
