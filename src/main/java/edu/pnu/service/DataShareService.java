package edu.pnu.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.Repo.EpcAnomalyStatsRepository;
import edu.pnu.Repo.EpcRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.FileAnomalyStatsRepository;
import edu.pnu.domain.Csv;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EpcAnomalyStats;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.FileAnomalyStats;
import edu.pnu.dto.dataShere.EpcAnomalyStatsDTO;
import edu.pnu.dto.dataShere.EventHistoryImportDTO;
import edu.pnu.dto.dataShere.ExportRowDTO;
import edu.pnu.dto.dataShere.FileAnomalyStatsDTO;
import edu.pnu.dto.dataShere.ImportDataDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataShareService {

    private final EventHistoryRepository eventHisotyrRepo;
    private final EpcRepository epcRepo;
    private final EpcAnomalyStatsRepository epcAnomalyStatsRepo;
    private final CsvRepository csvRepo;
    private final FileAnomalyStatsRepository fileAnomalyStatsRepo;

    // 1. 파일ID로 분석 데이터 추출 + 전송 (공통 로직)
    @Async
    public void sendDataAndSaveResultAsync(Long fileId) {
        System.out.println("[진입] : [DataShareService] AI로부터 받은 DB 저장 진입 (fileId=" + fileId + ")");

        List<ExportRowDTO> dtoList = exportByFileId(fileId);

        if (dtoList == null || dtoList.isEmpty()) {
            System.out.println("ExportRowDTO 리스트가 비어있습니다! fileId=" + fileId);
            return;
        }
        // 첫 번째 데이터 확인용
        System.out.println("==== ExportRowDTO 첫 번째 데이터 ====");
        System.out.println(dtoList.get(0));

        sendToAiAndSave(dtoList); // 실제 AI 전송 로직 분리
    }

    // 2. (처음 자동 호출: fileId 없을 때) 최근 파일만 전송
    @Async
    public void sendDataAndSaveResultAsync() {
        Long lastFileId = csvRepo.findTopByOrderByFileIdDesc()
            .map(Csv::getFileId)
            .orElse(null);

        if (lastFileId == null) {
            System.out.println("CSV 파일이 하나도 없습니다!");
            return;
        }
        sendDataAndSaveResultAsync(lastFileId); // 핵심 로직 재활용
    }

    // 실제 export (fileId별)
    public List<ExportRowDTO> exportByFileId(Long fileId) {
        List<EventHistory> entityList = eventHisotyrRepo.findByFileLog_FileId(fileId);
        return entityList.stream().map(ExportRowDTO::fromEntity).toList();
    }

    // 실제 AI 연동 (공통 함수)
    public void sendToAiAndSave(List<ExportRowDTO> dtoList) {
        RestTemplate restTemplate = new RestTemplate();
        String aiApiUrl = "http://10.125.121.177:8000/api/manager/barcode-anomaly-detect";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("data", dtoList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<ImportDataDTO> response = restTemplate.postForEntity(
                aiApiUrl, request, ImportDataDTO.class
            );
            ImportDataDTO importData = response.getBody();
            if (importData != null) {
                applyAnomalyResult(importData);
            }
        } catch (Exception e) {
            System.err.println("[오류] : [DataShareService] DB 전송 및 수신 실패 " + e.getMessage());
        }
    }
    

    // 3. 분석 결과(ImportDataDTO) 받아서 DB에 반영
    public void applyAnomalyResult(ImportDataDTO importData) {
    	System.out.println("[진입] : [DataShareService] AI로부터 받은 DB 저장 진입");
        if (importData.getEventHistoryImportDTO() != null) {
            for (EventHistoryImportDTO a : importData.getEventHistoryImportDTO()) {
                eventHisotyrRepo.findById(a.getEventId())
                        .ifPresent(entity -> {
                            entity.setAnomaly(true);
                            entity.setJump(a.isJump());
                            entity.setJumpScore(a.getJumpScore());
                            entity.setEvtOrderErr(a.isEvtOrderErr());
                            entity.setEvtOrderErrScore(a.getEvtOrderErrScore());
                            entity.setEpcFake(a.isEpcFake());
                            entity.setEpcFakeScore(a.getEpcFakeScore());
                            entity.setEpcDup(a.isEpcDup());
                            entity.setEpcDupScore(a.getEpcDupScore());
                            entity.setLocErr(a.isLocErr());
                            entity.setLocErrScore(a.getLocErrScore());
                            eventHisotyrRepo.save(entity);  // 업데이트!
                        });
            }
        }
        
        // epcAnomalyStats
        if(importData.getEpcAnomalyStatsDTO() !=null) {
        	List<EpcAnomalyStats> e = importData.getEpcAnomalyStatsDTO().stream()
        			.map(dto -> {
        				Epc epc = epcRepo.findById(dto.getEpcCode())
        						.orElseThrow(() -> new RuntimeException("EPC코드 없음: " + dto.getEpcCode()));
        				return EpcAnomalyStatsDTO.toEntity(dto, epc);
        			})
        			.toList();
        	
        	epcAnomalyStatsRepo.saveAll(e);
        }
        
        
        if(importData.getFileAnomalyStatsDTO() !=null) {
        	FileAnomalyStatsDTO f = importData.getFileAnomalyStatsDTO();
        	Csv csv = csvRepo.findById(importData.getFileId())
        			.orElseThrow(() -> new RuntimeException("File id 없음"));
        	FileAnomalyStats fas = FileAnomalyStatsDTO.toEntity(f, csv);
        	
        	fileAnomalyStatsRepo.save(fas);
        }
    }
}
