package edu.pnu.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.dataShere.ExportRowDTO;
import edu.pnu.service.DataShareService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DataShareController {
	
	private final DataShareService dataShareService;
	
	//DA에게 Data 전달
	@GetMapping("/api/manager/export")
	public Map<String, Object> exportAll() {
		List<ExportRowDTO> dtoList = dataShareService.exportAll();

	        Map<String, Object> result = new HashMap<>();
	        result.put("data", dtoList);
	    
	    return result;
	}
	
//	//DA로부터 Data 저장
//	@PostMappting("api/v1/barcode-anomaly-detect")
//	public Map
}
