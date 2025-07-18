package edu.pnu.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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

    /**
     * [비동기] 파일 ID로 분석 데이터 추출 + AI 서버에 전송
     */
    @Async
    public void sendDataAndSaveResultAsync(Long fileId) {
        System.out.println("\n[START][비동기] AI 데이터 자동 전송 트리거 (fileId=" + fileId + ")");

        System.out.println("[진행] : 분석 데이터 추출 시도...");
        List<ExportRowDTO> dtoList = exportByFileId(fileId);

        if (dtoList == null || dtoList.isEmpty()) {
            System.out.println("[경고] : ExportRowDTO 리스트가 비어있음! (fileId=" + fileId + ")");
            System.out.println("[END][비동기] 전송 중단\n");
            return;
        }

        System.out.println("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
        System.out.println("     > 첫 번째 데이터 샘플: " + dtoList.get(0));

        System.out.println("[진행] : AI 서버로 데이터 전송 단계로 이동");
        sendToAiAndSave(dtoList); // 실제 AI 전송 및 결과 반영

        System.out.println("[END][비동기] 전체 자동 전송 프로세스 완료\n");
    }

    /**
     * [비동기] 최근 파일 ID 자동 추출 후 AI 서버에 전송
     */
    @Async
    public void sendDataAndSaveResultAsync() {
        System.out.println("\n[START][비동기] 최근 파일 자동 탐색 → AI 전송 트리거");

        System.out.println("[진행] : 최신 CSV 파일 ID 조회...");
        Long lastFileId = csvRepo.findTopByOrderByFileIdDesc()
            .map(Csv::getFileId)
            .orElse(null);

        if (lastFileId == null) {
            System.out.println("[경고] : CSV 파일이 하나도 없음! [자동 전송 종료]");
            System.out.println("[END][비동기] 프로세스 중단\n");
            return;
        }

        System.out.println("[진행] : 최근 파일 ID = " + lastFileId + " / AI 자동 전송 시작");
        sendDataAndSaveResultAsync(lastFileId);

        System.out.println("[END][비동기] 최근 파일 자동 전송 프로세스 완료\n");
    }

    /**
     * [동기] 파일 ID로 분석 데이터 추출 + AI 서버에 전송 (타임아웃 적용을 원할 때 사용)
     */
    public void sendDataAndSaveResult(Long fileId) {
        System.out.println("\n[START][동기] AI 데이터 수동 전송 트리거 (fileId=" + fileId + ")");

        System.out.println("[진행] : 분석 데이터 추출 시도...");
        List<ExportRowDTO> dtoList = exportByFileId(fileId);

        if (dtoList == null || dtoList.isEmpty()) {
            System.out.println("[경고] : ExportRowDTO 리스트가 비어있음! (fileId=" + fileId + ")");
            System.out.println("[END][동기] 전송 중단\n");
            return;
        }

        System.out.println("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
        System.out.println("     > 첫 번째 데이터 샘플: " + dtoList.get(0));

        System.out.println("[진행] : AI 서버로 데이터 전송 단계로 이동");
        sendToAiAndSave(dtoList); // 실제 AI 전송 및 결과 반영

        System.out.println("[END][동기] 전체 수동 전송 프로세스 완료\n");
    }

    /**
     * 특정 파일 ID로 EventHistory 리스트를 DTO로 변환
     */
    public List<ExportRowDTO> exportByFileId(Long fileId) {
        System.out.println("[진행] : EventHistory 엔티티 → ExportRowDTO 변환 (fileId=" + fileId + ")");
        List<EventHistory> entityList = eventHisotyrRepo.findByFileLog_FileId(fileId);
        return entityList.stream().map(ExportRowDTO::fromEntity).toList();
    }

    /**
     * AI 서버에 데이터 전송 및 결과 수신 후 DB 반영
     */
    public void sendToAiAndSave(List<ExportRowDTO> dtoList) {
        int batchSize = 1000; // 1회 전송 데이터 수(상황에 따라 조정)
        int total = dtoList.size();
        int sent = 0;
        int successCount = 0;
        int failCount = 0;
        int consecutiveFailures = 0;  // 연속 실패 횟수 카운트
        List<Integer> failedBatches = new ArrayList<>();

        System.out.println("[배치전송] 전체 " + total + "건, 배치 사이즈 " + batchSize);

        // RestTemplate 및 HTTP 관련 객체 반복문 밖에서 한 번만 생성
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000); // 연결 타임아웃 5초
        factory.setReadTimeout(5000);    // 응답 대기 타임아웃 5초
        RestTemplate restTemplate = new RestTemplate(factory);
        String aiApiUrl = "http://10.125.121.177:8000/api/manager/barcode-anomaly-detect";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<ExportRowDTO> batch = dtoList.subList(i, end);

            System.out.println("[배치전송] " + (i + 1) + "~" + end + "건 AI 서버로 전송 시도...");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("data", batch);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            System.out.println("[진행] : [DataShareService] HTTP POST 요청 시작...");
            try {
                ResponseEntity<ImportDataDTO> response = restTemplate.postForEntity(
                    aiApiUrl, request, ImportDataDTO.class
                );
                System.out.println("[진행] : [DataShareService] AI 서버 응답 수신 완료 (status=" + response.getStatusCode() + ")");

                ImportDataDTO importData = response.getBody();
                if (importData != null) {
                    System.out.println("[진행] : [DataShareService] AI 결과를 DB에 반영 시작 (" + (i + 1) + "~" + end + "건)");
                    applyAnomalyResult(importData);
                    System.out.println("[성공] : [DataShareService] AI 결과 DB 반영 완료! (" + (i + 1) + "~" + end + "건)");
                    successCount += batch.size();
                } else {
                    System.out.println("[경고] : [DataShareService] AI 서버 응답은 왔으나, 결과 데이터가 null입니다. (" + (i + 1) + "~" + end + "건)");
                    failCount += batch.size();
                    failedBatches.add(i / batchSize + 1);
                }
                sent += batch.size();
            } catch (Exception e) {
                System.err.println("[오류] : [DataShareService] " + (i + 1) + "~" + end
                    + "건 AI 전송 및 수신 실패 (" + e.getClass().getSimpleName() + ") - " + e.getMessage());
                failCount += batch.size();
                failedBatches.add(i / batchSize + 1);
                consecutiveFailures++;
            }
            
         // 연속 실패 3번 이상 시 조기 종료
            if (consecutiveFailures >= 3) {
                System.err.println("[중단] : [DataShareService] 연속 3회 이상 실패 발생. 배치 전송 중단.");
                break;
            }


            // 필요시 배치 사이에 딜레이도 추가 가능 (실제 서비스 부하가 크면)
            // try { Thread.sleep(200); } catch (InterruptedException e) {}
        }
        System.out.println("[배치전송] 전체 전송 완료! 총 " + sent + "건 전송");
        System.out.println("[배치전송] 성공: " + successCount + "건 / 실패: " + failCount + "건");
        if (!failedBatches.isEmpty()) {
            System.out.println("[배치전송] 실패한 배치 인덱스: " + failedBatches);
        }
    }

    /**
     * AI 서버에서 받은 이상치 분석 결과(ImportDataDTO)를 DB에 반영
     */
    public void applyAnomalyResult(ImportDataDTO importData) {
        System.out.println("[진입] : [DataShareService] AI로부터 받은 DB 저장 진입");

        // EventHistory 업데이트
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
            System.out.println("[진행] : [DataShareService] EventHistory 이상치 반영 완료");
        }

        // EPC 이상치 통계 업데이트
        if (importData.getEpcAnomalyStatsDTO() != null) {
            List<EpcAnomalyStats> e = importData.getEpcAnomalyStatsDTO().stream()
                .map(dto -> {
                    Epc epc = epcRepo.findById(dto.getEpcCode())
                        .orElseThrow(() -> new RuntimeException("EPC코드 없음: " + dto.getEpcCode()));
                    return EpcAnomalyStatsDTO.toEntity(dto, epc);
                })
                .toList();
            epcAnomalyStatsRepo.saveAll(e);
            System.out.println("[진행] : [DataShareService] EPC 이상치 통계 반영 완료");
        }

        // 파일 전체 이상치 통계 업데이트
        if (importData.getFileAnomalyStatsDTO() != null) {
            FileAnomalyStatsDTO f = importData.getFileAnomalyStatsDTO();
            Csv csv = csvRepo.findById(importData.getFileId())
                .orElseThrow(() -> new RuntimeException("File id 없음"));
            FileAnomalyStats fas = FileAnomalyStatsDTO.toEntity(f, csv);
            fileAnomalyStatsRepo.save(fas);
            System.out.println("[진행] : [DataShareService] 파일 전체 이상치 통계 반영 완료");
        }
    }
}