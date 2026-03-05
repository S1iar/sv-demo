package org.goden.svdemo.utils;

import java.util.function.Supplier;

/**
 * ThreadLocal 工具类
 * 提供类型安全的线程本地变量管理
 *
 * 注意：此类设计为存储单一对象，适用于简单的线程上下文场景
 * 如果需要存储多个变量，请使用 Map 结构或独立的 ThreadLocal 实例
 */
public final class ThreadLocalUtil {

    /**
     * 使用泛型 ThreadLocal 保证类型安全
     * 通过 static final 确保全局唯一性
     */
    private static final ThreadLocal<Object> THREAD_LOCAL = new ThreadLocal<>();

    private ThreadLocalUtil() {
        throw new UnsupportedOperationException("工具类禁止实例化");
    }

    /**
     * 获取当前线程的 ThreadLocal 值
     *
     * @param <T> 期望的类型
     * @return 存储的值，如果不存在则返回 null
     * @throws ClassCastException 如果存储的类型与期望类型不匹配
     */
    @SuppressWarnings("unchecked")
    public static <T> T get() {
        return (T) THREAD_LOCAL.get();
    }

    /**
     * 获取当前线程的 ThreadLocal 值，如果不存在则返回默认值
     *
     * @param defaultValue 默认值
     * @param <T> 期望的类型
     * @return 存储的值或默认值
     */
    public static <T> T getOrDefault(T defaultValue) {
        T value = get();
        return value != null ? value : defaultValue;
    }

    /**
     * 获取当前线程的 ThreadLocal 值，如果不存在则通过 supplier 创建
     *
     * @param supplier 值提供函数
     * @param <T> 期望的类型
     * @return 存储的值或新创建的值
     */
    public static <T> T getOrCreate(Supplier<T> supplier) {
        T value = get();
        if (value == null) {
            value = supplier.get();
            set(value);
        }
        return value;
    }

    /**
     * 设置当前线程的 ThreadLocal 值
     * 注意：这会覆盖之前存储的值
     *
     * @param value 要存储的值
     */
    public static <T> void set(T value) {
        THREAD_LOCAL.set(value);
    }

    /**
     * 设置值并返回之前的值
     *
     * @param value 新值
     * @param <T> 值类型
     * @return 之前的值，如果没有则返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T getAndSet(T value) {
        T oldValue = (T) THREAD_LOCAL.get();
        THREAD_LOCAL.set(value);
        return oldValue;
    }

    /**
     * 清除当前线程的 ThreadLocal 值
     * 重要：在线程结束时必须调用，防止内存泄漏
     */
    public static void remove() {
        THREAD_LOCAL.remove();
    }

    /**
     * 判断当前线程是否有值
     *
     * @return 如果存在值则返回 true
     */
    public static boolean isPresent() {
        return THREAD_LOCAL.get() != null;
    }

    /**
     * 如果存在值，则执行给定的操作
     *
     * @param action 要执行的操作
     * @param <T> 值类型
     */
    public static <T> void ifPresent(java.util.function.Consumer<T> action) {
        T value = get();
        if (value != null) {
            action.accept(value);
        }
    }

    /**
     * 如果存在值，则返回该值，否则抛出异常
     *
     * @param exceptionSupplier 异常提供函数
     * @param <T> 值类型
     * @param <X> 异常类型
     * @return 存储的值
     * @throws X 如果值不存在
     */
    public static <T, X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        T value = get();
        if (value == null) {
            throw exceptionSupplier.get();
        }
        return value;
    }
}
