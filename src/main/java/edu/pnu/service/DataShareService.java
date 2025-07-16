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

import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.dataShere.ExportRowDTO;
import edu.pnu.dto.dataShere.ImportDataDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataShareService {
	
	private final EventHistoryRepository eventHisotyrRepo;
	//private final AnomalyResultRepository anomalyResultRepository;
	
	public List<ExportRowDTO> exportAll(){
		List<EventHistory> entityList = eventHisotyrRepo.findAll();
		return entityList.stream()
			.map(ExportRowDTO::fromEntity)
			.toList();
	}

	// 비동기 분석 트리거 메서드
	@Async
	public void sendDataAndSaveResultAsync() {
		List<ExportRowDTO> dtoList = exportAll();

		RestTemplate restTemplate = new RestTemplate();
		String aiApiUrl = "http://ai-server:8000/analyze";

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
				// 결과 DB 저장
				eventHisotyrRepo.saveAll(importData.getEpcAnomalyStats());
				// 추가 저장 로직
			}
		} catch (Exception e) {
			// 실패 시 로그 등
			System.err.println("AI 서버 전송 또는 저장 실패: " + e.getMessage());
		}
	}
}
