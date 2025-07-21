package edu.pnu.service.datashare;

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
import edu.pnu.config.DataShareProperties;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataShareService {

	private final EventHistoryRepository eventHistoryRepo;
	private final EpcRepository epcRepo;
	private final EpcAnomalyStatsRepository epcAnomalyStatsRepo;
	private final CsvRepository csvRepo;
	private final FileAnomalyStatsRepository fileAnomalyStatsRepo;
	private final DataShareProperties props; // 주입된 커스텀 설정 클래스
	
	
	// ■■■■■■■■■■■■■■■■■■■■■
	// ■■■ 외부 트리거 진입점 ■■■
	// ■■■■■■■■■■■■■■■■■■■■■
	
	// [비동기] AI 서버에 전송
	@Async
	public void sendDataAndSaveResultAsync(Long fileId) {
		log.info("\n[START][비동기] AI 데이터 자동 전송 트리거 (fileId=" + fileId + ")");

		log.info("[진행] : 분석 데이터 추출 시도...");
		List<ExportRowDTO> dtoList = exportByFileId(fileId);

		if (dtoList == null || dtoList.isEmpty()) {
			log.info("[경고] : ExportRowDTO 리스트가 비어있음! (fileId=" + fileId + ")");
			log.info("[END][비동기] 전송 중단\n");
			return;
		}

		log.info("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
		log.info("     > 첫 번째 데이터 샘플: " + dtoList.get(0));

		log.info("[진행] : AI 서버로 데이터 전송 단계로 이동");
		sendToAiAndSave(dtoList); // 실제 AI 전송 및 결과 반영

		log.info("[END][비동기] 전체 자동 전송 프로세스 완료\n");
	}


	 // [비동기] 최근 파일 ID를 자동으로 찾아서 sendDataAndSaveResultAsync(fileId) 호출
	@Async
	public void autoSendLatestFile() {
		log.info("[START][비동기] 최신 CSV 파일로 AI 전송 시작");
		Long lastFileId = csvRepo.findTopByOrderByFileIdDesc()
				.map(Csv::getFileId)
				.orElse(null);

		if (lastFileId == null) {
			log.info("[경고] : CSV 파일이 하나도 없음! [자동 전송 종료]");
			log.info("[END][비동기] 프로세스 중단\n");
			return;
		}

		log.info("[진행] : 최근 파일 ID = " + lastFileId + " / AI 자동 전송 시작");
		sendDataAndSaveResultAsync(lastFileId);

		log.info("[END][비동기] 최근 파일 자동 전송 프로세스 완료\n");
	}

	
	// [동기] 파일 ID로 분석 데이터 추출 + AI 서버에 전송 (타임아웃 적용을 원할 때 사용)	
	public void sendDataAndSaveResult(Long fileId) {
		log.info("\n[START][동기] AI 데이터 수동 전송 트리거 (fileId=" + fileId + ")");

		log.info("[진행] : 분석 데이터 추출 시도...");
		List<ExportRowDTO> dtoList = exportByFileId(fileId);

		if (dtoList == null || dtoList.isEmpty()) {
			log.info("[경고] : ExportRowDTO 리스트가 비어있음! (fileId=" + fileId + ")");
			log.info("[END][동기] 전송 중단\n");
			return;
		}

		log.info("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
		log.info("     > 첫 번째 데이터 샘플: " + dtoList.get(0));

		log.info("[진행] : AI 서버로 데이터 전송 단계로 이동");
		sendToAiAndSave(dtoList); // 실제 AI 전송 및 결과 반영

		log.info("[END][동기] 전체 수동 전송 프로세스 완료\n");
	}


//	 특정 파일 ID로 EventHistory 리스트를 DTO로 변환

	public List<ExportRowDTO> exportByFileId(Long fileId) {
		log.info("[진행] : EventHistory 엔티티 → ExportRowDTO 변환 (fileId=" + fileId + ")");
		
		List<EventHistory> entityList = eventHistoryRepo.findByFileLog_FileId(fileId);
		return entityList.stream().map(ExportRowDTO::fromEntity).toList();
	}



	//	■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	//	■■ AI 서버에 데이터 전송 및 결과 수신 후 DB 반영 ■■
	//	■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
	

    public void sendToAiAndSave(List<ExportRowDTO> dtoList) {
        int batchSize = props.getBatchSize();
        int maxRetries = props.getRetryMaxAttempts();
        long retryDelayMillis = props.getRetryDelayMs();
        long batchDelayMillis = props.getBatchDelayMs();
        int connectTimeout = props.getRestConnectTimeout();
        int readTimeout = props.getRestReadTimeout();
        String aiApiUrl = props.getAiApiUrl();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        RestTemplate restTemplate = new RestTemplate(factory);

        // HttpHeaders 선언 누락되어 있으므로 추가
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("[배치전송] 전체 " + dtoList.size() + "건, 배치 사이즈 " + batchSize);

        int total = dtoList.size();
        int sent = 0;
        int successCount = 0;
        int failCount = 0;
        int consecutiveFailures = 0;
        List<Integer> failedBatches = new ArrayList<>();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            List<ExportRowDTO> batch = dtoList.subList(i, end);

            log.info("[배치전송] " + (i + 1) + "~" + end + "건 AI 서버로 전송 시도...");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("data", batch);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            boolean success = false;

            for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
                try {
                    log.info("[진행] : [DataShareService] HTTP POST 요청 시작... (시도 " + attempt + ")");
                    ResponseEntity<ImportDataDTO> response = restTemplate.postForEntity(aiApiUrl, request,
                            ImportDataDTO.class);
                    log.info("[진행] : [DataShareService] AI 서버 응답 수신 완료 (status=" + response.getStatusCode() + ")");

                    ImportDataDTO importData = response.getBody();
                    if (importData != null) {
                        log.info("[진행] : [DataShareService] AI 결과를 DB에 반영 시작 (" + (i + 1) + "~" + end + "건)");
                        applyAnomalyResult(importData);
                        log.info("[성공] : [DataShareService] AI 결과 DB 반영 완료! (" + (i + 1) + "~" + end + "건)");
                        successCount += batch.size();
                    } else {
                        log.warn("[경고] : [DataShareService] AI 서버 응답은 왔으나, 결과 데이터가 null입니다. (" + (i + 1) + "~" + end
                                + "건)");
                        failCount += batch.size();
                        failedBatches.add(i / batchSize + 1);
                    }
                    sent += batch.size();
                    success = true;
                    break; // 성공 시 재시도 종료
                } catch (Exception e) {
                    log.error("[오류] : [DataShareService] " + (i + 1) + "~" + end + "건 AI 전송 및 수신 실패 ("
                            + e.getClass().getSimpleName() + ") - " + e.getMessage());

                    if (attempt == maxRetries + 1) {
                        // 최종 실패 처리
                        failCount += batch.size();
                        failedBatches.add(i / batchSize + 1);
                        consecutiveFailures++;
                    } else {
                        try {
                            Thread.sleep(retryDelayMillis);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("[오류] : 재시도 대기 중 인터럽트 발생");
                            break;
                        }
                    }
                }
            }

            if (!success) {
                if (consecutiveFailures >= 3) {
                    log.error("[중단] : [DataShareService] 연속 3회 이상 실패 발생. 배치 전송 중단.");
                    break;
                }
            } else {
                consecutiveFailures = 0; // 성공 시 연속 실패 초기화
            }

            try {
                Thread.sleep(batchDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[오류] : 배치 전송 딜레이 중 인터럽트 발생");
                break;
            }
        }

        log.info("[배치전송] 전체 전송 완료! 총 " + sent + "건 전송");
        log.info("[배치전송] 성공: " + successCount + "건 / 실패: " + failCount + "건");
        if (!failedBatches.isEmpty()) {
            log.info("[배치전송] 실패한 배치 인덱스: " + failedBatches);
        }
    }

	// ■■■■■■■■■■■■■■■■■■■■■■■■■■
	// ■■■ 결과 반영 (DB 업데이트) ■■■
	// ■■■■■■■■■■■■■■■■■■■■■■■■■■
	public void applyAnomalyResult(ImportDataDTO importData) {
		log.info("[진입] : [DataShareService] AI로부터 받은 DB 저장 진입");

		// EventHistory 업데이트
		if (importData.getEventHistoryImportDTO() != null) {
			for (EventHistoryImportDTO a : importData.getEventHistoryImportDTO()) {
				eventHistoryRepo.findById(a.getEventId()).ifPresent(entity -> {
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
					eventHistoryRepo.save(entity); // 업데이트!
				});
			}
			log.info("[진행] : [DataShareService] EventHistory 이상치 반영 완료");
		}

		// EPC 이상치 통계 업데이트
		if (importData.getEpcAnomalyStatsDTO() != null) {
			List<EpcAnomalyStats> e = importData.getEpcAnomalyStatsDTO().stream().map(dto -> {
				Epc epc = epcRepo.findById(dto.getEpcCode())
						.orElseThrow(() -> new RuntimeException("EPC코드 없음: " + dto.getEpcCode()));
				return EpcAnomalyStatsDTO.toEntity(dto, epc);
			}).toList();
			epcAnomalyStatsRepo.saveAll(e);
			log.info("[진행] : [DataShareService] EPC 이상치 통계 반영 완료");
		}

		// 파일 전체 이상치 통계 업데이트
		if (importData.getFileAnomalyStatsDTO() != null) {
			FileAnomalyStatsDTO f = importData.getFileAnomalyStatsDTO();
			Csv csv = csvRepo.findById(importData.getFileId()).orElseThrow(() -> new RuntimeException("File id 없음"));
			FileAnomalyStats fas = FileAnomalyStatsDTO.toEntity(f, csv);
			fileAnomalyStatsRepo.save(fas);
			log.info("[진행] : [DataShareService] 파일 전체 이상치 통계 반영 완료");
		}
	}
}