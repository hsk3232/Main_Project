package edu.pnu.service.member;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CustomUserDetails implements UserDetails { // Spring Security의 인증 객체로 사용
	// 객체 직렬화/역직렬화(저장, 네트워크 전송)할 때 클래스 버전이 바뀌어도 같은 객체로 인식할지 체크
	private static final long serialVersionUID = 1L; 
	
	// ========== 실제 데이터 필드들 ==========
	private String userId; // 로그인 아이디
	private String password; // 비밀번호
	private Collection<? extends GrantedAuthority> authorities; // 권한
	private Long locationId; // 공장코드

	// ======= UserDetails 인터페이스 구현 메소드 (Spring Security 필수!) =======
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities; // 사용자의 권한 목록 반환
	}

	@Override
	public String getPassword() {
		return password; // 비밀번호 반환
	}

	@Override
	public String getUsername() {
		return userId; // 로그인 아이디 반환
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // 계정 만료 여부 (true: 만료 안 됨)
	}

	@Override
	public boolean isAccountNonLocked() {
		return true; // 계정 잠김 여부 (true: 잠기지 않음)
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true; // 비밀번호 만료 여부 (true: 만료 안 됨)
	}

	@Override
	public boolean isEnabled() {
		return true; // 계정 활성화 여부 (true: 활성화)
	}

}
