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

    public static PdfExtractionProperties defaults() {
        return new PdfExtractionProperties();
    }
}
