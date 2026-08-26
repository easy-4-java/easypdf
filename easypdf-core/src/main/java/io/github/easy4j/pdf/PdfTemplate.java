package io.github.easy4j.pdf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;

/**
 * PDF 模板抽象：引擎适配器渲染模板为 HTML 后输出 PDF。
 * 对齐 easydoc 的 WordprocessingMLTemplate 抽象，但输出目标为 PDF。
 */
public abstract class PdfTemplate {

    /**
     * 渲染模板并输出 PDF。
     *
     * @param template  模板内容/路径，不能为 null
     * @param variables 模板变量，不能为 null
     * @param out       PDF 输出流，不能为 null
     * @throws Exception 渲染或转换异常
     */
    public abstract void process(String template, Map<String, Object> variables, OutputStream out) throws Exception;

    /** 便捷方法：渲染模板并返回 PDF 字节。 */
    public ByteArrayOutputStream process(String template, Map<String, Object> variables) throws Exception {
        Objects.requireNonNull(template, "template must not be null");
        Objects.requireNonNull(variables, "variables must not be null");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        process(template, variables, out);
        return out;
    }
}
