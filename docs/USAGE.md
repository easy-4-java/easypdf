# easypdf 使用指南（USAGE）

> 面向使用场景的端到端指南；README 是能力总览，本文是上手路径。所有代码片段的签名逐一取自 3.0.x 源码（包 `io.github.easy4j.pdf.xhtml.convert`），中英双语注释。

## 目录

- [1. Markdown → PDF](#1-markdown--pdf)
- [2. Tagged 无损往返：生成即保真](#2-tagged-无损往返生成即保真)
- [3. 结构化提取：pdfToStructured / pdfToStructuredMarkdown](#3-结构化提取pdftostructured--pdftostructuredmarkdown)
- [4. Agent 链路：summary → pageRange → chunked](#4-agent-链路summary--pagerange--chunked)
- [5. 大文件：extractPerPage 页级流式与取消](#5-大文件extractperPage-页级流式与取消)
- [6. Tier3 扩展点：接入 REST 布局服务](#6-tier3-扩展点接入-rest-布局服务)
- [7. 错误分级：ExtractionException.Code 与 ExtractReport](#7-错误分级extractionexceptioncode-与-extractreport)
- [附录 A：PdfExtractionProperties 全字段默认值](#附录-apdfextractionproperties-全字段默认值)

---

## 1. Markdown → PDF

最短路径：一行代码把 GFM Markdown（标题/段落/表格/代码块/列表）渲染为 PDF。

```java
import io.github.easy4j.pdf.xhtml.convert.EasyPdf;
// import io.github.easy4j.pdf.xhtml.convert.EasyPdf;

import java.io.File;
// import java.io.File;

// 写文件 / write to file
EasyPdf.markdownToPdf("# 季度报告\n\n| 指标 | 值 |\n|---|---|\n| GMV | 1.2亿 |", new File("q3.pdf"));

// 或写输出流（Web 下载场景）/ or stream it (e.g. HTTP download)
OutputStream out = response.getOutputStream();
EasyPdf.markdownToPdf(markdownBody, out);
```

中文等 CJK 字体若渲染为空白，注册字体后再转换：

```java
io.github.easy4j.pdf.core.convert.HtmlPdfConverter.registerFont("/usr/share/fonts/simsun.ttf");
// register a TTF before conversion if CJK glyphs render blank
```

对应能力矩阵中的"普通 PDF"：外来工具可读、复制可行，但没有语义结构树——需要语义还原时用下一节的 Tagged 版本。

## 2. Tagged 无损往返：生成即保真

`markdownToPdfTagged` 在生成时写入 PDF 结构树（PDF/UA 风格的 Tagged PDF）；之后任何一份这样的文件用 `pdfToStructuredMarkdown` 读回，标题层级/列表/表格按结构角色（H1–H6/L/Table）**语义级还原，保真度 ≈100%**。

```java
// 第一步：生成 Tagged PDF（带结构树）
// Step 1: generate a tagged PDF (structure tree embedded)
EasyPdf.markdownToPdfTagged(md, new File("archived.pdf"));

// …… months later / 数月后……

// 第二步：读回时利用结构树，正文/表格/列表逐项无损
// Step 2: read back via the structure tree — headings/lists/tables come back intact
String restored = EasyPdf.pdfToStructuredMarkdown(new File("archived.pdf"));
```

适用边界：

- 仅对 **easypdf 自己生成的** Tagged PDF 成立；外来 PDF 一般没有可用结构树，走第 3 节的规则引擎。
- 图片以 data URI 保内容；Tagged 角色识别时会做角色归一化（Word 导出的 `heading 1`、`h1`、`标题 1` 等别名会映射为标准 `H1`）。
- 快速自检：往返结果可直接与原文做归一化比对（去空白后逐项相等）。

## 3. 结构化提取：pdfToStructured / pdfToStructuredMarkdown

对任意电子版 PDF（含第三方产出），规则引擎（Tier1 格线表格+图片、Tier2 字号聚类标题/分栏/流式表格/列表/页眉页脚剔除）给出尽力而为的结构树，典型办公文档约 80% 还原。想要字符串还是对象树，二选一：

```java
// 对象树：智能体/程序化导航首选
// Object tree: preferred for programmatic navigation
DocumentStructure doc = EasyPdf.pdfToStructured(new File("third-party.pdf"));

doc.title;                    // 文档标题 / document title
for (DocumentSection sec : doc.sections) {
    sec.level;                // 标题级别 1..6（0 为隐式继承段）/ heading level
    sec.page;                 // 所在页码（流式回调里写入）/ page number
    sec.content;              // 正文文本 / body text
    sec.children;             // 子章节（树形）/ child sections
    sec.tables;               // 当节表格 headers=List<List<String>>, rows=List<List<String>>
    sec.images;               // 当节图片 src（内嵌图为 data URI）/ images (data URI)
}

// 等价的 Markdown 字符串 / same thing as a markdown string
String md = EasyPdf.pdfToStructuredMarkdown(new File("third-party.pdf"));

// 只要纯文本（无结构，最快）/ flat text only (fastest, no structure)
String text = io.github.easy4j.pdf.core.convert.HtmlPdfConverter.pdfToText(file);

// 底层入口：需要传 PdfExtractionProperties 时直接用提取器
// low-level entry when custom properties are needed
DocumentStructure doc2 = PdfStructureExtractor.extract(file, props);   // 另有单参重载 extract(File)
```

已知边界（诚实声明）：无文本层的扫描件不在此范围内（属 OCR）；`pdfToMarkdown(File)` 是旧行为的"扁平文本整理版"，结构敏感场景请用 `pdfToStructuredMarkdown`。

## 4. Agent 链路：summary → pageRange → chunked

面向 LLM 的标准用法是三段式：先看目录骨架决定读什么，再取页区间精读，或整篇切片进向量库。全程避免把整篇文档塞进上下文。

```java
File report = new File("annual-report.pdf");

// ① summary：页数/字符数/表格数/图片数 + level≤2 章节骨架（便宜、快）
// Step 1: cheap overview — counts + section skeleton up to level 2
DocumentSummary s = EasyPdf.summary(report);
System.out.println(s.title + ", pages=" + s.totalPages);
for (DocumentSummarySection sec : s.sections) {
    // title / level / pageNo / charCount / tableCount / imageCount
    System.out.println("p" + sec.pageNo + " L" + sec.level + " " + sec.title
            + " (" + sec.charCount + " chars)");
}

// ② pageRange：只取任务相关页（页码 1 起算、闭区间）
// Step 2: fetch only relevant pages (1-based, inclusive)
String financials = EasyPdf.pageRange(report, 12, 18);

// ③ chunked：RAG/Embedding 切片（默认单片 800 字符、相邻重叠 100 字符）
// Step 3: RAG-ready chunks (default maxChars=800, overlapChars=100)
ChunkOptions opts = new ChunkOptions();
opts.idPrefix = "annual-report.pdf";          // chunk.id 前缀 + source 字段
List<DocumentChunk> chunks = EasyPdf.chunked(report, opts);
for (DocumentChunk c : chunks) {
    // id / source / title / pageStart / pageEnd / level / text / charCount
}
```

字节流入（上传接口）同样覆盖：

```java
DocumentSummary s  = EasyPdf.summary(inputStream, "annual-report.pdf"); // filename 用于摘要溯源
String md          = EasyPdf.pdfToMarkdown(inputStream);
String structured  = EasyPdf.pdfToStructuredMarkdown(inputStream);
```

调参提示：`chunked` 直接作用于完整结构树；对超大文档想省内存，见下节先按页聚合再切片。

## 5. 大文件：extractPerPage 页级流式与取消

全量 `extract` 会把整篇结构驻留内存；几千页的 PDF 应改用页级流式：每页解析完立即回调，内存同时只有一页，且消费方可随时取消。

```java
import io.github.easy4j.pdf.xhtml.convert.PdfStructureExtractor.PageConsumer;
// PageConsumer: boolean page(int pageNo, DocumentStructure pagePartial)

List<DocumentStructure> kept = new ArrayList<>();
PdfStructureExtractor.extractPerPage(bigPdf, props, new PageConsumer() {
    @Override
    public boolean page(int pageNo, DocumentStructure partial) {
        if (!matches(partial)) {
            return true;             // 返回 true 继续 / return true to continue
        }
        kept.add(partial);
        return pageNo < 2000;        // 返回 false 取消：后续页不再解析、不再回调
                                     // return false to cancel streaming early
    }
});
```

行为细节：

- 回调里的 `partial` 只含当页产物（title 继承文档标题）；页码从 1 起。
- 每页独立分析（无跨页统计），跨页断词合并、全局字号聚类等全局优化不生效——追求最高质量用全量 `extract`，大文件用本方法，这是显式权衡。
- 聚合各页时可参考库内的包级聚合逻辑（把后续页的隐式继承段并入上一节）自行实现。
- REST 引擎无法按页切分：整篇结果以一次 `page(0, wholeDoc)` 回调交付。

## 6. Tier3 扩展点：接入 REST 布局服务

规则引擎之外，easypdf 预留了 ML 布局模型扩展点：把 PDF 字节 POST 给外部布局理解服务（docling / MinerU 类部署即可），服务返回约定的 JSON。接入只需配置，不改代码。

服务端契约（你自己的服务要实现的全部内容）：

```text
POST {restEndpoint}
Content-Type: application/pdf          # body = 原始 PDF 字节 / raw PDF bytes
Response 200:
{
  "title": "string",
  "sections": [ {"title": "string", "level": 1, "content": "string"} ],
  "tables":   [ {"headers": [["h1","h2"]], "rows": [["a","b"]]} ]
}
```

客户端配置：

```java
PdfExtractionProperties p = PdfExtractionProperties.defaults();
p.engine         = PdfExtractionProperties.Engine.REST; // 或 AUTO（推荐生产用）
p.restEndpoint   = "http://layout-svc:8080/analyze";
p.restTimeoutMillis = 15000;                             // 连接/读取超时
p.restRetries    = 2;                                    // 429/5xx/IOException 指数退避重试

DocumentStructure doc = PdfStructureExtractor.extract(pdf, p);
```

三种引擎模式的行为差异：

| engine | 服务可达 | 服务不可达 |
|:---|:---|:---|
| `AUTO`（默认） | 用 REST 结果 | 记 WARN 后静默回退 RULE |
| `RULE` | 不发请求 | —（始终本地规则） |
| `REST` | 用 REST 结果 | 抛出失败（适合强依赖高质量的场景） |

注意：`engine=REST` 时 `restEndpoint` 必须非空（空则构造分析器抛 `IllegalArgumentException`）。质量预期 90–95%，由外部服务决定；本仓库只定义契约与回退策略。

## 7. 错误分级：ExtractionException.Code 与 ExtractReport

解析失败不再是一律 IOException：`ExtractionException extends java.io.IOException` 且携带分类码，既有 `catch (IOException)` 代码无需改动即可平滑升级。

```java
try {
    DocumentStructure doc = PdfStructureExtractor.extract(untrustedUpload, props);
} catch (ExtractionException e) {
    switch (e.getCode()) {
        case NOT_FOUND:      // 文件不存在（沿用 "PDF not found" 语义）
            respond(404); break;
        case ENCRYPTED:      // 口令保护/加密 PDF，需用户解密后重试
            respond(415, "password protected PDF"); break;
        case CORRUPT:        // 字节损坏或不构成合法 PDF
            respond(422, "corrupt pdf"); break;
        case LIMIT_EXCEEDED: // 超出 maxFileBytes / maxPages 护栏
            respond(413, "file beyond size/page limits"); break;
    }
} catch (IOException e) {
    // 其余 I/O 故障（磁盘、网络流等）
    respond(500);
}
```

判定规则（实现于 `extract(File, props)` 解析前后）：

| Code | 触发条件 |
|:---|:---|
| `NOT_FOUND` | 路径不是存在的普通文件 |
| `LIMIT_EXCEEDED` | `length() > maxFileBytes`（默认 100 MB）或 `getNumberOfPages() > maxPages`（默认 5000） |
| `ENCRYPTED` | 打开文件即失败且异常消息含 password/encrypt（如口令保护 PDF） |
| `CORRUPT` | 打开失败的其余情况（非法字节流等） |

批处理/异步管线推荐永不抛异常的变体，附观测指标：

```java
ExtractReport r = PdfStructureExtractor.extractWithReport(untrustedUpload, props);
if (!r.success) {
    log.warn("extract failed: {}", r.error.getMessage());   // r.error = ExtractionException
} else {
    log.info("pages={} chars={} tables={} images={} ms={}",
             r.pages, r.chars, r.tables, r.images, r.durationMillis);
}
r.warnings.forEach(log::warn);      // 如 "no text extracted"（无文本层扫描件预警）
```

---

## 附录 A：PdfExtractionProperties 全字段默认值

静态工厂 `PdfExtractionProperties.defaults()` 返回以下默认状态；全字段 public，按需覆盖。

| 字段 | 类型 | 默认值 | 说明 |
|:---|:---|:---|:---|
| `engine` | `Engine` | `AUTO` | 提取引擎：AUTO/RULE/REST |
| `restEndpoint` | `String` | `null` | Tier3 布局服务地址 |
| `restTimeoutMillis` | `int` | `10000` | REST 超时（毫秒） |
| `restRetries` | `int` | `0` | REST 重试次数（429/5xx/IOException；指数退避 base 500ms，上限 3 次） |
| `maxFileBytes` | `long` | `104857600L` | 文件大小护栏（100 MB） |
| `maxPages` | `int` | `5000` | 页数护栏 |
| `cacheEnabled` | `boolean` | `false` | 提取结果 LRU 缓存（容量 16，key 含路径/mtime/长度） |
| `headFactor` | `float` | `1.22f` | 标题字号判定因子 |
| `maxHeadingTiers` | `int` | `3` | 标题字号最多档位数 |
| `columnGapPt` | `float` | `55f` | 分栏最小间隙（pt） |
| `streamAlignTolPt` | `float` | `6f` | 流式表格列对齐容差（pt） |
| `coverRatio` | `float` | `1.5f` | 封面艺术字比例阈值 |
| `coverRunMinLines` | `int` | `2` | 封面艺术字最少连续行数 |
| `cjkGapFactor` | `float` | `0.22f` | 中英文词间空格判定系数 |

构建命令（三条版本线一致）：`./mvnw -pl easypdf-xhtml -am clean verify`
