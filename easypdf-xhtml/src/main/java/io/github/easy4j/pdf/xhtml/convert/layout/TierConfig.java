package io.github.easy4j.pdf.xhtml.convert.layout;

/**
 * 规则引擎（RuleLayoutAnalyzer）阈值配置 POJO：将原散落的分析器魔法数字
 * （分栏间隙 / 流式表格对齐容差 / 封面艺术字 run / 标题因子与档位）收敛为可注入字段。
 * 默认值即历史硬编码行为；{@link #DEFAULT} 为进程级缺省单例，
 * {@link #from(PdfExtractionProperties)} 负责从提取属性做同名字段映射。
 */
public final class TierConfig {

    /** 分栏检测的最小空白间隙（pt），原静态常量 COLUMN_GAP=55f。 */
    public float columnGapPt = 55f;
    /** 流式表格列起始 x 的跨行对齐容差（pt），原硬编码 ±6f。 */
    public float streamAlignTolPt = 6f;
    /** 封面艺术字 run 的最少连续行数，原硬编码 2。 */
    public float coverRunMinLines = 2f;
    /** 封面艺术字相对次大字号的比例阈值，原硬编码 1.5f。 */
    public float coverRatio = 1.5f;
    /** 标题判定因子：字号 ≥ 正文×headFactor 视为候选标题，原 HEAD_FACTOR=1.22f。 */
    public float headFactor = 1.22f;
    /** 候选标题字号最多档位数（超出降为正文），原硬编码 3。 */
    public int maxHeadingTiers = 3;

    /** 进程级缺省配置（字段值=历史硬编码行为）。 */
    public static TierConfig DEFAULT = new TierConfig();

    /**
     * 从提取属性映射同名字段；null 安全（返回 {@link #DEFAULT}）。
     * 注意 {@code PdfExtractionProperties.coverRunMinLines} 为 int，此处拓宽为 float。
     */
    public static TierConfig from(PdfExtractionProperties p) {
        if (p == null) {
            return DEFAULT;
        }
        TierConfig t = new TierConfig();
        t.columnGapPt = p.columnGapPt;
        t.streamAlignTolPt = p.streamAlignTolPt;
        t.coverRunMinLines = p.coverRunMinLines;
        t.coverRatio = p.coverRatio;
        t.headFactor = p.headFactor;
        t.maxHeadingTiers = p.maxHeadingTiers;
        return t;
    }
}
