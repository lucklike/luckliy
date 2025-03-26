package com.luckyframework.httpclient.generalapi.openai;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.FunExecutor;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tool注册器，用于搜集和管理所有的Tool信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/25 14:50
 */
public class ToolRegister {

    /**
     * 所有的函数名-函数描述信息的映射关系
     */
    private final Map<String, ConfigurationMap> functionMap = new LinkedHashMap<>();

    /**
     * 所有函数名-函数执行器的映射关系
     */
    private final Map<String, FunExecutor> funExecutorMap = new LinkedHashMap<>();


    /**
     * 获取所有函数名与函数描述信息的映射
     *
     * @return 所有函数名与函数描述信息的映射
     */
    public Map<String, ConfigurationMap> getFunctionMap() {
        return this.functionMap;
    }

    /**
     * 获取所有函数描述信息
     *
     * @return 所有函数描述信息
     */
    public Collection<ConfigurationMap> getFunctions() {
        return this.functionMap.values();
    }

    /**
     * 获取所有的函数名
     *
     * @return 所有的函数名
     */
    public Set<String> getFunctionNames() {
        return this.functionMap.keySet();
    }

    /**
     * 根据函数名来获取函数描述信息
     *
     * @param functionName 函数名
     * @return 函数描述信息
     */
    public ConfigurationMap getFunction(String functionName) {
        return this.functionMap.get(functionName);
    }

    /**
     * 添加函数对象中所有的工具方法
     *
     * @param functionObject 函数所在对象
     */
    public void addFunctionObject(@NonNull Object functionObject) {
        Assert.notNull(functionObject, "functionObject must not be null");
        Method[] allMethod = ClassUtils.getAllMethod(functionObject.getClass());
        for (Method method : allMethod) {
            Tool toolAnn = AnnotationUtils.findMergedAnnotation(method, Tool.class);
            if (toolAnn == null) {
                continue;
            }

            String functionName = StringUtils.hasText(toolAnn.name()) ? toolAnn.name() : method.getName();
            String functionType = toolAnn.type();
            String functionDesc = toolAnn.desc();

            ConfigurationMap functionConfig = createFunctionConfig(functionName, functionType, functionDesc);
        }
    }


    /**
     * 创建一个函数配置
     *
     * <pre>
     *     输入：
     *          funcName=send_email
     *          funcType=function
     *          funcDesc=发送一条电子邮件
     *     输出：
     *          {
     *              "type": "function",
     *              "function": {
     *                  "name": "send_email",
     *                  "description": "发送一条电子邮件"
     *              }
     *          }
     * </pre>
     *
     * @param funcName 函数名
     * @param funcType 函数类型
     * @param funcDesc 函数描述
     * @return 函数配置
     */
    private ConfigurationMap createFunctionConfig(@NonNull String funcName, @NonNull String funcType, @NonNull String funcDesc) {

        Assert.hasText(funcName, "funcName must not be empty");
        Assert.hasText(funcDesc, "funcType must not be empty");
        Assert.hasText(funcDesc, "funcDesc must not be empty");


        ConfigurationMap function = new ConfigurationMap();
        function.addProperty("type", funcType);
        function.addProperty("function.name", funcName);
        function.addProperty("function.description", funcDesc);
        return function;
    }

    /**
     * 创建一个简单参数配置
     *
     *
     * <pre>
     *     输入：
     *         paramName=sex
     *         paramType=string
     *         paramDesc=性别，例如：男
     *         enumerate=[男， 女]
     *     输出：
     *         {
     *             "sex" : {
     *                 "type": "string",
     *                 "enum": ["男", "女"],
     *                 "description": "性别，例如：男"
     *             }
     *         }
     *
     * </pre>
     *
     * @param paramName 参数名
     * @param paramType 参数类型【string、integer、number、boolean】
     * @param paramDesc 参数描述
     * @param enumerate 参数枚举值
     * @return 简单参数配置
     */
    private ConfigurationMap createSimpleParam(@NonNull String paramName, @NonNull String paramType, @Nullable String paramDesc, @Nullable String[] enumerate) {

        Assert.notNull(paramName, "paramName must not be null");
        Assert.notNull(paramType, "paramType must not be null");

        ConfigurationMap param = new ConfigurationMap();
        param.addProperty(String.format("%s.type", paramName), paramType);

        if (StringUtils.hasText(paramDesc)) {
            param.addProperty(String.format("%s.description", paramName), paramDesc);
        }

        if (ContainerUtils.isNotEmptyArray(enumerate)) {
            param.addProperty(String.format("%s.enum", paramName), enumerate);
        }
        return param;
    }

    /**
     * 创建一个对象参数的基本结构
     *
     * <pre>
     *     输入：
     *          objParamName=user
     *     输出：
     *          {
     *              "user": {
     *                  "type": "object",
     *                  "properties": {
     *
     *                  }
     *              }
     *          }
     * </pre>
     *
     * @param objParamName 参数名
     * @return 对象参数的基本结构
     */
    private ConfigurationMap createObjectParamStructure(@NonNull String objParamName) {
        Assert.notNull(objParamName, "objParamName must not be null");

        ConfigurationMap param = new ConfigurationMap();
        param.addProperty(String.format("%s.type", objParamName), "object");
        param.addProperty(String.format("%s.properties", objParamName), new ConfigurationMap());
        return param;
    }

    private ConfigurationMap createArrayParamStructure(@NonNull String arrayParamName) {
        Assert.notNull(arrayParamName, "arrayParamName must not be null");

        ConfigurationMap param = new ConfigurationMap();
        param.addProperty(String.format("%s.type", arrayParamName), "array");

        return param;
    }

}
