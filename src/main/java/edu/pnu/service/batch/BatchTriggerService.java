package edu.pnu.service.batch;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import edu.pnu.Repo.AnalyzedTripRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.domain.AnalyzedTrip;
import edu.pnu.domain.EventHistory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BatchTriggerService {
	
	private final AnalyzedTripRepository analyzedTripRepo;
	private final EventHistoryRepository eventHistoryRepo;
	
	/**
     * [분석 및 저장]
     * EventHistory 전체 데이터를 epcCode+eventTime 기준으로 정렬 후
     * 연속된 이동 쌍(from→to)을 찾아서
     * AnalyzedTrip로 변환 & 중복 없이 저장
     */
	
	@Transactional
	public List<AnalyzedTrip> analyzeAndSaveAllTrips() {
		// 1. 전체 EventHistory를 epcCode, eventTime 기준 정렬 조회
	    List<EventHistory> allEvents = eventHistoryRepo.findAllByOrderByEpc_EpcCodeAscEventTimeAsc();
	    List<AnalyzedTrip> trips = new ArrayList<>();

	    EventHistory prev = null;
	    for (EventHistory curr : allEvents) {
	    	// 2. 이전과 현재가 같은 EPC코드(=같은 상품의 이동 기록)라면
	        if (prev != null && prev.getEpc().getEpcCode().equals(curr.getEpc().getEpcCode())) {
	            
	        	// 3. 중복 체크 (이전 쌍이 이미 저장돼 있다면 건너뜀)
	            boolean exists = analyzedTripRepo.existsByFromLocationIdAndToLocationIdAndFromEventTypeAndToEventType(
	            	prev.getLocation().getLocationId(),
	                curr.getLocation().getLocationId(),
	                prev.getEventType(),
	                curr.getEventType()
	            );
	            if (exists) {
	                prev = curr;
	                continue; // 이미 존재하면 건너뜀!
	            }
	        	
	            // 4. 새 이동경로(AnalyzedTrip) 객체 생성
	        	trips.add(AnalyzedTrip.builder()
	                .fromScanLocation(prev.getLocation().getScanLocation())
	                .toScanLocation(curr.getLocation().getScanLocation())
	                .fromLocationId(prev.getLocation().getLocationId())
	                .toLocationId(curr.getLocation().getLocationId())
	                .fromBusinessStep(prev.getBusinessStep())
	                .toBusinessStep(curr.getBusinessStep())
	                .fromEventType(prev.getEventType())
	                .toEventType(curr.getEventType())
	                .build());
	        }
	        prev = curr; // 다음 비교를 위해 prev 갱신
	    }
	    // 5. 새로 생성된 이동경로를 일괄 저장 (saveAll)
	    return analyzedTripRepo.saveAll(trips);
	}
}
