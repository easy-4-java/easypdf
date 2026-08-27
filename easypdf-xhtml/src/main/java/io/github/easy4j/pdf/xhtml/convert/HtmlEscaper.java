package io.github.easy4j.pdf.xhtml.convert;

/**
 * Package-private HTML-escape utility for embedding user-controlled text into
 * HTML-bearing contexts (e.g.&nbsp;when {@code <span style="color:...">}
 * wrapping is later added to markdown output).
 *
 * <p>This helper does <b>not</b> need to be called by current code paths, which
 * emit pure GFM where {@code &}, {@code <}, and {@code >} are literal
 * characters. It exists now so that future color-rendering code can rely on a
 * well-tested escape routine.</p>
 *
 * <p><b>Important:</b> escaping is <em>not</em> idempotent. Calling
 * {@code escape(escape(x))} will double-escape. This is by design &mdash; the
 * method is meant to be applied exactly once to raw text before it is inserted
 * into an HTML context.</p>
 */
final class HtmlEscaper {

    private HtmlEscaper() {
    }

    /**
     * Escapes the four fundamental HTML/XML special characters in the given
     * string.
     *
     * <ul>
     *   <li>{@code &} &rarr; {@code &amp;amp;}</li>
     *   <li>{@code <} &rarr; {@code &amp;lt;}</li>
     *   <li>{@code >} &rarr; {@code &amp;gt;}</li>
     *   <li>{@code "} &rarr; {@code &amp;quot;}</li>
     * </ul>
     *
     * <p>{@code null} in &rarr; {@code null} out.
     * Empty string in &rarr; empty string out.</p>
     *
     * @param value the raw text to escape; may be {@code null}
     * @return the escaped text, or {@code null} if the input was {@code null}
     */
    static String escape(String value) {
        if (value == null) {
            return null;
        }
        // Fast-path: no characters to escape
        if (value.isEmpty()) {
            return value;
        }
        // & MUST be escaped first to avoid double-escaping the & we inject for
        // the other four entities.
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
