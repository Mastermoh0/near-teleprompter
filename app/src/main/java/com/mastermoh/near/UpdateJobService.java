package com.mastermoh.near;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.io.IOException;
import java.util.concurrent.Future;

public final class UpdateJobService extends JobService {
    private Future<?> work;
    private volatile int generation;
    @Override public boolean onStartJob(JobParameters params) {
        if(!UpdateChecker.prefs(this).getBoolean("alerts",false)) return false;
        final int run=++generation;
        work=UpdateChecker.NETWORK.submit(() -> {
            boolean retry=false;
            try { UpdateChecker.check(this,false); if(run==generation) UpdateChecker.notifyIfAvailable(this); }
            catch(IOException unavailable) { retry=true; }
            if(run==generation) jobFinished(params,retry);
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) { generation++; if(work!=null) work.cancel(true); return true; }
}
