package io.github.easy4j.pdf.core.context;

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
import io.github.easy4j.pdf.core.context.constants.ConfigConstants;
import io.github.easy4j.pdf.core.document.elements.ItextXMLElement;
/**
 * 
 * @description:Logx 上下文 
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date 2012-4-23
 */
public class ItextContext extends AbstractContext {

	// 日志记录
	protected static Logger LOG = LoggerFactory.getLogger(ItextContext.class);
	// 上下文本地线程
	private static final ThreadLocal<ItextContext> localContext = new ThreadLocal<ItextContext>();
	// 初始化配置文件
	private static Properties defaults = ConfigUtils.getProperties(ItextContext.class,"default-config.properties");
	// 对象方法与css映射关系
	private static Properties method_mappings = ConfigUtils.getProperties(ItextContext.class,"method-mapping.properties");
	// 对象属性与css映射关系
	private static Properties property_mappings = ConfigUtils.getProperties(ItextContext.class,"property-mapping.properties");
	// 初始化配置文件
	private Properties configs = null;

	private String[] keys = { ConfigConstants.KEY_CONFIG_FILE_PATH,
			ConfigConstants.KEY_CONFIG_XML_PATH,
			ConfigConstants.KEY_CONFIG_ENCODING,
			ConfigConstants.KEY_FONTS_DIR,
			ConfigConstants.KEY_STORE_ENABLE,
			ConfigConstants.KEY_STORE_TMPDIR,
			ConfigConstants.KEY_STORE_PREFIX, 
			ConfigConstants.KEY_STORE_SUFFIX
	};
	
	//初始化配置文件
	private static Map<String,ItextXMLElement> documents = new HashMap<String, ItextXMLElement>();
	private static List<String> links = new ArrayList<String>();
	private static Map<String,String> styles = new HashMap<String, String>();
	
	// 无参构造
	private ItextContext() {
		this.setConfigs(defaults);
		localContext.set(this);
	}

	private ItextContext(Properties configs) {
		this.setConfigs(configs);
		localContext.set(this);
	}

	private ItextContext(Map<String, String> parameters) {
		this.setParameters(parameters);
		localContext.set(this);
	}

	public static ItextContext getInstance() {
		return getInstance(defaults);
	}

	public static ItextContext getInstance(Properties configs) {
		ItextContext ctx = (ItextContext) localContext.get();
		if (ctx == null) {
			ctx = new ItextContext(configs);
		} else {
			ctx.setConfigs(configs);
		}
		return ctx;
	}

	public static ItextContext getInstance(Map<String, String> multipartParameters) {
		ItextContext ctx = (ItextContext) localContext.get();
		if (ctx == null) {
			ctx = new ItextContext(multipartParameters);
		} else {
			ctx.setParameters(multipartParameters);
		}
		return ctx;
	}
	
	//------抽象method实现--------------------------------------------------------------------------------------------
	
	@Override
	public void initialize(Config config) throws ServletException {
		LOG.info("Init JReportContext ...");
		setServletContext(config.getServletContext());
		configs = new Properties(defaults);
    	for (int i = 0; i < keys.length; i++) {
        	configs.setProperty(keys[i],safeGet(config.getInitParameter(keys[i]), defaults.getProperty(keys[i])));
        	System.setProperty(keys[i],configs.getProperty(keys[i]));
        	LOG.info("config:["+keys[i]+"]:"+configs.getProperty(keys[i]));
		}
    	LOG.info("read Property from config file ："+this.getConfigPath());
		Properties configs1 = ConfigUtils.getProperties(this.getClass(),this.getConfigPath());
		Iterator<Entry<Object,Object>> ite = configs1.entrySet().iterator();
		while(ite.hasNext()){
			Entry<Object,Object> entry = ite.next();
			String key = String.valueOf(entry.getKey());
			String value = String.valueOf(entry.getValue());
			configs.setProperty(key,safeGet(value,defaults.getProperty(key)));
			LOG.info("config:["+key+"]:"+value);
			System.setProperty(key,value);
		}
		LOG.info("resolver xml document ："+this.getConfigXmlPath());
		/*try {
			ItextXMLResolver.getInstance().resolver(getRealPath(this.getConfigXmlPath()));
		} catch (JDOMException e) {
			
		} catch (IOException e) {
			
		}*/
		//注册自定义字体
		FontFactory.registerDirectory(getRealPath(getFontDir()), true);
	}
	
	@Override
	public void destroy() {
		defaults.clear();
		defaults = null;
		configs.clear();
		configs = null;
		localContext.remove();
		System.gc();
	}
	
	@Override
	public void setParameters(Map<String,String> params){
		if(null==params){
			params = new HashMap<String, String>();  
		}
		for (int i = 0; i < keys.length; i++) {
			configs.setProperty(keys[i], safeGet(params.get(keys[i]),defaults.getProperty(keys[i])));
		}	
	}
	
	@Override
	public void setConfigs(Properties config) {
		for (int i = 0; i < keys.length; i++) {
			configs.setProperty(keys[i],safeGet(config.getProperty(keys[i]),defaults.getProperty(keys[i])));
		}
	}
	
	@Override
	public ItextContext setRequest(HttpServletRequest request, HttpServletResponse response) {
		setRequest(request);
		setResponse(response);
		setSession(request.getSession());
		return this;
	}

	// ----------------预置的参数取值method--------------------------------------------------------
	
	@Override
	public String getConfigProperty(String key) {
		return this.configs.getProperty(key,defaults.getProperty(key));
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String,String> params = new HashMap<String, String>();  
		for (int i = 0; i < keys.length; i++) {
			params.put(keys[i],this.configs.getProperty(keys[i],defaults.getProperty(keys[i])));
		}				
		return params;
	}
	
	
	// ---------初始化---------------------------------------

	/**
	 * IMexportContext 初始化文件路径 . 默认 ： imexport-config.xml
	 */
	@Override
	public String getConfigPath() {
		return getConfigProperty(ConfigConstants.KEY_CONFIG_FILE_PATH);
	}

	@Override
	public String getConfigXmlPath() {
		return getConfigProperty(ConfigConstants.KEY_CONFIG_XML_PATH);
	}
	
	/**
	 * 编码格式 ，默认： UTF-8
	 */
	@Override
	public String getEncoding() {
		return getConfigProperty(ConfigConstants.KEY_CONFIG_ENCODING);
	}

	public String getFontDir() {
		return getConfigProperty(ConfigConstants.KEY_FONTS_DIR);
	}
	
	public boolean getStoreEnable() {
		return BooleanUtils.parse(getConfigProperty(ConfigConstants.KEY_STORE_ENABLE));
	}
	
	public String getStoreDir() {
		return getConfigProperty(ConfigConstants.KEY_STORE_TMPDIR);
	}
	
	public String getStorePrefix() {
		return getConfigProperty(ConfigConstants.KEY_STORE_PREFIX);
	}
	
	public String getStoreSuffix() {
		String suffix = getConfigProperty(ConfigConstants.KEY_STORE_SUFFIX);
		if("Date".equalsIgnoreCase(suffix)){
			return format.format(new Date());
		}else{//UUID
			return UUID.randomUUID().toString();
		}
	}
	
	// ----------------预置的参数取值method--------------------------------------------------------
	
	public static String getRealPath(String path){
		//返回最终路径
		return getInstance().getAbsolutePath(path);
	}
	

	public static Map<String, ItextXMLElement> getElements() {
		return documents;
	}

	public static ItextXMLElement getElement(String documentID) {
		ItextXMLElement element = ItextContext.documents.get(documentID);
		if(element==null){
			/*try {
				String xmlPath = ItextContext.getInstance().getConfigXmlPath();
				element = ItextXMLResolver.getInstance().resolver(getRealPath(xmlPath), documentID);
			} catch (IOException e) {
				
			}*/
		}
		return element;
	}

	public static void addDocument(String name,ItextXMLElement document) {
		ItextContext.documents.put(name, document);
	}
	
	public static List<String> getLinks() {
		return links;
	}

	public static void addLink(String link) {
		ItextContext.links.add(link);
	}

	public static Map<String, String> getStyles() {
		return styles;
	}
	
	public static String getStyle(String key) {
		return styles.get(key);
	}

	public static void addStyle(String name,String style) {
		ItextContext.styles.put(name, style);
	}
	
	public String getMethodMapping(String css){
		return method_mappings.getProperty(css.toLowerCase());
	}
	
	public String getPropertyMapping(String css){
		return property_mappings.getProperty(css.toLowerCase());
	}
	
}