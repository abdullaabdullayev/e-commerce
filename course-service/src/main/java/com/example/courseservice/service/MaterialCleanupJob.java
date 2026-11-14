package com.example.courseservice.service;

import com.example.courseservice.storage.ObjectStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MaterialCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(MaterialCleanupJob.class);
    private final MaterialTransactions transactions;
    private final ObjectStorage storage;

    public MaterialCleanupJob(MaterialTransactions transactions, ObjectStorage storage) {
        this.transactions = transactions;
        this.storage = storage;
    }

    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void cleanFailedUploads() {
        for (var id : transactions.cleanupCandidates()) {
            try {
                var claim = transactions.claimCleanup(id);
                if (claim.isEmpty()) continue;
                var ticket = claim.get();
                storage.remove(ticket.objectKey());
                transactions.completeCleanup(ticket);
            } catch (Exception exception) {
                // The persisted lease expires and a subsequent run retries this object.
                log.warn("Material cleanup deferred for {} ({})", id, exception.getClass().getSimpleName());
            }
        }
    }
}
