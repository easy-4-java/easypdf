package io.github.easy4j.pdf.xhtml.convert.layout;

import java.util.List;
import java.util.Objects;

import com.itextpdf.kernel.pdf.PdfDocument;

/**
 * @deprecated Use {@link PageModelCollector} directly. This class delegates all calls
 * to the renamed collector for backward compatibility during the transition period.
 */
@Deprecated
public final class PageModelListener {

    private final int pageNo;

    public PageModelListener(int pageNo) {
        this.pageNo = pageNo;
    }

    /** @deprecated Use {@link PageModelCollector#collect(PdfDocument)} instead. */
    @Deprecated
    public static List<PageModel> collect(PdfDocument doc) {
        return PageModelCollector.collect(doc);
    }
}
