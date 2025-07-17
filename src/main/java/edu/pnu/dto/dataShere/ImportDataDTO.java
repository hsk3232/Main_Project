package edu.pnu.dto.dataShere;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder

public class ImportDataDTO {
    private Long fileId;
    private List<EventHistoryImportDTO> eventHistoryImportDTO;
    private List<EpcAnomalyStatsDTO> epcAnomalyStatsDTO;
    private FileAnomalyStatsDTO fileAnomalyStatsDTO;
}
