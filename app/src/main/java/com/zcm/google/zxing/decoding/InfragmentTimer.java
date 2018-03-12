package com.zcm.google.zxing.decoding;



import android.support.v4.app.Fragment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by Zhucmao on 2017/12/20 17:45.
 * 修改！否则程序会在5 * 60秒后关闭闪退！
 *
 * TODO:InactivityTimer、BeepManager、FinishListener 基于休眠、声音、退出的辅助管理类
 */

public class InfragmentTimer {
    private static final int INFRAGMENT_DELAY_SECONDS = 5*60;

    private final ScheduledExecutorService infragmentTimer =
            Executors.newSingleThreadScheduledExecutor(new InfragmentTimer.DaemonThreadFactory());
    private final Fragment fragment;
    private ScheduledFuture<?> infragmentFuture = null;

    public InfragmentTimer(Fragment fragment) {
        this.fragment = fragment;
        onFragment();
    }

    public void onFragment() {
        cancel();
        infragmentFuture = infragmentTimer.schedule(new FinishFragmentListener(fragment),
                INFRAGMENT_DELAY_SECONDS,
                TimeUnit.SECONDS);
    }

    private void cancel() {
        if (infragmentFuture != null) {
            infragmentFuture.cancel(true);
            infragmentFuture = null;
        }
    }

    public void shutdown() {
        cancel();
        infragmentTimer.shutdown();
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    }
}
