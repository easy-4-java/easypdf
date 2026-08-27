# easypdf R4-P2 生产加固计划（错误分级报告 + 安全护栏 + 文档）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让提取引擎达到生产可运维标准：① 失败可分类（损坏/加密/超限）而非一律 IOException；② 提取附带 ExtractReport（页数/字符数/表格数/图片数/警告列表/耗时），智能体与服务端可观测；③ 安全护栏（文件大小上限、页数上限，防恶意巨型 PDF DoS）；④ 文档真实化（README 更新为纯 PDF 库现状 + 新增 USAGE.md）。

**Tech Stack:** iText 7.1.10（`PdfReader.isOpenedWithPassword()` 已核验可用；加密判断用 `pdfDoc.isEncrypted()`）、Java 8 语法、JUnit 5。零新依赖。

## Global Constraints

- 改动集中在 `convert/` 与 `convert/layout/PdfExtractionProperties.java`
- 公共 API 兼容：既有 `extract(File)` / `extractPerPage` 签名不变；`ExtractionException extends IOException`——现有 catch IOException 的调用方无需改动
- 既有 136 tests 保持全绿；新增预计 ~12 tests
- Java 8 语法；提交风格 `feat(extract): ...` / `docs: ...`
- 验证命令：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

---

### Task 1: ExtractionException（错误分级异常）

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/ExtractionException.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/ExtractionExceptionTest.java`

**Interfaces:**
```java
public class ExtractionException extends java.io.IOException {
    public enum Code { CORRUPT, ENCRYPTED, LIMIT_EXCEEDED, NOT_FOUND }
    public Code getCode();
    public ExtractionException(Code code, String message);
    public ExtractionException(Code code, String message, Throwable cause);
}
```

- [ ] **Step 1: 写失败测试**
```java
package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExtractionExceptionTest {
    @Test
    void carriesCodeAndIsIOException() {
        ExtractionException e = new ExtractionException(ExtractionException.Code.ENCRYPTED, "受密码保护");
        assertThat(e.getCode()).isEqualTo(ExtractionException.Code.ENCRYPTED);
        assertThat(e).isInstanceOf(java.io.IOException.class);
        assertThat(e.getMessage()).contains("密码");
    }
    @Test
    void wrapsCause() {
        RuntimeException cause = new RuntimeException("root");
        ExtractionException e = new ExtractionException(ExtractionException.Code.CORRUPT, "bad", cause);
        assertThat(e.getCause()).isSameAs(cause);
    }
}
```
- [ ] Step 2 确认失败 → Step 3 实现 → Step 4 通过 → Step 5 Commit `feat(extract): add ExtractionException with error classification codes`

---

### Task 2: 安全护栏（maxFileBytes / maxPages）+ 分类抛出

**Files:**
- Modify: `layout/PdfExtractionProperties.java`（追加 `public long maxFileBytes = 104857600L; public int maxPages = 5000;`）
- Modify: `convert/PdfStructureExtractor.java`（extract(File, props) 解析前校验 + catch 映射）
- Test: `RobustnessTest.java` 追加 3 用例

**规则**：
1. 文件大小 > maxFileBytes → `ExtractionException(LIMIT_EXCEEDED, "...")`
2. 打开后 `pdfDoc.getNumberOfPages() > props.maxPages` → LIMIT_EXCEEDED
3. `PdfReader` 构造失败且异常消息含 "password"/"encrypt"（不区分大小写）→ ENCRYPTED；其余 → CORRUPT
4. `/nonexistent` 维持原 IOException("PDF not found") 不变（改为 NOT_FOUND 包装亦兼容——保持原文案）

- [ ] **Step 1: 写失败测试**（3 个）：
```java
@Test void oversizedPdfRejected() // props.maxFileBytes=10，真实小 pdf → LIMIT_EXCEEDED
@Test void passwordProtectedDetected(@TempDir File dir)
    // 用 iText 生成加密 PDF：
    // PdfWriter w = new PdfWriter(out); PdfDocument d = new PdfDocument(w);
    // d.setTagged(); ... 生成后用 WriterProperties 加密版重新生成：
    // 实际做法：new PdfWriter(out, new WriterProperties().setStandardEncryption(
    //   "user".getBytes(), "owner".getBytes(), EncryptionConstants.ALLOW_PRINTING,
    //   EncryptionConstants.STANDARD_ENCRYPTION_128));
    // HtmlConverter.convertToPdf("<html><body><p>x</p></body></html>", new PdfDocument(w), null);
    // 断言 extract 抛出 code==ENCRYPTED
@Test void corruptBytesMapToCorrupt()
    // 写入 "%PDF-1.4\n垃圾" 字节 → code==CORRUPT
```
- [ ] Step 2 确认失败 → Step 3 实现（注意：加密 PDF 的读取失败发生在 `new ParsedDoc(pdf)` 内部 `new PdfReader(pdf)` 或首次 getNumberOfPages——在 extract 的 try 块 catch BadPasswordException / IOException 分诊）→ Step 4 回归全绿 → Step 5 Commit `feat(extract): size/page guards and classified extraction errors`

---

### Task 3: ExtractReport + extractWithReport

**Files:**
- Create: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/ExtractReport.java`
- Modify: `convert/PdfStructureExtractor.java`（新增方法，不动旧签名）
- Test: `RobustnessTest.java` 追加

**Interfaces:**
```java
public final class ExtractReport {
    public DocumentStructure document;   // 成功时非 null
    public ExtractionException error;     // 失败时非 null
    public boolean success;
    public int pages;
    public long chars;
    public long tables;
    public long images;
    public long durationMillis;
    public java.util.List<String> warnings = new ArrayList<String>();
}
public static ExtractReport extractWithReport(File pdf, PdfExtractionProperties props)
```
语义：永不抛异常——成功填 document；失败填 error 并保留已统计的计数。warnings 至少包含：无文本层 PDF（"no text extracted"）。

- [ ] **Step 1: 写失败测试**（3 个）：成功路径 fields 正确 + durationMillis ≥ 0；不存在文件 → success=false && error.code==NOT_FOUND；空文本 HTML 生成的 PDF → warning 含 "no text"
- [ ] Step 2 确认失败 → Step 3 实现（内部调 extract，成功后从 document 统计 chars=遍历 sections.content.length、tables/images 尺寸）→ Step 4 回归 → Step 5 Commit `feat(extract): add ExtractReport and never-throw extractWithReport`

---

### Task 4: README 更新 + USAGE.md

**Files:**
- Modify: `README.md` / `README.zh-CN.md`（架构描述改为纯 PDF 现状：Tier1-3 提取、Tagged 往返、Agent API）
- Create: `docs/USAGE.md`（中英双语代码片段：md→pdf / tagged 往返 / 结构化提取 / summary/chunked / extractPerPage 取消 / REST 扩展点配置）

**内容要点**（替换旧 docx4j Word 描述）：
- 模块图：core(html2pdf+FontProvider+结构树) / xhtml(convert 门面) / 9 引擎 / webmvc(jakarta 仅 3.0.x)
- 三条质量路径表：Tagged 往返 100% / 规则 ~80% / REST ML 90-95%
- Agent API 示例（summary → pageRange → chunked 全链路）
- 配置项表（PdfExtractionProperties 全字段）

- [ ] Step 1 撰写并自查代码片段与实际签名一致 → Step 2 Commit `docs: rewrite README for pdf-only architecture and add USAGE guide`

---

### Task 5: 三分支同步 + 推送 + 勾选

- [ ] Step 1: 3.0.x 全量 verify → Step 2 同步 1.0.x（convert/ 新文件 + Properties + README/docs）→ verify → commit → Step 3 同步 2.0.x 同流程 → Step 4 push 三分支 → Step 5 勾选本计划 + 清理工作区

---

## Self-Review

- 覆盖：A2 错误分级+报告 ✓（T1/T3）；A4 大小/页数护栏 ✓（T2）；C4 README/USAGE ✓（T4）；其余显式排除：JS 执行审计（需 iText 源码级验证，另行专项）、PII redact、多租户、发布
- 无占位符；加密 PDF 测试用 iText 自身 WriterProperties 生成夹具（API 存在于 kernel 7.1.10：WriterProperties.setStandardEncryption + EncryptionConstants）
- 类型一致：ExtractionException.Code 四值贯穿 T1/T2/T3；ExtractReport.error 类型即 T1 异常
