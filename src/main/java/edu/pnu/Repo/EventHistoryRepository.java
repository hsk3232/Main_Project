package edu.pnu.Repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;

public interface EventHistoryRepository extends JpaRepository<EventHistory, Long> {

	// 검색(필터) + 커서 페이징
	List<EventHistory> findByEventTypeAndEventIdLessThanOrderByEventIdDesc(String eventType, Long cursor,
			Pageable pageable);

	List<EventHistory> findByEpc_EpcCodeAndEventIdLessThanOrderByEventIdDesc(String epcCode, Long cursor,
			Pageable pageable);

	List<EventHistory> findByBusinessStepAndEventTimeBetweenAndEventIdLessThanOrderByEventIdDesc(String businessStep,
			LocalDateTime min, LocalDateTime max, Long cursor, Pageable pageable);

	// 특정 기간, 이벤트 타입, 조건별 건수
	long countByEventTypeAndEventTimeBetween(String eventType, LocalDateTime from, LocalDateTime to);

	// 특정 이벤트 타입 건수(예: pos_sell, dispatch 등)
	long countByEventType(String eventType);

	

	// epcCode별 이벤트 시간순 정렬
	List<EventHistory> findByEpc_EpcCodeOrderByEventTimeAsc(String epcCode);

	

	List<EventHistory> findByCsv_FileId(Long fileId);

	Optional<EventHistory> findFirstByLocationOrderByEventTimeDesc(Location l);

	List<EventHistory> findAllByOrderByEpc_EpcCodeAscEventTimeAsc();
}
