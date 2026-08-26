# easypdf 提取引擎加固计划（Round 2：流式表格增强 / fullMarkdown 去重 / 嵌套表格 / 标题防误判）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 Round 1 提取引擎的 4 个已验证短板：① `fullMarkdown` 文档标题与首个 section 标题重复输出；② 流式表格（无边框表格）为保守基础版且无单测；③ Tagged 路径嵌套表格直接丢失结构；④ 封面大字被误判为多级标题。全部完成后三分支同步推送。

**范围外（需外部环境，后续独立进行）**：真实 docling/MinerU 端点联调（Tier3 服务侧）；扫描件 OCR；倾斜/旋转页面。

**Architecture:** 全部改动收敛在 `easypdf-xhtml` 的 `convert/` 与 `convert/layout/` 既有类内，零新类零新依赖；每项先写失败测试再修（TDD），公共 API 不变。

**已核实事实（写计划前验证）**：
- `DocumentStructure.fullMarkdown()` 现状为 `# title` 前置 + `toMarkdown()`（首 section level-1 同名时会输出两次 `# 标题`）——见 `DocumentStructure.java:32-39`；Tier2 页眉测试的失败输出中实际出现过 `# t\n\n# t`
- 流式表格 `streamTableLength/buildStreamTable` 无单测，触发条件仅"连续行 chunk≥2"
- Tagged 路径 `readRows` 对嵌套 Table 直接递归拍平到同一 `DocumentTable`，内层单元格文本与外层混在同格
- `headingLevel` 对全部候选字号排序映射，封面艺术大字会产生 4+ 级跳变标题

**Tech Stack:** iText 7.1.10（零新依赖）、JUnit 5 + AssertJ、Java 8 语法。

## Global Constraints

- 改动文件：`convert/DocumentStructure.java`、`convert/PdfStructureExtractor.java`、`convert/layout/RuleLayoutAnalyzer.java` + 对应测试
- **Java 8 语法**；**零新依赖**；公共 API 签名不变
- 既有 84 tests 必须保持全绿（每个 Task 的 Step 均含回归）
- 提交风格：`fix(extract): ...` / `feat(extract): ...`
- 验证命令（3.0.x）：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

---

### Task 1: fullMarkdown 标题去重

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/DocumentStructure.java`
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/DocumentStructureTest.java`（强化既有用例）

**Interfaces:**
- Produces: `fullMarkdown()` 语义——当 `title` 非空且 `sections[0]` 为 level-1 且标题与 `title` 相等（trim 后）时，**跳过前置的 `# title`**（以 section 渲染为准）；其余情况维持前置

- [ ] **Step 1: 写失败测试**（在 `DocumentStructureTest` 追加/强化）
```java
@Test
void fullMarkdownDeduplicatesDocTitleAndFirstHeading() {
    DocumentStructure doc = new DocumentStructure();
    doc.title = "合同";
    DocumentSection h1 = new DocumentSection();
    h1.title = "合同"; h1.level = 1; h1.content = "正文";
    doc.sections = Collections.singletonList(h1);
    String md = doc.fullMarkdown();
    assertThat(md).contains("# 合同").contains("正文");
    assertThat(md.indexOf("# 合同")).isEqualTo(md.lastIndexOf("# 合同")); // 只出现一次
}

@Test
void fullMarkdownKeepsTitleWhenFirstSectionDiffers() {
    DocumentStructure doc = new DocumentStructure();
    doc.title = "文档元标题";
    DocumentSection h1 = new DocumentSection();
    h1.title = "章标题"; h1.level = 1; h1.content = "x";
    doc.sections = Collections.singletonList(h1);
    assertThat(doc.fullMarkdown()).contains("# 文档元标题").contains("# 章标题");
}
```
- [ ] **Step 2: 确认失败**（第一个用例应失败：当前输出两次）
- [ ] **Step 3: 实现**——`fullMarkdown()` 中：
```java
boolean dedup = title != null && !title.isEmpty()
        && sections != null && !sections.isEmpty()
        && sections.get(0).level == 1
        && title.trim().equals(sections.get(0).title == null ? "" : sections.get(0).title.trim());
if (!dedup && title != null && !title.isEmpty()) {
    sb.append("# ").append(title).append('\n').append('\n');
}
```
- [ ] **Step 4: 模块回归全绿**（重点盯 `RuleLayoutAnalyzerTier2Test`、`TaggedRoundTripTest`）
- [ ] **Step 5: Commit** `fix(extract): deduplicate doc title and first heading in fullMarkdown`

---

### Task 2: 流式表格增强 + 单测

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/layout/RuleLayoutAnalyzer.java`（`streamTableLength`/`buildStreamTable`）
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/layout/StreamTableTest.java`（新建）

**规则增强（逐条可测）**：
1. **列数一致**：连续 N 行（N≥3）的"列簇数"必须相等才触发（当前只要 chunk≥2）
2. **列簇对齐**：各行第 k 列的起始 x 在 ±6pt 内对齐（跨行聚类）
3. **表头判定**：首行 bold 比例 ≥50% 或与后续行字号差 >0 → 首行为 headers（已是首行作 headers，补 bold 证据断言即可）
4. **列内文本合并**：cell 内多 chunk 按 x 拼接（沿用）

- [ ] **Step 1: 写失败测试**（3 个）：
```java
// 1) 无边框对齐表格 → pipe table（headers + rows 精确断言）
html: <table style='border:none'><tr><td>列甲</td><td>列乙</td></tr><tr><td>值一</td><td>值二</td></tr><tr><td>值三</td><td>值四</td></tr></table>
→ ds.tables 非空，headers=[列甲,列乙]，rows=[[值一,值二],[值三,值四]]

// 2) 普通多段正文（每行单 chunk）→ 不得误判为表格
html: <p>第一段落</p><p>第二段落</p><p>第三段落</p> → ds.tables 为空

// 3) 行间列数不一致（2列,2列,1列 混排）→ 不触发
用两个 float div 宽度不同的行制造错列 → ds.tables 为空
```
- [ ] **Step 2: 确认失败** → **Step 3: 实现规则 1-2**（列簇 = 行内 chunk 按间隙 `> max(size*4, 30pt)` 切分；跨行对齐校验 ±6pt）
- [ ] **Step 4: 模块回归全绿**
- [ ] **Step 5: Commit** `feat(extract): harden stream tables with column-count and alignment checks`

---

### Task 3: Tagged 嵌套表格并入父单元格

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/PdfStructureExtractor.java`（`readRows`/`readCells`）
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/TaggedRoundTripTest.java`（追加用例）

**规则**：`readCells` 遇到 TD 内的嵌套 `Table`：不再丢弃/拍平到兄弟行，而是把内层表格渲染为 **GFM 子表文本**并入该 cell（cell 内换行用 `<br>` 连接，保证外层 pipe 表不被破坏）。

- [ ] **Step 1: 写失败测试**：
```java
@Test
void nestedTableLandsInParentCell() throws Exception {
    String md = "| 外列 | 明细 |\n|---|---|\n| 汇总 |  |\n";  // 先用 HTML 直测更直接：
    // 直接构造嵌套 HTML → tagged → 读取
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    HtmlPdfConverter.htmlToPdfTagged(
        "<html><body><table><tr><th>外列</th><th>明细</th></tr>"
      + "<tr><td>汇总</td><td><table><tr><td>子项</td><td>1</td></tr></table></td></tr>"
      + "</table></body></html>", out);
    File pdf = new File(dir, "nested.pdf");
    Files.write(pdf.toPath(), out.toByteArray());
    DocumentStructure doc = EasyPdf.pdfToStructured(pdf);
    // 断言：外层 2×2；cell(1,1) 含 "子项" 与 "1"（且含管道或 <br>）
    assertThat(doc.tables.get(0).rows.get(0).get(1)).contains("子项").contains("1");
}
```
- [ ] **Step 2: 确认失败**（当前嵌套表拍平成额外行或丢失）
- [ ] **Step 3: 实现**——`readCells` 递归时检测 `Table` 子元素 → 调 `readTable` 得子 `DocumentTable` → 用现有 `toMarkdown()` 逻辑输出子表字符串，`<br>` 连接进 cell
- [ ] **Step 4: 回归**（含原 `roundTripPreservesHeadingsTableAndList`）
- [ ] **Step 5: Commit** `feat(tagged): merge nested tables into parent cells with br-joined subtable markdown`

---

### Task 4: 标题防误判（封面大字）

**Files:**
- Modify: `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/layout/RuleLayoutAnalyzer.java`（`headingLevel` + 标题判定处）
- Test: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/layout/RuleLayoutAnalyzerTier2Test.java`（追加用例）

**规则**：
1. 候选标题字号种类 >3 时只取**最大 3 档**映射 level 1-3（其余降为正文）
2. 行长 >80 字符的"大字行"不判标题（封面段落/艺术字常超长）
3. bold 且字号 ∈ [body, body*1.22) 的孤立短行（<40 字符）升为 level 3 标题（次要小节）

- [ ] **Step 1: 写失败测试**：
```java
@Test
void coverArtTextNotMultiLevelHeadings() throws Exception {
    // 封面大字段落（>80 字符）+ 正常 h2
    html: <div style='font-size:42px'>这是封面宣传语这是一段很长的封面艺术文字超过八十个字符用于模拟封面大段文字场景内容继续补充到达阈值以上</div>
          <h2>正文小节</h2><p>正文内容。</p>
    → fullMarkdown 恰好一次 "## 正文小节"；封面段不产生任何 "#"（断言 md 中 '#' 计数 == 标题数）
}

@Test
void headingLevelCappedAtThreeTiers() throws Exception {
    // 5 种递减字号 + 正文 → 最多 3 个标题级
    html: <div style='font-size:40px'>A40</div><div style='font-size:32px'>A32</div>
          <div style='font-size:26px'>A26</div><div style='font-size:21px'>A21</div>
          <div style='font-size:18px'>A18</div><p>正文。</p>
    → 产生的 heading 行数 ≤ 3（A21/A18 降为正文）
}
```
- [ ] **Step 2: 确认失败** → **Step 3: 实现规则 1-2**（规则 3 视断言需要实现或降级为可选）
- [ ] **Step 4: 回归全绿**
- [ ] **Step 5: Commit** `fix(extract): guard heading detection against cover art with 3-tier cap and length limit`

---

### Task 5: 三分支同步 + 推送 + 勾选

- [ ] **Step 1: 3.0.x 全量 `clean verify`**（预期 84 + 新增 ≈ 7-9 = 91+ tests）
- [ ] **Step 2: 同步 1.0.x**（`git checkout feature/3.0.x -- easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/ easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/`）→ **Step 3: 全量 verify** → **Step 4: Commit**
- [ ] **Step 5: 同步 2.0.x** → **Step 6: verify** → **Step 7: Commit**
- [ ] **Step 8: 推送三分支 + `sed 's/- \[ \]/- [x]/g'` 勾选本计划 + commit + push**

---

## Self-Review

- **覆盖**：4 个短板 ↔ Task 1-4 一一对应；三分支 ↔ Task 5
- **事实先行**：fullMarkdown 无去重已读源码确认（`:32-39`）；其余三项均在 Round 1 实测/代码走查中暴露
- **范围纪律**：docling 联调、OCR、倾斜页面明确排除（需外部环境/属其他组件）
- **回归护栏**：每 Task 的 Step 4 均要求既有 84 tests 全绿，防止启发式调整破坏已验收行为
