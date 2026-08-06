# easypdf

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://github.com/easy-4-java/easypdf) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

Fast Word / PDF document generation based on docx4j / iText and a variety of template engines. Generate WordprocessingML documents from templates (Freemarker, Velocity, Thymeleaf, Beetl, Rythm, Jetbrick, HTTL, Webit, JSP) or directly from XHTML, with iText-based PDF support in the core module.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage / API](#8-core-usage--api)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

`easypdf` (project description: *Building Word/PDF documents based on XHTML templates using Docx4j and iText*) is the Word/PDF sibling of `easydoc`. A core module (`easypdf-core`) defines the `WordprocessingMLTemplate` abstraction plus docx4j/WML utilities and iText-based context support; dedicated modules adapt each template engine and the XHTML import path.

| What it is | What it is not |
|:---|:---|
| Template-driven Word (.docx) generation via docx4j | A pure PDF-only library (PDF support is iText-based, in the core module) |
| Pluggable template engines + XHTML import | A document editor or viewer |
| iText context / font / cache helpers for PDF workflows | A cloud document service |

Typical use cases:

| Use case | Module |
|:---|:---|
| Fill a Word template with a variable map | `easypdf-core` (`WordprocessingMLDocxTemplate`) |
| Render templates with your favorite engine | `easypdf-freemarker` / `-velocity` / `-thymeleaf` / `-beetl` / `-rythm` / `-jetbrick` / `-httl` / `-webit` / `-jsp` |
| Convert XHTML to a WordprocessingML package | `easypdf-xhtml` |
| iText-based PDF rendering context | `easypdf-core` (`ItextContext`, `BaseFontFactory`, cache managers) |
| Manage shared dependency versions | `easypdf-bom` |

**Project status:** stable.

## 2. Features & Status

| Feature | Status | Notes |
|:---|:---|:---|
| `WordprocessingMLTemplate` abstraction | Available | `process(String template, Map<String,Object> variables)` -> `WordprocessingMLPackage` |
| `WordprocessingMLDocxTemplate` | Available | Source/placeholder/output configuration, e.g. `process(File sourceDocx, String template, Map, File outputDocx)`, `placeholderStart` / `placeholderEnd` |
| Freemarker / Velocity / Thymeleaf / Beetl / Rythm / Jetbrick / HTTL / Webit / JSP engines | Available | One module per engine (`WordprocessingML{Engine}Template`) |
| XHTML import | Available | `WordprocessingMLHtmlTemplate` + `XHTMLImporterUtils` (`easypdf-xhtml`) |
| iText support | Available | `ItextContext` (singleton), `BaseFontFactory`, `D07_ParseHtmlAsian`, template/cache managers |
| WML utilities | Available | Element/paragraph/border utilities, font mapping (`ChineseFont`, `FontMapperHolder`) |
| Output pipeline | Available | `WordprocessingMLPackageRender` / `-Writer` / `-Extractor` |
| Build events / error handling | Available | `bus.event` (build start/finish) and `bus.error.Slf4jLogger` |
| CI pipeline | Not configured | No CI workflow files in the repository |

## 3. Requirements & Compatibility

| Requirement | Version (1.0.x line) |
|:---|:---|
| JDK | 8 |
| Maven | 3.0+ |
| docx4j | 8.3.15 (`docx4j-core` + JAXB variants) |
| docx4j-ImportXHTML | 8.3.15 |
| docx4j-xhtmlrenderer | 3.0.0 |
| iText | 7.1.10 |

### Version lines

| Branch | JDK | Version pattern |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

### docx4j version matrix (extension modules decoupled from core)

| Line | JDK | `docx4j` (core/JAXB) | `docx4j-export-fo` | `docx4j-ImportXHTML` | `xhtmlrenderer` |
|:---|:---|:---|:---|:---|:---|
| 1.0.x | 8 | 8.3.15 | 8.3.15 | 8.3.15 | 3.0.0 |
| 2.0.x | 17 | 11.5.2 | 11.5.2 | 11.4.8 | 3.0.0 |
| 3.0.x | 21 | 11.5.2 | 11.5.2 | 11.4.8 | 3.0.0 |

The three lines keep the same Java sources/comments/docs; only the JDK baseline and the matching Maven dependency versions differ. The main code does not depend directly on `javax/jakarta.xml.bind` types, which keeps the sources identical across lines.

## 4. Architecture & Modules

```text
  Template sources                      easypdf modules                 output
  ----------------                      ---------------                 ------
  .docx template   ->  easypdf-core  (WordprocessingMLTemplate)
  .ftl / .vm / .tpl ->  easypdf-{freemarker,velocity,beetl,thymeleaf,
                          rythm,jetbrick,httl,webit,jsp}
  .html / .xhtml   ->  easypdf-xhtml (WordprocessingMLHtmlTemplate +
                          XHTMLImporterUtils)
                                 |
                                 v
                      WordprocessingMLPackage (docx4j)
                                 |
              +------------------+------------------+
              v                                     v
       render / write / extract               iText context
       (easypdf-core io.*)                    (ItextContext, fonts,
              |                                cache managers)
              v
           output .docx  /  PDF-oriented rendering support
```

| Module | Responsibility |
|:---|:---|
| `easypdf-core` | Template abstraction, docx4j/WML utilities, iText context and cache helpers, render/write/extract pipeline |
| `easypdf-xhtml` | HTML/XHTML -> `WordprocessingMLPackage` |
| `easypdf-freemarker` / `easypdf-velocity` / `easypdf-thymeleaf` / `easypdf-beetl` / `easypdf-rythm` / `easypdf-jetbrick` / `easypdf-httl` / `easypdf-webit` / `easypdf-jsp` | One adapter per template engine |
| `easypdf-bom` | Dependency management BOM |

## 5. Installation

### Maven

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>easypdf-core</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

Add the engine module(s) you need, e.g.:

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>easypdf-freemarker</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.easy4j:easypdf-core:3.0.x.x.20260630-SNAPSHOT'
implementation 'io.github.easy4j:easypdf-freemarker:3.0.x.x.20260630-SNAPSHOT'
```

**Availability:** the artifacts are published to the Aliyun private Maven repository and distributed through GitHub Releases; they have not yet been published to Maven Central.

## 6. Quick Start

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

Expected result: `invoice.docx` is generated from the template with the variables applied.

## 7. Configuration

The core library is template-driven and requires no configuration file. `WordprocessingMLDocxTemplate` exposes bean-style settings:

| Setting | Description |
|:---|:---|
| `sourceDocx` | Source `.docx` template file (optional) |
| `outputDocx` | Output `.docx` file (optional) |
| `placeholderStart` / `placeholderEnd` | Placeholder delimiters for variable replacement |

Engine adapters additionally accept engine-specific settings programmatically (e.g. FreeMarker `Configuration` via `setEngine(...)`).

## 8. Core Usage / API

### 8.1 Template abstraction

```java
public abstract class WordprocessingMLTemplate {
    public abstract WordprocessingMLPackage process(String template, Map<String, Object> variables) throws Exception;
}
```

### 8.2 Freemarker template

```java
WordprocessingMLFreemarkerTemplate tpl = new WordprocessingMLFreemarkerTemplate();
WordprocessingMLPackage doc = tpl.process("report.ftl", variables);
```

### 8.3 XHTML to Word

```java
WordprocessingMLHtmlTemplate html = new WordprocessingMLHtmlTemplate();
WordprocessingMLPackage doc = html.process(new File("page.html"));
```

### 8.4 Core packages

| Package | Contents |
|:---|:---|
| `io.github.easy4j.pdf` | `WordprocessingMLTemplate`, `WordprocessingMLDocxTemplate`, `Docx4jConstants` |
| `io.github.easy4j.pdf.io` | `WordprocessingMLPackageRender` / `-Writer` / `-Extractor` / `WordprocessingMLTemplateWriter` |
| `io.github.easy4j.pdf.core` | iText context (`ItextContext`, `ItextContextInitListener`), `BaseFontFactory`, `D07_ParseHtmlAsian` |
| `io.github.easy4j.pdf.core.cache` | `PDFTemplateCacheManager`, `XMLEclmentCacheManager` |
| `io.github.easy4j.pdf.core.filter` | Document caching filters (`DocumentCacheFilter`, `CacheResponseWrapper`, ...) |
| `io.github.easy4j.pdf.utils` | docx4j / WML / zip / font / paragraph / border utilities |
| `io.github.easy4j.pdf.wml` | WML element rendering and `WMLType` |
| `io.github.easy4j.pdf.fonts` | `ChineseFont`, `FontMapperHolder` |

## 9. Testing & Build

```bash
./mvnw clean verify        # build all modules, run tests, generate coverage report
./mvnw clean install       # install all modules into the local repository
```

- Engine modules carry `WordprocessingML{Engine}Template_Test` / `WordprocessingMLTemplate_Test` test classes (freemarker, rythm, thymeleaf, jetbrick, webit, ...).
- Coverage is measured with the JaCoCo Maven plugin (target: 90% line coverage, `haltOnFailure=false`).
- The `release` profile assembles GPG signing + sources + Javadoc + deployment (`./mvnw -Prelease clean deploy`).

## 10. Versioning & Branches

Three parallel version lines are maintained; the Java sources, comments and docs stay identical across lines — only the JDK baseline and the matching Maven dependency versions differ:

| Branch | JDK | Version pattern |
|:---|:---|:---|
| `feature/1.0.x` | JDK 8 | `1.0.x.*` |
| `feature/2.0.x` | JDK 17 | `2.0.x.*` |
| `feature/3.0.x` | JDK 21 | `3.0.x.*` |

Maintenance strategy: the 1.0.x line receives bug fixes while JDK 8 remains the baseline; feature development primarily targets the 2.0.x / 3.0.x lines (see the docx4j version matrix above).

## 11. Contributing & License

Contributions are welcome — open an issue or submit a pull request against the matching version-line branch (`feature/3.0.x` for JDK 21 changes).

This project is licensed under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0). See the `LICENSE` file in the repository root for details.

References:
- https://www.docx4java.org/
- https://itextpdf.com/
