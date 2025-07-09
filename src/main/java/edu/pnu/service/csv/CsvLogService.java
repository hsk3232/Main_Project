package edu.pnu.service.csv;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.domain.Csv;
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
    
    // 파일명 
    public String getFileName(Long fileLogId) {
        Csv log = csvRepo.findById(fileLogId)
            .orElseThrow(() -> new RuntimeException("파일 기록 없음"));
        return log.getFileName();
    }
}
