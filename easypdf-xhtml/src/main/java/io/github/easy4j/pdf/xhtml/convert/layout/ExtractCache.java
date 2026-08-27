package io.github.easy4j.pdf.xhtml.convert.layout;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;

/**
 * 提取结果 LRU 缓存：手写 LinkedHashMap（accessOrder），容量默认 16，方法级 synchronized。
 *
 * <p>缓存 key = 绝对路径 + lastModified + length——文件内容变化自然失效。
 * 默认不启用（{@link PdfExtractionProperties#cacheEnabled} 为 false，
 * 行为与历史版本一致）；开启后同一文件的二次 {@code extract} 直接命中、跳过解析。
 *
 * <p>注意：缓存的 {@link DocumentStructure} 为共享实例，调用方不得原地修改
 * （需要变更请自行拷贝）。
 */
public final class ExtractCache {

    private static final int DEFAULT_CAPACITY = 16;
    private static final ExtractCache SHARED = new ExtractCache(DEFAULT_CAPACITY);

    /** extract(File, props) 接入的共享实例。 */
    public static ExtractCache shared() {
        return SHARED;
    }

    /** 缓存 key：绝对路径 + 修改时间 + 字节长度（任一变化即视为新条目）。 */
    public static String keyOf(File pdf) {
        return pdf.getAbsolutePath() + "|" + pdf.lastModified() + "|" + pdf.length();
    }

    private long hits;
    private long misses;
    private final Map<String, DocumentStructure> map;

    public ExtractCache(int capacity) {
        this.map = new LinkedHashMap<String, DocumentStructure>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, DocumentStructure> eldest) {
                return size() > capacity;
            }
        };
    }

    /** 取缓存；命中/未命中分别计入 hits / misses（公开以便观测与测试验证）。 */
    public synchronized DocumentStructure get(String key) {
        DocumentStructure v = map.get(key);
        if (v != null) {
            hits++;
        } else {
            misses++;
        }
        return v;
    }

    public synchronized void put(String key, DocumentStructure doc) {
        map.put(key, doc);
    }

    public synchronized long hits() {
        return hits;
    }

    public synchronized long misses() {
        return misses;
    }

    public synchronized int size() {
        return map.size();
    }

    /** 清空条目并重置命中统计（测试隔离用）。 */
    public synchronized void clear() {
        map.clear();
        hits = 0L;
        misses = 0L;
    }
}
