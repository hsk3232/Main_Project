package edu.pnu.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import edu.pnu.dto.ExportRowDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DataShareService {
	 public Map<String, Object> exportAll(){
		 
		 List<ExportRowDTO> rows = ...; // DB에서 데이터 조회 및 변환
	 }
}
