package edu.pnu.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.service.statistics.StatisticsFindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class StatisticsController {
	
	private final StatisticsFindService statisticsFindService;

//	@GetMapping("/kpi")
//	public List<KPIExportDTO> getKPIAnlaysis(@RequestParam Long fileId) {
//		List<KPIExportDTO> dto = statisticsFindService.getKPIAnlaysis(fileId);
//		return dto;
//	}
	
}
