package edu.pnu.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pnu.dto.LoginDTO;
import edu.pnu.dto.MemberJoinDTO;
import edu.pnu.service.member.MemberJoinService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class MemberJoinController {

	private final MemberJoinService memberJoinsvc;
	
	@PostMapping("/join")
	public void postJoin(@RequestBody MemberJoinDTO dto) {
		try {
		System.out.println("[진입] : [MemberJoinController] 회원가입 진입");
		
		memberJoinsvc.postJoin(dto);
		System.out.println("[성공] : [MemberJoinController] 회원가입 성공");
		}
		catch(Exception e) {
			System.out.println("[실패] : [MemberJoinController] 회원가입 실패 " + e.getMessage());
			
		}
	}
	
	@PostMapping("/join/idsearch")
	public boolean postIdSearch(@RequestBody LoginDTO dto) {
		boolean result = memberJoinsvc.postIdSearch(dto.getUserId());
		if(result) {
			System.out.println("[경고/오류] : [JoinController][이미 사용 중인 아이디입니다.]" + "\n");
	        return true;
		}
			System.out.println("[성공] : [JoinController] 사용할 수 있는 아이디 검색^^ \n");
			return false;
	}

	
}
