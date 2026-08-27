# easypdf 提取引擎 Round 3 计划（提取质量 8 项 + Tagged 2 项 + 工程健壮性 4 项）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成三条线共 14 项优化：① 跨页表格续接、② 合并单元格、③ 嵌套列表层级、④ 代码块检测、⑤ 页眉剔除已有(跳过)、⑥ 题注识别、⑦ 倾斜页面（明确排除）、⑧ 中英空格阈值配置化；⑨ Word 导出 Tagged 角色适配、⑩ fullMarkdown 深度去重；⑪ 大文件页级流式 API、⑫ REST 重试（降级为可选，默认关）、⑬ 结果缓存、⑭ 全部 magic number 配置化。完成后三分支同步推送。

**范围调整说明**：原⑤已在 Round 1 完成（页眉剔除）；⑦倾斜/旋转维持排除；⑫ REST 重试仅加"可配置次数+指数退避"，不做熔断器（避免过度设计）。

**并行分组（文件冲突域）——三个智能体三个 worktree：**

| 组 | 任务 | 独占文件 |
|---|---|---|
| **W1 表格域** | #1 跨页续表、#2 合并单元格 | `LatticeTableFinder.java`（重命名扩展为 TableExtractor 思路保留原名）、`RuleLayoutAnalyzer.java` 的 buildTable 区、新增 `TableContinuationTest.java` |
| **W2 结构域** | #3 嵌套列表、#4 代码块、#6 题注、#8 空格配置化 | `RuleLayoutAnalyzer.java` 行流水线区、`PdfExtractionProperties.java`、新增 `StructureDetectTest.java` |
| **W3 Tagged+工程** | #9 角色适配、#10 深度去重、#11 流式 API、#13 缓存、#14 配置化收尾 | `PdfStructureExtractor.java`、`DocumentStructure.java`、`HtmlPdfConverter.java`（缓存类新增 `ExtractCache.java`）、新增 `RobustnessTest.java` |

**⚠️ W1 与 W2 同改 RuleLayoutAnalyzer.java —— 冲突协调方案**：W1 只动 `buildTable/cellContent/tableRegion` 区（文件尾 Tier1 段）；W2 只动行流水线区（analyze 中段 + 私有判定方法）。**合并顺序强制 W1 先、W2 后**，主控负责解决 merge 冲突后统一回归。W3 文件与两者不相交，可随时合并。

**Tech Stack:** iText 7.1.10（零新依赖；缓存用 LinkedHashMap LRU 手写）、JUnit 5、Java 8 语法。

## Global Constraints

- 公共 API 兼容：现有 `EasyPdf`/`PdfStructureExtractor.extract(File)` 签名不变（#11 新增重载而非修改）
- 既有 92 tests 必须保持全绿（每 Task 回归）
- Java 8 语法；提交风格 `feat(extract): ...` / `fix(tagged): ...`
- 每个 worktree 独立分支 `r3/w1`、`r3/w2`、`r3/w3`，基于 feature/3.0.x
- 验证命令：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

---

## W1 组任务（表格域）

### Task W1-1: 跨页表格续接

**Files:**
- Modify: `layout/RuleLayoutAnalyzer.java`（Tier1 区）
- Test: `layout/TableContinuationTest.java`（新建）

**规则**：
1. 相邻两页各自检出格线表，若满足续接条件则合并为一张：第二张**无独立表头证据**（首行非 bold 且字号=正文）且两表列数相同且首末列 x 对齐（±6pt）
2. 续表的首行不作为 headers（并入 rows）
3. 断言：2 页各 1 张同构表 → 合并后 `doc.tables.size()==1` 且 rows 含两页数据

- [ ] Step 1 写失败测试（HTML 用 `page-break-before:always` 构造 2 页续表）→ Step 2 确认失败 → Step 3 实现 → Step 4 回归 → Step 5 Commit `feat(extract): merge continued tables across pages`

### Task W1-2: 合并单元格（colspan/rowspan 语义）

**Files:**
- Modify: `layout/RuleLayoutAnalyzer.java`
- Test: `TableContinuationTest.java` 追加

**规则**（格线路径启发式，无 colSpan 属性可用）：
1. cell 内文本块横向覆盖 >1 列宽（文本 x 范围跨过中间列线且该列线在行内无分隔作用——即相邻两格该行内容为空）→ 输出 colspan：文本放首个非空列，其余列填空串（保持 Markdown 列数对齐）
2. rowspan 类似：纵向覆盖多行高（单 chunk y 高度 >2×行距且垂直方向无中线穿插）→ 首行填值余行留空
3. 不引入 HTML table 输出（保持 GFM pipe），以"空占位列"保形

- [ ] Step 1 失败测试（bordered 表一格横跨两列宽度）→ Step 2 确认 → Step 3 实现 → Step 4 回归 → Step 5 Commit `feat(extract): heuristics for merged cells in lattice tables`

---

## W2 组任务（结构域）

### Task W2-1: 嵌套列表层级

**Files:**
- Modify: `layout/RuleLayoutAnalyzer.java`
- Test: `layout/StructureDetectTest.java`（新建）

**规则**：Tagged 路径 `readList` 递归深度参数 level→ 输出缩进 `  - `（2 空格/级）；规则路径：列表行 x 起点 ≥ 上级起点+12pt → 子级缩进。断言嵌套 OL/UL 输出含 `  - ` 缩进行。

- [ ] Step 1 失败测试（ul>li>ul>li 两级）→ Step 2 → Step 3（Tagged readList 加 level 参数 + 规则路径 x 缩进推断）→ Step 4 回归 → Step 5 Commit `feat(extract): nested list indentation (tagged recursion depth + x-indent inference)`

### Task W2-2: 代码块检测

**Files:**
- 同上

**规则**：连续 ≥3 行同一等宽字体（fontName 含 mono/courier/consolas）且行距均匀 → 围栏代码块 ```` ``` ````包裹，内容原样保留（不做列表/markdown 二次解析）。`PdfChunk` 无字体名——通过 `PageChunk.bold` 同法新增 `mono` 字段（W2 同时改 PageModelListener 与 PageChunk——注意与 W3 的 PageModelListener 冲突由合并序解决：W2 改 fontName 判定处，W3 只在 Extractor/DocumentStructure）。
**修正**：PageChunk 归 W1/W2 共享冲突——由 **W2 独占 PageChunk/PageModelListener**（其测试需 mono 字段），W3 不碰这两个文件。

- [ ] Step 1 失败测试（pre/code 渲染 PDF → ```` ``` ```` 包裹）→ Step 2 → Step 3 → Step 4 → Step 5 Commit `feat(extract): detect monospace runs as fenced code blocks`

### Task W2-3: 题注识别

**Files:** 同上

**规则**：行匹配 `^(图|表|Figure|Table|Fig\.?)\s*\d+[:：.]?` 且字号 ≤ 正文 → 斜体行 `*原文*` 单独成段。断言输出 `*Figure 1: xxx*`。

- [ ] Step 1-5 流程同上。Commit `feat(extract): italicize figure/table captions`

### Task W2-4: 中英空格阈值配置化

**Files:** `PdfExtractionProperties.java` + `RuleLayoutAnalyzer.java`

**规则**：新属性 `cjkGapFactor`（默认 0.22，现状值）；行构建处读属性。analyzer 构造器接受 properties（现有无参构造委托 defaults）。

- [ ] Step 1 失败测试（设 0.05 时紧凑英文词间不再插空格——构造宽字距夹具或直接单测 `isLatinTail/isLatinHead+gap>size*f` 抽取方法）→ 建议：抽静态方法 `shouldInsertSpace(gap, size, factor)` 直接单测三档 → Step 5 Commit `refactor(extract): configurable cjk gap factor via properties`

---

## W3 组任务（Tagged + 工程健壮性）

### Task W3-1: Word 导出 Tagged 角色适配（#9）

**Files:**
- Modify: `convert/PdfStructureExtractor.java`
- Test: `convert/RobustnessTest.java`（新建）

**规则**：`normRole` 归一化时额外映射别名表：`heading 1..6` / `h1..6` / `标题 1..6` → H1..H6；`table` → Table；`p`/`paragraph`/`正文` → P；`l`/`list` → L；`li`/`list item` → LI；`tr`/`table row` → TR；`td`/`th`/`table header cell` 等。别名表静态 Map。

- [ ] Step 1 失败测试：直接单测私有逻辑不便 → 抽 `static String canonicalRole(String raw)` 包级可见单测 6 个代表值；集成路径用自制 setTagged PDF（Word 样本无法离线获得，注明限制）→ Step 5 Commit `fix(tagged): alias map normalizes word-exported structure roles`

### Task W3-2: fullMarkdown 深度去重（#10）

**Files:**
- Modify: `convert/DocumentStructure.java`
- Test: 强化 `DocumentStructureTest.java`

**规则**：序列化 sections 时跳过与前一个已输出 section 标题相同的空 content 条目（去重从"只查 title vs sections[0]"扩展到"任意相邻重复"）。

- [ ] Step 1 失败测试（两个同名 level-1 空段相邻只输出一次）→ Step 5 Commit `fix(extract): skip adjacent duplicate empty sections in markdown output`

### Task W3-3: 页级流式提取 API（#11 大文件内存）

**Files:**
- Modify: `convert/PdfStructureExtractor.java`（新增方法不动旧签名）
- Test: `RobustnessTest.java`

**接口**：
```java
public interface PageConsumer { void page(int pageNo, DocumentStructure pagePartial); }
public static void extractPerPage(File pdf, PdfExtractionProperties props, PageConsumer consumer)
```
逐页产出 partial DocumentStructure（title 继承、sections/tables 为当页），消费方自行聚合——大文件不再全量驻留。旧 `extract(File)` 内部改调用 extractPerPage 聚合（等价重构）。

- [ ] Step 1 失败测试（3 页文档回调计数=3 且第 N 页含第 N 页独有文本）→ Step 5 Commit `feat(extract): per-page streaming extraction for large documents`

### Task W3-4: 提取结果 LRU 缓存（#13）

**Files:**
- Create: `convert/layout/ExtractCache.java`
- Modify: `PdfStructureExtractor.java`（extract(File,props) 接缓存；key = path+lastModified+len）
- Test: `RobustnessTest.java`

**规则**：`ExtractCache` 手写 LinkedHashMap（accessOrder，容量 16，synchronized）；`PdfExtractionProperties.cacheEnabled` 默认 false（行为不变），开则二次 extract 命中（用计数器验证 parse 未重复执行——以可注入 analyzer 或计数文件读取次数证明；简化：暴露 package 可见命中统计）。

- [ ] Step 1 失败测试（开缓存第二次调用 hits==1）→ Step 5 Commit `perf(extract): optional LRU cache for extraction results`

### Task W3-5: 阈值配置化收尾（#14 + #12）

**Files:**
- Modify: `layout/PdfExtractionProperties.java`、`RestLayoutAnalyzer.java`
- Test: `RobustnessTest.java`

**属性新增**（默认值=现行为）：`headFactor=1.22`、`maxHeadingTiers=3`、`coverRunMinLines=2`、`coverRatio=1.5`、`columnGapPt=55`、`streamAlignTolPt=6`、`restRetries=0`（>0 时指数退避重试 429/5xx/IOException，上限 3 次）。

- [ ] Step 1 失败测试（改 headFactor 影响 tiny-case 判定 或直接断言默认值存在 + restRetries 用本地 HttpServer 前 1 次 500 第 2 次 200 验证 retries=1 成功）→ Step 5 Commit `feat(extract): configurable extraction thresholds and rest retries with backoff`

---

### Task M（主控）: 三分支同步 + 合并冲突处置 + 推送 + 勾选

- [ ] 合并序：r3/w1 → r3/w2（解决 RuleLayoutAnalyzer 冲突，跑双方测试）→ r3/w3 → 全量 verify → 同步 1.0.x/2.0.x → push → 勾选本计划 → 清理 worktree

---

## Self-Review

- **覆盖**：一(#1✓W1-1 #2✓W1-2 #3✓W2-1 #4✓W2-2 #6✓W2-3 #8✓W2-4)；二(#9✓W3-1 #10✓W3-2)；三(#11✓W3-3 #13✓W3-4 #14+#12✓W3-5)；明确不含 #15 发布、#7 倾斜、#16 docling 真实联调
- **冲突预案**：PageChunk/PageModelListener 归 W2 独占；RuleLayoutAnalyzer 双方分段 + 强制合并序 W1→W2 主控裁决
- **API 兼容**：全部新增重载/新增类/properties 默认值=旧行为；92 tests 护栏不变
