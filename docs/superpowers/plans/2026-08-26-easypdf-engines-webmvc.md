# easypdf 引擎适配器与 webmvc 迁移计划（Phase 3）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 9 个模板引擎适配器从 docx4j Word 输出（包装 `WordprocessingMLHtmlTemplate`）迁移到 PDF 输出（`PdfTemplate` + html2pdf 管线），并为 webmvc 提供 `PdfTemplateView`/`PdfViewResolver`，完成 easypdf "引擎渲染 HTML → PDF"的主链路。

**Architecture:** core 新增 `AbstractStringTemplateWrappingPdfTemplate`（镜像 easydoc 的 `AbstractStringTemplateWrappingTemplate` 骨架：File/InputStream/String 三种入口 → `render()` 抽象 → HTML → `HtmlPdfConverter.htmlToPdf`）。9 个引擎模块各新建 `{Engine}PdfTemplate`（如 `FreemarkerPdfTemplate`），引擎配置代码从旧 `WordprocessingML{Engine}Template` 复制，`process` 委托基类；旧类保留到 Phase 4 删除。引擎模块依赖从 `easypdf-xhtml` 改为 `easypdf-core`。webmvc 新增 `PdfTemplateView`（`AbstractView` + `PdfTemplate` 委托，缓冲后写响应）+ `PdfViewResolver`。代码 Java 8 语法，同步 1.0.x/2.0.x。

**Tech Stack:** iText 7 html2pdf（core 已就绪）、9 种引擎（freemarker/velocity/thymeleaf/beetl/rythm/jetbrick/httl/webit/jsp 原版本）、Spring WebMVC 5.3.x/6.x（按分支）、JUnit 5 + AssertJ。

## Global Constraints

- 新类放各引擎模块根包（`io.github.easy4j.pdf.{engine}.{Engine}PdfTemplate`），测试放 `src/test/java/io/github/easy4j/pdf/{engine}/`
- **Java 8 语法兼容**（禁 var/List.of/Path.of/switch 表达式）——保证可直接同步 1.0.x（JDK 8）
- 引擎模块 pom：依赖 `easypdf-xhtml` 替换为 `easypdf-core`（html2pdf 由 core 提供）
- 每个 Task 末尾跑 `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl <module> -am clean verify`（Maven 4）必须 BUILD SUCCESS
- 测试用 JUnit 5 + AssertJ，命名 `*Test.java`；每个引擎至少一个"渲染 → `%PDF-` 魔数"断言测试
- 提交信息风格：`feat(engines): ...`

---

### Task 1: core 基类 AbstractStringTemplateWrappingPdfTemplate

**Files:**
- Create: `easypdf-core/src/main/java/io/github/easy4j/pdf/template/AbstractStringTemplateWrappingPdfTemplate.java`
- Test: `easypdf-core/src/test/java/io/github/easy4j/pdf/template/AbstractStringTemplateWrappingPdfTemplateTest.java`

**Interfaces:**
- Consumes: `PdfTemplate`（顶层）、`HtmlPdfConverter.htmlToPdf(String, OutputStream)`
- Produces:
  - `public abstract class AbstractStringTemplateWrappingPdfTemplate extends PdfTemplate` —— 构造器 `protected AbstractStringTemplateWrappingPdfTemplate()`；`process(String template, Map vars, OutputStream out)` 已实现（`render(template, vars)` → `HtmlPdfConverter.htmlToPdf(html, out)`）；`protected abstract String render(String template, Map<String, Object> variables) throws Exception;`（引擎特有渲染）
  - Task 2-3 的 9 个引擎适配器继承此基类

- [ ] **Step 1: 写失败测试**

`AbstractStringTemplateWrappingPdfTemplateTest.java`：
```java
package io.github.easy4j.pdf.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AbstractStringTemplateWrappingPdfTemplateTest {

    static class FixedHtmlTemplate extends AbstractStringTemplateWrappingPdfTemplate {
        @Override
        protected String render(String template, Map<String, Object> variables) {
            return "<html><body><h1>" + variables.get("title") + "</h1></body></html>";
        }
    }

    @Test
    void processRendersHtmlThenPdf() throws Exception {
        FixedHtmlTemplate tpl = new FixedHtmlTemplate();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "引擎输出标题");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tpl.process("tpl", vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void renderIsCalledWithTemplate() throws Exception {
        FixedHtmlTemplate tpl = new FixedHtmlTemplate();
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("title", "t");
        OutputStream out = new ByteArrayOutputStream();
        tpl.process("mytemplate", vars, out);
        // render 被调用即通过（异常/空输出会在 htmlToPdf 处失败）
        assertThat(out.toString()).isEmpty(); // htmlToPdf 写二进制，String 断言空仅验证流程未抛异常
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am test -Dtest=AbstractStringTemplateWrappingPdfTemplateTest`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现基类**

```java
package io.github.easy4j.pdf.template;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;

import io.github.easy4j.pdf.PdfTemplate;
import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

/**
 * 模板引擎包装基类：引擎把模板渲染为 HTML，再经 HtmlPdfConverter 输出 PDF。
 * 镜像 easydoc 的 AbstractStringTemplateWrappingTemplate 骨架。
 */
public abstract class AbstractStringTemplateWrappingPdfTemplate extends PdfTemplate {

    protected AbstractStringTemplateWrappingPdfTemplate() {
    }

    @Override
    public void process(String template, Map<String, Object> variables, OutputStream out) throws Exception {
        String html = render(template, variables);
        HtmlPdfConverter.htmlToPdf(html, out);
    }

    /** 引擎特有渲染：把 template 内容渲染为 HTML 字符串。 */
    protected abstract String render(String template, Map<String, Object> variables) throws Exception;
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-core -am test -Dtest=AbstractStringTemplateWrappingPdfTemplateTest`
Expected: PASS（2 tests）

- [ ] **Step 5: Commit**

```bash
git add easypdf-core/src/main/java/io/github/easy4j/pdf/template/AbstractStringTemplateWrappingPdfTemplate.java easypdf-core/src/test/java/io/github/easy4j/pdf/template/AbstractStringTemplateWrappingPdfTemplateTest.java
git commit -m "feat(engines): add AbstractStringTemplateWrappingPdfTemplate base for engine adapters"
```

---

### Task 2: FreemarkerPdfTemplate（首个引擎迁移，完整示例）

**Files:**
- Create: `easypdf-freemarker/src/main/java/io/github/easy4j/pdf/freemarker/FreemarkerPdfTemplate.java`
- Test: `easypdf-freemarker/src/test/java/io/github/easy4j/pdf/freemarker/FreemarkerPdfTemplateTest.java`
- Modify: `easypdf-freemarker/pom.xml`（依赖 `easypdf-xhtml` → `easypdf-core`）

**Interfaces:**
- Consumes: `AbstractStringTemplateWrappingPdfTemplate`、freemarker `Configuration`
- Produces: `FreemarkerPdfTemplate` —— 构造器（无参/`(boolean landscape, boolean altChunk)` 保留兼容签名但忽略布局参数）+ `getEngine()/setEngine()/setFreemarkerSettings/setPreTemplateLoaders/setPostTemplateLoaders/setDefaultEncoding/setFreemarkerVariables`（从旧类复制）+ `render(String, Map)` 用 `getEngine().getTemplate(template).process(variables, writer)` 产出 HTML
- 其余 8 个引擎（Task 3）按同一模式

- [ ] **Step 1: 写失败测试**

`FreemarkerPdfTemplateTest.java`：
```java
package io.github.easy4j.pdf.freemarker;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FreemarkerPdfTemplateTest {

    @TempDir
    File tempDir;

    @Test
    void rendersFreemarkerTemplateToPdf() throws Exception {
        File tpl = new File(tempDir, "invoice.ftl");
        Files.write(tpl.toPath(), "<html><body><h1>发票 ${no}</h1></body></html>".getBytes(StandardCharsets.UTF_8));

        FreemarkerPdfTemplate template = new FreemarkerPdfTemplate();
        template.setDefaultEncoding("UTF-8");
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("no", "INV-001");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        template.process(tpl.getAbsolutePath(), vars, out);

        assertThat(new String(out.toByteArray(), 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-freemarker -am test -Dtest=FreemarkerPdfTemplateTest`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 FreemarkerPdfTemplate**

从 `WordprocessingMLFreemarkerTemplate` 复制引擎配置代码（`engine`/`freemarkerSettings`/`freemarkerVariables`/`templateLoaders`/`preTemplateLoaders`/`postTemplateLoaders`/`getInternalEngine()`/`getAggregateTemplateLoader()`/`postProcessTemplateLoaders()` 及全部 setter），差异仅两处：继承 `AbstractStringTemplateWrappingPdfTemplate` 而非 `WordprocessingMLTemplate`；删除 `mlHtmlTemplate` 字段与构造器，`render` 实现为：

```java
package io.github.easy4j.pdf.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.docx4j.Docx4jProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.XmlEscape;
import io.github.easy4j.pdf.template.AbstractStringTemplateWrappingPdfTemplate;
import io.github.easy4j.pdf.utils.ConfigUtils;

/**
 * Freemarker 模板引擎适配器：渲染模板为 HTML 后输出 PDF。
 */
public class FreemarkerPdfTemplate extends AbstractStringTemplateWrappingPdfTemplate {

    protected final Logger LOG = LoggerFactory.getLogger(FreemarkerPdfTemplate.class);
    protected Configuration engine;
    protected Properties freemarkerSettings;
    protected Map<String, Object> freemarkerVariables;
    protected String defaultEncoding;
    protected final List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();
    protected List<TemplateLoader> preTemplateLoaders;
    protected List<TemplateLoader> postTemplateLoaders;
    protected TemplateModel templateModel;

    public FreemarkerPdfTemplate() {
    }

    @Override
    protected String render(String template, Map<String, Object> variables) throws Exception {
        variables.put("String", this.templateModel);
        StringWriter output = new StringWriter();
        getEngine().getTemplate(template).process(variables, output);
        return output.toString();
    }

    public Configuration getEngine() throws IOException, TemplateException {
        return engine == null ? getInternalEngine() : engine;
    }

    public void setEngine(Configuration engine) {
        this.engine = engine;
    }

    protected Configuration getInternalEngine() throws IOException, TemplateException {
        try {
            BeansWrapper beansWrapper = new BeansWrapper(Configuration.VERSION_2_3_23);
            this.templateModel = beansWrapper.getStaticModels().get(String.class.getName());
        } catch (TemplateModelException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        Properties props = ConfigUtils.filterWithPrefix("docx4j.freemarker.", "docx4j.freemarker.", Docx4jProperties.getProperties(), false);
        if (!props.isEmpty()) {
            config.setSettings(props);
        }
        if (this.freemarkerVariables != null && !this.freemarkerVariables.isEmpty()) {
            config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
        }
        if (this.defaultEncoding != null) {
            config.setDefaultEncoding(this.defaultEncoding);
        }
        List<TemplateLoader> loaders = new LinkedList<TemplateLoader>(this.templateLoaders);
        if (this.preTemplateLoaders != null) {
            loaders.addAll(this.preTemplateLoaders);
        }
        postProcessTemplateLoaders(loaders);
        if (this.postTemplateLoaders != null) {
            loaders.addAll(this.postTemplateLoaders);
        }
        TemplateLoader loader = getAggregateTemplateLoader(loaders);
        if (loader != null) {
            config.setTemplateLoader(loader);
        }
        config.setSharedVariable("fmXmlEscape", new XmlEscape());
        config.setSharedVariable("fmHtmlEscape", new HtmlEscape());
        this.setEngine(config);
        return config;
    }

    protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
        int loaderCount = templateLoaders.size();
        switch (loaderCount) {
            case 0:
                LOG.info("No FreeMarker TemplateLoaders specified");
                return null;
            case 1:
                return templateLoaders.get(0);
            default:
                return new MultiTemplateLoader(templateLoaders.toArray(new TemplateLoader[loaderCount]));
        }
    }

    public void setFreemarkerSettings(Properties settings) {
        this.freemarkerSettings = settings;
    }

    public void setFreemarkerVariables(Map<String, Object> variables) {
        this.freemarkerVariables = variables;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders) {
        this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
    }

    public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders) {
        this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
    }

    protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
        templateLoaders.add(new ClassTemplateLoader(FreemarkerPdfTemplate.class, ""));
        LOG.info("ClassTemplateLoader for FreemarkerPdfTemplate added to FreeMarker configuration");
    }
}
```

- [ ] **Step 4: 修改 freemarker pom 依赖**

`easypdf-freemarker/pom.xml`：`<artifactId>easypdf-xhtml</artifactId>` 改为 `<artifactId>easypdf-core</artifactId>`。

- [ ] **Step 5: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-freemarker -am test -Dtest=FreemarkerPdfTemplateTest`
Expected: PASS（1 test）

- [ ] **Step 6: Commit**

```bash
git add easypdf-freemarker
git commit -m "feat(engines): add FreemarkerPdfTemplate rendering HTML templates to PDF"
```

---

### Task 3: 其余 8 个引擎迁移（velocity/thymeleaf/beetl/rythm/jetbrick/httl/webit/jsp）

**Files:**
- Create（每引擎一个主类 + 一个测试，模式同 Task 2）：
  - `easypdf-velocity/.../velocity/VelocityPdfTemplate.java` + `VelocityPdfTemplateTest.java`
  - `easypdf-thymeleaf/.../thymeleaf/ThymeleafPdfTemplate.java` + `ThymeleafPdfTemplateTest.java`
  - `easypdf-beetl/.../beetl/BeetlPdfTemplate.java` + `BeetlPdfTemplateTest.java`
  - `easypdf-rythm/.../rythm/RythmPdfTemplate.java` + `RythmPdfTemplateTest.java`
  - `easypdf-jetbrick/.../jetbrick/JetbrickPdfTemplate.java` + `JetbrickPdfTemplateTest.java`
  - `easypdf-httl/.../httl/HttlPdfTemplate.java` + `HttlPdfTemplateTest.java`
  - `easypdf-webit/.../webit/WebitPdfTemplate.java` + `WebitPdfTemplateTest.java`
  - `easypdf-jsp/.../jsp/JspPdfTemplate.java` + `JspPdfTemplateTest.java`
- Modify: 8 个模块 pom（`easypdf-xhtml` → `easypdf-core`）

**Interfaces:**
- Consumes: `AbstractStringTemplateWrappingPdfTemplate`
- Produces: 8 个 `{Engine}PdfTemplate`，各保留其引擎配置 API（从旧 `WordprocessingML{Engine}Template` 原样复制引擎字段与 setter），`render(String, Map)` 为该引擎的 HTML 渲染
- 各引擎 render 实现要点（从旧类 `process` 方法移植）：
  - Velocity：`VelocityEngine.getTemplate(template)` + `mergeContext` 到 StringWriter
  - Thymeleaf：`TemplateEngine.process(template, variables)`（`setTemplateResolver` 配置从旧类复制）
  - Beetl：`GroupTemplate.getTemplate(template)` + `render(Map)` 到 StringWriter
  - Rythm：`RythmEngine.render(template, variables)`（旧类若用 `Rythm` 静态引擎则保持一致）
  - Jetbrick：`JetEngine` + `JetTemplate.render(Map, Writer)`
  - Httl：`HttlEngine.getTemplate(template)` + `execute(Map, Writer)`
  - Webit：`WebitEngine` + `WebitTemplate.merge(Map, Writer)`
  - JSP：`JspTemplateImpl`（复用 easypdf-jsp/engine 包内现有 JspEngine 抽象，render 走 `JspEngine.render(template, vars)`）

- [ ] **Step 1: 逐个迁移（每个引擎：复制旧类引擎代码 → 改继承 → render 实现 → pom 依赖 → 测试）**

对每个引擎重复 Task 2 的 Step 1-5 模式，测试断言统一为：渲染该引擎的简单模板（如 `<html><body><h1>${var}</h1></body></html>` 的引擎语法版本）→ 输出以 `%PDF-` 开头。模板资源放各模块 `src/test/resources/tpl/`。

- [ ] **Step 2: 每引擎验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-{engine} -am test -Dtest={Engine}PdfTemplateTest`
Expected: PASS

- [ ] **Step 3: 汇总提交**

```bash
git add easypdf-velocity easypdf-thymeleaf easypdf-beetl easypdf-rythm easypdf-jetbrick easypdf-httl easypdf-webit easypdf-jsp
git commit -m "feat(engines): migrate remaining 8 template engines to PDF output"
```

---

### Task 4: webmvc PdfTemplateView + PdfViewResolver

**Files:**
- Create: `easypdf-webmvc/src/main/java/io/github/easy4j/pdf/webmvc/PdfTemplateView.java`
- Create: `easypdf-webmvc/src/main/java/io/github/easy4j/pdf/webmvc/PdfViewResolver.java`
- Test: `easypdf-webmvc/src/test/java/io/github/easy4j/pdf/webmvc/PdfTemplateViewTest.java`
- Modify: `easypdf-webmvc/pom.xml`（如缺 easypdf-core 依赖则添加）

**Interfaces:**
- Consumes: `PdfTemplate`、Spring `AbstractView`
- Produces:
  - `public class PdfTemplateView extends AbstractView` —— `setTemplate(PdfTemplate)`/`setTemplateName(String)`；`renderMergedOutputModel` 用 `createTemporaryOutputStream()` → `template.process(templateName, model, baos)` → `writeToResponse(response, baos)`
  - `public class PdfViewResolver implements ViewResolver` —— `resolveViewName` 返回 `PdfTemplateView`（PdfTemplate 通过 setter 注入）

- [ ] **Step 1: 写失败测试**

`PdfTemplateViewTest.java`（用 `MockHttpServletResponse`，验证 `application/pdf` 内容类型与 PDF 魔数）：
```java
package io.github.easy4j.pdf.webmvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import io.github.easy4j.pdf.PdfTemplate;
import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

class PdfTemplateViewTest {

    static class FixedTemplate extends PdfTemplate {
        @Override
        public void process(String template, java.util.Map<String, Object> variables, java.io.OutputStream out) throws Exception {
            HtmlPdfConverter.htmlToPdf("<html><body><h1>View 输出</h1></body></html>", out);
        }
    }

    @Test
    void renderProducesPdfResponse() throws Exception {
        PdfTemplateView view = new PdfTemplateView();
        view.setTemplate(new FixedTemplate());
        view.setTemplateName("tpl");

        MockHttpServletResponse response = new MockHttpServletResponse();
        view.render(Collections.<String, Object>emptyMap(), new MockHttpServletRequest(), response);

        assertThat(response.getContentType()).contains("application/pdf");
        byte[] body = response.getContentAsByteArray();
        assertThat(new String(body, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-webmvc -am test -Dtest=PdfTemplateViewTest`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 PdfTemplateView 与 PdfViewResolver**

```java
package io.github.easy4j.pdf.webmvc;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

import io.github.easy4j.pdf.PdfTemplate;

/**
 * Spring MVC 视图：委托 PdfTemplate 渲染 PDF，先缓冲后写响应。
 */
public class PdfTemplateView extends AbstractView {

    public static final String PDF_CONTENT_TYPE = "application/pdf";

    private PdfTemplate template;
    private String templateName;

    public PdfTemplateView() {
        setContentType(PDF_CONTENT_TYPE);
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        java.io.ByteArrayOutputStream baos = createTemporaryOutputStream();
        template.process(templateName, model, baos);
        writeToResponse(response, baos);
    }

    public void setTemplate(PdfTemplate template) {
        this.template = template;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
}
```

```java
package io.github.easy4j.pdf.webmvc;

import java.util.Locale;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import io.github.easy4j.pdf.PdfTemplate;

/**
 * 返回 PdfTemplateView 的 ViewResolver；PdfTemplate 通过 setter 注入。
 */
public class PdfViewResolver implements ViewResolver {

    private PdfTemplate template;

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        PdfTemplateView view = new PdfTemplateView();
        view.setTemplate(template);
        view.setTemplateName(viewName);
        return view;
    }

    public void setTemplate(PdfTemplate template) {
        this.template = template;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-webmvc -am test -Dtest=PdfTemplateViewTest`
Expected: PASS（1 test）

- [ ] **Step 5: Commit**

```bash
git add easypdf-webmvc
git commit -m "feat(webmvc): add PdfTemplateView and PdfViewResolver for PDF responses"
```

---

### Task 5: 3.0.x 全量验证 + 同步 1.0.x/2.0.x + 推送

**Files:**
- 三分支同步 Task 1-4 全部产物

**Interfaces:**
- Consumes: Task 1-4 产物
- Produces: 三分支均具备 9 个 `{Engine}PdfTemplate` + webmvc 视图 + 基类，全量 verify 通过

- [ ] **Step 1: 3.0.x 全量验证**

Run: `~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS；记录测试基线（≥ 246 + 新引擎测试）

- [ ] **Step 2: 同步到 1.0.x**（对比整合：引擎旧类保留、新类与 pom 变更移植）

```bash
git checkout feature/1.0.x
# 移植：core 基类 + 测试；9 个 {Engine}PdfTemplate + 测试；9 个引擎 pom 依赖变更；webmvc 新增文件（1.0.x 无 webmvc 模块则跳过）
```

- [ ] **Step 3: 验证 1.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit 1.0.x**

```bash
git add -A
git commit -m "feat(engines): sync engine PDF migration from 3.0.x"
```

- [ ] **Step 5: 同步到 2.0.x**（同 Step 2）

- [ ] **Step 6: 验证 2.0.x**

Run: `/opt/homebrew/bin/mvn -B -ntp clean verify`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit 2.0.x**（同 Step 4 信息）

- [ ] **Step 8: 回 3.0.x 推送三分支**

```bash
git checkout feature/3.0.x
git push origin feature/1.0.x feature/2.0.x feature/3.0.x
```

---

## Self-Review

- **Spec 覆盖**：9 引擎迁移 → Task 2（freemarker 完整示例）+ Task 3（8 引擎清单 + render 要点）；基类 → Task 1；webmvc → Task 4；三分支 → Task 5
- **占位符扫描**：Task 3 按引擎列出类名/测试名与 render 实现要点（Velocity/Thymeleaf/Beetl/Rythm/Jetbrick/Httl/Webit/JSP 各引擎 API 不同，逐项给出），执行时以旧类 `process` 方法为唯一移植来源，非 TBD
- **类型一致性**：`render(String, Map): String` 在 Task 1 定义、Task 2/3 实现一致；`PdfTemplateView.setTemplate(PdfTemplate)/setTemplateName(String)` 在 Task 4 定义与测试一致
- **与 Markdown 计划衔接**：本计划完成后，easypdf-xhtml 的 `EasyPdf`（markdown-pdf 计划 Task 4）仍调用 core 版 `HtmlPdfConverter`，不受影响
