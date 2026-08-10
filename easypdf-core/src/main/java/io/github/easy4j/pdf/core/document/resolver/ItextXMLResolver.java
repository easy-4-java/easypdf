package io.github.easy4j.pdf.core.document.resolver;

import java.io.IOException;
import java.util.Map;

import org.jdom2.JDOMException;

import io.github.easy4j.pdf.core.context.ItextContext;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import com.jeefw.fastxml.jdom.XMLElement;
import com.jeefw.fastxml.jdom.xhtml.sax.XMLElementResolver;
/**
 * 
 * @className: XMLResolver
 * @description: XML配置文件解析器
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date : 上午9:39:10 2013-8-14
 * @modify by:
 * @modify date :
 * @modify description :
 */
public class ItextXMLResolver {

	private static ItextXMLResolver instance = null;
	private ItextXMLResolver(){}
	
	public static ItextXMLResolver getInstance(){
		instance = instance==null?new ItextXMLResolver():instance;
		return instance;
	}
	
	/**
 * Implementation of itext x m l resolver functionality.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
	public Map<String, ItextXMLElement> resolver(String path)throws JDOMException, IOException {
		// 解析XML
		Map<String, XMLElement> elements = XMLElementResolver.getInstance().resolver(path);
		for (String key : elements.keySet()) {
			ItextXMLElement element = (ItextXMLElement)elements.get(key);
			ItextContext.addDocument(key,element);
		}
		return ItextContext.getElements();
	}
	
	
	public ItextXMLElement resolver(String path,String name) throws JDOMException, IOException  {
		// 解析XML
		XMLElement element = XMLElementResolver.getInstance().resolver(path, name);
		ItextContext.addDocument(name, (ItextXMLElement)element);
		return ItextContext.getElement(name);
	}
	
}
