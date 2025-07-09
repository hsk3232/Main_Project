package edu.pnu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImgConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		/*
		 * 이미지 공개 접근 구조 예시:
		 * 실제 파일: c:/workspace-fullstack/projectDB/img/goods/123.png
		 * URL로 접근: http://localhost:8080/api/public/img/goods/123.png
		 * 이렇게 연결해주는 게 바로 ResourceHandlerRegistry의 역할
		 * 
		 * => 공개 방식으로 보안이 중요할 때는 사용 하면 안됨
		 * 
		 */

		// 상품 이미지
//		registry.addResourceHandler("/api/public/img/goods/**")
//				.addResourceLocations("file:c:/workspace-fullstack/projectDB/img/goods/"); // 위치 설정
//		
		
	}

}
