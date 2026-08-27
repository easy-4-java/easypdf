package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.EncryptionConstants;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.layout.ExtractCache;
import io.github.easy4j.pdf.xhtml.convert.layout.PdfExtractionProperties;
import io.github.easy4j.pdf.xhtml.convert.layout.RestLayoutAnalyzer;

/**
 * Round 3 工程健壮性与 Tagged 适配回归：
 * W3-1 角色别名归一化、W3-2 相邻重复空段去重、W3-3 页级流式提取、
 * W3-4 提取缓存、W3-5 阈值配置化与 REST 重试。
 *
 * 注：Word 导出的 Tagged PDF 样本无法离线获得，角色适配以包级可见静态方法
 * {@link PdfStructureExtractor#canonicalRole(String)} 直接单测为主，
 * 集成路径由自制 Tagged PDF（EasyPdf.markdownToPdfTagged）回归覆盖。
 */
class RobustnessTest {

    // ---------------- W3-1: 角色别名归一化 ----------------

    @Test
    void wordHeadingAliasesNormalizeToStandardRoles() {
        assertThat(PdfStructureExtractor.canonicalRole("heading 1")).isEqualTo("H1");
        assertThat(PdfStructureExtractor.canonicalRole("heading 6")).isEqualTo("H6");
        assertThat(PdfStructureExtractor.canonicalRole("h2")).isEqualTo("H2");
        assertThat(PdfStructureExtractor.canonicalRole("/h3")).isEqualTo("H3"); // 带斜杠的 PdfName 形式
        assertThat(PdfStructureExtractor.canonicalRole("标题 1")).isEqualTo("H1");
    }

    @Test
    void wordBodyListAliasesNormalizeToStandardRoles() {
        assertThat(PdfStructureExtractor.canonicalRole("p")).isEqualTo("P");
        assertThat(PdfStructureExtractor.canonicalRole("paragraph")).isEqualTo("P");
        assertThat(PdfStructureExtractor.canonicalRole("正文")).isEqualTo("P");
        assertThat(PdfStructureExtractor.canonicalRole("l")).isEqualTo("L");
        assertThat(PdfStructureExtractor.canonicalRole("list")).isEqualTo("L");
        assertThat(PdfStructureExtractor.canonicalRole("li")).isEqualTo("LI");
        assertThat(PdfStructureExtractor.canonicalRole("list item")).isEqualTo("LI");
    }

    @Test
    void wordTableAliasesNormalizeToStandardRoles() {
        assertThat(PdfStructureExtractor.canonicalRole("table")).isEqualTo("Table");
        assertThat(PdfStructureExtractor.canonicalRole("tr")).isEqualTo("TR");
        assertThat(PdfStructureExtractor.canonicalRole("table row")).isEqualTo("TR");
        assertThat(PdfStructureExtractor.canonicalRole("td")).isEqualTo("TD");
        assertThat(PdfStructureExtractor.canonicalRole("th")).isEqualTo("TH");
        assertThat(PdfStructureExtractor.canonicalRole("table header cell")).isEqualTo("TH");
    }

    @Test
    void standardRolesPassThroughAndUnknownPreserved() {
        // 标准角色名不因小写查找被改写
        assertThat(PdfStructureExtractor.canonicalRole("Table")).isEqualTo("Table");
        assertThat(PdfStructureExtractor.canonicalRole("/H1")).isEqualTo("H1");
        assertThat(PdfStructureExtractor.canonicalRole(null)).isEmpty();
        assertThat(PdfStructureExtractor.canonicalRole("")).isEmpty();
        // 未识别的自定义角色原样保留
        assertThat(PdfStructureExtractor.canonicalRole("MyCustom")).isEqualTo("MyCustom");
    }

    // ---------------- W3-3: 页级流式提取 ----------------

    private static File writePdf(File dir, String name, String html) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf(html, out);
        File pdf = new File(dir, name);
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());
        return pdf;
    }

    @Test
    void perPageCallbacksCoverEveryPageWithLocalText(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "three-pages.pdf", "<html><body>"
            + "<p>第一页独有内容甲</p>"
            + "<p style='page-break-before:always'>第二页独有内容乙</p>"
            + "<p style='page-break-before:always'>第三页独有内容丙</p>"
            + "</body></html>");

        final List<Integer> pageNos = new ArrayList<Integer>();
        final List<DocumentStructure> partials = new ArrayList<DocumentStructure>();
        PdfStructureExtractor.extractPerPage(pdf, null, new PdfStructureExtractor.PageConsumer() {
            @Override public boolean page(int pageNo, DocumentStructure pagePartial) {
                pageNos.add(Integer.valueOf(pageNo));
                partials.add(pagePartial);
                return true;
            }
        });

        assertThat(pageNos).containsExactly(1, 2, 3); // 回调计数 = 页数，页码 1..N
        String md1 = partials.get(0).fullMarkdown();
        String md2 = partials.get(1).fullMarkdown();
        String md3 = partials.get(2).fullMarkdown();
        // 第 N 页 partial 含第 N 页独有文本、不含他页文本
        assertThat(md1).contains("第一页独有内容甲").doesNotContain("乙").doesNotContain("丙");
        assertThat(md2).contains("第二页独有内容乙").doesNotContain("甲").doesNotContain("丙");
        assertThat(md3).contains("第三页独有内容丙").doesNotContain("甲").doesNotContain("乙");
        // title 继承文档标题
        assertThat(partials.get(0).title).isNotEmpty();
        assertThat(partials.get(1).title).isEqualTo(partials.get(0).title);
    }

    @Test
    void taggedSinglePageStreamsAsOneCallback(@TempDir File dir) throws Exception {
        File pdf = new File(dir, "tagged-one.pdf");
        EasyPdf.markdownToPdfTagged("# 标题页\n\n正文内容在此。\n", pdf);

        final List<Integer> pageNos = new ArrayList<Integer>();
        final List<String> texts = new ArrayList<String>();
        PdfStructureExtractor.extractPerPage(pdf, null, new PdfStructureExtractor.PageConsumer() {
            @Override public boolean page(int pageNo, DocumentStructure pagePartial) {
                pageNos.add(Integer.valueOf(pageNo));
                texts.add(pagePartial.fullMarkdown());
                return true;
            }
        });
        assertThat(pageNos).containsExactly(1);
        assertThat(texts.get(0)).contains("标题页").contains("正文内容在此。");
    }

    @Test
    void aggregateMergesPerPagePartialsLikeWholeDocFlow() {
        // part1：隐式继承段(正文X) + 标题"第二章"；part2：隐式继承段(续流Y)
        DocumentStructure p1 = new DocumentStructure();
        p1.title = "文档标题";
        DocumentSection lead1 = new DocumentSection();
        lead1.title = "文档标题"; lead1.level = 1; lead1.content = "开篇正文";
        DocumentSection h2 = new DocumentSection();
        h2.title = "第二章"; h2.level = 2; h2.content = "";
        p1.sections = new ArrayList<DocumentSection>(Arrays.asList(lead1, h2));

        DocumentStructure p2 = new DocumentStructure();
        p2.title = "文档标题";
        DocumentSection lead2 = new DocumentSection();
        lead2.title = "文档标题"; lead2.level = 1; lead2.content = "第二章跨页续文";
        p2.sections = new ArrayList<DocumentSection>(Arrays.asList(lead2));

        DocumentStructure agg = PdfStructureExtractor.aggregate(Arrays.asList(p1, p2));
        assertThat(agg.sections).hasSize(2); // 不新增重复继承段
        assertThat(agg.sections.get(0).content).isEqualTo("开篇正文");
        // 续流并入上一节（全篇流动正文的等价形态）
        assertThat(agg.sections.get(1).title).isEqualTo("第二章");
        assertThat(agg.sections.get(1).content).isEqualTo("第二章跨页续文");
    }

    @Test
    void extractEndToEndKeepsAllPageTexts(@TempDir File dir) throws Exception {
        // 行为护栏：整篇 extract 的结果仍覆盖每页文本
        File pdf = writePdf(dir, "flow.pdf", "<html><body>"
            + "<p>首页要点记录。</p>"
            + "<p style='page-break-before:always'>次页补充说明。</p>"
            + "</body></html>");
        String md = PdfStructureExtractor.extract(pdf).fullMarkdown();
        assertThat(md).contains("首页要点记录。").contains("次页补充说明。");
    }

    // ---------------- W3-4: 提取结果 LRU 缓存 ----------------

    @Test
    void cacheEnabledSecondExtractHits(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "cached.pdf",
            "<html><body><p>缓存命中验证文本。</p></body></html>");
        ExtractCache.shared().clear(); // 测试隔离
        PdfExtractionProperties props = PdfExtractionProperties.defaults();
        props.cacheEnabled = true;

        DocumentStructure first = PdfStructureExtractor.extract(pdf, props);
        assertThat(ExtractCache.shared().misses()).isEqualTo(1);

        DocumentStructure second = PdfStructureExtractor.extract(pdf, props);
        assertThat(ExtractCache.shared().hits()).isEqualTo(1); // 二次调用直接命中
        assertThat(second.fullMarkdown()).isEqualTo(first.fullMarkdown());
        assertThat(second.fullMarkdown()).contains("缓存命中验证文本。");
    }

    @Test
    void cacheDisabledByDefaultNeverCountsHits(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "plain.pdf",
            "<html><body><p>未开启缓存应重复解析。</p></body></html>");
        ExtractCache.shared().clear();
        PdfStructureExtractor.extract(pdf);
        PdfStructureExtractor.extract(pdf);
        assertThat(ExtractCache.shared().hits()).isEqualTo(0);
        assertThat(ExtractCache.shared().misses()).isEqualTo(0); // 默认关：完全不触缓存
    }

    @Test
    void cacheKeyBindsPathSizeAndMtime(@TempDir File dir) throws Exception {
        File f = new File(dir, "k.pdf");
        java.nio.file.Files.write(f.toPath(), new byte[] {1});
        String k1 = ExtractCache.keyOf(f);
        assertThat(ExtractCache.keyOf(f)).isEqualTo(k1);
        f.setLastModified(f.lastModified() + 60_000L); // 修改时间变化 → 新 key
        assertThat(ExtractCache.keyOf(f)).isNotEqualTo(k1);
    }

    @Test
    void lruEvictsLeastRecentlyUsedBeyondCapacity() {
        ExtractCache cache = new ExtractCache(2);
        DocumentStructure a = new DocumentStructure();
        DocumentStructure b = new DocumentStructure();
        DocumentStructure c = new DocumentStructure();
        cache.put("k1", a);
        cache.put("k2", b);
        cache.get("k1");              // k1 升为最近使用
        cache.put("k3", c);           // 容量 2：淘汰最久未用的 k2
        assertThat(cache.get("k1")).isSameAs(a);
        assertThat(cache.get("k2")).isNull();
        assertThat(cache.get("k3")).isSameAs(c);
    }

    // ---------------- W3-5: 阈值配置化 + REST 重试 ----------------

    @Test
    void extractionThresholdDefaultsMatchCurrentBehavior() {
        PdfExtractionProperties d = PdfExtractionProperties.defaults();
        assertThat(d.headFactor).isEqualTo(1.22f);
        assertThat(d.maxHeadingTiers).isEqualTo(3);
        assertThat(d.coverRunMinLines).isEqualTo(2);
        assertThat(d.coverRatio).isEqualTo(1.5f);
        assertThat(d.columnGapPt).isEqualTo(55f);
        assertThat(d.streamAlignTolPt).isEqualTo(6f);
        assertThat(d.restRetries).isEqualTo(0);
        assertThat(d.cacheEnabled).isFalse();
    }

    @Test
    void restRetriesWithExponentialBackoffSucceedsAfterTransient500() throws Exception {
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        final String json = "{\"title\":\"重试成功\",\"sections\":[{\"title\":\"章节\",\"level\":1,\"content\":\"正文\"}],\"tables\":[]}";
        server.createContext("/layout", exchange -> {
            boolean firstCall = calls.incrementAndGet() == 1;
            byte[] resp = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (firstCall) {
                exchange.sendResponseHeaders(500, 0); // 第 1 次瞬时故障
                exchange.close();
                return;
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
        try {
            PdfExtractionProperties props = PdfExtractionProperties.defaults();
            props.restEndpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/layout";
            props.restTimeoutMillis = 3000;
            props.restRetries = 1;

            DocumentStructure ds = new RestLayoutAnalyzer(props)
                    .analyze(new byte[] {9}, "本地标题");
            assertThat(ds.title).isEqualTo("重试成功"); // 第 2 次 200 成功
            assertThat(calls.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void restRetriesCapAppliedOnPersistent500() throws Exception {
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/layout", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(500, 0);
            exchange.close();
        });
        server.start();
        try {
            PdfExtractionProperties props = PdfExtractionProperties.defaults();
            props.restEndpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/layout";
            props.restTimeoutMillis = 3000;
            props.restRetries = 5; // 超上限应被压到 3

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new RestLayoutAnalyzer(props).analyze(new byte[] {9}, "t"))
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("HTTP 500");
            assertThat(calls.get()).isEqualTo(4); // 1 次原始请求 + 上限 3 次重试
        } finally {
            server.stop(0);
        }
    }

    @Test
    void restDoesNotRetryClientErrors() throws Exception {
        java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/layout", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(403, 0); // 4xx（非 429）不重试
            exchange.close();
        });
        server.start();
        try {
            PdfExtractionProperties props = PdfExtractionProperties.defaults();
            props.restEndpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/layout";
            props.restTimeoutMillis = 3000;
            props.restRetries = 2;

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> new RestLayoutAnalyzer(props).analyze(new byte[] {9}, "t"))
                    .isInstanceOf(java.io.IOException.class)
                    .hasMessageContaining("HTTP 403");
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void backoffGrowsExponentiallyFromBase500ms() {
        assertThat(RestLayoutAnalyzer.backoffMillis(0)).isEqualTo(500L);
        assertThat(RestLayoutAnalyzer.backoffMillis(1)).isEqualTo(1000L);
        assertThat(RestLayoutAnalyzer.backoffMillis(2)).isEqualTo(2000L);
        assertThat(RestLayoutAnalyzer.backoffMillis(7)).isEqualTo(8000L); // 封顶防移位溢出
    }

    @Test
    void cancelStopsPerPageStreaming(@org.junit.jupiter.api.io.TempDir java.io.File dir) throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(dir.isDirectory());
        // 3 页文档：回调在第 2 页后返回 false，第 3 页不应被消费
        String html = "<html><body>"
            + "<p>第一页内容标记。</p>"
            + "<p style='page-break-before:always'>第二页内容标记。</p>"
            + "<p style='page-break-before:always'>第三页内容标记。</p>"
            + "</body></html>";
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        io.github.easy4j.pdf.core.convert.HtmlPdfConverter.htmlToPdf(html, out);
        java.io.File pdf = new java.io.File(dir, "cancel.pdf");
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());

        final java.util.List<Integer> seen = new java.util.ArrayList<Integer>();
        PdfStructureExtractor.extractPerPage(pdf, null, new PdfStructureExtractor.PageConsumer() {
            @Override public boolean page(int pageNo, DocumentStructure pagePartial) {
                seen.add(Integer.valueOf(pageNo));
                return pageNo < 2; // 第 2 页后取消
            }
        });
        assertThat(seen).containsExactly(1, 2); // 第 3 页未回调
    }

    // ---------------- Round4-P2 Task 2: 安全护栏（maxFileBytes/maxPages）与错误分级 ----------------

    @Test
    void oversizedPdfRejected(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "tiny-but-over-limit.pdf",
            "<html><body><p>超限护栏验证文本。</p></body></html>");
        PdfExtractionProperties props = PdfExtractionProperties.defaults();
        props.maxFileBytes = 10L;

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> PdfStructureExtractor.extract(pdf, props))
            .isInstanceOf(ExtractionException.class)
            .isInstanceOfSatisfying(ExtractionException.class,
                e -> assertThat(e.getCode()).isEqualTo(ExtractionException.Code.LIMIT_EXCEEDED));
    }

    @Test
    void passwordProtectedDetected(@TempDir File dir) throws Exception {
        // 用 iText 自身生成标准加密样本：user/owner 口令 + RC4-128 + 仅允许打印
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        WriterProperties wp = new WriterProperties().setStandardEncryption(
            "user".getBytes(StandardCharsets.UTF_8),
            "owner".getBytes(StandardCharsets.UTF_8),
            EncryptionConstants.ALLOW_PRINTING,
            EncryptionConstants.STANDARD_ENCRYPTION_128);
        PdfDocument doc = new PdfDocument(new PdfWriter(out, wp));
        HtmlConverter.convertToPdf("<html><body><p>加密样本文本。</p></body></html>", doc, null);
        File pdf = new File(dir, "encrypted.pdf");
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PdfStructureExtractor.extract(pdf))
            .isInstanceOf(ExtractionException.class)
            .isInstanceOfSatisfying(ExtractionException.class,
                e -> assertThat(e.getCode()).isEqualTo(ExtractionException.Code.ENCRYPTED));
    }

    @Test
    void corruptBytesMapToCorrupt(@TempDir File dir) throws Exception {
        File pdf = new File(dir, "corrupt.pdf");
        java.nio.file.Files.write(pdf.toPath(),
            "%PDF-1.4\n垃圾字节流非合法PDF".getBytes(StandardCharsets.UTF_8));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PdfStructureExtractor.extract(pdf))
            .isInstanceOf(ExtractionException.class)
            .isInstanceOfSatisfying(ExtractionException.class,
                e -> assertThat(e.getCode()).isEqualTo(ExtractionException.Code.CORRUPT));
    }

    // ---------------- Round5-Observability Task 1: pageRange 参数校验 ----------------

    @Test
    void pageRangeRejectsFromGreaterThanTo(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "range-order.pdf",
            "<html><body><p>页序校验文本。</p></body></html>");

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> EasyPdf.pageRange(pdf, 5, 2)) // from > to 必须立即拒绝
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("fromPage")
            .hasMessageContaining("toPage");
    }

    @Test
    void pageRangeRejectsZeroOrNegativeArgs(@TempDir File dir) throws Exception {
        File pdf = writePdf(dir, "range-zero.pdf",
            "<html><body><p>零负参校验文本。</p></body></html>");

        // from=0 / to=0 / 负数：页码从 1 起算，全部拒绝
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> EasyPdf.pageRange(pdf, 0, 1))
            .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> EasyPdf.pageRange(pdf, 1, 0))
            .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> EasyPdf.pageRange(pdf, -1, 2))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageRangeAcceptsValidRange(@TempDir File dir) throws Exception {
        String html = "<html><body>"
            + "<p>区间第一页独有内容甲。</p>"
            + "<p style='page-break-before:always'>区间第二页独有内容乙。</p>"
            + "<p style='page-break-before:always'>区间第三页独有内容丙。</p>"
            + "</body></html>";
        File pdf = writePdf(dir, "three-pages-range.pdf", html);

        String md = EasyPdf.pageRange(pdf, 1, 3);
        assertThat(md).isNotEmpty();
        assertThat(md).contains("区间第一页独有内容甲").contains("区间第二页独有内容乙")
            .contains("区间第三页独有内容丙");
    }

    // ---------------- Round5-Security Task 1: 显式禁用嵌入式 JavaScript ----------------

    @Test
    void pdfWithEmbeddedJavaScriptIgnored(@TempDir File dir) throws Exception {
        // 夹具：catalog 挂顶层 /JS 与 /JavaScript 脚本 + OpenAction 为 JavaScript action，
        // 覆盖打开文档时最典型的脚本解析入口（iText 内核本无 JS 解释器、不执行脚本；
        // 提取侧的防护由 ParsedDoc 打开后立即剥离上述 catalog 向量白盒保证）
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument d = new PdfDocument(new PdfWriter(out))) {
            d.addNewPage();
            PdfDictionary jsAction = new PdfDictionary();
            jsAction.put(PdfName.S, PdfName.JavaScript);
            jsAction.put(PdfName.JS, new PdfString("app.alert('embedded')"));
            d.getCatalog().put(PdfName.OpenAction, jsAction);
            d.getCatalog().put(PdfName.JS, new PdfString("app.alert(1)"));
            d.getCatalog().put(PdfName.JavaScript, new PdfString("this.doSomething()"));
        }
        File pdf = new File(dir, "embedded-js.pdf");
        java.nio.file.Files.write(pdf.toPath(), out.toByteArray());

        // 主断言：提取成功返回结构（不抛、不阻塞），解析路径不因脚本字典失败
        DocumentStructure doc = PdfStructureExtractor.extract(pdf);
        assertThat(doc).isNotNull();
    }

    // ---------------- Round5-Security Task 3: canonical 路径校验 + 日志转义 ----------------

    @Test
    void requiresNonNullPdf() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> EasyPdf.pdfToMarkdown((File) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requiresFileThatExists() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> EasyPdf.pdfToMarkdown(new File("/no.pdf")))
            .isInstanceOf(ExtractionException.class)
            .isInstanceOfSatisfying(ExtractionException.class,
                e -> assertThat(e.getCode()).isEqualTo(ExtractionException.Code.NOT_FOUND));
    }

    @Test
    void escapeHandlesControlChars() {
        String in = "line1\nline2\tcol";
        String out = EasyPdf.escapeForLog(in); // 同包可见
        assertThat(out).doesNotContain("\n").doesNotContain("\t");
        // 反斜杠自身先转义，保证转义序列不可被二次解释
        assertThat(EasyPdf.escapeForLog("a\\nb")).isEqualTo("a\\\\nb");
        assertThat(EasyPdf.escapeForLog("r\\r")).isEqualTo("r\\\\r");
    }
}
