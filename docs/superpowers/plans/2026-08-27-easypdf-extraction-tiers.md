# easypdf 分层提取引擎计划（Line 1：Tier1 格线表格+图 → Tier2 字号聚类/分栏 → Tier3 ML 扩展点）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有骨架级 `PdfStructureExtractor` 升级为**三层质量分级**的提取引擎：Tier1（格线表格 Lattice + 图片 base64，含表格内嵌图片）、Tier2（字号聚类标题/分栏阅读顺序/流式表格/列表/页眉页脚剔除）、Tier3（ML 布局模型扩展点 SPI + REST 适配器骨架，默认关闭）。对外 API（`PdfStructureExtractor.extract` / `EasyPdf.pdfToStructuredMarkdown`）不变，默认走规则引擎（Tier1+2 叠加）。

**质量目标（明确预期，不夸大）**：
- Tier1+2 默认引擎：常规办公 PDF（电子版、有格线或对齐良好的表格）≈ **80%** 结构还原
- Tier3 接入 docling/MinerU 类服务后：≈ 90–95%（由外部服务决定，本仓库只留接口）
- **100% 不承诺**（PDF 为绘图指令流，语义只能反推）；无损路径见 Line 2 计划（Tagged 往返）

**Architecture:** 一次解析多次分析：
1. **PageModel 收集层**：自定义 `IEventListener` 挂 `PdfCanvasProcessor`，单遍收集每页的文本块（坐标/字号/字体）、图片（字节+CTM 位置）、路径笔画（格线检测用）
2. **分析层 SPI**：`LayoutAnalyzer` 接口；默认实现 `RuleLayoutAnalyzer`（Tier1+2 流水线），可选 `RestLayoutAnalyzer`（Tier3，HTTP 调外部布局服务）
3. **选择层**：`PdfExtractionProperties.engine = AUTO | RULE | REST`，AUTO=REST 可用则 REST 否则 RULE
4. **序列化层**：沿用现有 `DocumentStructure`（POJO 不动），表格单元格内容为"文本 + `![img](data:mime;base64,…)`"混合 Markdown

**iText7 7.1.10 API 已核验**（javap 实测，写入代码前验证过）：
- `com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor(IEventListener)` + `processPageContent(PdfPage)`
- `parser.data.TextRenderInfo`：`getText()/getBaseline()/getAscentLine()/getDescentLine()/getFont()/getGraphicsState()/getMcid()`
- `parser.data.ImageRenderInfo`：`getImage()→PdfImageXObject(getImageBytes()/identifyImageFileExtension()/getWidth()/getHeight())`、`getImageCtm()`、`getMcid()`
- `parser.data.PathRenderInfo`：`getPath()→Path(getSubpaths())`、`getOperation()(STROKE/FILL)`、`getCtm()`、`getLineWidth()`
- 坐标：`LineSegment.getStartPoint()→Vector`，取值 `v.get(Vector.I1)`(x)/`v.get(Vector.I2)`(y)；路径点经 `new Vector(x,y,1).multiply(ctm)` 变换

**Tech Stack:** iText 7.1.10（零新依赖；REST 适配器用 JDK `HttpURLConnection`）、JUnit 5 + AssertJ、Java 8 语法。

## Global Constraints

- 新文件全部在 `easypdf-xhtml/src/main/java/io/github/easy4j/pdf/xhtml/convert/`（子包 `layout/` 放引擎类）
- **Java 8 语法**（禁 var/List.of/switch 表达式）——1.0.x 可同步
- **零新依赖**：不引 PDFBox/Tabula（格线算法在 iText7 Path 上自研，即 Tabula lattice 思想的 iText 实现）
- 公共 API 不破坏：`PdfStructureExtractor.extract(File)`、`EasyPdf.pdfToStructuredMarkdown(File/InputStream)`、`pdfToStructured(File)` 行为兼容（输出质量提升，签名不变）；既有 66 tests 必须全绿
- 测试夹具自产：用自家 `HtmlPdfConverter.htmlToPdf` 生成含格线表格+内嵌图片的 PDF（html2pdf 支持 data URI 图片），不提交二进制样本
- 提交风格：`feat(extract): ...`
- 验证命令（3.0.x）：`~/tools/apache-maven-4.0.0-rc-6/bin/mvn -B -ntp -pl easypdf-xhtml -am clean verify`

---

### Task 1: PageModel 收集层（chunks/images/strokes 单遍收集）

**Files:**
- Create: `.../convert/layout/PageChunk.java`（文本块：text/x/y/size/bold/page/mcid）
- Create: `.../convert/layout/RawImage.java`（bytes/ext/x/y/w/h/page/mcid）
- Create: `.../convert/layout/RawStroke.java`（x1/y1/x2/y2/width/page，仅水平/垂直笔画）
- Create: `.../convert/layout/PageModel.java`（`int pageNo; List<PageChunk> chunks; List<RawImage> images; List<RawStroke> strokes;`）
- Create: `.../convert/layout/PageModelListener.java`（implements IEventListener）
- Test: `.../convert/layout/PageModelListenerTest.java`

**Interfaces:**
- Produces:
  - `public final class PageModelListener implements com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener`，构造器 `PageModelListener(int pageNo)`；`List<PageModel> getModels()`（单页一模型）；静态便捷 `public static List<PageModel> collect(PdfDocument doc)`（逐页 new listener + `new PdfCanvasProcessor(listener).processPageContent(page)`）
  - Task 3/4 的所有分析器消费 `List<PageModel>`

- [ ] **Step 1: 写失败测试（夹具用自家 html2pdf 生成）**

```java
package io.github.easy4j.pdf.xhtml.convert.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;

class PageModelListenerTest {

    private static PdfDocument render(String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        return new PdfDocument(new PdfReader(new java.io.ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void collectsTextChunksWithSizeAndPosition() throws Exception {
        try (PdfDocument doc = render("<html><body><h1>标题</h1><p>正文段落</p></body></html>")) {
            List<PageModel> models = PageModelListener.collect(doc);
            assertThat(models).hasSize(1);
            assertThat(models.get(0).chunks.toString()).contains("标题").contains("正文段落");
            // 标题字号 > 正文字号
            PageChunk h = findChunk(models.get(0), "标题");
            PageChunk p = findChunk(models.get(0), "正文");
            assertThat(h.size).isGreaterThan(p.size);
        }
    }

    @Test
    void collectsImagesWithBytesAndPosition() throws Exception {
        // 1x1 红色 PNG data URI
        String png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
        try (PdfDocument doc = render("<html><body><p><img src=\"" + png + "\"/></p></body></html>")) {
            List<PageModel> models = PageModelListener.collect(doc);
            assertThat(models.get(0).images).isNotEmpty();
            assertThat(models.get(0).images.get(0).bytes.length).isGreaterThan(0);
        }
    }

    @Test
    void collectsRulingStrokesFromBorderedTable() throws Exception {
        try (PdfDocument doc = render(
                "<html><body><table border='1'><tr><td>A</td><td>B</td></tr></table></body></html>")) {
            List<PageModel> models = PageModelListener.collect(doc);
            assertThat(models.get(0).strokes.size()).isGreaterThanOrEqualTo(3); // 至少若干横/竖线
        }
    }

    private static PageChunk findChunk(PageModel m, String contains) {
        for (PageChunk c : m.chunks) {
            if (c.text.contains(contains)) return c;
        }
        throw new AssertionError("chunk not found: " + contains);
    }
}
```

- [ ] **Step 2: 运行确认失败**（类不存在）

- [ ] **Step 3: 实现 4 个模型类 + Listener**

```java
// PageChunk.java
package io.github.easy4j.pdf.xhtml.convert.layout;
public final class PageChunk {
    public final String text; public final float x, y, size; public final boolean bold;
    public final int page, mcid;
    public PageChunk(String text, float x, float y, float size, boolean bold, int page, int mcid) {
        this.text = text; this.x = x; this.y = y; this.size = size; this.bold = bold; this.page = page; this.mcid = mcid;
    }
}

// RawImage.java
package io.github.easy4j.pdf.xhtml.convert.layout;
public final class RawImage {
    public final byte[] bytes; public final String ext; public final float x, y, w, h;
    public final int page, mcid;
    public RawImage(byte[] bytes, String ext, float x, float y, float w, float h, int page, int mcid) {
        this.bytes = bytes; this.ext = ext; this.x = x; this.y = y; this.w = w; this.h = h; this.page = page; this.mcid = mcid;
    }
}

// RawStroke.java（仅保留水平/垂直线段）
package io.github.easy4j.pdf.xhtml.convert.layout;
public final class RawStroke {
    public final float x1, y1, x2, y2, width; public final int page;
    public RawStroke(float x1, float y1, float x2, float y2, float width, int page) {
        this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2; this.width = width; this.page = page;
    }
    public boolean horizontal() { return Math.abs(y1 - y2) < 0.5f && Math.abs(x1 - x2) > 1f; }
    public boolean vertical() { return Math.abs(x1 - x2) < 0.5f && Math.abs(y1 - y2) > 1f; }
}

// PageModel.java
package io.github.easy4j.pdf.xhtml.convert.layout;
import java.util.ArrayList; import java.util.List;
public final class PageModel {
    public final int pageNo;
    public final List<PageChunk> chunks = new ArrayList<PageChunk>();
    public final List<RawImage> images = new ArrayList<RawImage>();
    public final List<RawStroke> strokes = new ArrayList<RawStroke>();
    public PageModel(int pageNo) { this.pageNo = pageNo; }
}

// PageModelListener.java
package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.itextpdf.kernel.geom.BezierCurve;
import com.itextpdf.kernel.geom.Line;
import com.itextpdf.kernel.geom.LineSegment;
import com.itextpdf.kernel.geom.Matrix;
import com.itextpdf.kernel.geom.Path;
import com.itextpdf.kernel.geom.Point;
import com.itextpdf.kernel.geom.Subpath;
import com.itextpdf.kernel.geom.Vector;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.data.AbstractRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.ImageRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.PathRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;

public final class PageModelListener implements IEventListener {

    private final PageModel model;

    public PageModelListener(int pageNo) { this.model = new PageModel(pageNo); }

    public static List<PageModel> collect(PdfDocument doc) {
        List<PageModel> all = new ArrayList<PageModel>();
        for (int i = 1; i <= doc.getNumberOfPages(); i++) {
            PdfPage page = doc.getPage(i);
            PageModelListener l = new PageModelListener(i);
            new PdfCanvasProcessor(l).processPageContent(page);
            all.add(l.model);
        }
        return all;
    }

    public PageModel getModel() { return model; }

    @Override
    public Set<EventType> getSupportedEvents() {
        Set<EventType> s = new HashSet<EventType>();
        s.add(EventType.RENDER_TEXT); s.add(EventType.RENDER_IMAGE); s.add(EventType.RENDER_PATH);
        return s;
    }

    @Override
    public void eventOccurred(IEventData data, EventType type) {
        if (data instanceof TextRenderInfo) onText((TextRenderInfo) data);
        else if (data instanceof ImageRenderInfo) onImage((ImageRenderInfo) data);
        else if (data instanceof PathRenderInfo) onPath((PathRenderInfo) data);
    }

    private void onText(TextRenderInfo ti) {
        String text = ti.getText();
        if (text == null || text.trim().isEmpty()) return;
        LineSegment base = ti.getBaseline();
        float x = base.getStartPoint().get(Vector.I1);
        float y = base.getStartPoint().get(Vector.I2);
        float size = ti.getAscentLine().getStartPoint().get(Vector.I2) - ti.getDescentLine().getStartPoint().get(Vector.I2);
        if (size <= 0f) size = ti.getFontSize();
        boolean bold = fontName(ti).toLowerCase().contains("bold");
        model.chunks.add(new PageChunk(text, x, y, Math.abs(size), bold, model.pageNo, mcid(ti)));
    }

    private void onImage(ImageRenderInfo ii) {
        try {
            PdfImageXObject xo = ii.getImage();
            if (xo == null) return;
            byte[] bytes = xo.getImageBytes();
            if (bytes == null || bytes.length == 0) return;
            String ext = xo.identifyImageFileExtension();
            Matrix ctm = ii.getImageCtm();
            float x = ctm.get(Matrix.I11), y = ctm.get(Matrix.I12) != 0 ? ctm.get(Matrix.I12) : ctm.get(Matrix.I21);
            // 简化：用 CTM 平移分量做锚点（html2pdf 输出 CTM 为平移+缩放）
            float px = ctm.get(Matrix.I31), py = ctm.get(Matrix.I32);
            float w = xo.getWidth(), h = xo.getHeight();
            model.images.add(new RawImage(bytes, ext == null ? "png" : ext, px, py, w, h, model.pageNo, ii.getMcid()));
        } catch (Exception ignored) {
            // 部分内联/损坏图片跳过
        }
    }

    private void onPath(PathRenderInfo pri) {
        if ((pri.getOperation() & PathRenderInfo.STROKE) == 0) return; // 只要描边线条
        Path path = pri.getPath();
        if (path == null) return;
        Matrix ctm = pri.getCtm();
        for (Subpath sp : path.getSubpaths()) {
            for (Object seg : sp.getSegments()) {
                List<Point> pts = null;
                if (seg instanceof Line) pts = ((Line) seg).getBasePoints();
                else if (seg instanceof BezierCurve) pts = ((BezierCurve) seg).getBasePoints();
                if (pts == null || pts.size() < 2) continue;
                Point a = transform(ctm, pts.get(0));
                Point b = transform(ctm, pts.get(pts.size() - 1));
                RawStroke rs = new RawStroke((float) a.getX(), (float) a.getY(),
                        (float) b.getX(), (float) b.getY(), pri.getLineWidth(), model.pageNo);
                if (rs.horizontal() || rs.vertical()) model.strokes.add(rs);
            }
        }
    }

    private static Point transform(Matrix ctm, Point p) {
        if (ctm == null) return p;
        Vector v = new Vector((float) p.getX(), (float) p.getY(), 1f).multiply(ctm);
        return new Point(v.get(Vector.I1), v.get(Vector.I2));
    }

    private static String fontName(TextRenderInfo ti) {
        try {
            return ti.getFont().getFontProgram().getNames().getFontName();
        } catch (Exception e) {
            return "";
        }
    }

    private static int mcid(AbstractRenderInfo ri) {
        try {
            return ((TextRenderInfo) ri).getMcid();
        } catch (Exception e) {
            return -1;
        }
    }
}
```

> 实施注意：`TextRenderInfo.getMcid()` 若 7.1.10 签名不同（javap 已见 Image/Path 有 getMcid），编译期按实际调整；`ImageCtm` 锚点先取平移分量，测试通过即可（html2pdf 生成的 PDF 够用）。

- [ ] **Step 4: 运行测试确认通过（3 tests）**

- [ ] **Step 5: Commit** `feat(extract): add PageModel single-pass collector (chunks/images/strokes)`

---

### Task 2: LayoutAnalyzer SPI + 引擎切换（保持公共 API）

**Files:**
- Create: `.../convert/layout/LayoutAnalyzer.java`
- Create: `.../convert/layout/PdfExtractionProperties.java`
- Create: `.../convert/layout/RuleLayoutAnalyzer.java`（骨架：先只做"整页单节"等价旧行为，Task 3/4 逐步充实）
- Modify: `.../convert/PdfStructureExtractor.java`（内部改为：PageModel 收集 → 按 properties 选 analyzer → 产出 DocumentStructure；Tagged 角色作为 heading 提示传入）
- Test: `.../convert/layout/RuleLayoutAnalyzerTest.java`

**Interfaces:**
- Produces:
  - `public interface LayoutAnalyzer { String name(); DocumentStructure analyze(List<PageModel> pages, List<int[]> taggedHeadings, String title) throws java.io.IOException; }`（`taggedHeadings` 元素为 `{page, level}` 提示，RuleLayoutAnalyzer 用作字号聚类的先验；REST 实现可忽略）
  - `public final class PdfExtractionProperties { public enum Engine { AUTO, RULE, REST } public Engine engine = Engine.AUTO; public String restEndpoint; public int restTimeoutMillis = 10000; public static PdfExtractionProperties defaults(); }`
  - `PdfStructureExtractor.extract(File)` → `extract(File, PdfExtractionProperties)`（旧签名委托 defaults()）

- [ ] **Step 1: 写失败测试**：`RuleLayoutAnalyzer.analyze(单页模型, 空 hints, "t")` 返回 DocumentStructure，sections 非空、fullMarkdown 含 chunk 文本
- [ ] **Step 2: 确认失败** → **Step 3: 实现三类 + 改造 Extractor**（保持 null/missing-file 既有测试绿）
- [ ] **Step 4: 全部既有测试 + 新测试通过**（`mvn -pl easypdf-xhtml -am test` 全绿）
- [ ] **Step 5: Commit** `feat(extract): add LayoutAnalyzer SPI with engine selection, extractor rewired`

---

### Task 3: Tier1 —— 格线表格 Lattice（含表格内嵌图片）

**Files:**
- Modify: `.../convert/layout/RuleLayoutAnalyzer.java`（增加 lattice 阶段）
- Create: `.../convert/layout/LatticeTableFinder.java`
- Test: `.../convert/layout/LatticeTableFinderTest.java`

**Interfaces:**
- Produces:
  - `public final class LatticeTableFinder { public List<TableRegion> find(PageModel page); }`
  - `public final class TableRegion { float x1,y1,x2,y2; List<Float> colXs, rowYs; }`（网格线坐标，升序）
  - 算法：页面水平笔画按 y 聚类（差<2pt 合并）→ 候选行线；垂直笔画同理 → 列线；取"行≥2 且列≥2 且存在交叉"的网格为 TableRegion；越界剔除（页边距线、整页框）
  - 单元格归属：chunk 取 baseline 中点、image 取 CTM 锚点，落在 cell 矩形内即归入；cell Markdown = 文本 + 空格 + `![img](data:image/{ext};base64,{b64})`
  - 首行→headers，其余→rows（与现有 DocumentTable 兼容）

- [ ] **Step 1: 写失败测试（核心场景：表格内嵌图片）**

```java
@Test
void latticeTableWithEmbeddedImageProducesCellMarkdown() throws Exception {
    String png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==";
    String html = "<html><body><table border='1'>"
        + "<tr><td>名称</td><td>图示</td></tr>"
        + "<tr><td>部件A</td><td><img src='" + png + "'/></td></tr>"
        + "</table></body></html>";
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    HtmlPdfConverter.htmlToPdf(html, out);
    try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(out.toByteArray())))) {
        List<PageModel> pages = PageModelListener.collect(doc);
        DocumentStructure ds = new RuleLayoutAnalyzer().analyze(pages, java.util.Collections.<int[]>emptyList(), "t");
        String md = ds.toMarkdown();
        assertThat(md).contains("| 名称 | 图示 |");
        assertThat(md).contains("部件A");
        assertThat(md).contains("![img](data:image/png;base64,"); // 单元格内图片进 Markdown
    }
}
```

- [ ] **Step 2: 确认失败** → **Step 3: 实现 LatticeTableFinder + RuleLayoutAnalyzer 接入**（表格区域内的 chunks/images 从正文流剔除，避免重复输出）
- [ ] **Step 4: 测试通过 + 既有全绿** → **Step 5: Commit** `feat(extract): tier1 lattice table extraction with embedded cell images`

---

### Task 4: Tier2 —— 字号聚类标题 / 分栏 / 流式表格 / 列表 / 页眉页脚

**Files:**
- Modify: `.../convert/layout/RuleLayoutAnalyzer.java`（正文流水线）
- Test: `.../convert/layout/RuleLayoutAnalyzerTier2Test.java`

**规则（逐项可测）**：
1. **正文字号** = 全文档 chunk 按 `size` 加权（字符数）取众数；**标题判定**：`size ≥ body*1.25`（或 bold 且孤立行）；层级 = 候选字号降序映射 1..6；Tagged hints 直接锚定该行 level
2. **行合并**：同栏内相邻 chunk 的 y 差 < `max(2, size*0.4)` 且 x 连续 → 拼一行；行间空隙 > `size*1.8` → 段落分隔
3. **分栏**：chunk 起点 x 直方图找"宽空白带"（≥60pt 无起点）分列；阅读顺序 = 列内自上而下、列间左→右
4. **列表**：行首匹配 `^[•·◦‣-]\s+`（无序）或 `^(\d{1,2}|[a-z]|[ivxIVX]{1,4})[.)、]\s+`（有序）→ 输出 `- ` / `1. `
5. **页眉页脚**：同 y（±3pt）在 ≥60% 页面出现的近似行（前 12 字符相同）→ 剔除
6. **断词合并**：行尾 `[\u4e00-\u9fa5A-Za-z]-$` 与下一行行首拼接
7. **流式表格**：非 lattice 但连续 ≥3 行满足"≥2 列 x 对齐（±3pt）且行高一致"→ stream 表（首行为 header）

- [ ] **Step 1: 写失败测试**（4 个）：
  - `<h1>/<h2>/<p>` 渲染 → `# / ##` 且正文合并成段
  - 双栏 `<table><tr><td style="width:50%">` 或 float 两栏 → 阅读顺序不串列（断言左栏文本先于右栏）
  - `<ul><li>` → `- `；`<ol><li>` → `1. `
  - 页眉重复（`<div style="position:fixed;top:0">页眉X</div>` 多页）→ 输出不含"页眉"
- [ ] **Step 2: 确认失败** → **Step 3: 实现规则 1-7**（每规则独立私有方法，纯函数式便于测）
- [ ] **Step 4: 测试通过 + 全量回归** → **Step 5: Commit** `feat(extract): tier2 heuristics (font clustering, columns, lists, stream tables, header/footer strip)`

---

### Task 5: Tier3 —— ML 扩展点（REST 适配器骨架，默认关闭）

**Files:**
- Create: `.../convert/layout/RestLayoutAnalyzer.java`
- Test: `.../convert/layout/RestLayoutAnalyzerTest.java`

**Interfaces:**
- Produces:
  - `RestLayoutAnalyzer implements LayoutAnalyzer`：`name()="rest"`；`analyze()` 用 `HttpURLConnection` POST `{endpoint}`（body=PDF bytes，`Content-Type: application/pdf`），期望 JSON：`{"title":..,"sections":[{"title","level","content"}],"tables":[{"headers":[[..]],"rows":[[..]]}]}`（Jackson 已在依赖树？否——**用最小手写 JSON 解析或正则提取**，保持零新依赖；文档注明推荐端点返回该契约）
  - `PdfExtractionProperties.engine=REST` 且 endpoint 为空 → 构造时抛 `IllegalArgumentException`；AUTO+不可达（连接失败）→ 静默回退 RULE 并打 SLF4J warn
  - 超时 `restTimeoutMillis`；响应非 200 → 抛 IOException（AUTO 下回退）

- [ ] **Step 1: 写测试**：本地 `HttpServer`（`com.sun.net.httpserver`，JDK 自带）返回固定 JSON → 断言 sections 解析正确；endpoint 空抛 IAE；AUTO 不可达回退 RULE（结果含正文）
- [ ] **Step 2: 确认失败** → **Step 3: 实现** → **Step 4: 通过** → **Step 5: Commit** `feat(extract): tier3 REST layout analyzer extension point with AUTO fallback`

---

### Task 6: 三分支同步 + 推送 + 勾选

- [ ] **Step 1: 3.0.x 全量 verify**（总测试数 ≥ 66 + 新增 ≈ 12）
- [ ] **Step 2-7: 同步 1.0.x / 2.0.x（`git checkout feature/3.0.x -- easypdf-xhtml/src/.../convert/`）+ 各自全量 verify + commit**
- [ ] **Step 8: 推送三分支 + `sed 's/- \[ \]/- [x]/g'` 勾选本计划 + commit**

---

## Self-Review

- **覆盖**：Tier1（Task 3 含表格内嵌图）→ Tier2（Task 4 七条规则）→ Tier3（Task 5 SPI+REST+回退）→ 三分支（Task 6）
- **API 已核验**：全部 iText7 7.1.10 签名经 javap 实测（含 `processPageContent`、`parser.data.*`、`Vector.get(I1/I2)`）
- **兼容**：公共 API 不变；旧 3 个 extractor 测试必须继续绿（Task 2 Step 4 明确验证）
- **诚实边界**：80% 为规则引擎对常规办公 PDF 的目标；表格内图片以 data URI 进 cell；扫描件（无文本层）不在此线（属 OCR，由 ddd4j-ai-extension-ocr 协作）
