package edu.pnu.dto.dataShere;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileAnomalyStatsDTO {
    private int totalEvents;
    private int jumpCount;
    private int evtOrderErrCount;
    private int epcFakeCount;
    private int epcDupCount;
    private int locErrCount;
    private int abnormalCount;
    private double jumpRate;
    private double evtOrderErrRate;
    private double epcFakeRate;
    private double epcDupRate;
    private double locErrRate;
    private double abnormalRate;
}
