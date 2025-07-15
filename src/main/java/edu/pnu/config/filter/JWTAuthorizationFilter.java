package edu.pnu.config.filter;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import edu.pnu.Repo.MemberRepository;
import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Member;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JWTAuthorizationFilter extends OncePerRequestFilter {

	private final MemberRepository memberRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws IOException, ServletException {
		log.info("[진입] : [1][JWTAuthorizationFilter] 토큰 확인 필터 진입");
		
		String srcToken = request.getHeader(HttpHeaders.AUTHORIZATION); // 요청 헤더에서 Authorization을 얻어온다.
		log.info("[발행된 토큰] : "+ srcToken);
		
		if (srcToken == null || !srcToken.startsWith("Bearer ")) { // 없거나 “Bearer ”로 시작하지 않는다면
			System.out.println("[진입] : [2][JWTAuthorizationFilter] 토큰 없음 \n");
			filterChain.doFilter(request, response); // 필터를 그냥 통과 
			return;
		}
		log.info("[완료] : [2][JWTAuthorizationFilter] 토큰 확인 완료");
		

		String jwtToken = srcToken.replace("Bearer ", ""); // 토큰에서 “Bearer ”를 제거
		// 토큰에서 username 추출
		

		String userId = null;
		try {
			// 토큰 검증 과정 try-catch로 감싸기 (만료된 토큰 등 예외 처리)
			System.out.println("[진행] : [3][JWTAuthorizationFilter] username(id) 추출 시작 \n");
			userId = JWT.require(Algorithm.HMAC256("edu.pnu.jwt")).build().verify(jwtToken).getClaim("userId")
					.asString();
			
			// Token 유효기간 확인
			if(JWT.require(Algorithm.HMAC256("edu.pnu.jwt")).build().verify(jwtToken).getExpiresAt().before(new Date())) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				System.out.println("[진행] : [4][JWTAuthorizationFilter] 로그인한 아이디의 토큰 유효기간 만료");
				response.setContentType("text/plain");
			    response.getWriter().write("Expired Token");
			    
			    // 토큰 만료시, 토큰 재발급 해서 front로 다시 보냄
			    String token = JWT.create()
						.withExpiresAt(new Date(System.currentTimeMillis()+1000*60*100000))
						.withClaim("userId", userId)
						.sign(Algorithm.HMAC256("edu.pnu.jwt"));
				
			    log.info("[진행] : [4][JWTAuthorizationFilter] 로그인한 아이디의 토큰 재발행 전송 \n");
				response.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
				return ;
			}

		} catch (Exception e) {
			// 예외가 발생하면 로그 출력 후 필터 체인 그냥 통과
			log.info("[오류] : [4][JWTAuthorizationFilter] JWT 오류 발생 " + e.getMessage());
			
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
		    
			//filterChain.doFilter(request, response);
			return;
		}

		if (userId == null) {
			// 🔥 토큰에 username이 없을 경우도 예외로 처리
			 response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401

			//filterChain.doFilter(request, response);
			return;
		}

		Optional<Member> opt = memberRepository.findByUserId(userId); // 토큰에서 얻은 username으로 DB를 검색해서 사용자를 검색
		System.out.println("[진행] : [5][JWTAuthorizationFilter] username(id) 검색 시작");
		if (!opt.isPresent()) { // 사용자가 존재하지 않는다면
			
			 response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401

			//filterChain.doFilter(request, response); // 필터를 그냥 통과
			 log.info("[오류] : [5][JWTAuthorizationFilter]사용자가 없다.");
			return;
		} 
		System.out.println("[진행] : [6][JWTAuthorizationFilter] 사용자 찾음");
		
		Member findmember = opt.get();
		log.info("[진행] : [7][JWTAuthorizationFilter] ROLE 값: [" + findmember.getRole()+"]");


		try {
		    CustomUserDetails customUser = new CustomUserDetails(
		        findmember.getUserId(),
		        findmember.getPassword(),
		        AuthorityUtils.createAuthorityList(findmember.getRole().toString()),
		        findmember.getLocation().getLocationId());
		    Authentication auth = new UsernamePasswordAuthenticationToken(customUser, null, customUser.getAuthorities());
		    SecurityContextHolder.getContext().setAuthentication(auth);
		    
		    log.info("[성공] : [8][JWTAuthorizationFilter] 토큰 확인 완료 \n");
		    filterChain.doFilter(request, response);
		} catch (Exception e) {
		    e.printStackTrace();
		    System.out.println("[오류] : [8][JWTAuthorizationFilter] 오류 발생! \n");
		}
	}
}
