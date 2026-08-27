# easypdf R5-Hotfix 计划（A4 DoS 软护栏前置拦截）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 R4-P2 加的 maxFileBytes/maxPages 校验从"打开后"前置到"读取前"——目前恶意巨型 PDF 仍付出一次 IO + 解析成本后才被拒绝，前置拦截让 size 护栏真正生效。

**侦察结论**（写计划前已实测验证）：
- iText 7.1.10 jar 实测字节码 major=51（JDK 7），源码声明 Build-Jdk=1.8，**1.0.x/JDK 8 兼容性已确认无虞**——B4 风险解除，**不列为 hotfix**
- `easypdf-xhtml/src/main/java` 全文搜索 `isBlank / .repeat\( / .strip\(\) / .lines\(\)` 零命中——**C2 JDK 11+ API 误用零存在**，不列为 hotfix
- `PdfStructureExtractor.ParsedDoc`（328-352 行）已 `implements AutoCloseable` 且 `close()` 释放 pdfDoc——**B2 句柄泄漏已规避**，不列为 hotfix

**Tech Stack:** 仅 `PdfStructureExtractor` 一处位移；零新依赖；Java 8 语法。

## Global Constraints

- 改动文件仅 `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractor.java`（extract 入口处）+ 1 个新测试
- 公共 API 完全不变
- 既有 144 tests 全绿（特别注意含 `extractRejectsMissingFile` / `extractRejectsNullFile` / `extractRejectsMissingFile` 等负面用例——位移不能改变其抛出路径与文案）
- Java 8 语法
- 提交风格：`fix(extract): ...`
- 验证命令：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

---

### Task 1: maxFileBytes 前置拦截

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractor.java`（extract(File, props) 入口：`Files.size(pdf.toPath()) > props.maxFileBytes` 校验前置；maxFileBytes ≤ 0 视为不限制）
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/ExtractionExceptionTest.java`（追加超大文件用例；或新建 `RobustnessTest.java` 追加一条守卫用例）

**实现要点**（写计划前已读过 R4-P2 实施结果——`PdfExtractionProperties.maxFileBytes/maxPages` 字段已存在；当前 `extract(File, props)` 顺序为：null 校验 → 文件存在性 → ParsedDoc 构造 → 页数校验 → 引擎分支）：
1. 在 null 校验之后、文件存在性之前，加 `Files.size(pdf.toPath())` 检查
2. 超限抛 `ExtractionException(LIMIT_EXCEEDED, "PDF too large: X bytes > maxFileBytes Y")`（Y=0 时表示不限——参见下面边界）
3. `≤0` 视为不限制（兼容旧调用方 properties 未设置场景）

- [ ] **Step 1: 写失败测试**（在 RobustnessTest 或 ExtractionExceptionTest 末尾追加）

```java
@Test
void oversizedPdfRejectedBeforeParsing(@TempDir File dir) throws Exception {
    File tiny = new File(dir, "tiny.pdf");
    Files.write(tiny.toPath(), "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8));
    PdfExtractionProperties props = PdfExtractionProperties.defaults();
    props.maxFileBytes = 5L; // 文件实际 > 5 字节
    assertThatThrownBy(() -> PdfStructureExtractor.extract(tiny, props))
            .isInstanceOf(ExtractionException.class)
            .extracting(e -> ((ExtractionException) e).getCode())
            .isEqualTo(ExtractionException.Code.LIMIT_EXCEEDED);
}
```
- [ ] **Step 2: 确认失败**——运行 `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am test -Dtest=ExtractionExceptionTest -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "Tests run|FAIL"`
- [ ] **Step 3: 在 extract(File, PdfExtractionProperties) 加**：

```java
// 文件大小前置校验（在 null + 文件存在性之后、ParsedDoc 构造之前）
if (props.maxFileBytes > 0) {
    long size = Files.size(pdf.toPath());
    if (size > props.maxFileBytes) {
        throw new ExtractionException(ExtractionException.Code.LIMIT_EXCEEDED,
                "PDF too large: " + size + " bytes > maxFileBytes " + props.maxFileBytes);
    }
}
```
（需 import `java.nio.file.Files` 与 `io.github.easy4j.pdf.xhtml.convert.ExtractionException`——前者应已在 R4-P2 引入；后者 import 一次即可）
- [ ] **Step 4: 模块回归 `clean verify` 全绿**——既有 144 tests 不得破坏（含 `/nonexistent` IOException 文案、passwordProtected ENCRYPTED 等）
- [ ] **Step 5: Commit** `fix(extract): check maxFileBytes before parsing pdf`

---

### Task 2: 三分支同步 + 推送

- [ ] Step 1: 3.0.x 全量 verify
- [ ] Step 2: 同步 1.0.x（`easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractor.java` + 测试文件）→ verify → commit → push
- [ ] Step 3: 同步 2.0.x 同流程
- [ ] Step 4: 勾选本计划 + commit + push
- [ ] Step 5: 清理 worktree

---

## Self-Review

- **覆盖**：A4 DoS 软护栏 ✓（1-2 行位移 + 1 个测试）
- **范围纪律**：B4（iText 字节码 51 实测已 JDK 8 兼容——本次明确不列为 hotfix）/ C2（已 grep 验证零 JDK 11+ 字符串 API 使用）/ B2（ParsedDoc 已 AutoCloseable）三项均排除——不属"必须做"
- **API 兼容**：`extract(File, props)` 签名不变，行为变更仅限"超大文件提早抛 LIMIT_EXCEEDED"——抛出类型仍为 ExtractionException extends IOException，调用方 try-catch IOException 已可覆盖
- **测试护栏**：位移不能改变 `/nonexistent → IOException("PDF not found: ...")` 文案（NOT_FOUND 路径在文件存在性失败时抛——本 Task 在它之后）
