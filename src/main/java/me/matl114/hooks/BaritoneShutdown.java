package me.matl114.hooks;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * baritone 的退出清理。
 *
 * <p>{@code baritone.Baritone} 有一个 {@code private static ThreadPoolExecutor}，baritone
 * 从头到尾没有关闭它的路径（整个 jar 里只有 {@code ElytraProcess} 引用了 shutdown），
 * 而它用的是默认 ThreadFactory —— 线程全是**非守护线程**。
 *
 * <p>于是 {@code Main.main} 返回后 JVM 会一直卡在"等所有非守护线程结束"上（注意这一步在跑
 * shutdown hooks 之前），26.2 新增的 {@code ClientShutdownWatchdog} 等 15 秒就会产出一份
 * "Client shutdown from post-main" 崩溃报告并 {@code System.exit(-8)}。
 * 线程 dump 里的表现就是三条没有 daemon 标记的 {@code pool-N-thread-M}，分别停在
 * {@code baritone.cache.CachedWorld$PackerThread.run}、{@code CachedWorld.b}
 * 和 {@code ThreadPoolExecutor.getTask}。
 *
 * <p>退出时把这个池 {@code shutdownNow()} 掉就够了，三条线程都会自己结束：
 * <ul>
 *   <li>{@code PackerThread.run} 的 {@code LinkedBlockingQueue.take} 抛
 *       InterruptedException，异常表指向 {@code printStackTrace(); return;}
 *   <li>{@code CachedWorld.b} 的 {@code Thread.sleep} 同理，也是 printStackTrace 后 return
 *   <li>空闲 worker 在池进入 STOP 后由 {@code getTask()} 拿到 null 退出
 * </ul>
 *
 * <p>这里刻意**不 import 任何 baritone 类型**：baritone 不在时这个类完全惰性，
 * 反射失败也只是静默跳过，绝不影响退出流程。
 */
public final class BaritoneShutdown {
    private BaritoneShutdown() {}

    private static volatile boolean done;

    /**
     * 关掉 baritone 的静态线程池。可重复调用，只有第一次生效，任何失败都被吞掉。
     */
    public static void shutdownExecutorQuietly() {
        if (done) {
            return;
        }
        done = true;
        try {
            // baritone 没被用过时这个类根本没初始化，JVM 也就不存在它的线程，调用这里是空转；
            // 用过的话类早就初始化好了，forName 不会再触发 <clinit>。
            Class<?> clazz = Class.forName("baritone.Baritone");
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                // 按类型找而不是按名字找：baritone 是混淆过的，字段名每个构建都不一样
                if (!ThreadPoolExecutor.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                if (field.get(null) instanceof ThreadPoolExecutor executor) {
                    executor.shutdownNow();
                }
            }
        } catch (Throwable ignored) {
            // baritone 不在 / 字段实现变了 / 反射被拦……都不该影响退出
        }
    }
}
