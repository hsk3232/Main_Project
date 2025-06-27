package edu.pnu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
@Entity
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "memberid")
	private Long memberId; //auto-Increment
	
	@Column(name = "userid")
	private String userId; //사용자 id
	
	private String password; //비밀번호
	
	@Column(name = "username")
	private String userName; //가입자 이름
	
	@Builder.Default
	@Enumerated(EnumType.STRING)
	private Role role = Role.ROLE_UNAUTH; // 허용 범위
	
	@Column(name = "factorycode")
	private int factoryCode; // 공장 코드
	
	private String email;
	private String phone;
	
}
