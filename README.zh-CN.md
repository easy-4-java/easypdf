# easypdf

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://github.com/easy-4-java/easypdf) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

基于 iText 7 的纯 Java PDF 工具库：用九种可插拔模板引擎把 Markdown / XHTML 模板渲染成 PDF，并把 PDF 读回为面向 LLM 智能体的结构化内容（标题/表格/列表/图片）——默认零本地依赖、零外部服务。

详细使用指南：[docs/USAGE.md](./docs/USAGE.md)。

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 功能与状态](#2-功能与状态)
- [3. 环境要求与兼容性](#3-环境要求与兼容性)
- [4. 架构与模块](#4-架构与模块)
- [5. 安装](#5-安装)
- [6. 快速开始](#6-快速开始)
- [7. 配置](#7-配置)
- [8. 核心用法 / API](#8-核心用法--api)
- [9. 测试与构建](#9-测试与构建)
- [10. 版本线与分支](#10-版本线与分支)
- [11. 参与贡献与许可协议](#11-参与贡献与许可协议)

## 1. 项目概览

`easypdf` 是一个**纯 PDF** 库：一边把 Markdown/XHTML 生成为普通或 Tagged PDF，一边把 PDF 反向读回 Markdown 或结构化文档树（`DocumentStructure`），供智能体按"摘要 → 页区间 → 切片"导航。3.0.x 线已是 PDF-only 架构，旧的 docx4j Word 管线已移除。

| 是什么 | 不是什么 |
|:---|:---|
| Markdown / XHTML → PDF 生成（`EasyPdf.markdownToPdf`、`PdfTemplate` + 9 引擎） | Word/.docx 库 |
| PDF → 结构化提取引擎（Tier1 格线 + Tier2 启发式 + Tier3 REST ML 扩展点） | 扫描件 OCR 引擎（无文本层不在范围内） |
| 面向智能体的 API：`summary` / `pageRange` / `chunked` | 云文档服务 |
| 自有 Tagged PDF 的无损往返 | PDF 编辑器或阅读器 |

## 2. 功能与状态

| 功能 | 状态 | 说明 |
|:---|:---|:---|
| Markdown → PDF / Tagged PDF | 可用 | `EasyPdf.markdownToPdf(...)` / `markdownToPdfTagged(...)`（GFM 标题、表格、代码块、列表） |
| PDF → Markdown / 结构化 Markdown | 可用 | `EasyPdf.pdfToMarkdown(File)` / `pdfToStructuredMarkdown(File)` |
| 结构提取（规则引擎） | 可用 | `PdfStructureExtractor.extract(File)` —— Tier1 格线表格 + 内嵌图片，Tier2 字号聚类标题/分栏/流式表格/列表/页眉页脚剔除 |
| 提取质量分级 | 可用 | 三条路径，见下表 |
| Agent API | 可用 | `EasyPdf.summary` / `pageRange` / `chunked`；`PdfStructureExtractor.extractPerPage` 页级流式（可中断） |
| 错误分级与报告 | 可用 | `ExtractionException.Code`（`CORRUPT`/`ENCRYPTED`/`LIMIT_EXCEEDED`/`NOT_FOUND`），永不抛异常的 `extractWithReport` 返回 `ExtractReport` |
| 安全护栏 | 可用 | `maxFileBytes` / `maxPages` 上限，解析前拒绝超大或恶意构造的 PDF |
| 模板引擎 | 可用 | Freemarker / Velocity / Thymeleaf / Beetl / Rythm / Jetbrick / HTTL / Webit / JSP 适配器，产物均为 PDF |
| Spring WebMVC 视图层 | 可用 | `easypdf-webmvc`（本线为 `jakarta.servlet`） |

### 三条质量路径

还原率取决于 PDF 的来源。easypdf 不给单一数字，而是提供三条互补路径：

| 路径 | 入口 | 保真度 | 适用范围 |
|:---|:---|:---|:---|
| Tagged 往返 | 先 `markdownToPdfTagged` 再 `pdfToStructuredMarkdown` | ≈100%（标题层级/列表/表格/正文；图片以 data URI 保留） | 仅限 easypdf 自己生成的 PDF（生成时写入结构树） |
| 规则引擎（Tier1+Tier2） | `pdfToStructured(File)` / `pdfToStructuredMarkdown(File)`，默认 | 常规电子版办公 PDF ≈80% | 无网络、零额外依赖；无文本层的扫描件不在范围 |
| REST ML 布局服务（Tier3） | `PdfExtractionProperties.engine = REST` + `restEndpoint` | 90–95%（由外部服务决定） | 将原始 PDF 字节 POST 给布局服务（docling/MinerU 类）；AUTO 模式不可达时静默回退 RULE |

## 3. 环境要求与兼容性

| 要求 | 版本（3.0.x 线） |
|:---|:---|
| JDK | 21 |
| Maven | 3.0+（仓库自带 `./mvnw` 包装器） |
| iText | 7.1.10 kernel + pdfHTML（`html2pdf` 2.1.7） |

提取相关源码保持 Java 8 兼容语法，同一份源码可跑在三条版本线上。

### 版本线矩阵

| 分支 | JDK | 版本模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

三条线的 Java 源码、注释与文档完全一致，仅 JDK 基线与对应依赖版本不同。

## 4. 架构与模块

```text
  输入                      easypdf 模块                           输出
  ----                      ------------                           ----
  Markdown 字符串 ---------> easypdf-xhtml EasyPdf 门面
                              |- markdownToPdf(Tagged)             普通 / tagged .pdf
                              '- pdfToMarkdown / pdfToStructured   文本 / DocumentStructure
  XHTML 模板 --------------> easypdf-core HtmlPdfConverter
                              |- htmlToPdf / htmlToPdfTagged       .pdf
                              '- pdfToText                         扁平文本
  Freemarker/Velocity/... -> easypdf-{freemarker,...} {Engine}PdfTemplate
                                                       |                 .pdf
  HTTP 请求 ---------------> easypdf-webmvc PdfViewResolver / AbstractITextPdfView
                                                       v
                                            Agent API：summary → pageRange → chunked
```

| 模块 | 职责 |
|:---|:---|
| `easypdf-core` | `PdfTemplate` 抽象、`HtmlPdfConverter`（`htmlToPdf` / `htmlToPdfTagged` / `pdfToText`、字体注册）、共享转换上下文 |
| `easypdf-xhtml` | `EasyPdf` 门面（Markdown ↔ PDF）、结构树模型（`DocumentStructure` 等）、三层提取引擎（`convert/layout`：规则分析器、REST 分析器、LRU 缓存）、Agent API（`summary` / `pageRange` / `chunked`） |
| `easypdf-freemarker` / `-velocity` / `-thymeleaf` / `-beetl` / `-rythm` / `-jetbrick` / `-httl` / `-webit` / `-jsp` | 每个模板引擎一个 `{Engine}PdfTemplate` 适配器 |
| `easypdf-webmvc` | Spring WebMVC 集成（`jakarta.servlet`）：`PdfViewResolver`、`AbstractITextPdfView`、`PdfTemplateView` |
| `easypdf-bom` | 依赖版本管理 BOM |

## 5. 安装

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>easypdf-xhtml</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

按需追加引擎模块，例如：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>easypdf-freemarker</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:easypdf-xhtml:3.0.x.x.20260630-SNAPSHOT'
implementation 'io.github.easy4j:easypdf-freemarker:3.0.x.x.20260630-SNAPSHOT'
```

**获取方式**：构件通过 GitHub Releases 发布，尚未发布到 Maven Central。

## 6. 快速开始

```java
import io.github.easy4j.pdf.xhtml.convert.EasyPdf;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;

import java.io.File;

// Markdown → PDF（GFM 标题/表格/代码块/列表自动渲染）
EasyPdf.markdownToPdf("# 你好\n\n世界", new File("hello.pdf"));

// PDF → 结构化 Markdown（标题/表格/列表还原）
String md = EasyPdf.pdfToStructuredMarkdown(new File("hello.pdf"));

// PDF → 可导航的结构树
DocumentStructure doc = EasyPdf.pdfToStructured(new File("report.pdf"));
doc.title;                      // 文档标题
doc.sections.get(0).level;      // 标题级别（1..6）
doc.sections.get(0).content;    // 正文文本
doc.tables.get(0).rows;         // List<List<String>>
```

更多端到端示例（Tagged 无损往返、智能体链路、REST 扩展、错误处理）见 [docs/USAGE.md](./docs/USAGE.md)。

## 7. 配置

转换不需要任何配置。提取行为通过 `io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties` 调整（传给 `PdfStructureExtractor.extract(File, props)`）；全部字段为 public 且带安全默认值，只需覆盖关心的项：

| 字段 | 类型 | 默认值 | 说明 |
|:---|:---|:---|:---|
| `engine` | `Engine` 枚举 | `AUTO` | `AUTO`=REST 可达则用否则 RULE；`RULE`=仅规则引擎（零外部依赖）；`REST`=仅 REST 布局服务，失败即失败 |
| `restEndpoint` | `String` | `null` | Tier3 布局服务地址（POST body=原始 PDF 字节，响应 JSON 契约见 USAGE.md）；`engine=REST` 时必填 |
| `restTimeoutMillis` | `int` | `10000` | REST 连接/读取超时 |
| `restRetries` | `int` | `0` | 对 429/5xx/IOException 的重试次数，指数退避（base 500ms，上限 3 次） |
| `maxFileBytes` | `long` | `104857600`（100 MB） | 护栏：超过该大小的文件在解析前直接以 `LIMIT_EXCEEDED` 拒绝 |
| `maxPages` | `int` | `5000` | 护栏：超过该页数的文档以 `LIMIT_EXCEEDED` 拒绝 |
| `cacheEnabled` | `boolean` | `false` | LRU 提取缓存（共享实例容量 16；key=路径+修改时间+长度，文件变化自然失效） |
| `headFactor` | `float` | `1.22f` | 字号 ≥ 正文×该系数视为标题候选 |
| `maxHeadingTiers` | `int` | `3` | 标题字号最多档位数，超出档位降为正文 |
| `columnGapPt` | `float` | `55f` | 触发分栏检测的最小空白间隙（pt） |
| `streamAlignTolPt` | `float` | `6f` | 流式（无边框）表格列起始 x 的跨行对齐容差（pt） |
| `coverRatio` | `float` | `1.5f` | 封面艺术字相对次大字号的比例阈值 |
| `coverRunMinLines` | `int` | `2` | 构成封面艺术字 run 的最少连续行数 |
| `cjkGapFactor` | `float` | `0.22f` | 中英文字间空格系数：净间隙 > 前 chunk 字号×该系数时补一个空格 |

示例：

```java
PdfExtractionProperties p = PdfExtractionProperties.defaults();
p.maxFileBytes = 20L * 1024 * 1024;   // 对不可信上传收紧上限
p.cacheEnabled = true;                // 未变化文件的重复提取走缓存
DocumentStructure doc = PdfStructureExtractor.extract(pdf, p);
```

## 8. 核心用法 / API

所有入口集中在 `easypdf-xhtml` 的 `EasyPdf` 门面。以下签名逐一取自源码。

### 8.1 门面方法

| 方法 | 返回 |
|:---|:---|
| `markdownToPdf(String markdown, File output)` / `(String, OutputStream)` | `void` |
| `markdownToPdfTagged(String markdown, File output)` / `(String, OutputStream)` | `void`（Tagged PDF，用于无损往返） |
| `pdfToMarkdown(File pdf)` / `(InputStream in)` | `String` 尽力而为的 Markdown |
| `pdfToStructuredMarkdown(File pdf)` / `(InputStream in)` | `String` 结构化 Markdown（标题/表格/列表） |
| `pdfToStructured(File pdf)` | `DocumentStructure` 结构树 |
| `summary(File pdf)` / `(InputStream in, String filename)` | `DocumentSummary` |
| `pageRange(File pdf, int fromPage, int toPage)` | `String` 1 起算闭区间页码的 Markdown |
| `chunked(File pdf, ChunkOptions opts)` | `List<DocumentChunk>` RAG 友好切片 |

### 8.2 Agent API——先看摘要，再按需读取

```java
// 1) 低成本概览：页数/字符数/表格数/图片数 + level≤2 章节骨架
DocumentSummary s = EasyPdf.summary(new File("annual-report.pdf"));
for (DocumentSummarySection sec : s.sections) {
    System.out.println("p" + sec.pageNo + " L" + sec.level + " " + sec.title);
}

// 2) 任务只关心第 12–18 页时按页区间取内容
String detail = EasyPdf.pageRange(new File("annual-report.pdf"), 12, 18);

// 3) 或整篇切分为 Embedding 友好的切片
ChunkOptions opts = new ChunkOptions();     // maxChars=800, overlapChars=100
opts.idPrefix = "annual-report.pdf";
List<DocumentChunk> chunks = EasyPdf.chunked(new File("annual-report.pdf"), opts);
```

整篇文档无需驻留 LLM 上下文：先按目录树导航，再按需取页区间，检索场景一次切片。

### 8.3 流式提取与取消

```java
PdfStructureExtractor.extractPerPage(pdf, props, (pageNo, partial) -> {
    handle(partial);            // 单页的 sections/tables/images
    return pageNo < stopAt;     // 返回 false 即取消：后续页不再解析
});
```

每次回调收到单页的 partial `DocumentStructure`；内存中同时只驻留一页结果。REST 引擎整篇结果以 `pageNo == 0` 单次回调。

### 8.4 错误分级处理

```java
try {
    DocumentStructure doc = PdfStructureExtractor.extract(upload);
} catch (ExtractionException e) {
    switch (e.getCode()) {
        case NOT_FOUND:      respond(404); break;
        case ENCRYPTED:      respond(415, "受密码保护"); break;
        case CORRUPT:        respond(422, "无法读取的 PDF"); break;
        case LIMIT_EXCEEDED: respond(413, "超出大小限制"); break;
    }
}
// 或选择永不抛异常的变体：
ExtractReport r = PdfStructureExtractor.extractWithReport(upload, props);
if (!r.success) { log(r.error); } else { metrics(r.pages, r.chars, r.durationMillis); }
```

`ExtractionException extends IOException`，既有 `catch (IOException)` 的调用方无需改动。

### 8.5 模板引擎渲染

```java
FreemarkerPdfTemplate tpl = new FreemarkerPdfTemplate();   // 每个引擎一个模块
tpl.process("invoice.ftl", variables, new FileOutputStream("invoice.pdf"));
```

## 9. 测试与构建

```bash
./mvnw clean verify        # 构建全部模块、运行测试并产出覆盖率报告
./mvnw clean install       # 安装全部模块到本地仓库
```

- `easypdf-xhtml` 承载转换/提取测试套件（门面、结构模型、切片器、摘要构建器、健壮性用例）。
- 覆盖率由 JaCoCo Maven 插件度量。
- `release` profile 组装 GPG 签名 + sources + Javadoc + 部署（`./mvnw -Prelease clean deploy`）。

## 10. 版本线与分支

三条并行维护的版本线：Java 源码、注释与文档在各线之间完全一致，仅 JDK 基线与对应 Maven 依赖版本不同：

| 分支 | JDK | 版本模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

维护策略：三条线均跟踪 pdf-only 架构；功能开发以 3.0.x 线为主。

## 11. 参与贡献与许可协议

欢迎参与贡献——请针对对应版本线分支提 issue 或 PR（JDK 21 相关变更针对 `feature/3.0.x`）。

本项目基于 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) 授权，详见仓库根目录的 `LICENSE` 文件。

参考：
- https://itextpdf.com/
