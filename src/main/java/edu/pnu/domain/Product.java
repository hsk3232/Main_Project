package edu.pnu.domain;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name="product")
public class Product {
	@Id // 고유한 값임으로 AI 필요 없음
	private Long productId; // 상품코드
	
	private String productName; // 상품명
	
	// 1:N (제품하나 당 여러 개의 EPC 할당됨) -> List로 받아야함.
	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
	@ToString.Exclude
	private List<EPC> epc; 
}
