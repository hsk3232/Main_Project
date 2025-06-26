package edu.pnu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImgConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		// http://localhost:8080/images/123.png 같은 형식의 이미지 요청시 처리 핸들러 등록

		// 상품 이미지
//		registry.addResourceHandler("/api/public/img/goods/**")
//				.addResourceLocations("file:c:/workspace-fullstack/projectDB/img/goods/"); // 위치 설정
//		
	}

}
