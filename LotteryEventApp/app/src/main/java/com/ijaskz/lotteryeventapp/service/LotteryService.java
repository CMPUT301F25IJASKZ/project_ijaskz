package com.ijaskz.lotteryeventapp.service;

import androidx.annotation.Nullable;

import com.ijaskz.lotteryeventapp.WaitingListEntry;
import com.ijaskz.lotteryeventapp.WaitingListManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * LotteryService encapsulates the core lottery selection workflow.
 *
 * <p>Responsibilities:</p>
 * - Load eligible waiting-list entries for an event
 * - Randomly select up to a requested number of entries
 * - Persist selection by updating status/timestamps in batch via {@link WaitingListManager}
 * - Optionally apply a configurable per-draw response deadline (hours)
 *
 * <p>This class is UI-agnostic and suitable for unit testing by mocking the
 * underlying {@link WaitingListManager} if desired.</p>
 */
public class LotteryService {

    private final WaitingListManager waitingListManager;
    private final Random random;
    private static final int DEFAULT_RESPONSE_WINDOW_HOURS = 48;

    /**
     * Creates a LotteryService.
     * @param waitingListManager data access manager for waiting list operations
     */
    public LotteryService(WaitingListManager waitingListManager) {
        this.waitingListManager = waitingListManager;
        this.random = new Random();
    }

    /**
     * Runs a lottery for a given event by sampling up to {@code slots} entrants currently
     * in the "waiting" status. Selected entrants are updated to "selected" with timestamps.
     *
     * <p>If {@code responseWindowHoursOverride} is provided, it is written onto each selected
     * entry (field: response_window_hours). Otherwise, downstream logic should fall back to
     * the event's default response window.</p>
     *
     * @param eventId the target event id
     * @param slots maximum number of entrants to select
     * @param responseWindowHoursOverride optional per-draw response window in hours; may be null
     * @param callback completion callback returning the selected entries as loaded at selection time
     */
    public void runLottery(String eventId,
                           int slots,
                           @Nullable Integer responseWindowHoursOverride,
                           OnLotteryComplete callback) {
        if (slots <= 0) {
            callback.onFailure(new IllegalArgumentException("slots must be > 0"));
            return;
        }

        waitingListManager.getEntriesByStatus(eventId, "waiting", new WaitingListManager.OnEntriesLoadedListener() {
            @Override
            public void onEntriesLoaded(List<WaitingListEntry> entries) {
                if (entries == null || entries.isEmpty()) {
                    callback.onSuccess(Collections.emptyList());
                    return;
                }

                // Shuffle and take up to 'slots'
                List<WaitingListEntry> pool = new ArrayList<>(entries);
                Collections.shuffle(pool, random);
                List<WaitingListEntry> winners = pool.subList(0, Math.min(slots, pool.size()));

                List<String> winnerIds = new ArrayList<>(winners.size());
                for (WaitingListEntry e : winners) {
                    if (e.getId() != null) {
                        winnerIds.add(e.getId());
                    }
                }

                Integer hoursToApply = (responseWindowHoursOverride != null)
                        ? responseWindowHoursOverride
                        : DEFAULT_RESPONSE_WINDOW_HOURS;

                waitingListManager.updateEntriesStatus(
                        winnerIds,
                        "selected",
                        hoursToApply,
                        new WaitingListManager.OnCompleteListener() {
                            @Override
                            public void onSuccess() {
                                // Reflect in-memory status/fields for immediate return
                                long now = System.currentTimeMillis();
                                for (WaitingListEntry e : winners) {
                                    e.setStatus("selected");
                                    e.setSelected_at(now);
                                    e.setResponse_window_hours(hoursToApply);
                                }
                                callback.onSuccess(new ArrayList<>(winners));

                            }

                            @Override
                            public void onFailure(Exception e) {
                                callback.onFailure(e);
                            }
                        }
                );
            }

            @Override
            public void onError(Exception e) {
                callback.onFailure(e);
            }
        });
    }

    /**
     * Draws replacement winners from the waiting list, typically after spots free up due to
     * declines or cancellations. Behavior mirrors {@link #runLottery(String, int, Integer, OnLotteryComplete)}.
     *
     * @param eventId the target event id
     * @param slots number of replacements to draw
     * @param responseWindowHoursOverride optional per-draw response window in hours; may be null
     * @param callback completion callback returning the selected replacement entries
     */
    public void replenishFromWaitlistOnDecline(String eventId,
                                               int slots,
                                               @Nullable Integer responseWindowHoursOverride,
                                               OnLotteryComplete callback) {
        runLottery(eventId, slots, responseWindowHoursOverride, callback);
    }

    /**
     * Callback interface for lottery execution results.
     */
    public interface OnLotteryComplete {
        /**
         * Invoked when the lottery completes successfully.
         * @param winners the list of selected waiting-list entries
         */
        void onSuccess(List<WaitingListEntry> winners);

        /**
         * Invoked when the lottery fails for any reason.
         * @param e the error that occurred
         */
        void onFailure(Exception e);
    }
}
