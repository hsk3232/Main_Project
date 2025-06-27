package edu.pnu.service.member;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.domain.Role;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
	private final MemberRepository memberRepo;

	public Role getUnauthPage(String userId) {
		Role role = memberRepo.findByRole(userId);
		return role;
	}
}
