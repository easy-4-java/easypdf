package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;

/**
 * Task 3.2：验证 {@link PdfExtractionProperties#renderHtmlColor} 开关已就位且默认关闭。
 * 颜色渲染实际逻辑在 3.3 实现——本测试仅确认契约先行的基础设施。
 */
class RenderHtmlColorSwitchTest {

    @Test
    void defaultsRenderHtmlColorIsFalse() {
        assertThat(PdfExtractionProperties.defaults().renderHtmlColor).isFalse();
    }

    @Test
    void toMarkdownWithPropsProducesSameOutputAsWithout() {
        // 含 <, &, > 的文本——3.3 完成前 renderHtmlColor=true 也不触发转义
        DocumentSection sec = new DocumentSection();
        sec.title = "测试"; sec.level = 1; sec.content = "Tom & Jerry < 5 > 3";

        DocumentStructure doc = new DocumentStructure();
        doc.sections = Collections.singletonList(sec);

        String withoutProps = doc.toMarkdown();
        PdfExtractionProperties props = new PdfExtractionProperties();
        props.renderHtmlColor = true;
        String withProps = doc.toMarkdown(props);

        assertThat(withProps).isEqualTo(withoutProps);
        // 确认原始特殊字符未被转义（3.3 之前的行为）
        assertThat(withProps).contains("Tom & Jerry < 5 > 3");
    }

    @Test
    void toMarkdownWithNullPropsDoesNotThrow() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "空文档";

        String md = doc.toMarkdown(null);
        assertThat(md).isNotNull();
    }

    @Test
    void fullMarkdownWithPropsProducesSameOutputAsWithout() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "标题";
        DocumentSection sec = new DocumentSection();
        sec.title = "标题"; sec.level = 1; sec.content = "a & b < c";
        doc.sections = Collections.singletonList(sec);

        String withoutProps = doc.fullMarkdown();
        PdfExtractionProperties props = new PdfExtractionProperties();
        props.renderHtmlColor = true;
        String withProps = doc.fullMarkdown(props);

        assertThat(withProps).isEqualTo(withoutProps);
    }
}
