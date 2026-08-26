# easypdf-core PDF 重构实现计划（Phase 2：PdfTemplate + html2pdf 管线 + legacy 处置）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 把 easypdf-core 从"docx4j Word 工具 + iText 5 遗留混合体"收敛为纯 PDF 核心：删除 fastpdf 时代的 iText 5 遗留（`io.github.easy4j.pdf.core.**` 孤岛，55 个主类 + 55 个测试 + 3 个配置文件），移除 compiler 排除配置恢复测试执行，建立 `PdfTemplate` 抽象 + `HtmlPdfConverter`（iText 7 html2pdf + 中文字体 FontProvider）管线，用 golden 测试锁定中文 PDF 质量。

**Architecture:** 分四步：① 删除 legacy（core 包是孤岛——已实测跨模块与顶层类零引用，删除安全）；② 恢复 core 测试（33 个真实 JUnit 测试 + 41 个 demo 类甄别）；③ 新建 `HtmlPdfConverter`（`io.github.easy4j.pdf.core.convert`，iText 7 html2pdf 2.1.7 + FontProvider 系统字体，中文不乱码）；④ 新建 `PdfTemplate` 抽象（顶层 `io.github.easy4j.pdf`，`process(String template, Map vars, OutputStream)`，为后续引擎适配器与 webmvc 提供统一基座）。代码 Java 8 语法，直接同步 1.0.x/2.0.x。

**Tech Stack:** iText 7.1.10 html2pdf 2.1.7（依赖已在根 pom dependencyManagement，core pom 需添加 html2pdf 依赖）、JUnit 5 + AssertJ、Maven 4（3.0.x）/ Maven 3.9.16（1.0.x/2.0.x）。

## Global Constraints

- 新代码放 `easypdf-core`：`io.github.easy4j.pdf.core.convert.HtmlPdfConverter` + 顶层 `io.github.easy4j.pdf.PdfTemplate`；测试放 `easypdf-core/src/test/java/io/github/easy4j/pdf/...`（core 的 compiler 排除移除后测试可真实执行）
- **Java 8 语法兼容**（禁 var/List.of/Path.of/switch 表达式）——保证可直接同步 1.0.x（JDK 8）
- 删除范围严格限定：`easypdf-core/src/main/java/io/github/easy4j/pdf/core/**`、引用它的 55 个测试、`easypdf-core/src/main/resources/` 下 `default-config.properties`/`method-mapping.properties`/`property-mapping.properties`/`itext-config.dtd`；**不动**顶层 `Docx4jConstants`/`WordprocessingMLTemplate`/`WordprocessingMLDocxTemplate`（Word 侧 Phase 4 再处置）
- 验证命令（3.0.x）：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am clean verify` 必须 BUILD SUCCESS
- 测试用 JUnit 5 + AssertJ，命名 `*Test.java`
- 提交信息风格：`refactor(core): ...` / `feat(core): ...`

---

### Task A: 删除 legacy iText 5 core 包并移除 compiler 排除

**Files:**
- Delete: `easypdf-core/src/main/java/io/github/easy4j/pdf/core/**`（55 个类）
- Delete: `easypdf-core/src/test/java/**` 中引用 `io.github.easy4j.pdf.core` 的 55 个测试
- Delete: `easypdf-core/src/main/resources/default-config.properties`、`method-mapping.properties`、`property-mapping.properties`、`itext-config.dtd`
- Modify: `easypdf-core/pom.xml`（移除 `<excludes><exclude>**/pdf/core/**</exclude></excludes>` 与 `<testExcludes><exclude>**/*</exclude></testExcludes>`）

**Interfaces:**
- Consumes: 无
- Produces: core 模块无 compiler 排除；`io.github.easy4j.pdf.core.**` 不再存在；剩余代码（顶层 3 类 + io/wml/utils/fonts/handler/bus 包）可独立编译

- [x] **Step 1: 删除 legacy 源码与配置**

```bash
cd /Users/wandl/workspaces/workspace-github-easy-4-java/easypdf
git rm -r easypdf-core/src/main/java/io/github/easy4j/pdf/core
git rm easypdf-core/src/main/resources/default-config.properties \
       easypdf-core/src/main/resources/method-mapping.properties \
       easypdf-core/src/main/resources/property-mapping.properties \
       easypdf-core/src/main/resources/itext-config.dtd
```

- [x] **Step 2: 删除引用 core 包的测试**

```bash
# 先列出再删除
for f in $(find easypdf-core/src/test/java -name "*.java"); do
  grep -q "io.github.easy4j.pdf.core" "$f" && git rm "$f"
done
```

- [x] **Step 3: 编辑 easypdf-core/pom.xml 移除 compiler 排除**

删除 `easypdf-core/pom.xml` 中 `<build><plugins><plugin>maven-compiler-plugin` 配置块（`<excludes>` 与 `<testExcludes>` 整块删除）。

- [x] **Step 4: 验证 core 编译**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core clean compile 2>&1 | grep -E "ERROR|BUILD"`
Expected: BUILD SUCCESS（无 core 包引用残留）

- [x] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(core): remove legacy iText5 pdf.core sources, tests and compiler excludes"
```

---

### Task B: 恢复 core 测试执行（33 个真实测试 + demo 甄别）

**Files:**
- Modify: `easypdf-core/pom.xml`（如 demo 类编译失败，加 testExcludes 排除；对齐 easypdf-xhtml 的处理方式）
- 视结果修复: 编译失败/运行失败的测试

**Interfaces:**
- Consumes: Task A（compiler 排除已移除）
- Produces: core 模块测试真实执行；测试基线记录

- [x] **Step 1: 编译全部保留测试**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core clean test-compile 2>&1 | grep -E "Compiling|ERROR|BUILD"`
Expected: 观察 33 个真实测试 + 41 个 demo 类的编译结果

- [x] **Step 2: 排除编译失败的 demo 类（如存在）**

对编译失败且无 `@Test` 的 demo 类（如 `Docx4j_创建批注_S3_Test`、`CreateWordprocessingMLDocument` 等 docx4j 示例），在 `easypdf-core/pom.xml` 的 compiler 配置加 testExcludes 精准排除（保留文件）。

- [x] **Step 3: 运行测试**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core clean test 2>&1 | grep -E "Tests run:|BUILD|FAIL"`
Expected: 真实测试运行；失败项进入 Step 4

- [x] **Step 4: 修复失败测试（如有）**

对每个失败：读取 surefire 报告区分环境性问题（字体/平台路径）与真实缺陷；修复测试或测试资源，**不修改生产逻辑**；重跑直至通过。

- [x] **Step 5: 记录基线 + Commit**

```bash
git add -A
git commit -m "refactor(core): restore test execution after legacy removal"
```

---

### Task C: HtmlPdfConverter（iText 7 html2pdf + 中文字体 FontProvider）+ golden 测试

**Files:**
- Modify: `easypdf-core/pom.xml`（添加 html2pdf 依赖；根 pom dependencyManagement 已有 2.1.7）
- Create: `easypdf-core/src/main/java/io/github/easy4j/pdf/core/convert/HtmlPdfConverter.java`
- Test: `easypdf-core/src/test/java/io/github/easy4j/pdf/core/convert/HtmlPdfConverterTest.java`

**Interfaces:**
- Produces:
  - `public static void htmlToPdf(String html, OutputStream out)` —— html2pdf 渲染，FontProvider 注册系统字体（中文支持）
  - `public static void registerFont(String fontPath)` —— 服务器缺字体时的扩展点
  - `public static String pdfToText(File pdf)` —— 逐页文本提取（供 PdfTemplate 与 Markdown 转换复用）

- [x] **Step 1: core pom 添加 html2pdf 依赖**

在 `easypdf-core/pom.xml` 的 iText 依赖区添加：
```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>html2pdf</artifactId>
</dependency>
```

- [x] **Step 2: 写失败测试（golden：中文 PDF）**

`HtmlPdfConverterTest.java`：
```java
package io.github.easy4j.pdf.core.convert;

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
        String html = "<html><body><h1>中文合同标题</h1><p>正文段落 hello</p></body></html>";
        HtmlPdfConverter.htmlToPdf(html, out);

        byte[] bytes = out.toByteArray();
        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void pdfToTextExtractsChineseAndLatin() throws Exception {
        File pdf = new File(tempDir, "golden.pdf");
        String html = "<html><body><h1>发票 Invoice</h1><p>金额 amount 128.00</p></body></html>";
        HtmlPdfConverter.htmlToPdf(html, Files.newOutputStream(pdf.toPath()));

        String text = HtmlPdfConverter.pdfToText(pdf);
        assertThat(text).contains("发票").contains("Invoice").contains("128.00");
    }
}
```

- [x] **Step 3: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am test -Dtest=HtmlPdfConverterTest`
Expected: FAIL（类不存在）

- [x] **Step 4: 实现 HtmlPdfConverter**

```java
package io.github.easy4j.pdf.core.convert;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.font.FontProvider;

/**
 * HTML 字符串与 PDF 之间的转换：html2pdf 渲染 + 文本提取。
 * 静态 FontProvider 注册系统字体，保证中文等 CJK 字符可渲染。
 */
public final class HtmlPdfConverter {

    private static final FontProvider FONT_PROVIDER = new FontProvider();

    static {
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

- [x] **Step 5: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am test -Dtest=HtmlPdfConverterTest`
Expected: PASS（2 tests，中文断言通过）

- [x] **Step 6: Commit**

```bash
git add easypdf-core/pom.xml easypdf-core/src/main/java/io/github/easy4j/pdf/core/convert/HtmlPdfConverter.java easypdf-core/src/test/java/io/github/easy4j/pdf/core/convert/HtmlPdfConverterTest.java
git commit -m "feat(core): add HtmlPdfConverter with html2pdf and Chinese font support"
```

---

### Task D: PdfTemplate 抽象

**Files:**
- Create: `easypdf-core/src/main/java/io/github/easy4j/pdf/PdfTemplate.java`
- Test: `easypdf-core/src/test/java/io/github/easy4j/pdf/PdfTemplateTest.java`

**Interfaces:**
- Consumes: `HtmlPdfConverter.htmlToPdf(String, OutputStream)`
- Produces:
  - `public abstract class PdfTemplate` —— `public abstract void process(String template, Map<String, Object> variables, OutputStream out) throws Exception;` + 便捷方法 `public ByteArrayOutputStream process(String template, Map<String, Object> variables) throws Exception`
  - 后续引擎适配器（freemarker 等）继承此抽象，`render(template, vars)` 产出 HTML 后委托 `HtmlPdfConverter.htmlToPdf`

- [x] **Step 1: 写失败测试**

`PdfTemplateTest.java`：
```java
package io.github.easy4j.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PdfTemplateTest {

    /** 最小实现：把变量渲染进 HTML 后交给 HtmlPdfConverter。 */
    static class HtmlPdfTemplateImpl extends PdfTemplate {
        @Override
        public void process(String template, Map<String, Object> variables, OutputStream out) throws Exception {
            String html = "<html><body><h1>" + variables.get("title") + "</h1></body></html>";
            io.github.easy4j.pdf.core.convert.HtmlPdfConverter.htmlToPdf(html, out);
        }
    }

    @Test
    void processWritesPdfToOutputStream() throws Exception {
        PdfTemplate template = new HtmlPdfTemplateImpl();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "合同 Contract");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        template.process("tpl", vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void processConvenienceMethodReturnsByteArray() throws Exception {
        PdfTemplate template = new HtmlPdfTemplateImpl();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "报告 Report");

        ByteArrayOutputStream out = template.process("tpl", vars);
        assertThat(out.toByteArray()).isNotEmpty();
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am test -Dtest=PdfTemplateTest`
Expected: FAIL（PdfTemplate 不存在）

- [x] **Step 3: 实现 PdfTemplate**

```java
package io.github.easy4j.pdf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * PDF 模板抽象：引擎适配器渲染模板为 HTML 后输出 PDF。
 * 对齐 easydoc 的 WordprocessingMLTemplate 抽象，但输出目标为 PDF。
 */
public abstract class PdfTemplate {

    /**
     * 渲染模板并输出 PDF。
     *
     * @param template  模板内容/路径
     * @param variables 模板变量
     * @param out       PDF 输出流
     * @throws Exception 渲染或转换异常
     */
    public abstract void process(String template, Map<String, Object> variables, OutputStream out) throws Exception;

    /** 便捷方法：渲染模板并返回 PDF 字节。 */
    public ByteArrayOutputStream process(String template, Map<String, Object> variables) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        process(template, variables, out);
        return out;
    }
}
```

- [x] **Step 4: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am test -Dtest=PdfTemplateTest`
Expected: PASS（2 tests）

- [x] **Step 5: 全量回归 + Commit**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am clean verify`
Expected: BUILD SUCCESS

```bash
git add easypdf-core/src/main/java/io/github/easy4j/pdf/PdfTemplate.java easypdf-core/src/test/java/io/github/easy4j/pdf/PdfTemplateTest.java
git commit -m "feat(core): add PdfTemplate abstraction for PDF template rendering"
```

---

### Task E: 3.0.x 全量验证 + 同步 1.0.x/2.0.x + 推送

**Files:**
- 三分支同步：Task A-D 的全部变更（core 包删除、pom 调整、新增 4 个 java 文件）

**Interfaces:**
- Consumes: Task A-D 产物
- Produces: 三分支均具备 PdfTemplate + HtmlPdfConverter 管线且全量 verify 通过

- [x] **Step 1: 3.0.x 全量验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS（core 测试 + xhtml 19 + engine 测试全绿）

- [x] **Step 2: 同步到 1.0.x**

```bash
git checkout feature/1.0.x
# 应用：删除 core 包/测试/配置；core pom 移除排除 + 加 html2pdf；新增 HtmlPdfConverter/PdfTemplate + 测试
```

- [x] **Step 3: 验证 1.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS

- [x] **Step 4: Commit 1.0.x**

```bash
git add -A
git commit -m "refactor(core): sync PDF core rework from 3.0.x — remove iText5 legacy, add PdfTemplate and HtmlPdfConverter"
```

- [x] **Step 5: 同步到 2.0.x**（同 Step 2）

- [x] **Step 6: 验证 2.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS

- [x] **Step 7: Commit 2.0.x**（同 Step 4 信息）

- [x] **Step 8: 回 3.0.x 推送三分支**

```bash
git checkout feature/3.0.x
git push origin feature/1.0.x feature/2.0.x feature/3.0.x
```

---

## Self-Review

- **Spec 覆盖**：legacy 处置 → Task A（删除孤岛 core 包）+ Task B（恢复测试）；html2pdf 管线 → Task C；中文字体 FontProvider → Task C Step 4（静态 FontProvider + addSystemFonts + registerFont 扩展点）；PdfTemplate 抽象 → Task D；golden 测试 → Task C（中文 PDF 魔数 + 文本提取断言）；三分支同步 → Task E
- **占位符扫描**：无 TBD/TODO；Task B Step 2/4 为条件性步骤（demo 是否编译失败、测试是否失败不可预知），非占位符
- **类型一致性**：`htmlToPdf(String, OutputStream):void`、`pdfToText(File):String`、`registerFont(String):void` 在 Task C 定义、Task D 测试复用一致；`PdfTemplate.process(String, Map, OutputStream)` 与便捷方法签名在 Task D 内一致
- **与 Markdown 计划的衔接**：Markdown Plan（2026-08-26-easypdf-markdown-pdf.md）Task 3 原计划在 xhtml 建 HtmlPdfConverter——本计划在 core 建立后，Markdown 执行时改为复用 core 版（xhtml 不建重复类，EasyPdf 直接调 `io.github.easy4j.pdf.core.convert.HtmlPdfConverter`）
