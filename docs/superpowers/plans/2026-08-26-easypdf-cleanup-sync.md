# easypdf Word 侧清理与三分支终验计划（Phase 4）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成 easypdf 的"纯 PDF"收敛：删除全部 docx4j Word 侧代码（core 的 Word 工具包、xhtml 的 HTML→docx 转换器、9 个旧 `WordprocessingML{Engine}Template`），xhtml 模块收敛为"HTML/Markdown → PDF"转换模块；三分支全量验证通过并做发布演练，确保三分支可正常发布。

**Architecture:** 分四步清理：① core 删除 Word 侧（顶层 `WordprocessingMLTemplate`/`WordprocessingMLDocxTemplate`/`Docx4jConstants` + `io`/`wml`/`utils`/`fonts`/`handler`/`bus` 包 + 对应测试 + docx4j 依赖），保留 `PdfTemplate`/`HtmlPdfConverter`/`AbstractStringTemplateWrappingPdfTemplate`；② xhtml 删除 docx4j 转换类（`WordprocessingMLHtmlTemplate`/`WordprocessingMLPackageBuilder`/`XHTMLImporterUtils`/`Docx4jHtmlUtils`/`DocumentHandler` 等 + docx4j-ImportXHTML/xhtmlrenderer 依赖），保留 jsoup/html2pdf/openhtmltopdf/flexmark；③ 删除 9 个旧 `WordprocessingML{Engine}Template` 与 `WordprocessingMLTemplate_Test` 等旧测试（Phase 3 的新 `{Engine}PdfTemplate` 已就位）；④ 三分支全量 verify + `-P central` 发布演练（本地 staging 检查，不真正发布）。

**Tech Stack:** 无新增依赖；Maven 4（3.0.x）/ Maven 3.9.16（1.0.x/2.0.x）、JUnit 5 + AssertJ、JaCoCo。

## Global Constraints

- 删除前必须确认零引用（用 `grep -rln` 验证跨模块引用为空才执行删除）
- **保留**：`PdfTemplate`、`PdfTemplateTest`、`core/convert/**`、`template/**`、`core/convert` 测试、xhtml 的 `convert/**`（EasyPdf/MarkdownConverter 及其测试，Phase 3 后如有）
- core 删除后 docx4j 依赖一并移除（core pom 的 docx4j-core/docx4j-JAXB-ReferenceImpl/docx4j-export-fo + xhtml pom 的 docx4j-ImportXHTML/xhtmlrenderer/cssparser 等）
- 每个 Task 末尾跑 `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl <module> -am clean verify`（Maven 4）必须 BUILD SUCCESS
- 提交信息风格：`refactor(cleanup): ...`

---

### Task 1: 删除 core 的 Word 侧代码与 docx4j 依赖

**Files:**
- Delete: `easypdf-core/src/main/java/io/github/easy4j/pdf/WordprocessingMLTemplate.java`、`WordprocessingMLDocxTemplate.java`、`Docx4jConstants.java`
- Delete: `easypdf-core/src/main/java/io/github/easy4j/pdf/io/**`、`wml/**`、`utils/**`、`fonts/**`、`handler/**`、`bus/**`
- Delete: 对应测试（`easypdf-core/src/test/java/io/github/easy4j/pdf/` 下非 `core/convert`、非 `template` 的测试；含 8 个 javax.xml.bind 排除项文件——删除后 core pom 的 testExcludes 可整体移除）
- Modify: `easypdf-core/pom.xml`（移除 docx4j-core/docx4j-JAXB-ReferenceImpl/docx4j-export-fo 依赖 + javax.servlet-api + commons-lang3/commons-io（如无其他引用）+ testExcludes 配置）

**Interfaces:**
- Consumes: Phase 3（引擎已迁移，旧 Word 类无生产引用）
- Produces: core 仅含 `PdfTemplate` + `core/convert` + `core/template`；依赖仅保留 iText html2pdf/kernel/layout/io 等

- [ ] **Step 1: 验证零引用**

Run: `grep -rln "WordprocessingMLTemplate\|Docx4jConstants\|io.github.easy4j.pdf.io\|io.github.easy4j.pdf.wml\|io.github.easy4j.pdf.utils\|io.github.easy4j.pdf.fonts\|io.github.easy4j.pdf.handler\|io.github.easy4j.pdf.bus" --include="*.java" easypdf-*/src/main | grep -v "easypdf-core/src/main"`
Expected: 空输出（跨模块零引用；easydoc 无关）

- [ ] **Step 2: 删除 core Word 侧**

```bash
git rm -r easypdf-core/src/main/java/io/github/easy4j/pdf/io \
        easypdf-core/src/main/java/io/github/easy4j/pdf/wml \
        easypdf-core/src/main/java/io/github/easy4j/pdf/utils \
        easypdf-core/src/main/java/io/github/easy4j/pdf/fonts \
        easypdf-core/src/main/java/io/github/easy4j/pdf/handler \
        easypdf-core/src/main/java/io/github/easy4j/pdf/bus
git rm easypdf-core/src/main/java/io/github/easy4j/pdf/WordprocessingMLTemplate.java \
       easypdf-core/src/main/java/io/github/easy4j/pdf/WordprocessingMLDocxTemplate.java \
       easypdf-core/src/main/java/io/github/easy4j/pdf/Docx4jConstants.java
```

- [ ] **Step 3: 删除对应测试（保留 convert/template 测试）**

```bash
for f in $(find easypdf-core/src/test/java -name "*.java"); do
  case "$f" in
    *core/convert/*|*template/*|*PdfTemplateTest.java) ;;
    *) git rm "$f" ;;
  esac
done
```

- [ ] **Step 4: 编辑 core pom**

删除 docx4j 三个依赖、javax.servlet-api、commons-lang3/commons-io、testExcludes 配置块。

- [ ] **Step 5: 验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(core): remove docx4j Word-side code and dependencies, keep PDF pipeline only"
```

---

### Task 2: 删除 xhtml 的 docx4j 转换类与依赖

**Files:**
- Delete: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/WordprocessingMLHtmlTemplate.java`、`DataMap.java`
- Delete: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/handler/**`、`io/**`、`utils/**`（`XHTMLImporterUtils`/`Docx4jHtmlUtils`/`WordprocessingMLPackageBuilder`/`DocumentHandler`/`XHTMLDocumentHandler`）
- Delete: 对应测试（`WordprocessingMLHtmlTemplateTest`/`WordprocessingMLPackageBuilderTest`/`XHTMLImporterUtilsTest`/`Docx4jHtmlUtilsTest`/`DocumentHandlerTest`/`XHTMLDocumentHandlerTest`/`DataMapTest`）
- Delete: 编译失败的 demo 类（`Demo4HTMLCn`/`HtmlConverter`/`HtmlToDOCDemo`/`WebPageToImage` 等，testExcludes 随之移除）
- Modify: `easypdf-xhtml/pom.xml`（移除 docx4j-ImportXHTML/xhtmlrenderer/cssparser/flying-saucer-pdf 依赖；保留 jsoup/html2pdf/openhtmltopdf/flexmark/iText 全家）

**Interfaces:**
- Consumes: Phase 3（9 引擎已不依赖 easypdf-xhtml）
- Produces: xhtml 仅含 `convert/**`（EasyPdf/MarkdownConverter）与 HTML→PDF 相关依赖

- [ ] **Step 1: 验证零引用**

Run: `grep -rln "WordprocessingMLHtmlTemplate\|XHTMLImporterUtils\|io.github.easy4j.pdf.xhtml" --include="*.java" easypdf-*/src/main | grep -v "easypdf-xhtml/src/main"`
Expected: 空输出（引擎已迁移，无跨模块引用）

- [ ] **Step 2: 删除 xhtml Word 侧**

```bash
git rm -r easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/handler \
        easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/io \
        easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/utils
git rm easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/WordprocessingMLHtmlTemplate.java \
       easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/DataMap.java
```

- [ ] **Step 3: 删除对应测试与 demo**

```bash
git rm easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/WordprocessingMLHtmlTemplateTest.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/DataMapTest.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/Demo4HTMLCn.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/HtmlConverter.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/HtmlToDOCDemo.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/WebPageToImage.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/T.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/FlyingSaucerTest.java \
       easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/TestHtmlConverter.java
git rm -r easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/handler \
          easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/io \
          easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/utils
```

- [ ] **Step 4: 编辑 xhtml pom**（移除 docx4j-ImportXHTML/xhtmlrenderer/cssparser/flying-saucer-pdf 依赖与 testExcludes）

- [ ] **Step 5: 验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(xhtml): remove docx4j conversion classes, keep HTML/Markdown-to-PDF pipeline"
```

---

### Task 3: 删除 9 个旧 WordprocessingML{Engine}Template 与旧测试

**Files:**
- Delete: 9 个模块的 `WordprocessingML{Engine}Template.java`
- Delete: 9 个模块测试目录中的旧测试（`WordprocessingML{Engine}TemplateTest`/`WordprocessingML{Engine}Template_Test`/`WordprocessingMLTemplate_Test`——`{Engine}PdfTemplateTest` 保留）
- Modify: 各模块 pom 的 testExcludes（如有）

**Interfaces:**
- Consumes: Phase 3（新 `{Engine}PdfTemplate` 已就位且测试通过）
- Produces: 每个引擎模块仅含 `{Engine}PdfTemplate` 与其测试

- [ ] **Step 1: 删除旧类与旧测试**

```bash
for m in freemarker velocity thymeleaf beetl rythm jetbrick httl webit jsp; do
  git rm easypdf-$m/src/main/java/io/github/easy4j/pdf/$m/WordprocessingML${m^}Template.java 2>/dev/null
  git rm easypdf-$m/src/test/java/io/github/easy4j/pdf/$m/WordprocessingML${m^}TemplateTest.java 2>/dev/null
  git rm easypdf-$m/src/test/java/io/github/easy4j/pdf/$m/WordprocessingML${m^}Template_Test.java 2>/dev/null
  git rm easypdf-$m/src/test/java/io/github/easy4j/pdf/$m/WordprocessingMLTemplate_Test.java 2>/dev/null
done
```

- [ ] **Step 2: 全量验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS；记录测试基线（xhtml 的 19 个 docx4j 测试已删，engine 新测试 + core 测试为净增量）

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor(engines): remove legacy WordprocessingML engine adapters"
```

---

### Task 4: 三分支终验 + 发布演练

**Files:**
- 三分支同步 Task 1-3 全部产物

**Interfaces:**
- Consumes: Task 1-3 产物
- Produces: 三分支纯 PDF 形态、全量 verify 通过、发布配置可执行

- [ ] **Step 1: 3.0.x 最终验证 + 发布演练**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -DskipTests -P central install`（本地验证 central profile 装配，不触网发布）
Expected: BUILD SUCCESS（校验 central-publishing-plugin 配置与 GPG/源码/javadoc 插件装配无误）

- [ ] **Step 2: 同步到 1.0.x**（对比整合：Word 侧删除 + 依赖清理 + testExcludes 移除）

```bash
git checkout feature/1.0.x
# 移植 Task 1-3 变更
```

- [ ] **Step 3: 验证 1.0.x**（含 `-P central` 本地装配，1.0.x 用 Maven 3.9.16）

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify` + `/opt/homebrew/bin/mvn -B -ntp -DskipTests -P central install`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit 1.0.x**

```bash
git add -A
git commit -m "refactor(cleanup): sync Word-side removal and final PDF-only state from 3.0.x"
```

- [ ] **Step 5: 同步到 2.0.x**（同 Step 2）

- [ ] **Step 6: 验证 2.0.x**（同 Step 3）

- [ ] **Step 7: Commit 2.0.x**（同 Step 4 信息）

- [ ] **Step 8: 回 3.0.x 推送三分支 + 勾选本计划**

```bash
git checkout feature/3.0.x
sed -i '' 's/- \[ \]/- [x]/g' docs/superpowers/plans/2026-08-26-easypdf-cleanup-sync.md
git add -A && git commit -m "docs: mark cleanup plan tasks complete"
git push origin feature/1.0.x feature/2.0.x feature/3.0.x
```

---

## Self-Review

- **Spec 覆盖**：Word 侧清理 → Task 1（core）/ Task 2（xhtml）/ Task 3（引擎旧类）；纯 PDF 收敛 → 依赖清理在 Task 1/2 的 pom 编辑；三分支终验 + 发布演练 → Task 4
- **占位符扫描**：无 TBD/TODO；Task 4 的 `-P central install` 为本地装配校验（不触网发布），步骤明确
- **类型一致性**：无跨任务新类型；删除清单与 Phase 3/此前计划的类名一一对应
- **风险提示**：Task 3 的 shell 循环中 `${m^}`（首字母大写）依赖 bash 4+ 大小写转换；若环境不支持，改用显式文件清单删除（9 个类名在 Phase 3 计划 Task 3 中已列出）
