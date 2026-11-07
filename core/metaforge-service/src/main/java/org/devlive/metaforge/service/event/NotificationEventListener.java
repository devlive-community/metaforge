package org.devlive.metaforge.service.event;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devlive.metaforge.service.notify.NotificationDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The dispatcher is a Spring-managed singleton")
public class NotificationEventListener
{
    private final NotificationDispatcher dispatcher;

    // Runs after commit when raised inside a transaction, right away otherwise.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationEvent event)
    {
        try {
            dispatcher.dispatch(event);
        }
        catch (RuntimeException e) {
            log.warn("Notification {} failed: {}", event.getScene(), e.getClass().getSimpleName());
        }
    }
}
