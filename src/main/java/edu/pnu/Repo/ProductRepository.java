package edu.pnu.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
	
	// 제품명으로 검색
	List<Product> findByProductNameContaining(String name);

	// 고유 상품 종류 카운트
	long count();
}
