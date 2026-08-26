package io.github.easy4j.pdf.xhtml.convert.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import io.github.easy4j.pdf.core.convert.HtmlPdfConverter;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;
import io.github.easy4j.pdf.xhtml.convert.PdfStructureExtractor;

class RestLayoutAnalyzerTest {

    private static final String JSON = "{\"title\":\"远端标题\","
        + "\"sections\":[{\"title\":\"远端章节\",\"level\":2,\"content\":\"远端正文内容\\n第二行\"}],"
        + "\"tables\":[{\"headers\":[[\"列甲\",\"列乙\"]],\"rows\":[[\"值一\",\"值二\"]]}]}";

    @Test
    void parsesServiceJsonContract() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/layout", exchange -> {
            byte[] body = readAll(exchange.getRequestBody());
            Files.write(new File("/tmp/last-rest-body.bin").toPath(), body);
            byte[] resp = JSON.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        server.start();
        try {
            PdfExtractionProperties props = PdfExtractionProperties.defaults();
            props.engine = PdfExtractionProperties.Engine.REST;
            props.restEndpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/layout";
            props.restTimeoutMillis = 3000;
            RestLayoutAnalyzer rest = new RestLayoutAnalyzer(props);
            DocumentStructure ds = rest.analyze(new byte[]{1, 2, 3}, "本地标题");
            assertThat(ds.title).isEqualTo("远端标题");
            assertThat(ds.fullMarkdown()).contains("## 远端章节").contains("远端正文内容");
            assertThat(ds.tables.get(0).headers.get(0)).containsExactly("列甲", "列乙");
            // 服务端确实收到了 POST 的 PDF 字节
            assertThat(new File("/tmp/last-rest-body.bin").length()).isEqualTo(3);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void constructorRejectsMissingEndpoint() {
        assertThatThrownBy(() -> new RestLayoutAnalyzer(PdfExtractionProperties.defaults()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void autoFallsBackToRuleWhenServiceUnreachable(@TempDir File dir) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlPdfConverter.htmlToPdf("<html><body><p>回退正文验证。</p></body></html>", out);
        File pdf = new File(dir, "f.pdf");
        Files.write(pdf.toPath(), out.toByteArray());

        PdfExtractionProperties props = PdfExtractionProperties.defaults();
        props.engine = PdfExtractionProperties.Engine.AUTO;
        props.restEndpoint = "http://127.0.0.1:1/dead"; // 端口 1 必拒
        props.restTimeoutMillis = 1000;
        DocumentStructure ds = PdfStructureExtractor.extract(pdf, props);
        assertThat(ds.fullMarkdown()).contains("回退正文验证。");
    }

    private static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) > 0) {
            buf.write(b, 0, n);
        }
        return buf.toByteArray();
    }
}
