package edu.pnu.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.Repo.EPCRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.LocationRepository;
import edu.pnu.Repo.ProductRepository;
import edu.pnu.dto.ExportRowDTO;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class DataShareController {
	
	private final EventHistoryRepository eventRepo;
    private final ProductRepository productRepo;
    private final EPCRepository epcRepo;
    private final LocationRepository locationRepo;
	
	@GetMapping("/예은님 주소")
	public Map<String, Object> exportAll() {
	    
	    
	    return result;
	}
}
