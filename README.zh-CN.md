# easypdf

[English](./README.md) | [简体中文](./README.zh-CN.md)

基于 docx4j / iText 与多种模板引擎，快速生成 Word / PDF 文档。支持从模板（Freemarker、Velocity、Thymeleaf、Beetl、Rythm、Jetbrick、HTTL、Webit、JSP）或直接由 XHTML 渲染 WordprocessingML 文档，核心模块同时提供基于 iText 的 PDF 支持。

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 功能与状态](#2-features--status)
- [3. 环境要求与兼容性](#3-requirements--compatibility)
- [4. 架构与模块](#4-architecture--modules)
- [5. 安装](#5-installation)
- [6. 快速开始](#6-quick-start)
- [7. 配置](#7-configuration)
- [8. 核心用法 / API](#8-core-usage--api)
- [9. 测试与构建](#9-testing--build)
- [10. 版本线与分支](#10-versioning--branches)
- [11. 参与贡献与许可协议](#11-contributing--license)

## 1. 项目概览

`easypdf`（项目描述：*Building Word/PDF documents based on XHTML templates using Docx4j and iText*）是 `easydoc` 的 Word/PDF 姊妹项目。核心模块（`easypdf-core`）定义 `WordprocessingMLTemplate` 抽象以及 docx4j/WML 工具与 iText 上下文支持；各模板引擎适配模块与 XHTML 导入路径分别独立成模块。

| 是什么 | 不是什么 |
|:---|:---|
| 基于 docx4j 的模板化 Word（.docx）生成 | 纯 PDF 库（PDF 支持基于 iText，位于核心模块） |
| 可插拔模板引擎 + XHTML 导入 | 文档编辑器或查看器 |
| 面向 PDF 工作流的 iText 上下文 / 字体 / 缓存工具 | 云文档服务 |

典型使用场景：

| 场景 | 模块 |
|:---|:---|
| 用变量 Map 填充 Word 模板 | `easypdf-core`（`WordprocessingMLDocxTemplate`） |
| 使用常用引擎渲染模板 | `easypdf-freemarker` / `-velocity` / `-thymeleaf` / `-beetl` / `-rythm` / `-jetbrick` / `-httl` / `-webit` / `-jsp` |
| 将 XHTML 转换为 WordprocessingML 包 | `easypdf-xhtml` |
| 基于 iText 的 PDF 渲染上下文 | `easypdf-core`（`ItextContext`、`BaseFontFactory`、缓存管理器） |
| 统一管理依赖版本 | `easypdf-bom` |

**项目状态：** 稳定。

<a id="2-features--status"></a>
## 2. 功能与状态

| 能力 | 状态 | 说明 |
|:---|:---|:---|
| `WordprocessingMLTemplate` 抽象 | 可用 | `process(String template, Map<String,Object> variables)` -> `WordprocessingMLPackage` |
| `WordprocessingMLDocxTemplate` | 可用 | 源码/占位符/输出配置，如 `process(File sourceDocx, String template, Map, File outputDocx)`、`placeholderStart` / `placeholderEnd` |
| Freemarker / Velocity / Thymeleaf / Beetl / Rythm / Jetbrick / HTTL / Webit / JSP 引擎 | 可用 | 每引擎一个模块（`WordprocessingML{引擎}Template`） |
| XHTML 导入 | 可用 | `WordprocessingMLHtmlTemplate` + `XHTMLImporterUtils`（`easypdf-xhtml`） |
| iText 支持 | 可用 | `ItextContext`（单例）、`BaseFontFactory`、`D07_ParseHtmlAsian`、模板/缓存管理器 |
| WML 工具 | 可用 | 元素/段落/边框工具、字体映射（`ChineseFont`、`FontMapperHolder`） |
| 输出管线 | 可用 | `WordprocessingMLPackageRender` / `-Writer` / `-Extractor` |
| 构建事件 / 错误处理 | 可用 | `bus.event`（构建开始/结束）与 `bus.error.Slf4jLogger` |
| CI 流水线 | 未配置 | 仓库中无 CI 工作流文件 |

<a id="3-requirements--compatibility"></a>
## 3. 环境要求与兼容性

| 依赖项 | 版本（1.0.x 线） |
|:---|:---|
| JDK | 8 |
| Maven | 3.0+ |
| docx4j | 8.3.15（`docx4j-core` + JAXB 变体） |
| docx4j-ImportXHTML | 8.3.15 |
| docx4j-xhtmlrenderer | 3.0.0 |
| iText | 7.1.10 |

### 版本线矩阵

| 分支 | JDK | 版本号模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

### docx4j 版本矩阵（扩展模块与 core 解耦）

| 线 | JDK | `docx4j`（core/JAXB） | `docx4j-export-fo` | `docx4j-ImportXHTML` | `xhtmlrenderer` |
|:---|:---|:---|:---|:---|:---|
| 1.0.x | 8 | 8.3.15 | 8.3.15 | 8.3.15 | 3.0.0 |
| 2.0.x | 17 | 11.5.2 | 11.5.2 | 11.4.8 | 3.0.0 |
| 3.0.x | 21 | 11.5.2 | 11.5.2 | 11.4.8 | 3.0.0 |

三条线 Java 源码/注释/文档保持一致，仅 JDK 与配套 Maven 依赖版本不同。主代码不直接依赖 `javax/jakarta.xml.bind` 类型，保证三线源码一致。

<a id="4-architecture--modules"></a>
## 4. 架构与模块

```text
  模板来源                         easypdf 模块                      输出
  --------                        ------------                     ------
  .docx 模板   ->  easypdf-core  （WordprocessingMLTemplate）
  .ftl / .vm / .tpl ->  easypdf-{freemarker,velocity,beetl,thymeleaf,
                          rythm,jetbrick,httl,webit,jsp}
  .html / .xhtml   ->  easypdf-xhtml（WordprocessingMLHtmlTemplate +
                          XHTMLImporterUtils）
                                 |
                                 v
                      WordprocessingMLPackage（docx4j）
                                 |
              +------------------+------------------+
              v                                     v
       渲染 / 写出 / 抽取                     iText 上下文
       （easypdf-core io.*）                  （ItextContext、字体、
              |                                缓存管理器）
              v
         输出 .docx  / 面向 PDF 的渲染支持
```

| 模块 | 职责 |
|:---|:---|
| `easypdf-core` | 模板抽象、docx4j/WML 工具、iText 上下文与缓存工具、渲染/写出/抽取管线 |
| `easypdf-xhtml` | HTML/XHTML -> `WordprocessingMLPackage` |
| `easypdf-freemarker` / `easypdf-velocity` / `easypdf-thymeleaf` / `easypdf-beetl` / `easypdf-rythm` / `easypdf-jetbrick` / `easypdf-httl` / `easypdf-webit` / `easypdf-jsp` | 每模板引擎一个适配模块 |
| `easypdf-bom` | 依赖管理 BOM |

<a id="5-installation"></a>
## 5. 安装

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>easypdf-core</artifactId>
    <version>2.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

按需引入引擎模块，例如：

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>easypdf-freemarker</artifactId>
    <version>2.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:easypdf-core:2.0.x.x.20260630-SNAPSHOT'
implementation 'io.github.easy4j:easypdf-freemarker:2.0.x.x.20260630-SNAPSHOT'
```

**可用性：** 构件发布至阿里云私有 Maven 仓库，并通过 GitHub Releases 分发；尚未发布到 Maven Central。

<a id="6-quick-start"></a>
## 6. 快速开始

```java
import io.github.easy4j.pdf.WordprocessingMLDocxTemplate;
import io.github.easy4j.pdf.WordprocessingMLTemplate;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.util.HashMap;
import java.util.Map;

WordprocessingMLTemplate template = new WordprocessingMLDocxTemplate();

Map<String, Object> variables = new HashMap<>();
variables.put("title", "Invoice");
variables.put("amount", "128.00");

WordprocessingMLPackage doc = template.process("invoice.tpl", variables);
doc.save(new java.io.File("invoice.docx"));
```

预期结果：根据模板与变量生成 `invoice.docx`。

<a id="7-configuration"></a>
## 7. 配置

核心库为模板驱动，无需配置文件。`WordprocessingMLDocxTemplate` 提供 Bean 风格设置：

| 设置项 | 说明 |
|:---|:---|
| `sourceDocx` | 源 `.docx` 模板文件（可选） |
| `outputDocx` | 输出 `.docx` 文件（可选） |
| `placeholderStart` / `placeholderEnd` | 变量替换的占位符定界符 |

引擎适配模块额外支持以编程方式传入引擎专属设置（如 FreeMarker `Configuration` 通过 `setEngine(...)`）。

<a id="8-core-usage--api"></a>
## 8. 核心用法 / API

### 8.1 模板抽象

```java
public abstract class WordprocessingMLTemplate {
    public abstract WordprocessingMLPackage process(String template, Map<String, Object> variables) throws Exception;
}
```

### 8.2 Freemarker 模板

```java
WordprocessingMLFreemarkerTemplate tpl = new WordprocessingMLFreemarkerTemplate();
WordprocessingMLPackage doc = tpl.process("report.ftl", variables);
```

### 8.3 XHTML 转 Word

```java
WordprocessingMLHtmlTemplate html = new WordprocessingMLHtmlTemplate();
WordprocessingMLPackage doc = html.process(new File("page.html"));
```

### 8.4 核心包结构

| 包 | 内容 |
|:---|:---|
| `io.github.easy4j.pdf` | `WordprocessingMLTemplate`、`WordprocessingMLDocxTemplate`、`Docx4jConstants` |
| `io.github.easy4j.pdf.io` | `WordprocessingMLPackageRender` / `-Writer` / `-Extractor` / `WordprocessingMLTemplateWriter` |
| `io.github.easy4j.pdf.core` | iText 上下文（`ItextContext`、`ItextContextInitListener`）、`BaseFontFactory`、`D07_ParseHtmlAsian` |
| `io.github.easy4j.pdf.core.cache` | `PDFTemplateCacheManager`、`XMLEclmentCacheManager` |
| `io.github.easy4j.pdf.core.filter` | 文档缓存过滤器（`DocumentCacheFilter`、`CacheResponseWrapper` 等） |
| `io.github.easy4j.pdf.utils` | docx4j / WML / zip / 字体 / 段落 / 边框工具 |
| `io.github.easy4j.pdf.wml` | WML 元素渲染与 `WMLType` |
| `io.github.easy4j.pdf.fonts` | `ChineseFont`、`FontMapperHolder` |

<a id="9-testing--build"></a>
## 9. 测试与构建

```bash
./mvnw clean verify        # 构建全部模块、运行测试、生成覆盖率报告
./mvnw clean install       # 安装全部模块到本地仓库
```

- 引擎模块带有 `WordprocessingML{引擎}Template_Test` / `WordprocessingMLTemplate_Test` 测试类（freemarker、rythm、thymeleaf、jetbrick、webit 等）。
- 覆盖率由 JaCoCo Maven 插件度量（目标：90% 行覆盖率，`haltOnFailure=false`）。
- `release` profile 组装 GPG 签名 + 源码 + Javadoc + 部署（`./mvnw -Prelease clean deploy`）。

<a id="10-versioning--branches"></a>
## 10. 版本线与分支

仓库维护三条并行版本线，Java 源码/注释/文档保持一致，仅 JDK 与配套 Maven 依赖版本不同：

| 分支 | JDK | 版本号模式 |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

维护策略：在 JDK 8 作为基线的同时，1.0.x 版本线接收缺陷修复；新功能开发主要面向 2.0.x / 3.0.x 版本线（docx4j 版本见上文矩阵）。

<a id="11-contributing--license"></a>
## 11. 参与贡献与许可协议

欢迎参与贡献——请通过 Issue 反馈问题，或向对应版本线分支提交 Pull Request（JDK 17 相关改动提交到 `feature/2.0.x`）。

本项目基于 [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可发布。详见仓库根目录的 `LICENSE` 文件。

参考：
- https://www.docx4java.org/
- https://itextpdf.com/
