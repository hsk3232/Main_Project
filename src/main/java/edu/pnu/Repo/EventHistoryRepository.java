package edu.pnu.Repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	// epcCode별 이벤트 시간순 정렬
	List<EventHistory> findByEpc_EpcCodeOrderByEventTimeAsc(String epcCode);

	List<EventHistory> findByCsv_FileId(Long fileId);

	Optional<EventHistory> findFirstByLocationOrderByEventTimeDesc(Location l);

	List<EventHistory> findAllByOrderByEpc_EpcCodeAscEventTimeAsc();
	
	
	@Query(value = """
			  SELECT 
			    COUNT(*) AS totalTripCount,
			    COUNT(DISTINCT epc_product) AS uniqueProductCount,
			    SUM(CASE WHEN business_step='Factory' THEN 1 ELSE 0 END) AS codeCount,
			    COUNT(DISTINCT CASE WHEN event_type='pos_sell' then epc_code END) AS salesCount,
			  FROM eventhistory
			  WHERE file_id = :fileId
			""", nativeQuery = true)
			Map<String, Object> getKpiAggregates(@Param("fileId") Long fileId);
	
}
