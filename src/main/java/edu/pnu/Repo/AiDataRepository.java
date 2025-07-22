package edu.pnu.Repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.AiData;
import edu.pnu.domain.EventHistory;

public interface AiDataRepository extends JpaRepository<AiData, Long> {
	Optional<AiData> findByEventHistory(EventHistory eventHistory);

	Optional<AiData> findByEventHistory_EventId(Long eventId);

	// eventType별 집계, anomaly, 기간, 커서 페이징 등
	List<AiData> findByAnomalyIsNotNull(Pageable pageable);

	// 전체 이상 이벤트 커서 페이징 (Anomaly만, 최신순)
	List<AiData> findByAnomalyIsNotNullAndEventHistory_EventIdLessThanOrderByEventHistory_EventIdDesc(
		    Long eventId, Pageable pageable);
	
	List<AiData> findByEventHistory_EventIdIn(List<Long> eventIds);
}
