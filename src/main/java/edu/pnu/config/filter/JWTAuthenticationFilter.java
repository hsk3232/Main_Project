package edu.pnu.config.filter;

import java.io.IOException;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pnu.domain.Member;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
//@RequiredArgsConstructor => 롬복 생성자는 8080/login으로 연결되기 때문에 url 바꾸려면 직접 생성자를 선언해야함.
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter{
	// 인증 객체, fillter 객체는 컨테이너에 올리지 못함
	// 따라서 autowired 못함
	private final AuthenticationManager authenticationManager;
	
	//생성자 직접 선언
	public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
	        super(authenticationManager);
	        this.authenticationManager = authenticationManager;
	        // 이 줄이 있어야 /api/public/login으로 POST 요청 시 필터가 동작!
	        setFilterProcessesUrl("/api/public/login");
	    }
	
	// POST/login 요청이 왔을 때 인증을 시도하는 메소드
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		// request에서 json 타입의 [username/password]를 읽어서 Member 객체를 생성한다.
		
		System.out.println("\n"+"[진입] : [JWTAuthenticationFilter] POST 방식 로그인 시도 진입");
		
		ObjectMapper mapper = new ObjectMapper();
		
		 System.out.println("[성공] : [JWTAuthenticationFilter] request에서 json 타입의 [username/password]를 읽어서 Member 객체를 생성");
		
		try {
			Member member = mapper.readValue(request.getInputStream(), Member.class);
			// Security에게 자격 증명 요청에 필요한 객체 생성
			Authentication authToken = new UsernamePasswordAuthenticationToken(member.getUserId(), member.getPassword());
			// 인증 진행 -> UserDetailsService의 loadUserByUsername에서 DB로부터 사용자 정보를 읽어온 뒤
			// 사용자 입력 정보와 비교한 뒤 자격 증명에 성공하면 Authenticaiton객체를 만들어서 리턴한다.
			
			return authenticationManager.authenticate(authToken);
			
		}
		
		catch (Exception e) {
			// “자격 증명에 실패하였습니다.” 로그 출력
			System.out.println("[실패] : [JWTAuthenticationFilter] 사용자 인증 실패 \n");
			log.info(e.getMessage());
			// 자격 증명에 실패하면 응답코드 리턴
			response.setStatus(HttpStatus.UNAUTHORIZED.value());
		}
		
		return null;
	}
	
	// 인증이 성공했을 때 실행되는 후처리 메소드
	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		
		// 자격 증명이 성공하면 loadUserByUsername에서 만든 객체가 authResult에 담겨져 있다.
		System.out.println("[성공] : [JWTAuthenticationFilter] 사용자 인증 성공" + "\n");
		User user = (User)authResult.getPrincipal();
		System.out.println("auth:" + user); // user 객체를 콘솔에 출력해서 확인
		
		// username으로 JWT를 생성해서 Response Header - Authorization에 담아서 돌려준다.
		// 이것은 하나의 예시로서 필요에 따라 추가 정보를 담을 수 있다.
		System.out.println("[진행] : [JWTAuthenticationFilter] JWTAuthorizationFilter로 정보 이동 \n");
		String token = JWT.create()
				.withExpiresAt(new Date(System.currentTimeMillis()+1000*60*100))
				.withClaim("username", user.getUsername())
				.sign(Algorithm.HMAC256("edu.pnu.jwt"));
		
		response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		
		response.setStatus(HttpStatus.OK.value());
	
	}
	

}
