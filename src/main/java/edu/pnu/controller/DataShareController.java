package edu.pnu.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.service.DataShareService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DataShareController {
    private final DataShareService dataShareService;

    // 비동기 분석 트리거
    @PostMapping("/api/v1/export-and-analyze-async")
    public String exportAndAnalyzeAsync() {
        dataShareService.sendDataAndSaveResultAsync();  // 바로 리턴!
        return "비동기로 분석 요청을 보냈습니다!";
    }
	

}
