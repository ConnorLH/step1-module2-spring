package cn.connor.listener;

import cn.connor.factory.AnnotationBeanFactory;
import cn.connor.utils.BeanFactoryContextUtils;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * IOC容器启动入口
 * @company: yzw
 * @author: connor.h.liu
 * @version: V1.0
 * date: 2020-08-01 18:15
 */
public class ContextLoaderListener implements ServletContextListener {

    private AnnotationBeanFactory beanFactory;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        beanFactory = new AnnotationBeanFactory();
        BeanFactoryContextUtils.annotationBeanFactory = beanFactory;
        ServletContext servletContext = servletContextEvent.getServletContext();
        String componentScanBasePackage = servletContext.getInitParameter("componentScanBasePackage");
        beanFactory.startIoc(componentScanBasePackage);
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        beanFactory.destroy();
    }
}
