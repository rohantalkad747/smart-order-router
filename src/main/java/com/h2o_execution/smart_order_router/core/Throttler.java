package com.h2o_execution.smart_order_router.core;

import java.util.concurrent.TimeUnit;

public class Throttler
{
    private final int threshold;
    private final TimeUnit unit;
    private volatile int ordersThisSecond;
    private volatile boolean sleeping;

    public Throttler(int threshold, TimeUnit unit)
    {
        this.threshold = threshold;
        this.unit = unit;
    }

    public boolean canRoute()
    {
        return ordersThisSecond < threshold;
    }

    public synchronized void onOrder()
    {
        ordersThisSecond++;
        if (!sleeping)
        {
            sleeping = true;
            new Thread(() ->
            {
                try
                {
                    unit.sleep(1);
                    ordersThisSecond = 0;
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
