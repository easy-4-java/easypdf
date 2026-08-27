package io.github.easy4j.pdf.xhtml.convert.layout;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TierConfigTest {

    @Test
    void defaultsMatchHardcodedValues() {
        TierConfig d = TierConfig.DEFAULT;
        assertThat(d.columnGapPt).isEqualTo(55f);
        assertThat(d.streamAlignTolPt).isEqualTo(6f);
        assertThat(d.coverRunMinLines).isEqualTo(2f);
        assertThat(d.coverRatio).isEqualTo(1.5f);
        assertThat(d.headFactor).isEqualTo(1.22f);
        assertThat(d.maxHeadingTiers).isEqualTo(3);
    }

    @Test
    void customInstanceCanOverrideFields() {
        TierConfig c = new TierConfig();
        c.headFactor = 1.5f;
        assertThat(c.headFactor).isEqualTo(1.5f);
        assertThat(TierConfig.DEFAULT.headFactor).isEqualTo(1.22f); // DEFAULT 不变
    }

    @Test
    void fromPropertiesReadsAllMappedFields() {
        PdfExtractionProperties p = new PdfExtractionProperties();
        p.headFactor = 1.8f; p.columnGapPt = 100f; p.maxHeadingTiers = 5;
        TierConfig t = TierConfig.from(p);
        assertThat(t.headFactor).isEqualTo(1.8f);
        assertThat(t.columnGapPt).isEqualTo(100f);
        assertThat(t.maxHeadingTiers).isEqualTo(5);
    }
}
