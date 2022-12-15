package com.luckyframework.junit.core;

import com.luckyframework.context.AbstractApplicationContext;
import com.luckyframework.context.AnnotationScannerApplicationContext;
import com.luckyframework.definition.BeanDefinitionBuilder;
import com.luckyframework.definition.GenericBeanDefinition;
import com.luckyframework.junit.annoations.TestConfiguration;
import com.luckyframework.scanner.ComponentAutoScanner;
import com.luckyframework.scanner.Scanner;
import com.luckyframework.scanner.ScannerUtils;
import org.apache.logging.log4j.ThreadContext;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.AnnotationMetadata;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LuckyJUnit4ClassRunner extends BlockJUnit4ClassRunner {

	private static final RuntimeMXBean mxb = ManagementFactory.getRuntimeMXBean();
	private final AbstractApplicationContext applicationContext;

	static {
		String pid = mxb.getName().split("@")[0];
		ThreadContext.put("pid",pid);
	}
	
	public LuckyJUnit4ClassRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
		TestConfiguration mergedAnnotation = AnnotatedElementUtils.findMergedAnnotation(testClass, TestConfiguration.class);
		if(mergedAnnotation == null){
			Scanner scanner = new ComponentAutoScanner(testClass);
			List<AnnotationMetadata> scannerElements = scanner.getScannerElements();
			scannerElements.add(AnnotationMetadata.introspect(testClass));
			applicationContext = new AnnotationScannerApplicationContext(scannerElements);
		}else{
			Class<?>[] rootClasses = mergedAnnotation.rootClasses();
			String[] basePackages = mergedAnnotation.basePackages();
			Set<String> basePackageSet = new HashSet<>();
			basePackageSet.addAll(Arrays.asList(basePackages));
			basePackageSet.addAll(Stream.of(rootClasses).map(this::getPackageName).collect(Collectors.toList()));
			Scanner scanner = new ComponentAutoScanner(basePackageSet.toArray(new String[0]));
			List<AnnotationMetadata> scannerElements = scanner.getScannerElements();
			scannerElements.add(AnnotationMetadata.introspect(testClass));
			applicationContext = new AnnotationScannerApplicationContext(scannerElements);
		}
		applicationContext.refresh();
	}

	@Override
	protected Object createTest() throws Exception {
		Object createTest = super.createTest();
		return applicationContext.getBean(createTest.getClass());
	}

	@Override
	protected void runChild(FrameworkMethod method, RunNotifier notifier) {
		super.runChild(method, notifier);
        try {
            applicationContext.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	private String getPackageName(Class<?> aClass){
		Package aPackage = aClass.getPackage();
		return aPackage==null?"":aPackage.getName();
	}
}
