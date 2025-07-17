package edu.pnu.dto.dataShere;

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
@Builder
@ToString

public class EventHistoryImportDTO {
	
	// 각 이벤트별 상세
    private Long eventId;
    private boolean anormaly;
    private boolean jump;
    private double jumpScore;
    private boolean evtOrderErr;
    private double evtOrderErrScore;
    private boolean epcFake;
    private double epcFakeScore;
    private boolean epcDup;
    private double epcDupScore;
    private boolean locErr;
    private double locErrScore;

//		이걸 쓰면, 기존 튜플의 내용을 업데이트하는 것이 아니라, 새로운 튜플을 생성함.
//    public static EventHistory toEntity(EventHistoryErrDTO e) {
//    	return EventHistory.builder()
//    			.anomaly(e.isAnormaly())
//    			.jump(e.isJump())
//    			.jumpScore(e.getJumpScore())
//    			.evtOrderErr(e.isEvtOrderErr())
//    			.evtOrderErrScore(e.getEvtOrderErrScore())
//    			.epcFake(e.isEpcFake())
//    			.epcFakeScore(e.getEpcFakeScore())
//    			.epcDup(e.isEpcDup())
//    			.epcDupScore(e.getEpcDupScore())
//    			.locErr(e.isLocErr())
//    			.locErrScore(e.getLocErrScore())
//    			.build();
//    }
}