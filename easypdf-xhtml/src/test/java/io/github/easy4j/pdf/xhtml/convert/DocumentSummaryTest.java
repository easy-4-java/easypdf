package io.github.easy4j.pdf.xhtml.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class DocumentSummaryTest {

    @Test
    void summaryCarriesMetricsAndSectionTree() {
        DocumentSummarySection s1 = new DocumentSummarySection();
        s1.title = "一"; s1.level = 1; s1.pageNo = 1; s1.charCount = 200;
        DocumentSummarySection s2 = new DocumentSummarySection();
        s2.title = "1.1"; s2.level = 2; s2.pageNo = 2; s2.charCount = 80;
        DocumentSummary sum = new DocumentSummary();
        sum.title = "测试"; sum.totalPages = 5; sum.totalChars = 280;
        sum.sections = Arrays.asList(s1, s2);
        assertThat(sum.sections).hasSize(2);
        assertThat(sum.sections.get(1).title).isEqualTo("1.1");
    }

    @Test
    void summaryDefaultsToEmptySections() {
        DocumentSummary sum = new DocumentSummary();
        sum.title = "空";
        sum.sections = Collections.emptyList();
        assertThat(sum.totalPages).isZero();
        assertThat(sum.sections).isEmpty();
    }
}
