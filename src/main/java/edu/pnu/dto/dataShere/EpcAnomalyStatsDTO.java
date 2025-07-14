package edu.pnu.dto.dataShere;

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
    private int abnormalCount;
}
