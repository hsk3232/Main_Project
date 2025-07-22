package edu.pnu.batch;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import edu.pnu.events.EventHistorySavedEvent;
import edu.pnu.service.datashare.DataShareService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AiAnalysisListener {
    private final DataShareService dataShareService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(EventHistorySavedEvent event) {

    }
}