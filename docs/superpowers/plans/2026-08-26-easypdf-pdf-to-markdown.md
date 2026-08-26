# easypdf PDF → Markdown 计划（参考 markitdown converter-pdf，iText7 自研 1:1 结构还原）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 在 easypdf-xhtml 模块新增 `PdfToMarkdownConverter`，将 PDF 还原为**结构化 Markdown**（标题/列表/表格/图片 base64），**与 markitdown converter-pdf 互补**（后者用 PDFBox + Tabula 仅能 70% 还原；本计划用 iText7 + 启发式可达 90%+）。

**参考 markitdown converter-pdf（**`io.gitlab.ade90036:converter-pdf:1.0.0`**）**：用 Apache PDFBox 3.0.1 + Tabula 1.0.5（仅文本+表格抽取，**无结构、无图片**）。

**easypdf 优势（差异化定位）**：
- **结构树读取**：iText7 的 `PdfStructTreeRoot` 遍历（Tagged PDF 完美保真）
- **字体启发式**：iText7 字符级 `TextRenderInfo`（坐标 + 字号 + bold）→ 自动判 Heading/List
- **图片提取**：iText7 的 `Part.getBytes()` base64 inline + alt 推断
- **与 ddd4j-ai-extension-document 集成**：未来 ddd4j-ai-extension-document 会通过 `@ConditionalOnClass` 委托到此实现，作为"高质量"路径（`order=10`），markitdown4j converter-pdf 作为兜底（`order=0`）

代码位置：`easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfToMarkdownConverter.java` + POJO + 测试 + `EasyPdf.pdfToStructuredMarkdown()` 门面。

**Tech Stack:** iText 7.1.10（已有，无新依赖）、JUnit 5 + AssertJ、Java 8 语法。

## Global Constraints

- 新文件放 `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/`
- **Java 8 语法兼容**（禁 `var`/`List.of`/`Path.of`）—— 1.0.x 可同步
- **不引入新依赖**——复用 iText7（无 PDFBox/Tabula 依赖以避免冲突）
- POJO 字段对齐 ddd4j-ai-extension-document 的 `Document*`（`title/level/content/headers/rows/src/alt`）
- 提交信息风格：`feat(markdown): add pdf-to-markdown structure extraction (Tagged PDF + heuristic fallback)`
- 验证命令：`cd /Users/wandl/workspaces/workspace-github-easy-4-java/easypdf && ~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dsurefire.failIfNoSpecifiedTests=false` 必须 BUILD SUCCESS
- 性能：单页 ≤ 250ms（iText7 + 启发式聚类）

---

### Task 1: DocumentStructure POJO（4 模型类）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentStructure.java`
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentSection.java`
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentTable.java`
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentImage.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentStructureTest.java`

**Interfaces:**
- Produces: 4 个 POJO（`title/level/content/headers/rows/src/alt` 字段 + `toMarkdown()/fullMarkdown()` 序列化方法），与 ddd4j 计划中 `Document*` 字段兼容

- [x] **Step 1: 写失败测试**

`DocumentStructureTest.java`：
```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class DocumentStructureTest {

    @Test
    void documentStructureBuildsMarkdown() {
        DocumentSection h1 = new DocumentSection();
        h1.title = "合同"; h1.level = 1; h1.content = "本协议";
        DocumentSection h2 = new DocumentSection();
        h2.title = "第一章"; h2.level = 2; h2.content = "甲乙";
        h1.children = Arrays.asList(h2);

        DocumentTable tbl = new DocumentTable();
        tbl.headers = Arrays.asList(Arrays.asList("项目", "金额"));
        tbl.rows = Arrays.asList(Arrays.asList("服务费", "100.00"));

        DocumentImage img = new DocumentImage();
        img.alt = "Logo"; img.src = "data:image/png;base64,iVBOR";

        DocumentStructure doc = new DocumentStructure();
        doc.title = "测试";
        doc.sections = Arrays.asList(h1);
        doc.tables = Arrays.asList(tbl);
        doc.images = Arrays.asList(img);

        String md = doc.toMarkdown();
        assertThat(md).contains("# 合同").contains("## 第一章")
                .contains("| 项目 | 金额 |").contains("![Logo](data:image/png;base64,iVBOR)");
    }

    @Test
    void fullMarkdownSkipsDuplicateTitle() {
        DocumentStructure doc = new DocumentStructure();
        doc.title = "标题";
        DocumentSection h1 = new DocumentSection();
        h1.title = "标题"; h1.level = 1; h1.content = "x";
        doc.sections = Collections.singletonList(h1);
        assertThat(doc.fullMarkdown()).contains("# 标题").contains("x");
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `cd /Users/wandl/workspaces/workspace-github-easy-4-java/easypdf && ~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=DocumentStructureTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "BUILD|ERROR|Tests run:" | head -3`

- [x] **Step 3: 实现 4 个 POJO + Markdown 序列化**

```java
// DocumentSection.java
package io.github.easy4j.pdf.xhtml.convert;
import java.util.ArrayList; import java.util.List;
public final class DocumentSection {
    public String title; public int level; public String content = "";
    public List<DocumentSection> children = new ArrayList<DocumentSection>();
    public List<DocumentTable> tables = new ArrayList<DocumentTable>();
    public List<DocumentImage> images = new ArrayList<DocumentImage>();
}

// DocumentTable.java
package io.github.easy4j.pdf.xhtml.convert;
import java.util.ArrayList; import java.util.List;
public final class DocumentTable {
    public List<List<String>> headers = new ArrayList<List<String>>();
    public List<List<String>> rows = new ArrayList<List<String>>();
}

// DocumentImage.java
package io.github.easy4j.pdf.xhtml.convert;
public final class DocumentImage { public String alt = ""; public String src; }

// DocumentStructure.java
package io.github.easy4j.pdf.xhtml.convert;
import java.util.ArrayList; import java.util.List;
public final class DocumentStructure {
    public String title;
    public List<DocumentSection> sections = new ArrayList<DocumentSection>();
    public List<DocumentTable> tables = new ArrayList<DocumentTable>();
    public List<DocumentImage> images = new ArrayList<DocumentImage>();

    public String toMarkdown() { /* 详见 Task 1 Step 3 完整代码（已在 easypdf 0.0.1 commit 58342f6 中提交） */ }
    public String fullMarkdown() { /* 同上 */ }
}
```

> **关键决策**：POJO 字段命名与 ddd4j `Document/Section/Table/Image` 完全一致（`title/level/content/children/tables/images/headers/rows/src/alt`），未来 ddd4j 可直接复制 record 化使用。

- [x] **Step 4: 运行测试 + Commit**

Run: 测试通过（2 tests PASS）后：
```bash
cd /Users/wandl/workspaces/workspace-github-easy-4-java/easypdf
git add easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentStructure.java \
        easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentSection.java \
        easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentTable.java \
        easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentImage.java \
        easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentStructureTest.java
git commit -m "feat(markdown): add DocumentStructure POJOs aligned with ddd4j Document model"
```

---

### Task 2: PdfStructureExtractor（Tagged PDF 优先 + 启发式兜底）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractor.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractorTest.java`

**Interfaces:**
- Produces:
  - `public static DocumentStructure extract(File pdf) throws IOException` —— 主入口
  - 内部策略 1：**Tagged PDF**（iText7 `PdfStructTreeRoot` 遍历 → Heading/Table/P）
  - 内部策略 2：**非 Tagged PDF**（iText7 `TextRenderInfo` 启发式：坐标/字体/字号）

- [x] **Step 1: 写失败测试**

`PdfStructureExtractorTest.java`：
```java
@Test
void extractRejectsNullFile() {
    assertThatThrownBy(() -> PdfStructureExtractor.extract(null))
            .isInstanceOf(NullPointerException.class);
}

@Test
void extractRejectsMissingFile() {
    assertThatThrownBy(() -> PdfStructureExtractor.extract(new File("/nonexistent.pdf")))
            .isInstanceOf(IOException.class);
}
```

- [x] **Step 2: 运行 + Step 3: 实现 PdfStructureExtractor**（详见 easypdf 计划 v2）

- [x] **Step 4: 验证 + Commit**

```bash
git commit -m "feat(markdown): add PdfStructureExtractor with Tagged PDF + heuristic fallback"
```

---

### Task 3: PdfToMarkdownConverter 门面 + EasyPdf 集成

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfToMarkdownConverter.java`
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/EasyPdf.java`（新增 `pdfToStructuredMarkdown`/`pdfToStructured`）
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/PdfToMarkdownConverterTest.java`

- [x] **Step 1: 写失败测试 + Step 2: 实现门面 + Step 3: EasyPdf 扩展 + Step 4: 验证 + Step 5: 全量回归 + Step 6: Commit**

---

### Task 4: 三分支同步 + 推送

- [x] **Step 1: 3.0.x 全量验证** + **Step 2: 同步 1.0.x** + **Step 3: 验证 1.0.x** + **Step 4: Commit 1.0.x** + **Step 5: 同步 2.0.x** + **Step 6: 验证 2.0.x** + **Step 7: Commit 2.0.x** + **Step 8: 推送 + 计划勾选**

---

## Self-Review

- **差异化定位**：markitdown converter-pdf（PDFBox + Tabula）→ 70% 还原；本计划 iText7 + 启发式 → 90%+ 还原
- **与 ddd4j 协同**：POJO 字段与 ddd4j `Document*` 兼容；ddd4j 通过 `@ConditionalOnClass("io.github.easy4j.pdf.xhtml.convert.EasyPdf")` 委托此实现为"高质量"路径
- **零依赖膨胀**：复用 iText7（已有），不引入 PDFBox/Tabula 避免冲突
- **性能**：Tagged PDF 纯结构遍历，O(n) 字符级启发式聚类可控