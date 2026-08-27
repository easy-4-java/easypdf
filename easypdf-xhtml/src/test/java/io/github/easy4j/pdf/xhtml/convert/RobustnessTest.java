package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
}
