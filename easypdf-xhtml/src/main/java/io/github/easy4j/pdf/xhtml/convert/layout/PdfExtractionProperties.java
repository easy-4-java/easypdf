package io.github.easy4j.pdf.xhtml.convert.layout;

/**
 * PDF 提取配置：引擎选择（AUTO/RULE/REST）与 REST 参数。
 */
public final class PdfExtractionProperties {

    public enum Engine {
        /** REST 可用则用 REST，否则回退 RULE（默认）。 */
        AUTO,
        /** 仅规则引擎（Tier1+2，零外部依赖）。 */
        RULE,
        /** 仅 REST 布局服务（Tier3），不可用即失败。 */
        REST
    }

    public Engine engine = Engine.AUTO;
    public String restEndpoint;
    public int restTimeoutMillis = 10000;
    /**
     * 中英文字间空格判定系数：行内相邻 chunk 净间隙 > 前一 chunk 字号 × 该系数 且两侧均为拉丁字符时，
     * 判定为词间空格补一个空格。默认 0.22（Round3 前的硬编码行为）。
     */
    public float cjkGapFactor = 0.22f;

    // ---- Round 3 追加属性（默认值=现行为）----

    /** 是否启用提取结果 LRU 缓存（共享实例，容量 16；key 含路径/mtime/长度）。 */
    public boolean cacheEnabled = false;
    /** 标题判定因子：字号 ≥ 正文×headFactor 视为候选标题。 */
    public float headFactor = 1.22f;
    /** 候选标题字号最多档位数（超出降为正文）。 */
    public int maxHeadingTiers = 3;
    /** 封面艺术字 run 的最少连续行数。 */
    public int coverRunMinLines = 2;
    /** 封面艺术字相对次大字号的比例阈值。 */
    public float coverRatio = 1.5f;
    /** 分栏检测的最小空白间隙（pt）。 */
    public float columnGapPt = 55f;
    /** 流式表格列起始 x 的跨行对齐容差（pt）。 */
    public float streamAlignTolPt = 6f;
    /** REST 失败重试次数（对 429/5xx/IOException 生效），指数退避 base 500ms，上限 3。 */
    public int restRetries = 0;

    public static PdfExtractionProperties defaults() {
        return new PdfExtractionProperties();
    }
}
