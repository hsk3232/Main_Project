package edu.pnu.domain;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name="epc")
public class EPC {
	
	@Id
	private String epcCode;
	private String epcCompany;
	private String epcProduct;
	private String epcIot;
	private String epcManufacture;
	private String epcSerial;
	
	//N:1 productId와 연결됨
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	// EPC 테이블에 생길 새로운 칼럼명, 참조할 테이블의 PK
    @JoinColumn(name = "productId", referencedColumnName = "productId")
	private Product product;
	
	//1:N 하나의 EPC가 여러 개의 이벤트 히스토리를 가짐
	@OneToMany(mappedBy = "epc", fetch = FetchType.LAZY)
	@ToString.Exclude
	private List<EventHistory> eventHistory;
}
