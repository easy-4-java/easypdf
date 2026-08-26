package io.github.easy4j.pdf.freemarker;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.docx4j.Docx4jProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.utility.HtmlEscape;
import freemarker.template.utility.XmlEscape;
import io.github.easy4j.pdf.template.AbstractStringTemplateWrappingPdfTemplate;
import io.github.easy4j.pdf.utils.ConfigUtils;

/**
 * Freemarker 模板引擎适配器：渲染模板为 HTML 后输出 PDF。
 */
public class FreemarkerPdfTemplate extends AbstractStringTemplateWrappingPdfTemplate {

    protected final Logger LOG = LoggerFactory.getLogger(FreemarkerPdfTemplate.class);
    protected Configuration engine;
    protected Properties freemarkerSettings;
    protected Map<String, Object> freemarkerVariables;
    protected String defaultEncoding;
    protected final List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();
    protected List<TemplateLoader> preTemplateLoaders;
    protected List<TemplateLoader> postTemplateLoaders;
    protected TemplateModel templateModel;

    public FreemarkerPdfTemplate() {
    }

    @Override
    protected String render(String template, Map<String, Object> variables) throws Exception {
        variables.put("String", this.templateModel);
        StringWriter output = new StringWriter();
        getEngine().getTemplate(template).process(variables, output);
        return output.toString();
    }

    public Configuration getEngine() throws IOException, TemplateException {
        return engine == null ? getInternalEngine() : engine;
    }

    public void setEngine(Configuration engine) {
        this.engine = engine;
    }

    protected Configuration getInternalEngine() throws IOException, TemplateException {
        try {
            BeansWrapper beansWrapper = new BeansWrapper(Configuration.VERSION_2_3_23);
            this.templateModel = beansWrapper.getStaticModels().get(String.class.getName());
        } catch (TemplateModelException e) {
            throw new IOException(e.getMessage(), e.getCause());
        }
        Configuration config = new Configuration(Configuration.VERSION_2_3_23);
        Properties props = ConfigUtils.filterWithPrefix("docx4j.freemarker.", "docx4j.freemarker.", Docx4jProperties.getProperties(), false);
        if (!props.isEmpty()) {
            config.setSettings(props);
        }
        if (this.freemarkerVariables != null && !this.freemarkerVariables.isEmpty()) {
            config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
        }
        if (this.defaultEncoding != null) {
            config.setDefaultEncoding(this.defaultEncoding);
        }
        List<TemplateLoader> loaders = new LinkedList<TemplateLoader>(this.templateLoaders);
        if (this.preTemplateLoaders != null) {
            loaders.addAll(this.preTemplateLoaders);
        }
        postProcessTemplateLoaders(loaders);
        if (this.postTemplateLoaders != null) {
            loaders.addAll(this.postTemplateLoaders);
        }
        TemplateLoader loader = getAggregateTemplateLoader(loaders);
        if (loader != null) {
            config.setTemplateLoader(loader);
        }
        config.setSharedVariable("fmXmlEscape", new XmlEscape());
        config.setSharedVariable("fmHtmlEscape", new HtmlEscape());
        this.setEngine(config);
        return config;
    }

    protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
        int loaderCount = templateLoaders.size();
        switch (loaderCount) {
            case 0:
                LOG.info("No FreeMarker TemplateLoaders specified");
                return null;
            case 1:
                return templateLoaders.get(0);
            default:
                return new MultiTemplateLoader(templateLoaders.toArray(new TemplateLoader[loaderCount]));
        }
    }

    public void setFreemarkerSettings(Properties settings) {
        this.freemarkerSettings = settings;
    }

    public void setFreemarkerVariables(Map<String, Object> variables) {
        this.freemarkerVariables = variables;
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders) {
        this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
    }

    public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders) {
        this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
    }

    protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
        templateLoaders.add(new ClassTemplateLoader(FreemarkerPdfTemplate.class, ""));
        LOG.info("ClassTemplateLoader for FreemarkerPdfTemplate added to FreeMarker configuration");
    }
}
