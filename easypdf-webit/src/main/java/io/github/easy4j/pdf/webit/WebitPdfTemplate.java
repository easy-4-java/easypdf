/**
 * Copyright (c) 2018, vindell (https://github.com/vindell).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.easy4j.pdf.webit;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.docx4j.Docx4jProperties;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import webit.script.CFG;
import webit.script.Engine;
import io.github.easy4j.pdf.template.AbstractStringTemplateWrappingPdfTemplate;

/**
 * Implementation of wordprocessing m l webit template extending WordprocessingMLTemplate.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public class WebitPdfTemplate extends AbstractStringTemplateWrappingPdfTemplate {
	
	protected Engine engine;
	
	
	
	
	
	

	/**
	 * 使用Webit模板引擎渲染模板
	 * @param template ：模板内容
	 * @param variables ：变量
	 * @return {@link WordprocessingMLPackage} 对象
	 * @throws Exception ：异常对象
	 */
	public Engine getEngine() throws IOException {
		return engine == null ? getInternalEngine() : engine;
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	protected synchronized Engine getInternalEngine() throws IOException{
		
		Map<String, Object> ps = new HashMap<String, Object>();
		ps.put(CFG.APPEND_LOST_SUFFIX, Docx4jProperties.getProperty("docx4j.webit.engine.appendLostSuffix", false));
		ps.put(CFG.INIT_TEMPLATES, Docx4jProperties.getProperty("docx4j.webit.engine.initTemplates"));
		//ps.put(CFG.FILTER, input_charset);
		ps.put(CFG.LOADER, Docx4jProperties.getProperty("docx4j.webit.engine.resourceLoader","webit.script.loaders.impl.ClasspathLoader"));
        ps.put(CFG.LOADER_ENCODING, Docx4jProperties.getProperty("docx4j.webit.loader.encoding", Engine.UTF_8) );
        ps.put(CFG.LOADER_ROOT, Docx4jProperties.getProperty("docx4j.webit.loader.root") );
        ps.put(CFG.LOGGER, Docx4jProperties.getProperty("docx4j.webit.engine.logger", "webit.script.loggers.impl.NOPLogger"));
		ps.put(CFG.LOOSE_VAR, Docx4jProperties.getProperty("docx4j.webit.engine.looseVar", false));
		ps.put(CFG.OUT_ENCODING, Docx4jProperties.getProperty("docx4j.webit.engine.encoding", Engine.UTF_8));
		//ps.put(CFG.RESOLVERS, input_charset);
		//ps.put(CFG.SECURITY_LIST, Docx4jProperties.getProperty("docx4j.webit.nativeSecurity.list"));
        //ps.put(CFG.SERVLET_CONTEXT, output_charset );
        ps.put(CFG.SHARE_ROOT, Docx4jProperties.getProperty("docx4j.webit.engine.shareRootData", true));
        ps.put(CFG.SUFFIX, Docx4jProperties.getProperty("docx4j.webit.engine.suffix", ".wit"));
        ps.put(CFG.TEXT_FACTORY, Docx4jProperties.getProperty("docx4j.webit.engine.textStatementFactory", CFG.SIMPLE_TEXT_FACTORY));
        ps.put(CFG.TRIM_CODE_LINE, Docx4jProperties.getProperty("docx4j.webit.engine.trimCodeBlockBlankLine",true));
        ps.put(CFG.VARS, Docx4jProperties.getProperty("docx4j.webit.engine.vars"));
        
        Engine engine = Engine.create("", ps);
        // 设置模板引擎，减少重复初始化消耗
        this.setEngine(engine);
        return engine;
	}

	@Override
	protected String render(String template, Map<String, Object> variables) throws Exception {

		//创建模板输出内容接收对象
		StringWriter output = new StringWriter();
		//使用Webit模板引擎渲染模板
		getEngine().getTemplate(template).merge(variables, output);
		//获取模板渲染后的结果
		String html = output.toString();
		return html;
	}

}
