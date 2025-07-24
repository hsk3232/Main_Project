package edu.pnu.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name="epc")
public class Epc {
	
	@Id
	@Column(name ="epc_code")
	private String epcCode;
	
	private String epcHeader;
	
	private String epcLot;
	
	private String epcSerial;

	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "location_id")
	private Location location;
	
	//N:1 여러개의 epc_code가 하나의 상품에 있을 수 있음.
	//N:1에서 N은 자식이며, 관계의 주인!
	@ManyToOne(fetch = FetchType.LAZY) // FK
	@JoinColumn(name = "epc_product")
	private Product product;
	
	private LocalDateTime manufactureDate;
	private LocalDate expiryDate;
	
}
