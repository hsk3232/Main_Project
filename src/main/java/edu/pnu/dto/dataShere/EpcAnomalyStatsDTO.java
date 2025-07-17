package edu.pnu.dto.dataShere;

import edu.pnu.domain.Epc;
import edu.pnu.domain.EpcAnomalyStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EpcAnomalyStatsDTO {
    private String epcCode;
    private int totalEvents;
    private int jumpCount;
    private int evtOrderErrCount;
    private int epcFakeCount;
    private int epcDupCount;
    private int locErrCount;
    
    public static EpcAnomalyStats toEntity(EpcAnomalyStatsDTO d, Epc e){
    	return EpcAnomalyStats.builder()
    			.epc(e) //EpcAnomalyStats entity에 있는 joinColum 변수명을 써야함.
    			.totalEvents(d.getTotalEvents())
    			.jumpCount(d.getJumpCount())
    			.evtOrderErrCount(d.getEvtOrderErrCount())
    			.epcFakeCount(d.getEpcFakeCount())
    			.epcDupCount(d.getEpcDupCount())
    			.locErrCount(d.getLocErrCount())
    			.build();
    }
}
