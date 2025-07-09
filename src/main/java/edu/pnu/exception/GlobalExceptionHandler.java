package edu.pnu.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.FileNotFoundException;

import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	  // 로그 필드 선언 (클래스 맨 위)
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	
	// 커스텀 예외 처리
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        // 로그 남기기 (원하면)
        // log.error("예외 발생!", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("에러 발생: " + ex.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<String> handleFileNotFound(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("파일을 찾을 수 없습니다: " + ex.getMessage());
    }

    @ExceptionHandler(FileAccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(FileAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("파일 접근 권한 없음: " + ex.getMessage());
    }

    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<String> handleFileUpload(FileUploadException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("파일 업로드 실패: " + ex.getMessage());
    }

    @ExceptionHandler(InvalidCsvFormatException.class)
    public ResponseEntity<String> handleInvalidCsv(InvalidCsvFormatException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("CSV 형식 오류: " + ex.getMessage());
    }
    
    // 400(Bad Request): 사용자가 잘못 보낸 입력
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<String> handleBadRequest(BadRequestException ex) {
        log.error("BadRequestException", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // 403(Forbidden): 권한 없음
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(ForbiddenException ex) {
        log.error("ForbiddenException", ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
    
    // 404(Not Found): 리소스 없음
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleNotFound(NotFoundException ex) {
        log.error("NotFoundException", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 예외 누락 방지용 - 500 및 기타 예외 전체 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAll(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("서버 내부 오류: " + ex.getMessage());
    }
    
	
}
