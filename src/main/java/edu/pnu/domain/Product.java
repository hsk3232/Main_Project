package edu.pnu.domain;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Builder
public class Product {
	@Id // 고유한 값임으로 AI 필요 없음
	private Long epcProduct; // 상품코드
	
	private String productName; // 상품명
	
//	// N:1 여러개의 상품이 1개의 장소에 있을 수 있음
//	@ManyToOne(fetch = FetchType.LAZY)
//	//@JoinColumn(name = 현재 테이블에 생길 새로운 칼럼명, referenced = 참조할 테이블의 PK  또는 UK)
//	@JoinColumn(name = "location_id", referencedColumnName = "location_id")
//	private Location location; // 생산지 코드
	
//	// 1:N (제품하나 당 여러 개의 EPC 할당됨) -> List로 받아야함.
//	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
//	@ToString.Exclude
//	private List<EPC> epc; 
}
