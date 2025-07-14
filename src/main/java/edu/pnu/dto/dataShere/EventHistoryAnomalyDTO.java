package edu.pnu.dto.dataShere;

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
@Builder
@ToString

public class EventHistoryAnomalyDTO {
	
	// 각 이벤트별 상세
    private Long eventId;
    private boolean abnormal;
    private boolean jump;
    private double jumpScore;
    private boolean evtOrderErr;
    private double evtOrderErrScore;
    private boolean epcFake;
    private double epcFakeScore;
    private boolean epcDup;
    private double epcDupScore;
    private boolean locErr;
    private double locErrScore;
}