package com.jessethouin.quant.broker;

import com.google.common.math.Quantiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TimedExecutor extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<>();
    private final List<Long> timings = new ArrayList<>();

    public TimedExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        startTime.set(System.currentTimeMillis());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        long taskExecutionTime = System.currentTimeMillis() - startTime.get();
        timings.add(taskExecutionTime);
    }

    public double getMedianExecutionTime() {
        return timings.isEmpty() ? 0 : Quantiles.median().compute(timings);
    }

    public double getAverageExecutionTime() {
        return timings.stream().collect(Collectors.averagingLong(value -> value));
    }

    public static TimedExecutor newFixedThreadPool(int noOfThreads) {
        return new TimedExecutor(noOfThreads, noOfThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }
}
