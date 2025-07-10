package edu.pnu.service.csv;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.domain.Csv;
import edu.pnu.dto.CsvFileListResponseDTO;
import edu.pnu.exception.FileNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvLogService {
	
	private final CsvRepository csvRepo;

	
    public Resource loadCsvResource(Long fileLogId) {
        Csv log = csvRepo.findById(fileLogId)
            .orElseThrow(() -> new RuntimeException("파일 기록 없음"));

        Path path = Paths.get(log.getFilePath());
        try {
            return new UrlResource(path.toUri());
        } catch (Exception e) {
            throw new RuntimeException("파일 리소스 접근 실패", e);
        }
    }
    
    // 업로드 fileList 조회
    public List<CsvFileListResponseDTO> getFileList(Integer page, String search) {
    	List<Csv> csvList;

        if (search != null && !search.isBlank()) {
            csvList = csvRepo.findByFileNameContaining(search);
        } else {
            csvList = csvRepo.findAll();
        }
        
        if (csvList.isEmpty()) {
            throw new FileNotFoundException("조회된 파일이 없습니다.");
        }
        
        List<CsvFileListResponseDTO> list = csvList.stream()
        		.map(CsvFileListResponseDTO::fromEntity)
        		.toList();
        
        return list;
    }
    
    public Resource getFileName(String fileLogId) {
    	return fileLogId;
    }
}
