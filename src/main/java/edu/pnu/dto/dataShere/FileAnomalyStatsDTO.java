package edu.pnu.dto.dataShere;

import edu.pnu.domain.Csv;
import edu.pnu.domain.FileAnomalyStats;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileAnomalyStatsDTO {
	private int fileId;
    private int totalEvents;
    private int jumpCount;
    private int evtOrderErrCount;
    private int epcFakeCount;
    private int epcDupCount;
    private int locErrCount;
    
    
    public static FileAnomalyStats toEntity(FileAnomalyStatsDTO f, Csv c) {
    	return FileAnomalyStats.builder()
    			.csv(c) //FileAnomalyStats entity에 있는 joinColum 변수명을 써야함.
    			.totalEvents(f.getTotalEvents())
    			.jumpCount(f.getJumpCount())
    			.evtOrderErrCount(f.getEvtOrderErrCount())
    			.epcDupCount(f.epcDupCount)
    			.epcFakeCount(f.getEpcFakeCount())
    			.locErrCount(f.getLocErrCount())
    			.build();
    }
}
