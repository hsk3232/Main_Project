package edu.pnu.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.domain.Role;
import edu.pnu.service.member.LoginService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class LoginController {
	
	private final LoginService loginSvc;
	
	@GetMapping("/unauth")
	public ResponseEntity<?> getUnauthPage(Principal principal) {
		System.out.println("[진입] : [LoginController] 미승인 가입자 페이지 연결 진입");
		
		 Role r = loginSvc.getUnauthPage(principal.getName());

		    if (r == Role.ROLE_UNAUTH) {
		        System.out.println("[성공] : [LoginController] 미승인 가입자");
		        return ResponseEntity.ok().body("미승인 가입자입니다.");
		    } else {
		        System.out.println("[차단] : [LoginController] 승인된 사용자가 요청함");
		        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 접근입니다.");
		    }
	}
	
	
}
