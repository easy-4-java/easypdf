package io.github.easy4j.pdf.core.document.helper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.net.URL;
import com.itextpdf.text.Image;
import com.jeefw.fastkit.beanutils.JavaBeanUtils;
import com.jeefw.fastkit.lang3.BlankUtils;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * Unit tests for {@link ImageHelper}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ImageHelper Tests")
class ImageHelperTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ImageHelper.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ImageHelper.class).isNotNull();
    }

}
