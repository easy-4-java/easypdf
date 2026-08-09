package io.github.easy4j.pdf.core.document.render;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

/**
 * Implementation of document render functionality.
 *
 * [@Loong Wan](https://github.com/loong10k)
 */
public abstract class DocumentRender {

	
	public abstract <T> ByteArrayInputStream render(String documentID,List<T> datas) throws Exception;
	
	public abstract <T> ByteArrayInputStream render(String documentID,Map<String,String> attrs,List<T> datas) throws Exception;
	
	public abstract <T> ByteArrayInputStream render(String documentID,T data) throws Exception;
	
	public abstract <T> ByteArrayInputStream render(String documentID,Map<String,String> attrs,T data) throws Exception;
}



