package edu.pnu.service.member;

import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.domain.Member;
import lombok.RequiredArgsConstructor;

// --------------인증(로그인, 토큰 발급)에만 사용----------//

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService  implements UserDetailsService {
	private final MemberRepository memberRepo;

	@Override
	public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
		Member member = memberRepo.findByUserId(userId)
				.orElseThrow(() -> new UsernameNotFoundException("[오류] : [SecurityUserDetailsService][사용자 못찾겠다..]" + "\n"));

		return User.builder()
				.username(member.getUserId())
				.password(member.getPassword())
				.authorities(AuthorityUtils.createAuthorityList(member.getRole().toString()))
				.build();
				
	}
}
