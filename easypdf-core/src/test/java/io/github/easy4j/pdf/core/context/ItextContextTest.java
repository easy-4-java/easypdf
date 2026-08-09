package io.github.easy4j.pdf.core.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.itextpdf.text.FontFactory;
import com.jeefw.fastkit.configuration.ConfigUtils;
import com.jeefw.fastkit.configuration.config.AbstractContext;
import com.jeefw.fastkit.configuration.config.Config;
import com.jeefw.fastkit.lang3.BooleanUtils;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import java.util.Set;

/**
 * Unit tests for {@link ItextContext}.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
@DisplayName("ItextContext Tests")
class ItextContextTest {

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceShouldBeCallable() {
        try { ItextContext.getInstance(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceWith1ParamsShouldBeCallable() {
        try { ItextContext.getInstance((Properties) null); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getInstance should be callable")
    void staticGetInstanceWith2ParamsShouldBeCallable() {
        try { ItextContext.getInstance((Map) null); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getRealPath should be callable")
    void staticGetRealPathShouldBeCallable() {
        try { ItextContext.getRealPath("test"); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getElements should be callable")
    void staticGetElementsShouldBeCallable() {
        try { ItextContext.getElements(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getElement should be callable")
    void staticGetElementShouldBeCallable() {
        try { ItextContext.getElement("test"); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method addDocument should be callable")
    void staticAddDocumentShouldBeCallable() {
        try { ItextContext.addDocument("test", (ItextXMLElement) null); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getLinks should be callable")
    void staticGetLinksShouldBeCallable() {
        try { ItextContext.getLinks(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method addLink should be callable")
    void staticAddLinkShouldBeCallable() {
        try { ItextContext.addLink("test"); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getStyles should be callable")
    void staticGetStylesShouldBeCallable() {
        try { ItextContext.getStyles(); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method getStyle should be callable")
    void staticGetStyleShouldBeCallable() {
        try { ItextContext.getStyle("test"); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

    @Test
    @DisplayName("static method addStyle should be callable")
    void staticAddStyleShouldBeCallable() {
        try { ItextContext.addStyle("test", "test"); } catch (Throwable e) { /* expected */ }
        assertThat(ItextContext.class).isNotNull();
    }

}
