package edu.pnu.controller.csv;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import edu.pnu.service.csv.CsvLogService;
import edu.pnu.service.csv.CsvSaveService;
import edu.pnu.service.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager")
public class CsvController {
	
	private final CsvLogService csvLogService;
	private final CsvSaveService csvSaveService;
	
	// Front -> Back csv 전달 및 저장
	@PostMapping("/upload")
	public ResponseEntity<?> postCsv(@RequestParam("file") MultipartFile file, 
			@AuthenticationPrincipal CustomUserDetails user) {
		try {
			csvSaveService.postCsv(file, user);
			return ResponseEntity.ok("업로드 및 저장 성공");
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("error: " + e.getMessage());
		}
	}
	
	 @GetMapping("/api/manager/download/{fileLogId}")
	    public ResponseEntity<Resource> downloadCsv(
	            @PathVariable Long fileLogId,
	            @AuthenticationPrincipal CustomUserDetails user
	    ) {
	        // 필요 시: user와 파일의 접근 권한 체크도 서비스에서 처리할 수 있음

	        Resource resource = csvLogService.loadCsvResource(fileLogId);
	        String filename = csvLogService.getFileName(fileLogId);

	        return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
	            .body(resource);
	    }
	
}
