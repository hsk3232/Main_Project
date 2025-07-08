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
@Table(name="product")
public class Product {
	@Id // 고유한 값임으로 AI 필요 없음
	@Column(name = "epc_product") //Join시에는 name 써줘야함
	private Long epcProduct; // 상품코드
	
	private String productName; // 상품명
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location; // 생산지 코드
	
	// 1:N (제품하나 당 여러 개의 EPC 할당됨) -> List로 받아야함.
	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
	@ToString.Exclude
	private List<EPC> epc; 
}
