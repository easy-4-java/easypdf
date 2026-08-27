# PDF → Markdown 样式映射（参考 XLSX 评估）

**日期**：2026-08-28
**状态**：基础设施就位（3.1-3.4 完成）。实际颜色/背景渲染留给后续 R 计划。
**背景**：原始评估文档给出 XLSX → Markdown 的样式映射方案。本文件评估该方案
对 PDF → Markdown（easypdf 的 R4 agent API）的可借鉴部分。

## 1. 现状（评估时点）

- `CellStyle` 类不存在于 PDF→Markdown 代码路径；评估文针对的是 XLSX converter
- `MarkdownConverter` 只有 `mdToHtml` 与 `textToMarkdown`，没有 `wrapWithFontMarkers`
- `DocumentTable.headers / rows` 是 `List<List<String>>`，**纯字符串，无 cell 元数据**
- `DocumentStructure.toMarkdown()` 是 GFM 序列化入口，未接受任何配置开关
- `PdfExtractionProperties` 已有 `maxFileBytes` / `maxPages` / REST endpoint 等开关

## 2. XLSX 方案对本项目的可借鉴原则

| XLSX 方案 | 对 PDF→Markdown 的可借鉴性 |
|---|---|
| 开关默认 OFF | **立即可用**：`PdfExtractionProperties.renderHtmlColor = false` |
| HTML escape helper | **立即可用**：`HtmlEscaper.escape(value)`，但只在开启颜色时调用 |
| `CellStyle` 加 2 字段 | **不可直接搬**：PDF 无结构化样式表 |
| XLSX `cellXfs` 采集样式 | **不可直接搬**：PDF 需重写 content stream 解析器 |
| `<span style="color:...">` 渲染 | **可借鉴**：渲染时按开关套 span |

## 3. 已完成（基础设施）

### 3.1 `HtmlEscaper`

`easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/HtmlEscaper.java`

- package-private（仅包内使用）
- 静态 `escape(String)`：`&` → `&amp;`（先转义避免双重转义）、`<` → `&lt;`、`>` → `&gt;`、`"` → `&quot;`
- null 入 → null 出；空串入 → 空串出
- **当前不调用**——纯 GFM 输出场景下 `<` `&` `>` 是字面字符；存在只为未来颜色渲染

### 3.2 `PdfExtractionProperties.renderHtmlColor`

新增 public boolean 字段，默认 `false`：

```java
/**
 * 是否将 PDF 字符级颜色/背景渲染为内嵌 HTML span。
 * 默认 false → 输出纯 GFM，不破坏既有承诺。
 * 开启后输出会含 {@code <span style="...">}，在 GitHub 渲染，
 * 在其他 Markdown 渲染器可能显示为源码或纯文本。
 */
public boolean renderHtmlColor = false;
```

### 3.3 `DocumentStructure.toMarkdown(props)`

新增 overload：`public String toMarkdown(PdfExtractionProperties props)` 与
`public String fullMarkdown(PdfExtractionProperties props)`。

- `props == null` 走默认值（与无参版本一致）
- 当前实现**仅委托**无参版本（行为零差异）
- 注释明确说"3.3 完成前：行为与无参版本完全一致（renderHtmlColor 默认 false，无颜色采集）"

### 3.4 `RenderHtmlColorSwitchTest`

`easypdf-xhtml/src/test/java/io/github/easy4j/pdf/xhtml/convert/RenderHtmlColorSwitchTest.java`

4 个用例：
1. `defaultsRenderHtmlColorIsFalse` — 默认 OFF
2. `toMarkdownWithPropsProducesSameOutputAsWithout` — props=true 与无参输出一致，特殊字符原样
3. `toMarkdownWithNullPropsDoesNotThrow` — props=null 不抛
4. `fullMarkdownWithPropsProducesSameOutputAsWithout` — fullMarkdown 同上

三分支均绿。

## 4. PDF 上颜色/背景采集的技术障碍

| XLSX | PDF |
|---|---|
| `cellXfs` 直接给出 font 索引 | 文字颜色由 graphics state 的 `rg`/`RG`/`g` 操作符决定 |
| `fonts` 段有 `<color rgb="...">` 明文 | 颜色在 content stream 操作符序列里，需完整解析 |
| `fill` 段有 `<patternFill><fgColor>` | 背景色**根本不存在**——只有 painted rectangle |
| 主题色 `theme="1"` 复杂 | 主题色**没有等价物**——PDF 没有"主题"概念 |
| 改动量估 150-200 行 | 估 200-400 行 + 重写 text/graphics 解析器 |

**结论**：在 PDF 上做完整样式映射 ≈ 重写至少一个完整的 text/graphics 解析管线，
**不是**"加两个字段"的简单工作。

## 5. 风险清单（与 XLSX 评估对齐）

| 风险 | 等级 | 缓解 |
|---|---|---|
| 非 GitHub 渲染器看到 HTML 源码 | 中 | 默认 OFF；开关显式开启 |
| 值含 `<` 转义后与 GFM 链接/图片语法交互 | 低 | 转义顺序在 marker 之后；`**` 不受影响 |
| `&amp;` 双重转义失真 | 低 | HtmlEscaper 仅在颜色 span 开启时调用；escape 不幂等（注释明确） |
| XLSX 主题色（`theme="1"`）解析 | n/a | PDF 无主题色 |
| **PDF 颜色采集误判（graphics state 栈错乱）** | 中-高 | 未来 R 计划必须先做精确 regression 用例集 |

## 6. 路线图（不在本轮）

### R6（建议）：PDF 字符级颜色采集

工作量估 200-400 行 + 单元测试 5-8 个。范围：
- 在 `PageChunk` / `RawStroke` 加 `fontColorHex` / `backgroundColorHex`（hex RGB）
- `RuleLayoutAnalyzer` 解析 `rg` / `RG` 操作符，更新 graphics state 栈
- 表格单元识别（已有 `LatticeTableFinder`）叠加 painted rect 判定背景色
- 主题色**不实现**（PDF 无概念）
- 渲染层：`DocumentStructure.toMarkdown(props)` 检测 `props.renderHtmlColor`，开启时按 cell / section 文本节点套 span

### R7（更远）：完整样式保留 → 输出 HTML 表格

如果目标是"完整保留边框 / 合并单元格 / 全颜色"，正确出口是**新 API `toHtml`**
（HTML 表格）而非 markdown。这是新功能增量，不是本轮讨论范围。

## 7. 关联归档

- 3.1-3.4 实现 commit：`feat(pdf-md): scaffold renderHtmlColor switch + HtmlEscaper escape helper`
  - `feature/3.0.x` commit `4b0012d`
  - `feature/2.0.x` commit `d5ee6b0`
  - `feature/1.0.x` commit `4a571a9`

- 受影响的测试：`RenderHtmlColorSwitchTest`，4 用例 三分支均绿

- 已对每条原则加 javadoc 注释，避免后人误读"已完成色彩渲染"