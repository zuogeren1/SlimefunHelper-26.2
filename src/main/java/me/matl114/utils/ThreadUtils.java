package me.matl114.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程工具。
 *
 * <p>模组自建的线程池一律要使用守护线程：26.2 起客户端退出时会启动
 * {@code ClientShutdownWatchdog}，只要还有非守护线程存活，JVM 就结束不了，
 * 看门狗超时后直接抛出 "Client shutdown from post-main" 的 Watchdog 错误
 * （线程转储里只剩几个停在 {@code ScheduledThreadPoolExecutor$DelayedWorkQueue.take}
 * 的 {@code pool-N-thread-M}）。
 *
 * <p>顺带一提：非守护线程活着时 JVM 进不了 shutdown 阶段，
 * {@code Config} 注册的 "Config-Shutdown-Save" 钩子也就永远不会执行。
 */
public class ThreadUtils {

    /** 生成命名形如 {@code prefix-N}、且标记为 daemon 的线程工厂。 */
    public static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
