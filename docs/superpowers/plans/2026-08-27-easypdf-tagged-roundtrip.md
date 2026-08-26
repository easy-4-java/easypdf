# easypdf Tagged 无损往返计划（Line 2：markdownToPdfTagged + round-trip）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 打通"**我们生成的 PDF 无损取回**"路径：`EasyPdf.markdownToPdfTagged(md)` 生成**带结构树（Tagged PDF）**的文件，`EasyPdf.pdfToStructuredMarkdown(file)` 读回时利用结构角色（H1-H6/Table/L）实现**语义级还原**。对自有 PDF 的往返保真度目标：标题层级/列表/表格 **100%**，正文文本 **100%**（图片以 data URI 保内容）。

**Architecture:**
1. **生成侧**：`HtmlPdfConverter.htmlToPdfTagged(String, OutputStream)` —— 自建 `PdfDocument(writer)` 并 `setTagged()`，再交 `HtmlConverter.convertToPdf(html, pdfDocument, props)`（pdfHTML 会把 HTML 元素语义写入结构树）
2. **读取侧**：Line 1 的 `RuleLayoutAnalyzer` 已接收 `taggedHeadings` 提示；本计划补 `PdfStructureExtractor` 的 Tagged 角色提取（真实文本解析 marked-content：按 mcid 把 PageChunk 关联回结构元素，解决现有 actualText 为空的缺口）
3. **往返验证**：`markdownToPdfTagged(md, out) → pdfToStructuredMarkdown(file)` 归一化断言（去空白/大小写后，标题/表格/列表逐项相等）

**与 Line 1 的关系**：Line 1 管"外来 PDF 尽力而为"（80%→95%），本线管"自有 PDF 100% 往返"，两条腿互补。

**Tech Stack:** iText 7.1.10 + html2pdf 2.1.7（零新依赖）、JUnit 5、Java 8 语法。

## Global Constraints

- 文件均在 `easypdf-xhtml`（生成侧方法放 `io.github.easy4j.pdf.core.convert.HtmlPdfConverter`——core 已有 html2pdf 依赖；门面在 `EasyPdf`）
- **Java 8 语法**；**零新依赖**
- 不破坏既有 API；既有测试全绿
- 提交风格：`feat(tagged): ...`
- 验证命令（3.0.x）：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core,easypdf-xhtml -am clean verify`

---

### Task 1: 生成侧 —— htmlToPdfTagged + markdownToPdfTagged

**Files:**
- Modify: `easypdf-core/src/main/java/io/github/easy4j/pdf/core/convert/HtmlPdfConverter.java`
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/EasyPdf.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/TaggedGenerationTest.java`

**Interfaces:**
- Produces:
  - `HtmlPdfConverter.htmlToPdfTagged(String html, OutputStream out) throws IOException` —— 与 `htmlToPdf` 同字体配置，额外 `pdfDoc.setTagged()`
  - `EasyPdf.markdownToPdfTagged(String markdown, File output)` / `(String markdown, OutputStream out)`

- [x] **Step 1: 写失败测试**

```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

class TaggedGenerationTest {

    @Test
    void markdownToPdfTaggedProducesStructTree() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyPdf.markdownToPdfTagged("# 标题一\n\n正文\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n- 项目", out);
        byte[] bytes = out.toByteArray();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");

        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            assertThat(doc.getStructTreeRoot()).isNotNull();
            assertThat(doc.isTagged()).isTrue();
        }
    }
}
```

- [x] **Step 2: 确认失败**（方法不存在）

- [x] **Step 3: 实现**

```java
// HtmlPdfConverter.java 新增
/** 渲染 HTML 为 Tagged PDF（写入结构树，供无损往返读取）。 */
public static void htmlToPdfTagged(String html, OutputStream out) throws IOException {
    java.util.Objects.requireNonNull(html, "html must not be null");
    java.util.Objects.requireNonNull(out, "out must not be null");
    com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(out);
    com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
    pdfDoc.setTagged();
    com.itextpdf.html2pdf.ConverterProperties props = new com.itextpdf.html2pdf.ConverterProperties();
    props.setFontProvider(FONT_PROVIDER);
    com.itextpdf.html2pdf.HtmlConverter.convertToPdf(html, pdfDoc, props);
}

// EasyPdf.java 新增
/** Markdown → Tagged PDF 文件（无损往返用：pdfToStructuredMarkdown 可语义级还原）。 */
public static void markdownToPdfTagged(String markdown, File output) throws IOException {
    java.util.Objects.requireNonNull(output, "output must not be null");
    try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(output.toPath())) {
        markdownToPdfTagged(markdown, out);
    }
}

/** Markdown → Tagged PDF 输出流。 */
public static void markdownToPdfTagged(String markdown, java.io.OutputStream out) throws IOException {
    java.util.Objects.requireNonNull(markdown, "markdown must not be null");
    java.util.Objects.requireNonNull(out, "out must not be null");
    HtmlPdfConverter.htmlToPdfTagged(MarkdownConverter.mdToHtml(markdown), out);
}
```

> 实施注意：若 `convertToPdf(html, pdfDocument, props)` 在 2.1.7 不写结构树（`isTagged()` 为 false），则回退方案：改用 `HtmlConverter.convertToElements` + 手动 `setTagged` 前置；测试 Step 1 就是守门员，失败即调整实现路径。

- [x] **Step 4: 测试通过** → **Step 5: Commit** `feat(tagged): markdownToPdfTagged writes structure tree`

---

### Task 2: 读取侧 —— mcid 关联真实文本 + round-trip 测试

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractor.java`（Tagged 分支重写：结构树元素携带 mcid → 从 PageModel 按 `(page, mcid)` 取真实 chunk 文本，替代 actualText）
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/TaggedRoundTripTest.java`

**Interfaces:**
- Produces:
  - Tagged 路径：`walk(elem)` 时收集该元素所有后继 mcid；`PageModelListener.collect` 产出的 chunks 按 `(page, mcid)` 建索引 `Map<String, List<PageChunk>>`（key=`page+":"+mcid`）；section.content = 索引文本拼接；`TR`（Table 结构子元素）的 mcid 落入对应 cell
  - 既有 `DocumentStructure` POJO 不动

- [x] **Step 1: 写 round-trip 失败测试（核心验收）**

```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TaggedRoundTripTest {

    @Test
    void roundTripPreservesHeadingsTableAndList(@TempDir File dir) throws Exception {
        String md = "# 合同标题\n\n"
            + "## 第一章 双方义务\n\n甲方应当……乙方应当……\n\n"
            + "| 项目 | 金额 |\n|---|---|\n| 服务费 | 100.00 |\n| 运输费 | 50.00 |\n\n"
            + "- 首要条款\n- 次要条款\n";
        File pdf = new File(dir, "rt.pdf");
        EasyPdf.markdownToPdfTagged(md, pdf);

        DocumentStructure doc = EasyPdf.pdfToStructured(pdf);
        String back = doc.toMarkdown();

        assertThat(back).contains("# 合同标题").contains("## 第一章");
        assertThat(back).contains("甲方应当").contains("乙方应当");
        assertThat(back).contains("| 项目 | 金额 |").contains("100.00").contains("50.00");
        assertThat(back).contains("首要条款").contains("次要条款");
    }
}
```

- [x] **Step 2: 确认失败**（当前 Tagged 路径 actualText 为空 → 断言不过）

- [x] **Step 3: 实现 mcid 关联**
  - `PdfStructureExtractor.extract`：先 `PageModelListener.collect(pdfDoc)` 建 mcid 索引；Tagged 时遍历结构树（H1-H6/TR/TH/TD/P/L/LI 角色），每个 `PdfStructElem` 通过其后继 marked-content 引用取 mcid 集合（`PdfMcr`/`PdfStructElem.getKids()` 递归，`((PdfMcr) kid).getMcid()` + 所属页 ref 匹配 page）；文本=索引拼接；TD→cell、TR→row、首 TR→headers
- [x] **Step 4: round-trip 测试 + 全量回归通过**
- [x] **Step 5: Commit** `feat(tagged): mcid-linked tagged extraction enables lossless round-trip`

---

### Task 3: 三分支同步 + 推送 + 勾选

- [x] **Step 1: 3.0.x 全量 verify**
- [x] **Step 2-7: 同步 1.0.x / 2.0.x（HtmlPdfConverter.java + EasyPdf.java + PdfStructureExtractor.java + 2 个测试）+ verify + commit**
- [x] **Step 8: 推送三分支 + 勾选本计划 + commit**

---

## Self-Review

- **覆盖**：生成侧 Tagged（Task 1，`isTagged()` 断言守门）→ 读取侧 mcid 关联（Task 2，round-trip 验收标题/表格/列表/正文）→ 三分支（Task 3）
- **API 风险预案**：`HtmlConverter.convertToPdf(html, pdfDoc, props)` 是否传播 tagged 由 Task 1 测试把关；不通过则切 `convertToElements` 路径（已写明）
- **依赖**：零新增；`PdfMcr` 在 kernel 7.1.10 `tagging` 包内（实施时 javap 复核 `getMcid()`/`getPageObject()`）
- **边界**：无损承诺仅限"本工具生成的 Tagged PDF"；外来 Tagged PDF 尽力（角色可信但 mcid 结构可能不完整）
