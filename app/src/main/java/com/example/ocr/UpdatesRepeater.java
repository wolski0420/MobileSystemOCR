package com.example.ocr;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.util.ArrayList;
import java.util.List;

public class UpdatesRepeater extends JobService {
    private static final List<Runnable> updates = new ArrayList<>();
    private final long timeToSleep = 1000;
    private boolean shouldContinueRepeating = true;

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        startRepeating();
        return true;
    }

    private void startRepeating() {
        new Thread(() -> {
            while(shouldContinueRepeating) {
                updates.forEach(Runnable::run);

                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        shouldContinueRepeating = false;
        return true;
    }

    public static void addAtomicUpdate(Runnable update) {
        UpdatesRepeater.updates.add(update);
    }
}
