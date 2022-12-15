package com.luckyframework.junit.core;

import org.junit.jupiter.api.extension.*;

/**
 * @author fk7075
 * @version 1.0
 * @date 2021/9/18 2:58 下午
 */
public abstract class LuckyExtensionAdapter implements BeforeAllCallback, AfterAllCallback,
        TestInstancePostProcessor, BeforeEachCallback
        , AfterEachCallback, BeforeTestExecutionCallback,
        AfterTestExecutionCallback, ParameterResolver,TestInstanceFactory{

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return null;
    }

    @Override
    public Object createTestInstance(TestInstanceFactoryContext testInstanceFactoryContext, ExtensionContext extensionContext) throws TestInstantiationException {
        return null;
    }

    @Override
    public void postProcessTestInstance(Object o, ExtensionContext extensionContext) throws Exception {

    }
}
