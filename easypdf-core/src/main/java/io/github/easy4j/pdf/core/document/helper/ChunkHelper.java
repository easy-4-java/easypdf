package io.github.easy4j.pdf.core.document.helper;

import com.itextpdf.text.Chunk;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
import io.github.easy4j.pdf.core.document.style.PDFStyleTransformer;
import com.jeefw.fastxml.jdom.xhtml.css.ElementStyleRender;

/**
 * @package io.github.easy4j.pdf.core.document.helper
 * @className: ChunkHelper
 * @description: TODO
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date : 2014-1-14
 * @time : 下午2:31:21
 */
public final class ChunkHelper {

	private static ChunkHelper instance = null;
	private ChunkHelper(){}
	
	public static ChunkHelper getInstance(){
		instance = instance==null?new ChunkHelper():instance;
		return instance;
	}
	
	public Chunk getChunk(ItextXMLElement element){
		if(element.isElement("br")){
			//换行
			return Chunk.NEWLINE;
  		}else if(element.isElement("a")){
  			//带连接的文字
  			Chunk chunk = ChunkHelper.getInstance().getChunk(element);
  			chunk.setAnchor(element.attr("href"));
			return chunk;
  		}else if(element.isElement("i")){
  			//空白行
  			return Chunk.SPACETABBING;
  		}else if(element.isElement("span")){
  			//空白行
  			return Chunk.SPACETABBING;
  		}else if(element.isElement("font")){
  			Chunk chunk = new Chunk(element.text(), element.getFont()); 
  			//chunk.setBackground(color)
  			
  			/*chunk.setBackground(color)
  			
  			chunk.setHorizontalScaling(scale)*/
  			ElementStyleRender.getInstance(PDFStyleTransformer.getInstance()).render(chunk, element);
  			return chunk;
  		}
		return null;
	}
	
	public Chunk getUnderLineChunk(ItextXMLElement element){
		return Chunk.SPACETABBING;
	}
	
	public Chunk getDeleteLineChunk(ItextXMLElement element){
		return Chunk.SPACETABBING;
	}
}
