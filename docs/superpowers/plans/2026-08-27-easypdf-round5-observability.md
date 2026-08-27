# easypdf R5-Observability 计划（参数校验 + 日志 + metrics + 维护性清理）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 智能体场景可观测性：① `pageRange` 参数校验（from>to/越界）；② SLF4J INFO 级结构化日志（file/pages/chars/duration）；③ `ExtractorMetrics` 进程内计数；④ 维护性清理（魔法数字 → `TierConfig` POJO；测试夹具 → `MarkdownTestSupport`；类名 `PageModelListener` → `PageModelCollector` deprecated 兼容）。

**Tech Stack:** SLF4J 2.0（现有）、`AtomicLong`（JDK）；零新依赖；Java 8 语法。

**Global Constraints**

- 改动分散：`convert/EasyPdf.java`、`convert/PdfStructureExtractor.java`、`layout/RuleLayoutAnalyzer.java`、`convert/layout/PageModelListener.java`（改名为 `PageModelCollector.java`）；新建 `convert/TierConfig.java`、`convert/ExtractorMetrics.java`、测试工具类 `MarkdownTestSupport.java`
- 公共 API 兼容性：metrics 提供只读 getter；PageModelListener 保留作为 `@Deprecated` 别名（1 版过渡）
- 既有 145+ tests 全绿（Round 5-Hotfix 后）保持
- Java 8 语法
- 验证：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

---

### Task 1: pageRange 参数校验

**Files:**
- Modify: `convert/EasyPdf.java`（`pageRange(File, int from, int to)` 加 throw）
- Test: RobustnessTest 追加 3 用例

**实现**：from > to 抛 IAE；from ≤ 0 或 to ≤ 0 抛 IAE；to > totalPages 不 throw 但内部 skip 越界页（不回归"无内容即空串"的旧行为）。
- [x] Step 1 写失败测试（RobustnessTest）→ Step 2 确认失败 → Step 3 实现 → Step 4 回归 → Step 5 Commit `fix(extract): validate pageRange parameters`

---

### Task 2: SLF4J INFO 级结构化日志

**Files:**
- Modify: `convert/PdfStructureExtractor.java`（extract / extractWithReport 入口加 logger 与 result 后的摘要 INFO；warnings 数组 WARN 输出）
- Test: 现有 RobustnessTest 扩展 + LoggerRule

**实现要点**：
1. 入口：`log.info("extract file={} pages={} chars={} tables={} images={} durationMs={}", pd.title, rep.pages, rep.chars, rep.tables, rep.images, rep.durationMillis);`（file 取 basename 防日志 PII）
2. warnings 非空：`log.warn("extract warnings: file={} warnings={}", pd.title, rep.warnings);`
3. 失败：`log.warn("extract failed file={} code={} msg={}", pdf.getName(), rep.error.getCode(), rep.error.getMessage());`
- [x] Step 1 复用现有 RobustnessTest 套件（不需新增），Step 2 用 java.util.logging 拦截器或 LogCaptor 不引入依赖——直接断言成功/失败路径无 Exception；Step 3 实现 → Step 4 跑全量 → Step 5 Commit `feat(extract): add structured slf4j logging for extract paths`

> **依赖评估**：SLF4J API 已传递依赖（logback-classic 1.2.3 在 1.0.x；2.0.x/3.0.x 用 jul binding）——直接 `LoggerFactory.getLogger(...)` 即可。

---

### Task 3: ExtractorMetrics 进程内计数

**Files:**
- Create: `convert/ExtractorMetrics.java`
- Modify: `convert/EasyPdf.java`（静态 final instance + 静态 getter）
- Test: ExtractorMetricsTest

**接口**：
```java
public final class ExtractorMetrics {
    public static final ExtractorMetrics INSTANCE = new ExtractorMetrics();
    private final AtomicLong totalExtracts, totalDurationMs, totalFailures;
    private final ConcurrentHashMap<ExtractionException.Code, AtomicLong> failureByCode;
    public void recordSuccess(long durationMs);
    public void recordFailure(ExtractionException.Code code, long durationMs);
    public Map<String,Long> snapshot();
    public void reset();
}
```

**实现要点**：
1. EasyPdf 的 markdownToPdf/pdfToStructured/chunked/summary 成功路径 `recordSuccess(durationMs)`；构造 ExtractionException.Code 的失败路径 `recordFailure(code, durationMs)`
2. `snapshot()` 返回不可变 Map 用于诊断接口（无需暴露 JMX/Spring Boot Actuator）
3. `MarkdownConverter.mdToHtml` 等纯字符串操作不计

- [x] Step 1 写失败测试（ExtractorMetricsTest）：调用 3 次成功 + 2 次 NOT_FOUND + 1 次 LIMIT_EXCEEDED → snapshot 含 6 total / 2 NOT_FOUND / 1 LIMIT_EXCEEDED 总时长累加正确 → Step 2 确认失败 → Step 3 实现（懒初始化 / final class / 全部 Atomic 字段） → Step 4 回归 → Step 5 Commit `feat(extract): add process-level ExtractorMetrics counter`

---

### Task 4: TierConfig 魔法数字 POJO

**Files:**
- Create: `convert/layout/TierConfig.java`
- Modify: `convert/layout/RuleLayoutAnalyzer.java`（静态常量迁移到 TierConfig.DEFAULT；构造器接受 TierConfig 参数）

**接口**：
```java
public final class TierConfig {
    public float columnGapPt = 55f;
    public float streamAlignTolPt = 6f;
    public float coverRunMinLines = 2f;
    public float coverRatio = 1.5f;
    public float headFactor = 1.22f;
    public int maxHeadingTiers = 3;
    public static TierConfig DEFAULT = new TierConfig();
    public static TierConfig from(PdfExtractionProperties p); // 读 props 对应字段
}
```

**实现**：
- RuleLayoutAnalyzer 构造器签名：`RuleLayoutAnalyzer() / RuleLayoutAnalyzer(PdfExtractionProperties) / RuleLayoutAnalyzer(TierConfig)`（向后兼容）
- 所有魔法数字改为读 TierConfig 字段
- 现 properties 已部分接线（headFactor 等），T1/2 字段接入 TierConfig 后 props→TierConfig 转换在 PdfStructureExtractor 入口完成

- [x] Step 1 写失败测试（TierConfigTest）：defaults == DEFAULT 全部值；custom 实例化可覆盖；T1 现有 145+ tests 仍全绿（逐步迁移字段时同步修改静态方法签名）→ Step 2 确认失败 → Step 3 实施（**小步迁移**：先建 TierConfig + 静态常量保留同时新增字段读取，再逐步删除静态常量——保证全量测试任何阶段不破）→ Step 4 回归 → Step 5 Commit `feat(extract): introduce TierConfig POJO for rule-engine thresholds`

---

### Task 5: MarkdownTestSupport 工具类

**Files:**
- Create: `easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/MarkdownTestSupport.java`
- Refactor: 5+ 测试文件中重复的 `HtmlPdfConverter.htmlToPdf + tmp file` 夹具调用

**接口**：
```java
public final class MarkdownTestSupport {
    public static File writePdf(File dir, String filename, String html) throws Exception;
    public static File writeTaggedPdf(File dir, String filename, String html) throws Exception;
}
```

**实现**：将 5+ 测试文件（`RobustnessTest / LatticeTableFinderTest / TaggedGenerationTest / TaggedRoundTripTest / TableContinuationTest / RuleLayoutAnalyzerTier2Test` 等）中的 `HtmlPdfConverter.htmlToPdf(html, baos); Files.write(tmp, baos)` 模式替换为单行 `MarkdownTestSupport.writePdf(dir, "name.pdf", html)`；新增辅助需 ≥2 个测试复用时提取（Rule of Three）。

- [x] Step 1 选 1 个测试改造示范 → Step 2 编译/回归确认 → Step 3 推广到 ≥3 个测试（不强求一次性全部）→ Step 4 回归 → Step 5 单 commit

> 评估：此 Task 收益边际递减，建议**最简化推进**（仅抽 1 个公共方法 + 改造 2-3 个测试即可）——不列为必须。

---

### Task 6: PageModelListener → PageModelCollector 重命名（保留兼容）

**Files:**
- Create: `convert/layout/PageModelCollector.java`（新名）
- Modify: `convert/layout/PageModelListener.java`（仅 `@Deprecated extends PageModelCollector` 标 + 委托构造 + 静态方法委托）
- Modify: 引用处（仅 `PageModelListener.collect(...)` 一处） → `PageModelCollector.collect(...)`

**实现**：
1. `class PageModelCollector implements IEventListener`（原内容迁移）
2. `class PageModelListener extends PageModelCollector { @Deprecated(...) PageModelListener(int pageNo) { super(pageNo); } public static List<PageModel> collect(PdfDocument doc) { return PageModelCollector.collect(doc); } }`
3. 单点调用由子智能体机械替换

- [x] Step 1 写失败测试：`PageModelCollectorTest`（与原 `PageModelListenerTest` 等价）→ Step 2 确认失败 → Step 3 实施（不留中间态——一次性合并：PageModelCollector 新建 + PageModelListener 改为薄壳 deprecated）→ Step 4 回归 → Step 5 Commit `refactor(extract): rename PageModelListener to PageModelCollector (keep deprecated alias)`

---

### Task 7: 三分支同步 + 推送

- [x] Step 1: 3.0.x verify → Step 2 同步 1.0.x/2.0.x → Step 3 push → Step 4 勾选 + commit

---

## Self-Review

- **覆盖**：D1 参数校验 ✓（T1）/ D2 日志 ✓（T2）/ D3 metrics ✓（T3）/ D4 chunk 默认值（已留作文档层，未在本计划实施）/ E1 魔法数字 ✓（T4 TierConfig）/ E2 夹具 ⃟（T5 最简化）/ E3 类名 ✓（T6）
- **范围纪律**：chunk 默认值（动态策略）属下一轮专项；客户端集成 / 发布 / 多租户 全部排除
- **API 兼容**：T4 构造器三态兼容 / T6 deprecated alias 保留 / T3 INSTANCE 单例为新增不破坏旧
- **测试护栏**：每 Task 强制回归 `clean verify` 全绿
