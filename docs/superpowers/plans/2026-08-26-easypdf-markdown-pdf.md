# easypdf Markdown ↔ PDF 转换实现计划（Phase 2）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 为 easypdf 补齐 Markdown ↔ PDF 快速双向转换，镜像 easydoc 的 Markdown ↔ docx 方案：复用现有 HTML 管线、零新引擎——`markdownToPdf` 走「flexmark 解析 MD → HTML → iText 7 html2pdf → PDF」，`pdfToMarkdown` 走「iText PdfTextExtractor 提取文本 → 结构化 → Markdown」。

**Architecture:** 转换能力全部落在 `easypdf-xhtml` 模块（已具备 html2pdf/jsoup/iText 7 全套依赖，且无 compiler 排除、测试可真实执行；与 easydoc 的 markdown 方案放 easydoc-xhtml 对称）。新增 `io.github.easy4j.pdf.xhtml.convert` 包：`MarkdownConverter`（flexmark mdToHtml + textToMarkdown）、`EasyPdf` 门面（markdownToPdf / pdfToMarkdown）。代码保持 Java 8 语法，flexmark 为 Java 8 库，可直接同步 1.0.x/2.0.x。

**计划更新（2026-08-26，core 重构先行完成）**：core 重构计划（2026-08-26-easypdf-core-pdf.md）已在 `easypdf-core` 建立 `io.github.easy4j.pdf.core.convert.HtmlPdfConverter`（html2pdf + FontProvider 中文字体 + pdfToText）。本计划原 Task 3（在 xhtml 建 HtmlPdfConverter）**取消**，`EasyPdf` 与测试直接复用 core 版 `HtmlPdfConverter`；本计划 Task 1 的 flexmark 依赖已按 core 重构中的修正落地（`flexmark` + `flexmark-ext-tables/strikethrough/tasklist`，`flexmark-html` 在 Maven Central 不存在）。

**Tech Stack:** flexmark-java 0.64.8（core + html）、iText 7.1.10 html2pdf 2.1.7（已有）、JUnit 5 + AssertJ（已有）、Maven 4（3.0.x）/ Maven 3.9.16（1.0.x/2.0.x）。

## Global Constraints

- 代码放 `easypdf-xhtml`，包 `io.github.easy4j.pdf.xhtml.convert`，测试放 `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/`
- **纯新增，零破坏**：不改现有 `WordprocessingMLHtmlTemplate`/`XHTMLImporterUtils` 任何签名；现有测试必须全绿（3.0.x 基线 103 tests）
- **Java 8 语法兼容**（禁 var/List.of/Path.of/switch 表达式）——保证可直接同步 1.0.x（JDK 8）
- 每个 Task 末尾跑 `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`（Maven 4）必须 BUILD SUCCESS
- 测试用 JUnit 5 + AssertJ（easypdf-xhtml 现有框架），命名 `*Test.java`
- 提交信息风格：`feat(markdown): ...`
- 中文字体：`FontProvider.addSystemFonts()` 为主（macOS/Linux 桌面可解析宋体/苹方），并提供 `registerFont(String path)` 扩展点；不做 classpath 打包字体（依赖体积考量，服务器缺字体时由用户注册）

---

### Task 1: flexmark 依赖（根 pom + easypdf-xhtml/pom.xml）

**Files:**
- Modify: `pom.xml`（properties + dependencyManagement）
- Modify: `easypdf-xhtml/pom.xml`（dependencies）

**Interfaces:**
- Produces: `com.vladsch.flexmark:flexmark:0.64.8` 与 `com.vladsch.flexmark:flexmark-html:0.64.8` 可由 easypdf-xhtml 解析（供 Task 2 使用）

- [x] **Step 1: 根 pom 加版本属性**

在 `pom.xml` properties 区（`<jsoup.version>` 附近）加：
```xml
<flexmark.version>0.64.8</flexmark.version>
```

- [x] **Step 2: 根 pom dependencyManagement 加 flexmark**

在 dependencyManagement 的 jsoup 条目附近加：
```xml
<!-- https://mvnrepository.com/artifact/com.vladsch.flexmark/flexmark -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark</artifactId>
    <version>${flexmark.version}</version>
</dependency>
<!-- https://mvnrepository.com/artifact/com.vladsch.flexmark/flexmark-html -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-html</artifactId>
    <version>${flexmark.version}</version>
</dependency>
```

- [x] **Step 3: easypdf-xhtml/pom.xml 加依赖**

在 `</dependencies>` 前加：
```xml
<!-- https://mvnrepository.com/artifact/com.vladsch.flexmark/flexmark -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark</artifactId>
</dependency>
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-html</artifactId>
</dependency>
```

- [x] **Step 4: 验证依赖解析**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml validate 2>&1 | grep -E "ERROR|BUILD"`
Expected: BUILD SUCCESS

- [x] **Step 5: Commit**

```bash
git add pom.xml easypdf-xhtml/pom.xml
git commit -m "feat(markdown): add flexmark-java dependencies for markdown parsing"
```

---

### Task 2: MarkdownConverter（flexmark mdToHtml）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/MarkdownConverter.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/MarkdownConverterTest.java`

**Interfaces:**
- Produces: `public static String mdToHtml(String markdown)` —— CommonMark + GFM 扩展（表格/删除线/任务列表）渲染为 HTML 字符串，供 Task 3/4 的 html2pdf 消费
- Produces: `public static String textToMarkdown(String text)` —— PDF 提取的纯文本 → 简单结构化 Markdown（段落合并、空行分隔），供 Task 4 的 pdfToMarkdown 使用

- [x] **Step 1: 写失败测试**

`MarkdownConverterTest.java`：
```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MarkdownConverterTest {

    @Test
    void mdToHtmlRendersHeadingAndParagraph() {
        String html = MarkdownConverter.mdToHtml("# 标题\n\n正文内容");
        assertThat(html).contains("<h1>").contains("标题").contains("正文内容");
    }

    @Test
    void mdToHtmlRendersTable() {
        String html = MarkdownConverter.mdToHtml("| a | b |\n|---|---|\n| 1 | 2 |");
        assertThat(html).contains("<table>").contains("<th>").contains("a");
    }

    @Test
    void mdToHtmlRendersCodeBlockAndList() {
        String md = "```java\nint x = 1;\n```\n\n- item1\n- item2";
        String html = MarkdownConverter.mdToHtml(md);
        assertThat(html).contains("<pre>").contains("int x = 1;").contains("<ul>").contains("item1");
    }

    @Test
    void textToMarkdownKeepsLinesAndParagraphs() {
        String md = MarkdownConverter.textToMarkdown("第一行\n\n第二行");
        assertThat(md).contains("第一行").contains("第二行");
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=MarkdownConverterTest`
Expected: FAIL（MarkdownConverter 类不存在）

- [x] **Step 3: 实现 MarkdownConverter**

```java
package io.github.easy4j.pdf.xhtml.convert;

import java.util.Arrays;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Markdown 与 HTML/纯文本之间的转换工具。
 */
public final class MarkdownConverter {

    private static final MutableDataSet OPTIONS = new MutableDataSet();

    static {
        OPTIONS.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create()));
        OPTIONS.set(HtmlRenderer.SOFT_BREAK, "<br />");
    }

    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    private MarkdownConverter() {
    }

    /** 将 Markdown 文本渲染为 HTML 字符串（CommonMark + GFM 表格/删除线/任务列表）。 */
    public static String mdToHtml(String markdown) {
        return RENDERER.render(PARSER.parse(markdown));
    }

    /**
     * 将 PDF 提取出的纯文本整理为简单 Markdown：保留段落（空行分隔）与换行结构。
     * 尽力而为——PDF 无结构信息，仅做可读性整理，对齐 pandoc 的定位。
     */
    public static String textToMarkdown(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        return normalized.replaceAll("\n{3,}", "\n\n");
    }
}
```

- [x] **Step 4: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=MarkdownConverterTest`
Expected: PASS（4 tests）

- [x] **Step 5: Commit**

```bash
git add easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/MarkdownConverter.java easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/MarkdownConverterTest.java
git commit -m "feat(markdown): add MarkdownConverter with flexmark mdToHtml and textToMarkdown"
```

---

### Task 3: HtmlPdfConverter（html2pdf 封装 + 中文字体 + pdfToText）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/HtmlPdfConverter.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/HtmlPdfConverterTest.java`

**Interfaces:**
- Consumes: 无（独立）
- Produces:
  - `public static void htmlToPdf(String html, OutputStream out)` —— iText 7 html2pdf，注册系统字体（中文支持）
  - `public static void registerFont(String fontPath)` —— 追加注册字体（服务器无中文字体时的扩展点）
  - `public static String pdfToText(File pdf)` —— 逐页 PdfTextExtractor 提取文本（供 Task 4 pdfToMarkdown 使用）

- [x] **Step 1: 写失败测试**

`HtmlPdfConverterTest.java`：
```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HtmlPdfConverterTest {

    @TempDir
    File tempDir;

    @Test
    void htmlToPdfProducesPdfWithChineseText() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String html = "<html><body><h1>中文标题</h1><p>正文段落 hello</p></body></html>";
        HtmlPdfConverter.htmlToPdf(html, out);

        byte[] bytes = out.toByteArray();
        assertThat(bytes).isNotEmpty();
        // PDF 魔数
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void pdfToTextExtractsContent() throws Exception {
        File pdf = new File(tempDir, "out.pdf");
        String html = "<html><body><h1>Invoice</h1><p>amount 128.00</p></body></html>";
        HtmlPdfConverter.htmlToPdf(html, Files.newOutputStream(pdf.toPath()));

        String text = HtmlPdfConverter.pdfToText(pdf);
        assertThat(text).contains("Invoice").contains("128.00");
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=HtmlPdfConverterTest`
Expected: FAIL（类不存在）

- [x] **Step 3: 实现 HtmlPdfConverter**

```java
package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.font.FontProvider;

/**
 * HTML 字符串与 PDF 之间的转换：html2pdf 渲染 + 文本提取。
 */
public final class HtmlPdfConverter {

    private static final FontProvider FONT_PROVIDER = new FontProvider();

    static {
        // 注册系统字体，保证中文等 CJK 字符可渲染（SimSun/PingFang 等）
        FONT_PROVIDER.addSystemFonts();
    }

    private HtmlPdfConverter() {
    }

    /** 追加注册字体文件（服务器缺少系统字体时的扩展点）。 */
    public static void registerFont(String fontPath) {
        FONT_PROVIDER.addFont(fontPath);
    }

    /** 将 HTML 字符串渲染为 PDF 写入输出流（自动处理中文字体）。 */
    public static void htmlToPdf(String html, OutputStream out) {
        ConverterProperties props = new ConverterProperties();
        props.setFontProvider(FONT_PROVIDER);
        HtmlConverter.convertToPdf(html, out, props);
    }

    /** 从 PDF 文件逐页提取纯文本。 */
    public static String pdfToText(File pdf) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (PdfDocument doc = new PdfDocument(new PdfReader(pdf))) {
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
        }
        return sb.toString();
    }
}
```

- [x] **Step 4: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=HtmlPdfConverterTest`
Expected: PASS（2 tests）。若中文断言失败：检查系统是否有 CJK 字体（macOS 默认有 PingFang；`fc-list :lang=zh` 可查 Linux）

- [x] **Step 5: Commit**

```bash
git add easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/HtmlPdfConverter.java easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/HtmlPdfConverterTest.java
git commit -m "feat(markdown): add HtmlPdfConverter with html2pdf rendering and pdf text extraction"
```

---

### Task 4: EasyPdf 门面（markdownToPdf / pdfToMarkdown）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/EasyPdf.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/EasyPdfTest.java`

**Interfaces:**
- Consumes: `MarkdownConverter.mdToHtml(String)`、`MarkdownConverter.textToMarkdown(String)`、`HtmlPdfConverter.htmlToPdf(String, OutputStream)`、`HtmlPdfConverter.pdfToText(File)`
- Produces:
  - `public static void markdownToPdf(String markdown, File output)` / `(String markdown, OutputStream out)` —— MD → PDF 一站式
  - `public static String pdfToMarkdown(File pdf)` / `(InputStream in)` —— PDF → Markdown（尽力而为）

- [x] **Step 1: 写失败测试**

`EasyPdfTest.java`（`EasyPdf` 直接调用 core 版 `HtmlPdfConverter`，不建 xhtml 版重复类）：
```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EasyPdfTest {

    @TempDir
    File tempDir;

    @Test
    void markdownToPdfProducesPdf() throws Exception {
        File out = new File(tempDir, "doc.pdf");
        String md = "# 合同\n\n甲方：张三\n\n| 项目 | 金额 |\n|---|---|\n| 服务 | 100 |";
        EasyPdf.markdownToPdf(md, out);

        byte[] bytes = Files.readAllBytes(out.toPath());
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void pdfToMarkdownExtractsText() throws Exception {
        File out = new File(tempDir, "roundtrip.pdf");
        EasyPdf.markdownToPdf("# 标题\n\n正文 hello", out);

        String md = EasyPdf.pdfToMarkdown(out);
        assertThat(md).contains("标题").contains("hello");
    }

    @Test
    void pdfToMarkdownAcceptsInputStream() throws Exception {
        File out = new File(tempDir, "stream.pdf");
        EasyPdf.markdownToPdf("流式输入测试", out);

        try (FileInputStream in = new FileInputStream(out)) {
            String md = EasyPdf.pdfToMarkdown(in);
            assertThat(md).contains("流式输入测试");
        }
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=EasyPdfTest`
Expected: FAIL（类不存在）

- [x] **Step 3: 实现 EasyPdf**

```java
package io.github.easy4j.pdf.xhtml.convert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

/**
 * Markdown ↔ PDF 快速转换门面：mdToHtml → html2pdf → PDF；pdfToText → textToMarkdown。
 */
public final class EasyPdf {

    private EasyPdf() {
    }

    /** Markdown 文本 → PDF 文件（自动渲染标题/表格/代码块/列表等 GFM 语法）。 */
    public static void markdownToPdf(String markdown, File output) throws IOException {
        try (OutputStream out = Files.newOutputStream(output.toPath())) {
            markdownToPdf(markdown, out);
        }
    }

    /** Markdown 文本 → PDF 输出流。 */
    public static void markdownToPdf(String markdown, OutputStream out) {
        HtmlPdfConverter.htmlToPdf(MarkdownConverter.mdToHtml(markdown), out);
    }

    /** PDF 文件 → Markdown 文本（尽力而为：文本提取 + 段落整理，结构还原以可读性为准）。 */
    public static String pdfToMarkdown(File pdf) throws IOException {
        return MarkdownConverter.textToMarkdown(HtmlPdfConverter.pdfToText(pdf));
    }

    /** PDF 输入流 → Markdown 文本。 */
    public static String pdfToMarkdown(InputStream in) throws IOException {
        File tmp = File.createTempFile("easypdf-", ".pdf");
        try {
            Files.copy(in, tmp.toPath());
            return pdfToMarkdown(tmp);
        } finally {
            tmp.delete();
        }
    }
}
```

- [x] **Step 4: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=EasyPdfTest`
Expected: PASS（3 tests）

- [x] **Step 5: 全量回归 + Commit**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`
Expected: BUILD SUCCESS，测试计数 = 原 19 + 新 9 = 28

```bash
git add easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/EasyPdf.java easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/EasyPdfTest.java
git commit -m "feat(markdown): add EasyPdf facade for markdown-to-pdf and pdf-to-markdown"
```

---

### Task 5: 3.0.x 全量验证 + 同步 1.0.x/2.0.x

**Files:**
- Modify: 三分支的 `pom.xml`（flexmark 属性 + dependencyManagement）、`easypdf-xhtml/pom.xml`（flexmark 依赖）、新增 5 个 java 文件

**Interfaces:**
- Consumes: Task 1-4 的全部产物
- Produces: 三分支均含 Markdown ↔ PDF 能力且全量 verify 通过

- [x] **Step 1: 3.0.x 全量验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS，总测试数 ≥ 103 + 9 = 112

- [x] **Step 2: 同步到 1.0.x**

```bash
git checkout feature/1.0.x
# 应用：根 pom flexmark 属性 + dependencyManagement；xhtml pom flexmark 依赖；
#       5 个 java 文件（MarkdownConverter/HtmlPdfConverter/EasyPdf + 3 个测试）
```

- [x] **Step 3: 验证 1.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS，总测试数 ≥ 112

- [x] **Step 4: Commit 1.0.x**

```bash
git add -A
git commit -m "feat(markdown): add markdown-to-pdf and pdf-to-markdown (sync from 3.0.x)"
```

- [x] **Step 5: 同步到 2.0.x**（同 Step 2）

- [x] **Step 6: 验证 2.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS，总测试数 ≥ 112

- [x] **Step 7: Commit 2.0.x**（同 Step 4 信息）

- [x] **Step 8: 回 3.0.x 推送三分支**

```bash
git checkout feature/3.0.x
git push origin feature/1.0.x feature/2.0.x feature/3.0.x
```

---

## Self-Review

- **Spec 覆盖**：MD→PDF → Task 2（mdToHtml）+ Task 3（htmlToPdf）+ Task 4（门面）；PDF→MD → Task 3（pdfToText）+ Task 2（textToMarkdown）+ Task 4；库选择 flexmark → Task 1；三分支同步 → Task 5；中文字体 → Task 3 的 FontProvider + registerFont 扩展点
- **占位符扫描**：无 TBD/TODO；所有步骤含完整代码与验证命令
- **类型一致性**：`mdToHtml(String):String`、`htmlToPdf(String, OutputStream):void`、`pdfToText(File):String`、`textToMarkdown(String):String` 在 Task 2-4 中签名一致；测试断言与实现输出一致（`%PDF-` 魔数、`<h1>`/`<table>` 结构）
