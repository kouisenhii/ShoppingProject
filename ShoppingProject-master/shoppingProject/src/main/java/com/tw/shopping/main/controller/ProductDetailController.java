package com.tw.shopping.main.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tw.shopping.main.dto.ProductDetailDto;
import com.tw.shopping.main.dto.ProductDetailDto.RelatedProductDto;
import com.tw.shopping.main.dto.ProductDetailDto.ReviewPageDto;
import com.tw.shopping.main.service.ProductDetailService;

// @CrossOrigin("*")
@RestController
@RequestMapping("/api/product")
public class ProductDetailController {
	
    @Autowired
    private ProductDetailService productDetailService;

    
    /**
     * 商品詳細頁 API
     * GET /api/product/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductDetail(@PathVariable Integer id) {

    ProductDetailDto product = productDetailService.getProductDetail(id);

        if(product == null) {
            return ResponseEntity.status(404).body(
                java.util.Map.of("success", false, "message", "查無此商品")
            );
        }

        return ResponseEntity.ok(product);
    }

    
    
    @GetMapping("/{id}/reviews")
    public ResponseEntity<?> getProductReviews(@PathVariable Integer id) {
        // 呼叫 Service 取得分頁資料
        ReviewPageDto reviewPage = productDetailService.getProductReviews(id);

        if (reviewPage == null) {
            return ResponseEntity.status(404).body("查無商品");
        }

        return ResponseEntity.ok(reviewPage);
    }

    //限制相關商品數量為25
    @GetMapping("/related/{categoryid}")
    public List<RelatedProductDto> getRelatedProducts(@PathVariable Integer categoryid,
                                                    @RequestParam Integer exclude){
        return productDetailService.getRelatedProducts(categoryid, exclude, 25);
    }



}
