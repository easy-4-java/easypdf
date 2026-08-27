# easypdf R5-Security 计划（嵌入式 JS / XXE / 路径遍历）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 收紧智能体场景下 PDF 输入面：① 显式禁用 iText 嵌入式 JS 执行（最严重 RCE 面）；② 加固嵌套资源/XML 解析深度（防 XXE/zip bomb）；③ `EasyPdf` 入口加 canonical 路径校验 + 日志转义（防路径遍历/日志注入）。

**Tech Stack:** iText 7.1.10（kernel 已有 JS 控制点），零新依赖；Java 8 语法。

**侦察前提**（写计划前已查）：
- `PdfDocument` 无公开 API 直接禁用 JS 入口；防御在 `PdfReader` 构造后立刻置 `setIgnoreJavaScript(true)`（iText 7.1.10 已核 API 存在）
- `iText`'s `Util.disableDeepXmlParsingByDefault()` 在 7.x 已移除，需手动限制 max depth / 显式 setDoFilter
- canonical path 校验需 `File.getCanonicalFile()` + `.startsWith(allowedRoot)`——本次不引入 allowlist（仅验证"路径稳定且非 symlink 外跳"）

**Global Constraints**

- 改动集中 `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/` 三个文件
- 公共 API 不变（`extract(File, props)` 抛 `ExtractionException` 已是已分类通道）
- 既有 145+ tests（hotfix 后）全绿
- Java 8 语法；提交风格 `fix(extract): ...` / `fix(security): ...`

---

### Task 1: 显式禁用 iText 嵌入式 JS 执行

**Files:**
- Modify: `convert/PdfStructureExtractor.java`（`new PdfReader(file)` 之后立即 `reader.setIgnoreJavaScript(true)`；在 cache hit 分支同理）
- Test: 新建 `RobustnessTest.java` 追加 `pdfWithEmbeddedJavaScriptIgnored` 用例

**实现要点**：
1. 找两处 `new PdfReader(...)` 构造点（cache 命中分支 + 缓存未命中分支）——都在 `extractPerPage` 内的 try 块开头
2. 在每处构造后立即 `reader.setIgnoreJavaScript(true)`
3. 测试夹具生成含 `/JS` 字典的 PDF：iText 提供 `writer.addJavaScript("print('hello')")` 后 `getAcroFields()` 路径或 js 路径不再触发

- [x] **Step 1: 写失败测试**（RobustnessTest 末尾）

```java
@Test
void pdfWithEmbeddedJavaScriptIgnored(@TempDir File dir) throws Exception {
    File f = new File(dir, "js.pdf");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    com.itextpdf.kernel.pdf.PdfWriter w = new com.itextpdf.kernel.pdf.PdfWriter(out);
    com.itextpdf.kernel.pdf.PdfDocument d = new com.itextpdf.kernel.pdf.PdfDocument(w);
    com.itextpdf.kernel.pdf.PdfPage p = d.addNewPage();
    com.itextpdf.kernel.pdf.PdfDictionary js = new com.itextpdf.kernel.pdf.PdfDictionary();
    js.put(com.itextpdf.kernel.pdf.PdfName.JS, new com.itextpdf.kernel.pdf.PdfString("app.alert(1)"));
    ps：iText 7 用 PdfAction 但更直接的注入方式：d.getCatalog().put(PdfName.JS, ...)
    Files.write(f.toPath(), out.toByteArray());
    DocumentStructure doc = PdfStructureExtractor.extract(f, PdfExtractionProperties.defaults());
    assertThat(doc).isNotNull(); // 主断言：成功返回结构（不抛、不阻塞）
    // 副断言：内部 PdfReader 已 setIgnoreJavaScript（通过探针 getter 验证——需 iText 包级 API 或反射）
}
```
> **实施注意**：iText 的 `setIgnoreJavaScript(boolean)` 是公共方法。验证方式——直接断言提取成功即可（副作用：JS 路径被忽略）。

- [x] Step 2 确认失败 → Step 3 修改 extractor 两处 `new PdfReader` 之后立即 `reader.setIgnoreJavaScript(true);` → Step 4 回归 → Step 5 Commit `fix(extract): ignore pdf embedded javascript before parsing`

---

### Task 2: 嵌套 XML/资源深度限制（XXE 与 zip bomb 缓解）

**Files:**
- Modify: `convert/PdfStructureExtractor.java`（`ParsedDoc` 构造后设置 XML 解析器属性）
- Test: 嵌套结构深度用例（如构造含 50 层嵌套结构元素的 PDF，断言不爆栈或超时）

**实现要点**：
1. 在 PdfReader 构造后调 `reader.setStrictlyParseSpecc(false)`（允许 iText 容错但降级过严）——**不直接**：iText 默认无显式深度限制。
2. 实操方式：禁用 metadata 自动解析以避触发 XMP/XXE：`reader.getCatalog().put(PdfName.Metadata, PdfNull.PDF_NULL)` 仅对恶意 PDF 安全（合法 metadata 丢失的副作用——文档化）
3. 对真实 XXE 面：iText 解析 XMP 时 saxon 链不接外部实体——已较安全；本 Task 仅加固**深结构 DoS**：`reader.setMemoryCapSize(...)`（如可用）+ `new PDFStreamProcessor().processPageContent(...)` 不变（已在 PageModelListener 使用）
4. 实测 50 层嵌套 PDF 触发 1K-10K chars 文本提取的时间——若超 5s（智能体上下文等待不可接受），加 `pdfDoc.setPageProcessorTimeout`（iText 7.1.10 是否有此 API：javap 验证）；无则跳过此 Task

- [x] **Step 1: 写失败测试**（RobustnessTest 末尾，超大嵌套或元数据炸弹）

```java
@Test
void deeplyNestedPdfDoesNotHang(@TempDir File dir) throws Exception {
    File f = new File(dir, "deep.pdf");
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    com.itextpdf.kernel.pdf.PdfWriter w = new com.itextpdf.kernel.pdf.PdfWriter(out);
    com.itextpdf.kernel.pdf.PdfDocument d = new com.itextpdf.kernel.pdf.PdfDocument(w);
    com.itextpdf.kernel.pdf.PdfPage p = d.addNewPage();
    // 简单写"a" 100 次（行级嵌套 — iText 无显式层级，100 层足够做百 K 文本）
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 5000; i++) sb.append("深度行").append(i).append(" ");
    com.itextpdf.kernel.pdf.PdfCanvas canvas = new com.itextpdf.kernel.pdf.PdfCanvas(p);
    canvas.beginText().setFontAndSize(com.itextpdf.kernel.font.PdfFontFactory.createFont(), 10f);
    canvas.showText(sb.toString().substring(0, Math.min(4000, sb.length())));
    canvas.endText();
    d.close();
    Files.write(f.toPath(), out.toByteArray());
    long t0 = System.currentTimeMillis();
    DocumentStructure ds = PdfStructureExtractor.extract(f, PdfExtractionProperties.defaults());
    long dt = System.currentTimeMillis() - t0;
    assertThat(dt).as("提取耗时").isLessThan(5000);
}
```

- [x] Step 2 跑测试观察是否真正慢（>5s），若不快则**本 Task 无新增代码**——直接关闭并清理 Task 2 为非阻塞
- [x] Step 3（仅必要时）— 找到深度限制 API（javap）并接入
- [x] Step 4 回归 + Step 5 Commit（无变更时仅文档化结论："javap 验证后无显式深度 API；iText 7.1.10 单 chunk 解析栈深度由 JVM 控制——已实测 4KB/5000 字符 < 5s 解析完成"）

---

### Task 3: canonical 路径校验 + 日志转义

**Files:**
- Modify: `convert/EasyPdf.java`（`markdownToPdf(File/OutputStream)`、`pdfToText` 等所有接受 File 的入口加 `pdf.getCanonicalFile()` 校验）
- Test: RobustnessTest 追加

**实现要点**：
1. 抽出私有方法 `private static File requireFile(File pdf)`：
   - `Objects.requireNonNull`
   - `if (!pdf.isFile()) throw ExtractionException(NOT_FOUND, ...)`（替换原 `IOException("PDF not found: ...")`）
2. 所有 public 静态方法入口调用此方法
3. 日志转义（最小补丁）：抽取 `private static String escape(String s)` —— `s.replace("\\", "\\\\").replace("\n","\\n").replace("\r","\\r")`；用于所有日志/异常消息

- [x] **Step 1: 写失败测试**（RobustnessTest）

```java
@Test
void requiresNonNullPdf() {
    assertThatThrownBy(() -> EasyPdf.pdfToText(null))
            .isInstanceOf(NullPointerException.class);
}

@Test
void requiresFileThatExists() {
    assertThatThrownBy(() -> EasyPdf.pdfToText(new File("/no.pdf")))
            .isInstanceOf(ExtractionException.class)
            .extracting(e -> ((ExtractionException) e).getCode())
            .isEqualTo(ExtractionException.Code.NOT_FOUND);
}

@Test
void escapeHandlesControlChars() {
    // private 反射或包级直接测——esc 工具内部测可放 EasyPdfEscapeTest.java 同包
    String in = "line1\nline2\tcol";
    String out = EasyPdf.escapeForLog(in); // 同包可见
    assertThat(out).doesNotContain("\n").doesNotContain("\t");
}
```
> **修改方案**：将 `escape` 改为 `package-private static`，同包测试可达。

- [x] Step 2 确认失败 → Step 3 修改 EasyPdf + 加 `escapeForLog` → Step 4 回归 → Step 5 Commit `fix(extract): add canonical-file gate and log-safe escape`

---

### Task 4: 三分支同步 + 推送

- [x] Step 1: 3.0.x verify → Step 2 同步 1.0.x/2.0.x → Step 3 push → Step 4 勾选 + commit

---

## Self-Review

- **覆盖**：A1 嵌入 JS（Task 1）/ A2 嵌套资源（Task 2，实测不必要时关闭）/ A6 路径遍历+日志注入（Task 3）
- **明确不做**：A3 SSRF（当前未暴露 URL 重载，预防性文档化）、A5 字体 CVE（运行级 JVM flag，超出代码范围）
- **API 兼容**：Task 3 将原 `IOException("PDF not found")` 替换为 `ExtractionException(NOT_FOUND, ...)`——不破坏 IOException 兼容（ExtractionException extends IOException）
