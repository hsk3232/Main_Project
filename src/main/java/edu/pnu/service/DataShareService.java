package edu.pnu.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.domain.EventHistory;
import edu.pnu.dto.dataShere.ExportRowDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataShareService {
	
	private final EventHistoryRepository eventHisotyrRepo;
	
	 public List<ExportRowDTO> exportAll(){
		 List<EventHistory> entityList = eventHisotyrRepo.findAll();
	        return entityList.stream()
	            .map(ExportRowDTO::fromEntity)
	            .toList();
	    }
}
