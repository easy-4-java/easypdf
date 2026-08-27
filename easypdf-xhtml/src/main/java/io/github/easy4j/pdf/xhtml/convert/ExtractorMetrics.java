package io.github.easy4j.pdf.xhtml.convert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PDF 提取的进程内计数器（无需 JMX / Spring Boot Actuator，直接 {@link #snapshot()} 读数即可）。
 *
 * <p>埋点口径：每次结构化提取计一次（成功与失败都计入 {@code total}）——
 * 由 {@link PdfStructureExtractor#extract(File, io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties)
 * extract} 单一收口，
 * 报告式入口 {@code extractWithReport} 内部委托同一方法，因此不会重复计数；
 * 纯字符串操作（如 {@code MarkdownConverter.mdToHtml}）不计。
 *
 * <p>线程安全：多字段更新在 synchronized 块内完成，{@link #snapshot()} 读取一致视图；
 * 返回的 Map 为独立不可变副本，后续计数不影响已取出的快照。
 *
 * <p>用法：诊断/巡检接口直接读 {@link #INSTANCE}；单测可用 {@code new ExtractorMetrics()}
 * 构造独立实例避免进程级状态串扰。
 */
public final class ExtractorMetrics {

    /** 进程级共享实例：常规调用方经此读数。 */
    public static final ExtractorMetrics INSTANCE = new ExtractorMetrics();

    /** 总提取次数（成功 + 失败）。 */
    private final AtomicLong totalExtracts = new AtomicLong();

    /** 成功次数。 */
    private final AtomicLong totalSuccesses = new AtomicLong();

    /** 失败总次数（按分级码细分见 {@link #failureByCode}）。 */
    private final AtomicLong totalFailures = new AtomicLong();

    /** 累计耗时（毫秒，含失败尝试；时间源 System.currentTimeMillis()）。 */
    private final AtomicLong totalDurationMs = new AtomicLong();

    /** 按失败分级码的累计次数。 */
    private final ConcurrentHashMap<ExtractionException.Code, AtomicLong> failureByCode =
            new ConcurrentHashMap<ExtractionException.Code, AtomicLong>();

    /** 记一次成功提取及其耗时（负值按 0 计）。 */
    public void recordSuccess(long durationMs) {
        synchronized (this) {
            totalExtracts.incrementAndGet();
            totalSuccesses.incrementAndGet();
            addDuration(durationMs);
        }
    }

    /**
     * 记一次失败提取：分级码取自 {@link ExtractionException#getCode()}；
     * 未分级的异常由埋点侧归入 {@link ExtractionException.Code#CORRUPT} 后传入。
     */
    public void recordFailure(ExtractionException.Code code, long durationMs) {
        Objects.requireNonNull(code, "code must not be null");
        synchronized (this) {
            totalExtracts.incrementAndGet();
            totalFailures.incrementAndGet();
            addDuration(durationMs);
            counterFor(code).incrementAndGet();
        }
    }

    /**
     * 只读诊断快照（不可变、与内部状态隔离）。key 契约：
     * {@code total}（成功+失败总数）、{@code successes}、{@code durationMs}
     * 与每个分级码固定存在的 {@code failures.CORRUPT / failures.ENCRYPTED /
     * failures.LIMIT_EXCEEDED / failures.NOT_FOUND}（未发生为 0；
     * 无顶层 "failures" 聚合 key，避免与逐码 key 混淆）。
     */
    public Map<String, Long> snapshot() {
        synchronized (this) {
            Map<String, Long> snap = new LinkedHashMap<String, Long>();
            snap.put("total", Long.valueOf(totalExtracts.get()));
            snap.put("successes", Long.valueOf(totalSuccesses.get()));
            snap.put("durationMs", Long.valueOf(totalDurationMs.get()));
            for (ExtractionException.Code code : ExtractionException.Code.values()) {
                AtomicLong c = failureByCode.get(code);
                snap.put("failures." + code.name(), Long.valueOf(c == null ? 0L : c.get()));
            }
            return Collections.unmodifiableMap(snap);
        }
    }

    /** 清零全部计数（仅测试/巡检复位用；不影响进行中的提取）。 */
    public void reset() {
        synchronized (this) {
            totalExtracts.set(0);
            totalSuccesses.set(0);
            totalFailures.set(0);
            totalDurationMs.set(0);
            failureByCode.clear();
        }
    }

    private void addDuration(long durationMs) {
        if (durationMs > 0) {
            totalDurationMs.addAndGet(durationMs);
        }
    }

    private AtomicLong counterFor(ExtractionException.Code code) {
        return failureByCode.computeIfAbsent(code, new java.util.function.Function<ExtractionException.Code, AtomicLong>() {
            @Override
            public AtomicLong apply(ExtractionException.Code k) {
                return new AtomicLong();
            }
        });
    }
}
