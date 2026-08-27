package io.github.easy4j.pdf.it.render;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Template-engine → PDF end-to-end render tests.
 *
 * <p>Will exercise {@code easypdf-beetl}, {@code easypdf-freemarker},
 * and {@code easypdf-thymeleaf} against hand-curated HTML templates under
 * {@code src/test/resources/templates/}.
 *
 * <p><b>Status:</b> all cases {@link Disabled @Disabled} until templates and
 * expected PDF byte-length snapshots are committed by hand.
 */
class HtmlTemplateIT {

    @Test
    @Disabled("enable after src/test/resources/templates/beetl-invoice.btl is committed")
    @DisplayName("beetl: invoice.html → PDF")
    void beetlInvoice() {
    }

    @Test
    @Disabled("enable after src/test/resources/templates/freemarker-report.ftl is committed")
    @DisplayName("freemarker: report.ftl → PDF")
    void freemarkerReport() {
    }

    @Test
    @Disabled("enable after src/test/resources/templates/thymeleaf-letter.html is committed")
    @DisplayName("thymeleaf: letter.html → PDF")
    void thymeleafLetter() {
    }
}