package io.github.easy4j.pdf.core.cache;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;

/**
 * Implementation of x m l eclment cache manager functionality.
 *
 * [@Loong Wan](https://github.com/loong10k)
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

