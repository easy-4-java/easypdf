package io.github.easy4j.pdf.xhtml.convert.layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.easy4j.pdf.xhtml.convert.DocumentSection;
import io.github.easy4j.pdf.xhtml.convert.DocumentStructure;
import io.github.easy4j.pdf.xhtml.convert.DocumentTable;

/**
 * Tier3 ML 布局服务适配器：把 PDF 字节 POST 到外部布局分析服务
 * （docling / MinerU / LayoutLMv3 类），按约定 JSON 契约解析结果：
 *
 * <pre>
 * {"title":"..",
 *  "sections":[{"title":"..","level":1,"content":".."}],
 *  "tables":[{"headers":[["a","b"]],"rows":[["1","2"]]}]}
 * </pre>
 *
 * 零新依赖：JDK HttpURLConnection + 正则化 JSON 解析（契约字段固定，可接受）。
 */
public final class RestLayoutAnalyzer implements LayoutAnalyzer {

    private static final Pattern SECTION = Pattern.compile(
            "\\{\"title\":\"((?:[^\"\\\\]|\\\\.)*?)\",\"level\":(\\d+),\"content\":\"((?:[^\"\\\\]|\\\\.)*?)\"\\}");
    private static final Pattern TABLE = Pattern.compile(
            "\\{\"headers\":\\[([^\\{]*?)\\],\"rows\":\\[([^\\{]*?)\\]}");
    private static final Pattern ARRAY = Pattern.compile(
            "\\[(?:\"(?:[^\"\\\\]|\\\\.)*\"|\\d+)*(?:,(?:\"(?:[^\"\\\\]|\\\\.)*\"|\\d+))*\\]");

    private final PdfExtractionProperties props;

    public RestLayoutAnalyzer(PdfExtractionProperties props) {
        Objects.requireNonNull(props, "props must not be null");
        if (props.restEndpoint == null || props.restEndpoint.isEmpty()) {
            throw new IllegalArgumentException("restEndpoint must be configured for REST engine");
        }
        this.props = props;
    }

    @Override
    public String name() {
        return "rest";
    }

    @Override
    public DocumentStructure analyze(List<PageModel> pages, List<int[]> taggedHeadings, String title)
            throws IOException {
        throw new IOException("REST engine requires raw PDF bytes; use analyze(byte[], String)");
    }

    /** 以原始 PDF 字节调用外部布局服务（PdfStructureExtractor 传文件字节）。 */
    public DocumentStructure analyze(byte[] pdfBytes, String title) throws IOException {
        String json = post(pdfBytes);
        return parse(json, title);
    }

    /**
     * POST（含指数退避重试）：仅对 IOException / HTTP 429 / 5xx 重试，
     * 次数取 {@code props.restRetries} 且上限 {@value #MAX_REST_RETRIES}，
     * 退避间隔 base 500ms 指数递增（500/1000/2000ms）。
     */
    private String post(byte[] body) throws IOException {
        int retries = Math.max(0, Math.min(props.restRetries, MAX_REST_RETRIES));
        for (int attempt = 0; ; attempt++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(props.restEndpoint).openConnection();
            try {
                return exchange(conn, body);
            } catch (RestServiceException e) {
                if (attempt >= retries || !e.retryable) {
                    throw e;
                }
            } catch (IOException e) {
                if (attempt >= retries) {
                    throw e;
                }
            }
            sleep(backoffMillis(attempt));
        }
    }

    static final int MAX_REST_RETRIES = 3;
    private static final long BACKOFF_BASE_MS = 500L;

    /** 第 failedAttempt 次失败后的退避间隔：base 500ms 指数递增，移位封顶防溢出。 */
    public static long backoffMillis(int failedAttempt) {
        return BACKOFF_BASE_MS * (1L << Math.min(Math.max(failedAttempt, 0), 4));
    }

    private static void sleep(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new java.io.InterruptedIOException("rest retry interrupted");
        }
    }

    /** 布局服务返回非 2xx；retryable 标记 429/5xx 可重试。 */
    private static final class RestServiceException extends IOException {
        final boolean retryable;
        RestServiceException(String message, boolean retryable) {
            super(message);
            this.retryable = retryable;
        }
    }

    private static boolean isRetryableStatus(int code) {
        return code == 429 || code >= 500;
    }

    /** 单次交换：写出请求体、读干响应流；2xx 返回文本，否则抛 RestServiceException。 */
    private String exchange(HttpURLConnection conn, byte[] body) throws IOException {
        conn.setConnectTimeout(props.restTimeoutMillis);
        conn.setReadTimeout(props.restTimeoutMillis);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/pdf");
        conn.setRequestProperty("Accept", "application/json");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }
        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) > 0) {
            buf.write(b, 0, n);
        }
        if (code < 200 || code >= 300) {
            throw new RestServiceException("layout service HTTP " + code, isRetryableStatus(code));
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    static DocumentStructure parse(String json, String fallbackTitle) {
        DocumentStructure doc = new DocumentStructure();
        String t = firstScalar(json, "title");
        doc.title = (t != null && !t.isEmpty()) ? t : fallbackTitle;
        Matcher sm = SECTION.matcher(json);
        while (sm.find()) {
            DocumentSection s = new DocumentSection();
            s.title = unescape(sm.group(1));
            try { s.level = Integer.parseInt(sm.group(2)); } catch (NumberFormatException e) { s.level = 1; }
            s.content = unescape(sm.group(3));
            doc.sections.add(s);
        }
        Matcher tm = TABLE.matcher(json);
        while (tm.find()) {
            DocumentTable tbl = new DocumentTable();
            tbl.headers = parseRows(tm.group(1));
            tbl.rows = parseRows(tm.group(2));
            doc.tables.add(tbl);
        }
        if (doc.sections.isEmpty()) {
            DocumentSection s = new DocumentSection();
            s.title = doc.title != null ? doc.title : "Document";
            s.level = 1;
            doc.sections.add(s);
        }
        return doc;
    }

    /** 解析 [["a","b"],["1","2"]] 形式的双层字符串数组。 */
    private static List<List<String>> parseRows(String nested) {
        List<List<String>> out = new ArrayList<List<String>>();
        Matcher am = ARRAY.matcher(nested);
        while (am.find()) {
            String inner = am.group();
            List<String> cells = new ArrayList<String>();
            Matcher cm = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(inner);
            while (cm.find()) {
                cells.add(unescape(cm.group(1)));
            }
            out.add(cells);
        }
        return out;
    }

    private static String firstScalar(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        return m.find() ? unescape(m.group(1)) : null;
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
