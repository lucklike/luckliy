package com.luckyframework.junit.core;

import org.junit.runners.model.InitializationError;

public class LuckyRunner extends LuckyJUnit4ClassRunner {

	public LuckyRunner(Class<?> testClass) throws InitializationError {
		super(testClass);
	}

}
