package io.github.easy4j.pdf.core.context;


import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jeefw.fastkit.configuration.config.Config;

/**
 * 
 * *******************************************************************
 * @className	： ItextContextInitListener
 * @description	： ServletContext监听器 ，用来监听ItextContext上下文初始化状态
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @date		： Mar 11, 2016 10:34:06 PM
 * @version 	V1.0 
 * *******************************************************************
 */
public class ItextContextInitListener implements ServletContextListener{
	public static final Log log = LogFactory.getLog(ItextContextInitListener.class);

	public void contextDestroyed(ServletContextEvent sce) {
		ItextContext.getInstance().destroy();
		log.info(" ItextContext has Destroyed ！");
	}

	public void contextInitialized(final ServletContextEvent sce) {
		log.info(" ItextContext initialize starting ...... ");
		try {
			ItextContext.getInstance().initialize(new Config() {
				
				public String getInitParameter(String name) {
					return sce.getServletContext().getInitParameter(name);
				}

				public ServletContext getServletContext() {
					return sce.getServletContext();
				}
				
			});
		} catch (ServletException e) {
			log.error(" ItextContext initialized ！ ",e);
		}
		log.info(" ItextContext initialize success ！ ");
	}

	
}
