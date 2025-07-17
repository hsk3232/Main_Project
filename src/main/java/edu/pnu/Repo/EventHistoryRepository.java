package edu.pnu.Repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.pnu.domain.EventHistory;

public interface EventHistoryRepository extends JpaRepository<EventHistory, Long> {

	// eventType별 집계, anomaly, 기간, 커서 페이징 등
	List<EventHistory> findByAnomalyIsNotNull(Pageable pageable);
	
	// 전체 이상 이벤트 커서 페이징 (Anomaly만, 최신순)
	List<EventHistory> findByAnomalyIsNotNullAndEventIdLessThanOrderByEventIdDesc(Long cursor, Pageable pageable);

	// 검색(필터) + 커서 페이징
	List<EventHistory> findByEventTypeAndEventIdLessThanOrderByEventIdDesc(String eventType, Long cursor, Pageable pageable);

	List<EventHistory> findByEpc_EpcCodeAndEventIdLessThanOrderByEventIdDesc(String epcCode, Long cursor, Pageable pageable);

	// 예시: businessStep, eventType, 기간, Anomaly 등 조건 추가
	List<EventHistory> findByBusinessStepAndEventTimeBetweenAndEventIdLessThanOrderByEventIdDesc(
	    String businessStep, LocalDateTime min, LocalDateTime max, Long cursor, Pageable pageable);

	// 전체 이동(Trip) 건수
	long countBy();

	// 이상 이벤트 건수
	long countByAnomalyIsNotNull();

	// 특정 이벤트 타입 건수(예: pos_sell, dispatch 등)
	long countByEventType(String eventType);

	// 특정 기간, 이벤트 타입, 조건별 건수
	long countByEventTypeAndEventTimeBetween(String eventType, LocalDateTime from, LocalDateTime to);

	// unique EPC(코드) 수 (JPA 쿼리/Native 사용 필요)
	@Query("SELECT COUNT(DISTINCT e.epc.epcCode) FROM EventHistory e WHERE e.eventType = :eventType")
	long countDistinctEpcCodeByEventType(@Param("eventType") String eventType);
	
	// epcCode별 이벤트 시간순 정렬
	List<EventHistory> findByEpc_EpcCodeOrderByEventTimeAsc(String epcCode);
	
	// epcCode 전체 리스트 (distinct)
	@Query("select distinct e.epc.epcCode from EventHistory e")
	List<String> findAllDistinctEpcCodes();
	
	List<EventHistory> findByFileLog_FileId(Long fildId);
	
}
