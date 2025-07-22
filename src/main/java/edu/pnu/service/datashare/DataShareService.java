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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import edu.pnu.Repo.AiDataRepository;
import edu.pnu.Repo.CsvRepository;
import edu.pnu.Repo.EpcAnomalyStatsRepository;
import edu.pnu.Repo.EpcRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.FileAnomalyStatsRepository;
import edu.pnu.config.DataShareProperties;
import edu.pnu.domain.AiData;
import edu.pnu.domain.Csv;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EpcAnomalyStats;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.FileAnomalyStats;
import edu.pnu.dto.dataShere.AiDataDTO;
import edu.pnu.dto.dataShere.EpcAnomalyStatsDTO;
import edu.pnu.dto.dataShere.ExportRowDTO;
import edu.pnu.dto.dataShere.FileAnomalyStatsDTO;
import edu.pnu.dto.dataShere.ImportDataDTO;
import edu.pnu.exception.NoDataFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataShareService {

	private final EventHistoryRepository eventHistoryRepo;
	private final AiDataRepository aiDataRepo;
	private final EpcRepository epcRepo;
	private final EpcAnomalyStatsRepository epcAnomalyStatsRepo;
	private final CsvRepository csvRepo;
	private final FileAnomalyStatsRepository fileAnomalyStatsRepo;
	private final DataShareProperties props; // 주입된 커스텀 설정 클래스


//	 ■■■■■■■■■■■■■ 외부 트리거 진입점 ■■■■■■■■■■■■■■

	public void autoSendLatestFile() {
		log.info("[진입] : [DataShareService] 최신 CSV 파일로 AI 전송 시작");
		Long lastFileId = csvRepo.findTopByOrderByFileIdDesc().map(Csv::getFileId).orElse(null);

		if (lastFileId == null) {
			log.error("[경고] : [DataShareService] CSV 파일이 하나도 없음! [자동 전송 종료]");
			log.error("[경고] : [DataShareService] 프로세스 중단\n");
			return;
		}

		log.info("[진행] :[DataShareService] 최근 파일 ID = " + lastFileId + " / AI 자동 전송 시작");
		sendDataAndSaveResult(lastFileId);
		log.info("[END][비동기] : [DataShareService] 최근 파일 자동 전송 프로세스 완료\n");
	}

	
//	 ■■■■■■■■■■■■■ [동기] 파일 ID로 분석 데이터 추출 + AI 서버에 전송 ■■■■■■■■■■■■■■
	public void sendDataAndSaveResult(Long fileId) {
		log.info("\n[START][동기] : [DataShareService] AI 데이터 수동 전송 트리거 (fileId=" + fileId + ")");

		log.info("[진행] : [DataShareService] 분석 데이터 추출 시도...");
		List<ExportRowDTO> dtoList = exportByFileId(fileId);

		if (dtoList.isEmpty()) {
			log.error("[경고] : [DataShareService] ExportRowDTO 리스트가 비어있음! (fileId=" + fileId + ")");
			log.error("[END][동기]: [DataShareService] 전송 중단\n");
			return;
		}

		log.info("[진행] : 분석 데이터 추출 완료 (" + dtoList.size() + "건)");
		log.info("[진행] : AI 서버로 데이터 전송 단계로 이동");
		sendToAiAndSave(dtoList);
		log.info("[END][동기] 전체 수동 전송 프로세스 완료\n");
	}

	
//	 ■■■■■■■■■■■■■  특정 파일 ID로 EventHistory 리스트를 DTO로 변환 ■■■■■■■■■■■■■■
	public List<ExportRowDTO> exportByFileId(Long fileId) {
		log.info("[진행] : EventHistory 엔티티 → ExportRowDTO 변환 (fileId=" + fileId + ")");

		// [1] fileId null 체크를 쿼리 전에!
		if (fileId == null) {
			throw new NoDataFoundException("fileId가 null입니다. 업로드한 파일 ID를 확인하세요!");
		}

		// [2] 쿼리 실행
		List<EventHistory> entityList = eventHistoryRepo.findByCsv_FileId(fileId);
		return entityList.stream().map(ExportRowDTO::fromEntity).toList();
	}

	
//	 ■■■■■■■■■■  AI 서버에 데이터 전송 및 결과 수신 후 DB 반영  ■■■■■■■■■■
	@Transactional
	public void sendToAiAndSave(List<ExportRowDTO> dtoList) {
		// [1-1] 설정값 읽기
		int batchSize = props.getBatchSize();
		int maxRetries = props.getRetryMaxAttempts();
		long retryDelayMillis = props.getRetryDelayMs();
		long batchDelayMillis = props.getBatchDelayMs();
		int connectTimeout = props.getRestConnectTimeout();
		int readTimeout = props.getRestReadTimeout();
		String aiApiUrl = props.getAiApiUrl();

		// [1-2] RestTemplate 설정 (timeout 적용)
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(connectTimeout);
		factory.setReadTimeout(readTimeout);
		RestTemplate restTemplate = new RestTemplate(factory);

		// [1-3] HTTP 헤더 구성
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// [1-4] 초기 통계 변수 선언
		int total = dtoList.size();
		int sent = 0;
		int successCount = 0;
		int failCount = 0;
		int consecutiveFailures = 0;
		List<Integer> failedBatches = new ArrayList<>();

		log.info("[시작] AI 배치 전송 - 전체: {}건, 배치 사이즈: {}", total, batchSize);
		// [2] 배치 단위로 분할 전송
		for (int i = 0; i < total; i += batchSize) {
			int end = Math.min(i + batchSize, total);
			List<ExportRowDTO> batch = dtoList.subList(i, end);
			int batchIndex = (i / batchSize) + 1;

			log.info("[배치전송][{}] {}~{}번 전송 시도", batchIndex, i + 1, end);

			// [2-1] 요청 바디 구성
			Map<String, Object> requestBody = new HashMap<>();
			requestBody.put("data", batch);
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			// [2-2] 전송 및 재시도
			ImportDataDTO importData = sendBatchWithRetry(restTemplate, aiApiUrl, request, maxRetries, retryDelayMillis,
					batchIndex);

			if (importData != null) {
				try {
					applyAnomalyResult(importData);
					successCount += batch.size();
					sent += batch.size();
					log.info("[성공][{}] 분석 결과 저장 완료 ({}건)", batchIndex, batch.size());
					consecutiveFailures = 0;
				} catch (Exception e) {
					log.error("[오류][{}] DB 저장 중 예외 발생 - {}", batchIndex, e.getMessage(), e);
					failCount += batch.size();
					failedBatches.add(batchIndex);
					consecutiveFailures++;
				}
			} else {
				log.error("[실패][{}] 최종 전송 실패 - 해당 배치는 누락 처리", batchIndex);
				failCount += batch.size();
				failedBatches.add(batchIndex);
				consecutiveFailures++;
			}

			// [2-3] 연속 실패 3회 이상 시 중단
			if (consecutiveFailures >= 3) {
				log.error("[중단] 연속 3회 실패 발생 - 배치 전송 중단");
				break;
			}

			// [2-4] 배치 간 대기
			try {
				Thread.sleep(batchDelayMillis);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.error("[오류] 배치 간 딜레이 중 인터럽트 발생");
				break;
			}
		}

		// [3] 최종 요약 로그
		log.info("[완료] AI 배치 전송 요약 - 총 전송: {}건", sent);
		log.info("[완료] 성공: {}건 / 실패: {}건", successCount, failCount);
		if (!failedBatches.isEmpty()) {
			log.warn("[실패 배치 인덱스]: {}", failedBatches);
		}
	}

	// [재시도 처리 로직]
	private ImportDataDTO sendBatchWithRetry(RestTemplate restTemplate, String url, HttpEntity<?> request,
			int maxRetries, long retryDelayMillis, int batchIndex) {
		for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
			try {
				ResponseEntity<ImportDataDTO> response = restTemplate.postForEntity(url, request, ImportDataDTO.class);

				if (response.getStatusCode().is2xxSuccessful()) {
					log.info("[응답][{}][시도:{}] AI 서버 응답 수신 완료 (status: {})", batchIndex, attempt,
							response.getStatusCode());
					return response.getBody();
				} else {
					log.warn("[응답][{}][시도:{}] 비정상 응답 코드 수신 - status: {}", batchIndex, attempt,
							response.getStatusCode());
				}
			} catch (Exception e) {
				log.error("[전송실패][{}][시도:{}] 예외 발생 - {}", batchIndex, attempt, e.getMessage());
			}

			// 재시도 간 딜레이
			try {
				Thread.sleep(retryDelayMillis);
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				log.error("[오류][{}] 재시도 대기 중 인터럽트 발생", batchIndex);
				break;
			}
		}

		return null;
	}


	// ■■■■■■■■■■■■■■■■ 결과 반영 (DB 업데이트) ■■■■■■■■■■■■■■■■

	public void applyAnomalyResult(ImportDataDTO importData) {
		log.info("[진입] : [DataShareService] AI로부터 받은 이상치 결과 DB 반영 시작");

		if (importData.getEventHistoryImportDTO() != null) {
			List<AiDataDTO> dtoList = importData.getEventHistoryImportDTO();

			log.info("[진행] : [DataShareService] AiData 업데이트 대상 건수 = {}", dtoList.size());

			// [1-1] eventId 목록 추출
			List<Long> eventIds = dtoList.stream().map(AiDataDTO::getEventId).toList();

			// [1-2] eventId 기준으로 AiData 조회 (EventHistory.eventId 기준!)
			List<AiData> aiDataList = aiDataRepo.findByEventHistory_EventIdIn(eventIds);

			// [1-3] eventId → AiData 매핑
			Map<Long, AiData> aiDataMap = new HashMap<>();
			for (AiData a : aiDataList) {
				aiDataMap.put(a.getEventHistory().getEventId(), a);
			}

			// [1-4] DTO 값을 엔티티에 반영 (null일 경우 기본값 사용)
			for (AiDataDTO dto : dtoList) {
				AiData entity = aiDataMap.get(dto.getEventId());
				if (entity == null) {
					log.warn("[경고] : eventId={}에 해당하는 AiData 엔티티 없음 → 건너뜀", dto.getEventId());
					continue;
				}
				
				entity.setAnomaly(true); // 분석된 이상치는 무조건 true 처리

				entity.setJump(Boolean.TRUE.equals(dto.isJump()));
				entity.setJumpScore(dto.getJumpScore());

				entity.setEvtOrderErr(Boolean.TRUE.equals(dto.isEvtOrderErr()));
				entity.setEvtOrderErrScore(dto.getEvtOrderErrScore());

				entity.setEpcFake(Boolean.TRUE.equals(dto.isEpcFake()));
				entity.setEpcFakeScore(dto.getEpcFakeScore());

				entity.setEpcDup(Boolean.TRUE.equals(dto.isEpcDup()));
				entity.setEpcDupScore(dto.getEpcDupScore());

				entity.setLocErr(Boolean.TRUE.equals(dto.isLocErr()));
				entity.setLocErrScore(dto.getLocErrScore());
			}

			// [1-5] 일괄 저장
			aiDataRepo.saveAll(aiDataList);
			log.info("[완료] : [DataShareService] AiData 이상치 반영 완료 ({}건)", aiDataList.size());
		}

		// [2] EPC 이상치 통계 반영
		if (importData.getEpcAnomalyStatsDTO() != null) {
			List<EpcAnomalyStats> stats = new ArrayList<>();

			for (EpcAnomalyStatsDTO dto : importData.getEpcAnomalyStatsDTO()) {
				try {
					Epc epc = epcRepo.findById(dto.getEpcCode())
							.orElseThrow(() -> new RuntimeException("EPC코드 없음: " + dto.getEpcCode()));
					stats.add(EpcAnomalyStatsDTO.toEntity(dto, epc));
				} catch (Exception e) {
					log.error("[오류] : EPC 이상치 반영 실패 - epcCode={}, 원인: {}", dto.getEpcCode(), e.getMessage());
				}
			}

			epcAnomalyStatsRepo.saveAll(stats);
			log.info("[완료] : [DataShareService] EPC 이상치 통계 반영 완료 ({}건)", stats.size());
		}

		// [3] 파일 전체 이상치 통계 반영
		if (importData.getFileAnomalyStatsDTO() != null) {
			try {
				FileAnomalyStatsDTO f = importData.getFileAnomalyStatsDTO();
				Csv csv = csvRepo.findById(importData.getFileId())
						.orElseThrow(() -> new RuntimeException("File id 없음: " + importData.getFileId()));

				FileAnomalyStats fas = FileAnomalyStatsDTO.toEntity(f, csv);
				fileAnomalyStatsRepo.save(fas);

				log.info("[완료] : [DataShareService] 파일 전체 이상치 통계 반영 완료 (fileId={})", f.getFileId());
			} catch (Exception e) {
				log.error("[오류] : 파일 통계 반영 중 실패 - 원인: {}", e.getMessage(), e);
			}
		}

		log.info("[END] : [DataShareService] 전체 이상치 결과 반영 완료\n");
	}

}