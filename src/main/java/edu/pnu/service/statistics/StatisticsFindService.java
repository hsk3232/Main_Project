package edu.pnu.service.statistics;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.KPIAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsFindService {
	
	private final KPIAnalysisRepository kpiRepo;
	
//	public List<KPIExportDTO> getKPIAnlaysis(Long fileId) {
//		List<KPIAnalysis> k = kpiRepo.findAll(fileId);
//		List<KPIExportDTO> dto = k.stream()
//				.map(KPIExportDTO::fromEntity)
//				.toList();
//		return dto;
//	}
}
