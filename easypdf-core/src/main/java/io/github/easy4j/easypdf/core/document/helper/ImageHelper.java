package io.github.easy4j.easypdf.core.document.helper;

import java.net.URL;

import com.itextpdf.text.Image;
import com.jeefw.fastkit.beanutils.JavaBeanUtils;
import com.jeefw.fastkit.lang3.BlankUtils;
import io.github.easy4j.easypdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.easypdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

 /**
  * 
  * @package io.github.easy4j.easypdf.core.document.helper
  * @className: ImageHelper
  * @description: TODO
  * @author : <a href="mailto:hnxyhcwdl1003@163.com">wandalong</a>
  * @date : 2014-1-21
  * @time : 下午2:17:33
  */
public final class ImageHelper {
	
	private static ImageHelper instance = null;
	private ImageHelper() {}

	public static ImageHelper getInstance(){
		instance = instance==null?new ImageHelper():instance;
		return instance;
	}
	
	public <T> Image getImage(ItextXMLElement element,T data) throws Exception{
		if(element.isElement("img")){
			String srcAttr = element.attr("src");
			String imageAttr = element.attr("image");
			String byteAttr = element.attr("bytes");
			Image image = null;
			if(!BlankUtils.isBlank(srcAttr)){
				URL url = new URL(srcAttr);
				image = Image.getInstance(url);
			}else if(!BlankUtils.isBlank(imageAttr)){
				Image tmp = (Image)JavaBeanUtils.getProperty(data, imageAttr);
				image = Image.getInstance(tmp);
			}else if(!BlankUtils.isBlank(byteAttr)){
				byte[] bytes = (byte[])JavaBeanUtils.getProperty(data, byteAttr);
				image = Image.getInstance(bytes);
			}
			if(image != null){
				ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(image, element);
			}
			return image;
		}
		return null;
	}
	
	
}



