package edu.pnu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import edu.pnu.domain.EPC;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExportRowDTO {
	private Long locationId;
	private String scanLocation;
	private String hubType;
	private String businessStep;
	private String eventType;
	private Integer operatorId;
	private Integer deviceId;
	private String epcCode;
	private String epcHeader;
	private String epcCompany;
	private String epcProduct;
	private Long epcLot;
	private String epcManufacture;
	private String epcSerial;
	private String productName;
	private LocalDateTime eventTime;
	private LocalDateTime manufactureDate;
	private LocalDate expiryDate;
	
	public ExportRowDTO fromEntity(Location l, Product p, EPC e, EventHistory h) {
		return ExportRowDTO.builder()
				.build();
	}
}
