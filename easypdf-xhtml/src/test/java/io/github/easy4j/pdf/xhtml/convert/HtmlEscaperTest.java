package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HtmlEscaper")
class HtmlEscaperTest {

    @Test
    @DisplayName("& is escaped first so other entities are not double-escaped")
    void ampersandEscapedFirst() {
        assertThat(HtmlEscaper.escape("&")).isEqualTo("&amp;");
    }

    @Test
    @DisplayName("< is escaped to &lt;")
    void lessThanEscaped() {
        assertThat(HtmlEscaper.escape("<")).isEqualTo("&lt;");
    }

    @Test
    @DisplayName("> is escaped to &gt;")
    void greaterThanEscaped() {
        assertThat(HtmlEscaper.escape(">")).isEqualTo("&gt;");
    }

    @Test
    @DisplayName("\" is escaped to &quot;")
    void doubleQuoteEscaped() {
        assertThat(HtmlEscaper.escape("\"")).isEqualTo("&quot;");
    }

    @Test
    @DisplayName("combined HTML tag is fully escaped")
    void combinedHtmlTag() {
        assertThat(HtmlEscaper.escape("<a href=\"x\">"))
                .isEqualTo("&lt;a href=&quot;x&quot;&gt;");
    }

    @Test
    @DisplayName("null in → null out")
    void nullReturnsNull() {
        assertThat(HtmlEscaper.escape(null)).isNull();
    }

    @Test
    @DisplayName("empty string in → empty string out")
    void emptyReturnsEmpty() {
        assertThat(HtmlEscaper.escape("")).isEmpty();
    }

    @Test
    @DisplayName("already-escaped text is NOT idempotent (double-escaped)")
    void alreadyEscapedIsDoubleEscaped() {
        // &lt; contains '&' which gets escaped again → &amp;lt;
        assertThat(HtmlEscaper.escape("&lt;")).isEqualTo("&amp;lt;");
    }
}
