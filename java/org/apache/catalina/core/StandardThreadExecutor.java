/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.core;

import org.apache.catalina.Executor;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.util.LifecycleMBeanBase;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.threads.ResizableExecutor;
import org.apache.tomcat.util.threads.TaskQueue;
import org.apache.tomcat.util.threads.TaskThreadFactory;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;

import java.util.concurrent.TimeUnit;

public class StandardThreadExecutor extends LifecycleMBeanBase
        implements Executor, ResizableExecutor {

    protected static final StringManager sm = StringManager.getManager(StandardThreadExecutor.class);

    // ---------------------------------------------- Properties
    /**
     * 执行程序中线程的线程优先级，默认为 5（Thread.NORM_PRIORITY常量的值）
     * Default thread priority
     */
    protected int threadPriority = Thread.NORM_PRIORITY;

    /**
     * 线程是否应该是守护程序线程
     * Run threads in daemon or non-daemon state
     */
    protected boolean daemon = true;

    /**
     * 执行程序创建的每个线程的名称前缀。单个线程的线程名称将是namePrefix+threadNumber
     * Default name prefix for the thread name
     */
    protected String namePrefix = "tomcat-exec-";

    /**
     * 此池中活动线程的最大数量，默认为 200
     * max number of threads
     */
    protected int maxThreads = 200;

    /**
     * 最小线程数（空闲和活动）始终保持活动状态，默认为 25
     * min number of threads
     */
    protected int minSpareThreads = 25;

    /**
     * 空闲线程关闭之前的毫秒数，除非活动线程数小于或等于minSpareThreads。默认值为60000（1分钟）
     * idle time in milliseconds
     */
    protected int maxIdleTime = 60000;

    /**
     * The executor we use for this component
     */
    protected ThreadPoolExecutor executor = null;

    /**
     * the name of this thread pool
     */
    protected String name;

    /**
     * 在我们拒绝之前可以排队等待执行的可运行任务的最大数量。默认值是Integer.MAX_VALUE
     * The maximum number of elements that can queue up before we reject them
     */
    protected int maxQueueSize = Integer.MAX_VALUE;

    /**
     * 如果配置了ThreadLocalLeakPreventionListener，它将通知此执行程序有关已停止的上下文。上下文停止后，池中的线程将被更新。
     * 为避免同时更新所有线程，此选项在任意2个线程的续订之间设置延迟。该值以ms为单位，默认值为1000ms。如果值为负，则不会续订线程。
     * After a context is stopped, threads in the pool are renewed. To avoid
     * renewing all threads at the same time, this delay is observed between 2
     * threads being renewed.
     */
    protected long threadRenewalDelay =
        org.apache.tomcat.util.threads.Constants.DEFAULT_THREAD_RENEWAL_DELAY;

    private TaskQueue taskqueue = null;
    // ---------------------------------------------- Constructors
    public StandardThreadExecutor() {
        //empty constructor for the digester
    }


    // ---------------------------------------------- Public Methods

    /**
     * Start the component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#startInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    @Override
    protected void startInternal() throws LifecycleException {

        taskqueue = new TaskQueue(maxQueueSize);
        TaskThreadFactory tf = new TaskThreadFactory(namePrefix,daemon,getThreadPriority());
        executor = new ThreadPoolExecutor(getMinSpareThreads(), getMaxThreads(), maxIdleTime, TimeUnit.MILLISECONDS,taskqueue, tf);
        executor.setThreadRenewalDelay(threadRenewalDelay);
        taskqueue.setParent(executor);

        setState(LifecycleState.STARTING);
    }


    /**
     * Stop the component and implement the requirements
     * of {@link org.apache.catalina.util.LifecycleBase#stopInternal()}.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    @Override
    protected void stopInternal() throws LifecycleException {

        setState(LifecycleState.STOPPING);
        if (executor != null) {
            executor.shutdownNow();
        }
        executor = null;
        taskqueue = null;
    }


    @Override
    @Deprecated
    public void execute(Runnable command, long timeout, TimeUnit unit) {
        if (executor != null) {
            executor.execute(command,timeout,unit);
        } else {
            throw new IllegalStateException(sm.getString("standardThreadExecutor.notStarted"));
        }
    }


    @Override
    public void execute(Runnable command) {
        if (executor != null) {
            // Note any RejectedExecutionException due to the use of TaskQueue
            // will be handled by the o.a.t.u.threads.ThreadPoolExecutor
            executor.execute(command);
        } else {
            throw new IllegalStateException(sm.getString("standardThreadExecutor.notStarted"));
        }
    }

    public void contextStopping() {
        if (executor != null) {
            executor.contextStopping();
        }
    }

    public int getThreadPriority() {
        return threadPriority;
    }

    public boolean isDaemon() {

        return daemon;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
        if (executor != null) {
            executor.setKeepAliveTime(maxIdleTime, TimeUnit.MILLISECONDS);
        }
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        if (executor != null) {
            executor.setMaximumPoolSize(maxThreads);
        }
    }

    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
        if (executor != null) {
            executor.setCorePoolSize(minSpareThreads);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMaxQueueSize(int size) {
        this.maxQueueSize = size;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public long getThreadRenewalDelay() {
        return threadRenewalDelay;
    }

    public void setThreadRenewalDelay(long threadRenewalDelay) {
        this.threadRenewalDelay = threadRenewalDelay;
        if (executor != null) {
            executor.setThreadRenewalDelay(threadRenewalDelay);
        }
    }

    // Statistics from the thread pool
    @Override
    public int getActiveCount() {
        return (executor != null) ? executor.getActiveCount() : 0;
    }

    public long getCompletedTaskCount() {
        return (executor != null) ? executor.getCompletedTaskCount() : 0;
    }

    public int getCorePoolSize() {
        return (executor != null) ? executor.getCorePoolSize() : 0;
    }

    public int getLargestPoolSize() {
        return (executor != null) ? executor.getLargestPoolSize() : 0;
    }

    @Override
    public int getPoolSize() {
        return (executor != null) ? executor.getPoolSize() : 0;
    }

    public int getQueueSize() {
        return (executor != null) ? executor.getQueue().size() : -1;
    }


    @Override
    public boolean resizePool(int corePoolSize, int maximumPoolSize) {
        if (executor == null) {
            return false;
        }

        executor.setCorePoolSize(corePoolSize);
        executor.setMaximumPoolSize(maximumPoolSize);
        return true;
    }


    @Override
    public boolean resizeQueue(int capacity) {
        return false;
    }


    @Override
    protected String getDomainInternal() {
        // No way to navigate to Engine. Needs to have domain set.
        return null;
    }

    @Override
    protected String getObjectNameKeyProperties() {
        return "type=Executor,name=" + getName();
    }
}
