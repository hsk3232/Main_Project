package edu.pnu.service.member;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.domain.Member;
import edu.pnu.dto.MemberJoinDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberJoinService {
	private final MemberRepository memberRepo;
	private final PasswordEncoder passwordEncoder;
	
	// 1. 회원가입
	public void postJoin(MemberJoinDTO dto) {
		// 1. 회원 가입 정보 저장 및 암호 해시화
		Member m = dto.toEntity();
		m.setPassword(passwordEncoder.encode(dto.getPassword()));
		
		memberRepo.save(m);
	}
	
	// 2. id 중복 여부 확인
	public boolean postIdSearch(String userId){
		boolean exist = memberRepo.existsByUserId(userId);
		System.out.println(exist);
		if(exist) {
			System.out.println("[오류] : [MemberJoinController] 동일 id 검색됨");
		}
		return exist;
	}
	
	
}
