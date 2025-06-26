package edu.pnu.dto;

import edu.pnu.domain.Member;
import edu.pnu.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter 
@Setter 
@ToString 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class MemberJoinDTO {
	private String userId;
	private String userName;
	private String password;
	private String email;
	private String phone;
	private int factoryCode;
	private Role role;
	
	//entity에 저장하는 Method
	public Member toEntity() {
		return Member.builder()
				.userId(userId)
				.userName(userName)
				.password(password)
				.email(email)
				.phone(phone)
				.factoryCode(factoryCode)
				.role(role != null ? role : Role.ROLE_MANAGER) //null값이면 Manger를 지정한다.
				.build();
	}
}
