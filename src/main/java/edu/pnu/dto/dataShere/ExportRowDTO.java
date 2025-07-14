package edu.pnu.dto.dataShere;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import edu.pnu.domain.EventHistory;
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
	private Long eventId;
	private String epcCode;
	private Long locationId;
	private String businessStep;
	private String eventType;
	private LocalDateTime eventTime;
	private Long fileId;
	
	public static ExportRowDTO fromEntity(EventHistory h) {
		return ExportRowDTO.builder()
				.eventId(h.getEventId())
		        .epcCode(h.getEpc().getEpcCode())
		        .locationId(h.getLocation().getLocationId())
		        .businessStep(h.getBusinessStep())
		        .eventType(h.getEventType())
		        .eventTime(h.getEventTime())
		        .fileId(h.getFileLog().getFileId())
				.build();
	}
}
