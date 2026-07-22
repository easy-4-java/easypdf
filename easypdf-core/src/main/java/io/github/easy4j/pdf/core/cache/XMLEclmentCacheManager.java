package io.github.easy4j.pdf.core.cache;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;

/**
 * 
 *@类名称	: POICellStyleCacheManager.java
 *@类描述	：单元格样式缓存管理
 *@创建人	：wandalong
 *@创建时间	：Mar 26, 2016 11:24:59 PM
 *@修改人	：
 *@修改时间	：
 *@版本号	:v1.0
 */
public class XMLEclmentCacheManager {
	
	protected static Logger LOG = LoggerFactory.getLogger(XMLEclmentCacheManager.class);
	protected static volatile XMLEclmentCacheManager singleton;
 	protected static ConcurrentMap<String,ItextXMLElement> COMPLIED_XML_ELEMENT = new ConcurrentHashMap<String,ItextXMLElement>();
	
	public static XMLEclmentCacheManager getInstance() {
		if (singleton == null) {
			synchronized (XMLEclmentCacheManager.class) {
				if (singleton == null) {
					singleton = new XMLEclmentCacheManager();
				}
			}
		}
		return singleton;
	}
	
	private XMLEclmentCacheManager(){
		
	}
	
	public ItextXMLElement getXMLElement(String xmlkey){
		if(xmlkey != null){
			/*ItextXMLElement ret = COMPLIED_XML_ELEMENT.get(xmlkey);
			if (ret != null) {
				return ret;
			}
			ret = new HashMap<POICellStyleKey, CellStyle>();
			ItextXMLElement existing = COMPLIED_XML_ELEMENT.putIfAbsent(xmlkey, ret);
 			if (existing != null) {
 				ret = existing;
 			}
			return ret;*/
		}
		return null;
	}
	 
	
	public void destroy(String xmlkey) {
		if(xmlkey != null){
			//清除缓存
			COMPLIED_XML_ELEMENT.remove(xmlkey);
		}
	}
	
}

