package cn.connor.factory;

import cn.connor.annotation.Autowired;
import cn.connor.annotation.Repository;
import cn.connor.annotation.Service;
import cn.connor.annotation.Transactional;
import com.alibaba.druid.util.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基于注解的BeanFactory
 * @company: yzw
 * @author: connor.h.liu
 * @version: V1.0
 * date: 2020-08-01 18:21
 */
@SuppressWarnings("ReflectionForUnavailableAnnotation")
public class AnnotationBeanFactory {

    private Reflections reflections;

    private static Map<String, Object> map = new HashMap<>();

    public void startIoc(String componentScanBasePackage) {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());

        reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(componentScanBasePackage))));
        Set<Class<?>> allClasses = reflections.getSubTypesOf(Object.class);
        for (Class<?> customClass : allClasses) {
            if (customClass.isAnnotationPresent(Service.class) || customClass.isAnnotationPresent(Repository.class)) {
                createBean(customClass);
            }
        }
    }

    public <T> T getBean(String id) {
        return (T) map.get(id);
    }

    private void dependencyInjection(Object bean) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                String fieldBeanId = field.getAnnotation(Autowired.class).value();
                if (StringUtils.isEmpty(fieldBeanId)) {
                    fieldBeanId = field.getType().getSimpleName();
                }
                if (!map.containsKey(fieldBeanId)) {
                    // 如果是接口需要查找实现类
                    Class<?> fieldClass = field.getType();
                    if (fieldClass.isInterface()) {
                        Set<Class<?>> subclass = reflections.getSubTypesOf((Class<Object>) fieldClass);
                        for (Class<?> aClass : subclass) {
                            if (aClass.isAnnotationPresent(Service.class) || aClass.isAnnotationPresent(Repository.class)) {
                                createBean(aClass);
                                break;
                            }
                        }
                    } else {
                        createBean(fieldClass);
                    }
                }
                Object fieldObject = map.get(fieldBeanId);
                try {
                    field.setAccessible(true);
                    field.set(bean, fieldObject);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void destroy() {
        map.clear();
    }

    private void createBean(Class<?> beanClass) {
        Object o;
        String id = null;
        if (beanClass.isAnnotationPresent(Service.class)) {
            id = beanClass.getAnnotation(Service.class).value();
        } else if (beanClass.isAnnotationPresent(Repository.class)) {
            id = beanClass.getAnnotation(Repository.class).value();
        }
        if (StringUtils.isEmpty(id)) {
            id = beanClass.getSimpleName();
        }
        if (map.containsKey(id)) {
            return;
        }
        try {
            // 是否需要事务控制
            Method[] methods = beanClass.getDeclaredMethods();
            boolean isTransactional = false;
            for (Method method : methods) {
                if (method.isAnnotationPresent(Transactional.class)) {
                    isTransactional = true;
                }
            }
            o = beanClass.newInstance();
            // 依赖注入
            dependencyInjection(o);
            // 省略启用声明式事务的开关控制。。。
            if (isTransactional) {
                o = createTransactionalProxyBean(o);
            }
            map.put(id, o);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Object createTransactionalProxyBean(Object originObject) {
        Class<?> beanClass = originObject.getClass();
        if (getBean("ProxyFactory") == null) {
            createBean(ProxyFactory.class);
        }
        ProxyFactory proxyFactory = getBean("ProxyFactory");
        if (beanClass.getInterfaces().length == 0) {
            return proxyFactory.getCglibProxy(originObject);
        } else {
            return proxyFactory.getJdkProxy(originObject);
        }
    }
}
