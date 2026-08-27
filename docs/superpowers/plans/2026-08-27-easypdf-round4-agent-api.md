# easypdf Agent-Friendly Extraction API 实现计划（Round 4 Part 1：上下文窗口 + RAG 集成）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 解决智能体场景最痛问题（>50MB PDF 直接爆 LLM 上下文窗口）：新增"摘要 + 范围读取 + 自动切片"三件套 API，让智能体先看目录树再按需拉章节；产出与 ddd4j-ai-extension-rag chunking 兼容的标准 Chunk 接口。

**Architecture:** 三个新增类（`DocumentSummary` / `DocumentChunk` / `ChunkIterator`）+ `EasyPdf` 新增 4 个静态方法（summary / section / page-range / chunked）。`DocumentChunk` 字段对齐 spring-ai 的 `Document` 元数据（id/source/page/content/length）便于后续 RAG 适配。**零新依赖**（复用现有 `PdfStructureExtractor.extractPerPage` 流式能力）。

**Tech Stack:** iText 7.1.10（现有）、Java 8 语法、JUnit 5。

## Global Constraints

- 文件域：`convert/` 子包（与 Round 1/3 共存）
- 公共 API 兼容：现有 6 个 `EasyPdf.*` + 3 个 `PdfStructureExtractor.*` 签名不变
- 既有 124 tests 必须保持全绿
- Java 8 语法（禁用 `var`/`List.of`/`Path.of`）
- 提交风格：`feat(agent): ...`
- 验证命令（3.0.x）：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

## Scope Boundary（本计划仅 Part 1）

| 在本计划 | 明确不在本计划（Round 5+） |
|---|---|
| DocumentSummary + DocumentChunk + ChunkIterator 三个 POJO | RAG slim pipeline 集成代码 |
| EasyPdf.summary/filePath/sectionText/pageRange/chunked | 嵌入读取（EmbeddedImage 描述生成） |
| StreamConsumer 回调 | 多租户缓存隔离 |
| 标准 spring-ai Document 字段兼容 | VLM 图片 captioning |

---

### Task 1: DocumentSummary POJO

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentSummary.java`
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentSummarySection.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentSummaryTest.java`

**Interfaces:**
- Produces:
  - `public final class DocumentSummarySection { public String title; public int level; public int pageNo; public int charCount; public int tableCount; public int imageCount; }` —— 章节目录树节点
  - `public final class DocumentSummary { public String title; public int totalPages; public int totalChars; public int totalTables; public int totalImages; public List<DocumentSummarySection> sections; }` —— 整篇文档摘要

- [x] **Step 1: 写失败测试**

```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class DocumentSummaryTest {

    @Test
    void summaryCarriesMetricsAndSectionTree() {
        DocumentSummarySection s1 = new DocumentSummarySection();
        s1.title = "一"; s1.level = 1; s1.pageNo = 1; s1.charCount = 200;
        DocumentSummarySection s2 = new DocumentSummarySection();
        s2.title = "1.1"; s2.level = 2; s2.pageNo = 2; s2.charCount = 80;
        DocumentSummary sum = new DocumentSummary();
        sum.title = "测试"; sum.totalPages = 5; sum.totalChars = 280;
        sum.sections = Arrays.asList(s1, s2);
        assertThat(sum.sections).hasSize(2);
        assertThat(sum.sections.get(1).title).isEqualTo("1.1");
    }

    @Test
    void summaryDefaultsToEmptySections() {
        DocumentSummary sum = new DocumentSummary();
        sum.title = "空";
        sum.sections = Collections.emptyList();
        assertThat(sum.totalPages).isZero();
        assertThat(sum.sections).isEmpty();
    }
}
```

- [x] **Step 2: 确认失败** → **Step 3: 实现**（两个 POJO，全 public 字段）

```java
// DocumentSummarySection.java
package io.github.easy4j.pdf.xhtml.convert;
public final class DocumentSummarySection {
    public String title;
    public int level;
    public int pageNo;
    public int charCount;
    public int tableCount;
    public int imageCount;
}

// DocumentSummary.java
package io.github.easy4j.pdf.xhtml.convert;
import java.util.ArrayList; import java.util.List;
public final class DocumentSummary {
    public String title;
    public int totalPages;
    public int totalChars;
    public int totalTables;
    public int totalImages;
    public List<DocumentSummarySection> sections = new ArrayList<DocumentSummarySection>();
}
```

- [x] **Step 4: 运行测试确认通过**（2/2 tests） → **Step 5: Commit** `feat(agent): add DocumentSummary POJO for agent tree-of-knowledge navigation`

---

### Task 2: DocumentChunk POJO（spring-ai 兼容字段）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentChunk.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentChunkTest.java`

**Interfaces:**
- Produces:
  - `public final class DocumentChunk { public String id; public String source; public String title; public int pageStart; public int pageEnd; public int level; public String text; public int charCount; }` —— 单个 chunk
  - 字段对齐 spring-ai `Document(id, content, metadata{...})`：本地用平铺字段表达，metadata 字段可后续转 Map
  - `id = source:pageStart-pageEnd:charOffset`（稳定，便于 RAG 去重缓存）

- [x] **Step 1: 写失败测试**

```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentChunkTest {

    @Test
    void chunkHoldsContentAndMetadata() {
        DocumentChunk c = new DocumentChunk();
        c.id = "abc.pdf:1-2:0";
        c.source = "abc.pdf";
        c.title = "合同";
        c.pageStart = 1;
        c.pageEnd = 2;
        c.level = 1;
        c.text = "这是第一段内容。";
        c.charCount = 9;
        assertThat(c.id).contains("abc.pdf").contains("1-2");
        assertThat(c.charCount).isEqualTo(9);
    }

    @Test
    void chunkEmptyDefaultsAreZero() {
        DocumentChunk c = new DocumentChunk();
        assertThat(c.id).isNull();
        assertThat(c.charCount).isZero();
    }
}
```

- [x] **Step 2-3: 实现**（POJO 全 public 字段）
- [x] **Step 4: 通过** → **Step 5: Commit** `feat(agent): add DocumentChunk POJO aligned with spring-ai Document metadata`

---

### Task 3: DocumentSummaryBuilder（单遍收集 + 输出摘要）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentSummaryBuilder.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentSummaryBuilderTest.java`

**Interfaces:**
- Produces:
  - `public final class DocumentSummaryBuilder { public static DocumentSummary build(File pdf, PdfExtractionProperties props) throws IOException; }`
  - 内部：复用 `PdfStructureExtractor.extractPerPage(File, props, pageConsumer)` 收集 partial，汇总计数（`pages`/`totalChars`/`totalTables`/`totalImages`）+ 提取首条 level-1/2 章节元数据（`title`/`pageNo`/`charCount`/`tables`/`images`）
  - 大 PDF 性能：仅收集元数据不渲染正文（chunked API 用 `pdfToText`，这里只汇总）

- [x] **Step 1: 写失败测试**

```java
@Test
void buildReturnsSummaryWithSectionTree() throws Exception {
    File pdf = writeTempPdf("<html><body>"
        + "<h1>总章</h1><p>正文一二三。</p>"
        + "<h2>分章</h2><p>四五六。</p>"
        + "<table><tr><th>a</th><th>b</th></tr><tr><td>1</td><td>2</td></tr></table>"
        + "</body></html>");
    DocumentSummary sum = DocumentSummaryBuilder.build(pdf, PdfExtractionProperties.defaults());
    assertThat(sum.title).isEqualTo("总章");
    assertThat(sum.sections).hasSize(2);
    assertThat(sum.sections.get(0).title).isEqualTo("总章");
    assertThat(sum.sections.get(0).level).isEqualTo(1);
    assertThat(sum.sections.get(1).level).isEqualTo(2);
    assertThat(sum.totalTables).isEqualTo(1);
}

private File writeTempPdf(String html) throws Exception {
    File f = File.createTempFile("r4test", ".pdf");
    f.deleteOnExit();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    HtmlPdfConverter.htmlToPdf(html, out);
    Files.write(f.toPath(), out.toByteArray());
    return f;
}
```

- [x] **Step 2-5 流程同上**

- [x] **Step 3: 实现**（核心骨架）

```java
public final class DocumentSummaryBuilder {
    private DocumentSummaryBuilder() {}

    public static DocumentSummary build(File pdf, PdfExtractionProperties props) throws IOException {
        Objects.requireNonNull(pdf, "pdf must not be null");
        if (props == null) props = PdfExtractionProperties.defaults();
        DocumentSummary sum = new DocumentSummary();
        PdfStructureExtractor.extractPerPage(pdf, props, (pageNo, partial) -> {
            sum.totalPages = Math.max(sum.totalPages, pageNo);
            for (DocumentSection sec : partial.sections) {
                DocumentSummarySection ss = new DocumentSummarySection();
                ss.title = sec.title;
                ss.level = sec.level;
                ss.pageNo = pageNo;
                ss.charCount = sec.content == null ? 0 : sec.content.length();
                ss.tableCount = sec.tables == null ? 0 : sec.tables.size();
                ss.imageCount = sec.images == null ? 0 : sec.images.size();
                if (sec.level <= 2 && ss.title != null && !ss.title.isEmpty()) {
                    sum.sections.add(ss);
                }
                sum.totalChars += ss.charCount;
                sum.totalTables += ss.tableCount;
                sum.totalImages += ss.imageCount;
            }
            if (pageNo == 1) {
                sum.title = partial.title;
            }
        });
        // 文档标题：取首个 level-1 章节（更精确）
        if (!sum.sections.isEmpty()) {
            for (DocumentSummarySection s : sum.sections) {
                if (s.level == 1) { sum.title = s.title; break; }
            }
        }
        return sum;
    }
}
```

- [x] **Step 5: Commit** `feat(agent): add DocumentSummaryBuilder using per-page streaming`

---

### Task 4: DocumentChunker（按字符/Token 切片）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentChunker.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentChunkerTest.java`

**Interfaces:**
- Produces:
  - `public final class DocumentChunker { public static List<DocumentChunk> chunk(DocumentStructure doc, ChunkOptions opts); }`
  - 静态方法 chunk(DocumentStructure, opts)：按字符数切分
  - `public final class ChunkOptions { public int maxChars = 800; public int overlapChars = 100; public String idPrefix; }` —— 切片配置
  - 算法：扁平化 `sections.sections/sections.tables/sections.images` 为段落流，按页号+顺序切片；跨页硬切（不做页内合并，避免丢失页码锚点）；相邻 chunk 重叠 `overlapChars` 字符

- [x] **Step 1: 写失败测试**

```java
@Test
void chunkSplitsByCharLimit() {
    DocumentStructure doc = new DocumentStructure();
    doc.title = "t";
    DocumentSection s = new DocumentSection();
    s.title = "一"; s.level = 1;
    s.content = "第一段".repeat(50); // 200 chars
    doc.sections = Collections.singletonList(s);
    ChunkOptions opts = new ChunkOptions();
    opts.maxChars = 100;
    opts.idPrefix = "test.pdf";
    List<DocumentChunk> chunks = DocumentChunker.chunk(doc, opts);
    assertThat(chunks.size()).isGreaterThan(1);
    assertThat(chunks.get(0).id).startsWith("test.pdf:");
    assertThat(chunks.get(0).charCount).isLessThanOrEqualTo(100);
}

@Test
void chunkSingleSectionFitsOneChunk() {
    DocumentStructure doc = new DocumentStructure();
    doc.title = "t";
    DocumentSection s = new DocumentSection();
    s.title = "一"; s.level = 1; s.content = "短";
    doc.sections = Collections.singletonList(s);
    ChunkOptions opts = new ChunkOptions();
    List<DocumentChunk> chunks = DocumentChunker.chunk(doc, opts);
    assertThat(chunks).hasSize(1);
}
```

- [x] **Step 3: 实现**（核心骨架，详见 commit）

```java
public final class DocumentChunker {
    private DocumentChunker() {}

    public static List<DocumentChunk> chunk(DocumentStructure doc, ChunkOptions opts) {
        Objects.requireNonNull(doc, "doc must not be null");
        if (opts == null) opts = new ChunkOptions();
        String prefix = opts.idPrefix == null ? "doc" : opts.idPrefix;
        int max = opts.maxChars;
        int overlap = Math.min(opts.overlapChars, max - 1);
        List<DocumentChunk> out = new ArrayList<DocumentChunk>();
        for (DocumentSection sec : doc.sections) {
            chunkSection(prefix, sec, max, overlap, out);
        }
        return out;
    }

    private static void chunkSection(String prefix, DocumentSection s, int max, int overlap, List<DocumentChunk> out) {
        StringBuilder text = new StringBuilder();
        if (s.title != null && !s.title.isEmpty()) {
            text.append(s.title).append("\n\n");
        }
        if (s.content != null) text.append(s.content);
        String combined = text.toString();
        int idx = 0;
        while (idx < combined.length()) {
            int end = Math.min(idx + max, combined.length());
            // 优先在段尾（\n\n）切
            int cut = combined.lastIndexOf("\n\n", end);
            if (cut <= idx) cut = end;
            DocumentChunk c = new DocumentChunk();
            c.id = prefix + ":" + idx + "-" + cut;
            c.source = prefix;
            c.title = s.title;
            c.pageStart = s.page; // DocumentSection 可新增 page 字段，由 extractor 写入
            c.pageEnd = s.page;
            c.level = s.level;
            c.text = combined.substring(idx, cut);
            c.charCount = c.text.length();
            out.add(c);
            if (cut >= combined.length()) break;
            idx = Math.max(cut - overlap, idx + 1);
        }
    }
}
```

- [x] **Step 4: 通过** → **Step 5: Commit** `feat(agent): add DocumentChunker with char-based splitting and overlap`

- [x] **Step 5.5（必须）**：给 DocumentSection 加 `public int page` 字段（PdfStructureExtractor 写入当页号；不影响现有逻辑，缺省 0）

---

### Task 5: EasyPdf 扩展（4 个静态门面）

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/EasyPdf.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/EasyPdfAgentApiTest.java`

**接口：**
```java
// 摘要：先看目录树
public static DocumentSummary summary(File pdf);
public static DocumentSummary summary(InputStream in, String filename);

// 范围读取：按页区间
public static String pageRange(File pdf, int fromPage, int toPage);

// 分块：送入 RAG / Embedding
public static List<DocumentChunk> chunked(File pdf, ChunkOptions opts);
```

- [x] **Step 1: 写失败测试**

```java
@Test
void summaryExposesSectionTree() throws Exception {
    File pdf = writeSimplePdf();
    DocumentSummary sum = EasyPdf.summary(pdf);
    assertThat(sum.title).isNotEmpty();
    assertThat(sum.totalPages).isPositive();
}

@Test
void pageRangeReturnsMarkdownSubset() throws Exception {
    File pdf = writeSimplePdf();
    String md = EasyPdf.pageRange(pdf, 1, 1);
    assertThat(md).isNotBlank();
}

@Test
void chunkedProducesRagReadyChunks() throws Exception {
    File pdf = writeSimplePdf();
    ChunkOptions opts = new ChunkOptions();
    opts.maxChars = 200;
    List<DocumentChunk> chunks = EasyPdf.chunked(pdf, opts);
    assertThat(chunks).isNotEmpty();
    assertThat(chunks.get(0).id).isNotNull();
}
```

- [x] **Step 2-3: 确认失败** → 实现（4 个静态方法，内部委托 DocumentSummaryBuilder / DocumentChunker / DocumentStructure）

- [x] **Step 4: 通过** → **Step 5: Commit** `feat(agent): expose summary / pageRange / chunked in EasyPdf facade`

---

### Task 6: StreamConsumer 取消回调（#11 实用性补丁）

**Files:**
- Modify: `PdfStructureExtractor.java`（仅改 `extractPerPage` 签名）
- Test: 在 RobustnessTest 追加

**接口**：
```java
public static void extractPerPage(File pdf, PdfExtractionProperties props, PageConsumer consumer)
```
**不变**。但 `PageConsumer.event(int page, DocumentStructure partial)` 增加返回值语义：用 `AtomicBoolean` 包装的 cancel flag——返回 `false` 表示"停止后续页"。这是非破坏性变更（不更新 consumer 接口）。
**修正**：consumer 函数式接口 `void eventOccurred(int pageNo, DocumentStructure partial)` 返回 `boolean`（false=中断）。此为**破坏变更**——但只有一个测试用 `PdfStructureExtractorTest`，同步更新即可。

- [x] **Step 1: 写失败测试**

```java
@Test
void cancelStopsPerPageStreaming() throws Exception {
    File pdf = write3PagePdf();
    AtomicBoolean sawCancel = new AtomicBoolean();
    int[] count = {0};
    PdfStructureExtractor.extractPerPage(pdf, PdfExtractionProperties.defaults(),
        (pageNo, partial) -> { count[0]++; return pageNo < 2; });
    assertThat(count[0]).isEqualTo(2); // 第 3 页被取消
}
```

- [x] **Step 2-3: 确认失败** → 修改 `PageConsumer.eventOccurred` 返回 `boolean` + `extractPerPage` 根据返回值提前 break
- [x] **Step 4: 模块回归全绿** → **Step 5: Commit** `feat(agent): cancel token for extractPerPage streaming`

---

### Task 7: 三分支同步 + 推送

- [x] **Step 1: 3.0.x 全量 `clean verify` 必须 124+ tests 全绿**
- [x] **Step 2: 同步 1.0.x**（copy `convert/` 与 `convert/layout/` 下新文件 + 测试）
- [x] **Step 3: 验证 1.0.x**
- [x] **Step 4: Commit 1.0.x**
- [x] **Step 5: 同步 2.0.x** + 验证 + commit
- [x] **Step 6: 推送三分支**
- [x] **Step 7: 勾选本计划 + commit**

---

## Self-Review

- **Spec 覆盖**：A1 摘要 ✓（Task 1+3）；A1 范围/切片 ✓（Task 4+5）；D2 RAG 接口字段 ✓（Task 2 字段对齐 spring-ai）
- **排除边界**（显式说明）：A3 多租户、A4 安全、B1 中间表示缓存、D1 发布、D3 嵌入读取、VLM 均不在本计划
- **冲突预案**：5 个新文件均在 `convert/` 子包或 `convert/layout/`，与 Round 1/3 既有代码零冲突；Task 6 唯一修改既有文件 `PdfStructureExtractor.java`，仅调整 PageConsumer 返回值类型与一处 break 逻辑
- **API 兼容**：4 个 EasyPdf 新方法为纯新增，既有签名零修改；DocumentStructure.DocumentSection 新增 `public int page` 字段不影响既有 setter/序列化
- **既有测试护栏**：Task 6 修改 PageConsumer 返回值需同步更新现有 PdfStructureExtractorTest 中 1 处使用点
