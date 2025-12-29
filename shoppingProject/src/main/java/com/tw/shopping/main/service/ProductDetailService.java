package com.tw.shopping.main.service;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.tw.shopping.main.dto.ProductDetailDto;
import com.tw.shopping.main.dto.ProductDetailDto.ColorOption;
import com.tw.shopping.main.dto.ProductDetailDto.Images;
import com.tw.shopping.main.dto.ProductDetailDto.Options;
import com.tw.shopping.main.dto.ProductDetailDto.RatingDto;
import com.tw.shopping.main.dto.ProductDetailDto.RelatedProductDto;
import com.tw.shopping.main.dto.ProductDetailDto.ReviewPageDto;
import com.tw.shopping.main.entity.CategoryEntity;
import com.tw.shopping.main.entity.ProductEntity;
import com.tw.shopping.main.repository.CategoryRepository;
import com.tw.shopping.main.repository.ProductRepository;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;


@Service
public class ProductDetailService {

	@Autowired
	private ProductRepository productRepo;
	@Autowired
	private CategoryRepository categoryRepo;
	@Autowired
    private ObjectProvider<ProductDetailService> selfProvider;
	// 建立一個固定10條執行緒的池子 (數量依CPU核心數調整，例如10~20)
	private final ExecutorService taskExecutor = Executors.newFixedThreadPool(10);	
	



	// 共用邏輯 -> 取得同系列(pname跟description)
	private List<ProductEntity> findSameSeries(ProductEntity product) {
		return productRepo.findAll((root, query, cb) -> cb.and(
			cb.equal(root.get("pname"),product.getPname()),
			cb.equal(root.get("description"),product.getDescription())
		));
	}




	//共用邏輯 -> 取得平均評分(取小數點第一位)
	private Double calculateAverageRating(List<RatingDto> reviews){
		if(reviews == null || reviews.isEmpty()){
			return 0.0;
		}
		double avg = reviews.stream()
				.mapToInt(RatingDto::getRating)
				.average()
				.orElse(0.0);

		// 數學邏輯：四捨五入到最近的0.5(半顆星)
		return Math.round(avg * 2) / 2.0;	
	}




	//共用邏輯 -> 去除所有id的重複顏色 (Variants)
	private List<ProductEntity> uniqueColorVairant(List<ProductEntity> variant){
		Map<String, ProductEntity> map = new LinkedHashMap<>();
		
		for(ProductEntity v : variant) {
			String color = v.getColor();
			if(color != null) {
				map.putIfAbsent(color, v);
			}
		}
		return new ArrayList<>(map.values()).stream().toList();
	}
	


	
	// 共用邏輯 -> 組顏色DTO
	private List<ColorOption> buildColorOptions(List<ProductEntity> uniqueVariants) {
		List<ColorOption> list = new ArrayList<>();

		for(ProductEntity v : uniqueVariants) {
			ColorOption colorOption = new ColorOption();
			colorOption.setProductid(v.getProductid());
			colorOption.setValue(v.getColor());
			colorOption.setValue(v.getColor());
			colorOption.setName(v.getColor());
			colorOption.setSpecification(v.getSpecification());
			colorOption.setPrice(v.getPrice());
			colorOption.setStock(v.getStock());
			list.add(colorOption);
		}
	return list;
	}

	
	

	//共用邏輯 -> 組圖片DTO
	private Images buildImageSet(List<ProductEntity> uniqueVariants){
		Images images = new Images();
		List<String> list = uniqueVariants.stream()
					.map(ProductEntity::getProductimage)
					.filter(img -> img != null && !img.isEmpty())
					.toList();
		images.setMain(list);
		images.setThumb(list);
		return images;
	}



	//共用邏輯 -> 組評價 DTO
    private List<RatingDto> buildRatingList(List<ProductEntity> variants){
        return variants.stream()
                .filter(v -> v.getRating() != null && v.getRating() > 0)
                .map(v -> {
                    RatingDto r = new RatingDto();
                    r.setColor(v.getColor());
                    r.setRating(v.getRating());
                    return r;
                })
                .toList();
    }




	// 共用邏輯 -> 推薦商品查詢 Specification
    private Specification<ProductEntity> buildRelatedSpec(Integer categoryId, String pname, String description){
		return (root, query, cb) -> {

			Subquery<Integer> sub = query.subquery(Integer.class);
			Root<ProductEntity> subRoot = sub.from(ProductEntity.class);

			sub.select(cb.literal(1));
			sub.where(
				cb.equal(subRoot.get("pname"), pname),
				cb.equal(subRoot.get("description"), description),
				cb.equal(subRoot.get("productid"), root.get("productid"))
			);

			return cb.and(
				cb.equal(root.get("category").get("categoryid"), categoryId),
				cb.not(cb.exists(sub))
			);
		};
	}



	//
	public ReviewPageDto getProductReviews(Integer productId) {
		// 1. 為了找到同系列，先抓出該商品
		ProductEntity product = productRepo.findById(productId).orElse(null);
		if (product == null) return null;

		// 2. 找出同系列所有變體
		List<ProductEntity> variants = findSameSeries(product);

		// 3. 取得所有有評分的資料 (尚未分頁)
		List<RatingDto> allReviews = buildRatingList(variants);

		// 6. 包裝回傳
		ReviewPageDto result = new ReviewPageDto();
		result.setReviews(allReviews);
		result.setTotalElements((long) allReviews.size());
		result.setCurrentPage(1);
		result.setTotalPages(1);
		
		
		return result;
	}


	
	// ==========================================================
    //  入口方法 (無快取，負責組裝)
    // ==========================================================
    public ProductDetailDto getProductDetail(Integer productId) {
        
        // 1. 先拿 靜態資料(Redis快取)
        ProductDetailDto dto = selfProvider.getObject().getCachedStaticData(productId);

        // 如果是空商品直接回傳
        if (dto.getProductid() == -1) return dto;

        // 2.即時更新庫存(直接查DB)
        syncRealTimeStock(dto);

        return dto;
    }

    // ==========================================================
    //  靜態資料來源 (有快取，負責很重的查詢)
    // ==========================================================
    @Cacheable(value = "product_detail", key = "#productId") 
    public ProductDetailDto getCachedStaticData(Integer productId) {
        
        ProductEntity product = productRepo.findById(productId).orElse(null);
        if(product == null) {
            ProductDetailDto dummy = new ProductDetailDto();
            dummy.setProductid(-1);
            return dummy;
        }

        ProductDetailDto dto = new ProductDetailDto();
        dto.setProductid(product.getProductid());
        dto.setPname(product.getPname());
        dto.setSpecification(product.getSpecification());
        dto.setPrice(product.getPrice());
        // 注意：這裡的 stock 是舊的，稍後會被 syncRealTimeStock 覆蓋
        dto.setStock(product.getStock()); 
        dto.setDescription(product.getDescription());
        dto.setShipping("<p>全店滿2000元宅配免運</p>");

        // 並行處理 1
        CompletableFuture<Void> variantsTask = CompletableFuture.runAsync(() -> {
            List<ProductEntity> variants = findSameSeries(product);
            List<ProductEntity> uniqueColorList = uniqueColorVairant(variants);

            Options options = new Options();
            options.setColor(buildColorOptions(uniqueColorList));
            dto.setOptions(options);

            dto.setImages(buildImageSet(uniqueColorList));

            List<RatingDto> reviews = buildRatingList(variants);
            dto.setReviewCount(reviews.size());
            dto.setAverageRating(calculateAverageRating(reviews));
        }, taskExecutor);
        
        // 並行處理 2
        CategoryEntity loadCategory = product.getCategory();
        CompletableFuture<Void> categoryTask = CompletableFuture.runAsync(() -> {
            if (loadCategory != null) {
                dto.setCategoryid(loadCategory.getCategoryid());
                dto.setSubCategoryCode(loadCategory.getCode());
                dto.setSubCategoryName(loadCategory.getCname());
            }
            if (loadCategory != null && loadCategory.getParentid() != null) {
                CategoryEntity main = categoryRepo.findById(loadCategory.getParentid()).orElse(null);
                if (main != null) {
                    dto.setMainCategoryCode(main.getCode());
                    dto.setMainCategoryName(main.getCname());
                }
            }
        }, taskExecutor);
        
        CompletableFuture.allOf(variantsTask, categoryTask).join();

        return dto;
    }




    //  Helper: 庫存同步邏輯 (私有方法)
    private void syncRealTimeStock(ProductDetailDto dto) {
        // 利用索引快速重查一次 DB，抓出同系列最新庫存
        ProductEntity probe = new ProductEntity();
        probe.setPname(dto.getPname());
        probe.setDescription(dto.getDescription());
        
        List<ProductEntity> freshVariants = findSameSeries(probe);

        // 轉成 Map 方便比對
        Map<Integer, Integer> stockMap = freshVariants.stream()
            .collect(Collectors.toMap(ProductEntity::getProductid, ProductEntity::getStock));

        // 更新 "主商品" 庫存
        if (stockMap.containsKey(dto.getProductid())) {
            dto.setStock(stockMap.get(dto.getProductid()));
        }

        // 更新 "規格選項" 庫存
        if (dto.getOptions() != null && dto.getOptions().getColor() != null) {
            for (ColorOption opt : dto.getOptions().getColor()) {
                if (stockMap.containsKey(opt.getProductid())) {
                    opt.setStock(stockMap.get(opt.getProductid()));
                }
            }
        }
    }




    // getRelatedProducts 使用共用邏輯(去除重複)
	@Cacheable(value = "related_products", key = "{#categoryid, #excludeId, #limit}")
    public List<RelatedProductDto> getRelatedProducts(Integer categoryid,
                                                      Integer excludeId,
                                                      int limit){

		System.out.println(">>> [DB查詢] 正在計算推薦商品...");
		
    	ProductEntity current = productRepo.findById(excludeId).orElse(null);
        if(current == null) return List.of();

       	Specification<ProductEntity> spec = buildRelatedSpec(
				current.getCategory().getCategoryid(),
				current.getPname(),
				current.getDescription()
		);

        Pageable pageable = PageRequest.of(0, limit);

        return productRepo.findAll(spec, pageable).stream()
                .map(p -> new RelatedProductDto(
                        p.getProductid(),
						p.getDescription(),
                        p.getPname(),
                        p.getPrice(),
                        p.getProductimage()
                ))
                .toList();
    }


}
