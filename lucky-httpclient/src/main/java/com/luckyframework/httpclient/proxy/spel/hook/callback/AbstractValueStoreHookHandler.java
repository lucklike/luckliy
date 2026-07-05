package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.spel.RootVarCtrlMap;
import com.luckyframework.httpclient.proxy.spel.SpELVariate;
import com.luckyframework.httpclient.proxy.spel.VarCtrlMap;
import com.luckyframework.httpclient.proxy.spel.VarType;
import com.luckyframework.httpclient.proxy.spel.hook.HookContext;
import com.luckyframework.httpclient.proxy.spel.hook.HookHandler;
import com.luckyframework.httpclient.proxy.spel.hook.NamespaceWrap;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 提供值存储功能的基本抽象Hook处理器
 */
public abstract class AbstractValueStoreHookHandler implements HookHandler {


    @Override
    public void handle(HookContext context, NamespaceWrap namespaceWrap) {

        // 注解转换
        ValueStore vsAnn = context.toAnnotation(ValueStore.class);

        // 解析Hook并返回执行结果
        Object hookResult = useHookReturnResult(context, namespaceWrap);

        // 根据配置来决定是否需要存储函数的返回结果
        if (vsAnn.storeOrNot() && hookResult != null) {
            addVariable(context, namespaceWrap, vsAnn, hookResult);
        }
    }

    /**
     * 运行Hook并返回执行结果
     *
     * @param context       Hook上下文
     * @param namespaceWrap 命名空间包装类
     * @return Hook的执行结果
     */
    protected abstract Object useHookReturnResult(HookContext context, NamespaceWrap namespaceWrap);

    /**
     * 获取默认的存储名称
     *
     * @param namespaceWrap 命名空间包装类
     * @return 默认的存储名称
     */
    protected abstract String geDefStoreName(NamespaceWrap namespaceWrap);

    /**
     * 获取存储描述，该描述用于出现异常时给出提示信息
     *
     * @param namespaceWrap 命名空间包装类
     * @return 存储描述
     */
    protected abstract String getStoreDesc(NamespaceWrap namespaceWrap);

    /**
     * 添加变量到上下文中
     *
     * @param context       上下文
     * @param namespaceWrap 命名空间包装对象
     * @param vsAnn         ValueStore注解实例
     * @param hookResult    Hook执行结果
     */
    private void addVariable(HookContext context, NamespaceWrap namespaceWrap, ValueStore vsAnn, Object hookResult) {

        // 获取变量相关参数
        String namespace = namespaceWrap.getNamespace();
        String varName = getVarName(vsAnn.name(), geDefStoreName(namespaceWrap));
        String storeDesc = getStoreDesc(namespaceWrap);

        //获取 Hook 运行后需要保存的变量 Map
        Map<String, Object> varMap = getVarMap(context, vsAnn, varName, hookResult, storeDesc);

        // 将变量填满到 SpEL 上下文中
        if (vsAnn.type() == VarType.ROOT) {
            addRootVariables(context, namespace, varMap);
        } else {
            addVariables(context, namespace, varMap);
        }
    }

    /**
     * 添加 Root 变量
     *
     * @param context   上下文
     * @param namespace 命名空间
     * @param varMap    变量Map
     */
    @SuppressWarnings("unchecked")
    private void addRootVariables(HookContext context, String namespace, Map<String, Object> varMap) {
        // 获取 SpEL 上下文变量
        SpELVariate contextVar = context.getContextVar();

        // 存在命名空间的情况
        if (StringUtils.hasText(namespace)) {
            // 尝试获取命名空间变量
            RootVarCtrlMap rootVar = contextVar.getRoot();
            Object namespaceVar = rootVar.get(namespace);

            // 命名空间变量存在
            if (namespaceVar instanceof Map) {
                ((Map<String, Object>) namespaceVar).putAll(varMap);
            } else {
                contextVar.addRootVariable(namespace, varMap);
            }
        } else {
            contextVar.addRootVariables(varMap);
        }
    }

    /**
     * 添加普通变量
     *
     * @param context   上下文
     * @param namespace 命名空间
     * @param varMap    变量Map
     */
    @SuppressWarnings("unchecked")
    private void addVariables(HookContext context, String namespace, Map<String, Object> varMap) {
        // 获取 SpEL 上下文变量
        SpELVariate contextVar = context.getContextVar();

        // 存在命名空间的情况
        if (StringUtils.hasText(namespace)) {
            // 尝试获取命名空间变量
            VarCtrlMap var = contextVar.getVar();
            Object namespaceVar = var.get(namespace);

            // 命名空间变量存在
            if (namespaceVar instanceof Map) {
                ((Map<String, Object>) namespaceVar).putAll(varMap);
            } else {
                contextVar.addVariable(namespace, varMap);
            }
        } else {
            contextVar.addVariables(varMap);
        }
    }

    /**
     * 获取变量名
     *
     * @param configName 配置的变量名
     * @param defName    默认名称
     * @return 用于存储当前回调方法运行结果的变量名
     */
    private String getVarName(String configName, String defName) {
        return StringUtils.hasText(configName) ? configName : defName;
    }


    /**
     * 根据配置获取变量Map
     *
     * @param context   上下文对象
     * @param vsAnn     ValueStore注解实例
     * @param name      存储结果的变量名
     * @param value     回调方法运行结果
     * @param storeDesc 解析异常时的报错信息
     * @return 变量Map
     */
    private Map<String, Object> getVarMap(HookContext context, ValueStore vsAnn, String name, Object value, String storeDesc) {
        name = getVarName(vsAnn.name(), name);
        Map<String, Object> varMap;
        if (vsAnn.unfold()) {
            try {
                varMap = ConversionUtils.conversion(value, new SerializationTypeToken<Map<String, Object>>() {
                });
            } catch (Exception e) {
                throw new VarUnfoldException(e, "An exception occurs when expanding the Hook property: '{}'", storeDesc);
            }
        } else {
            varMap = Collections.singletonMap(name, value);
        }
        if (vsAnn.literal()) {
            return varMap;
        }

        try {
            Map<String, Object> afterCalculationMap = new LinkedHashMap<>();
            varMap.forEach((k, v) -> {
                String varName = context.parseExpression(k);
                Object varValue = context.getParsedValue(v);
                afterCalculationMap.put(varName, varValue);
            });
            return afterCalculationMap;
        } catch (Exception e) {
            throw new HookResultParsedException(e, "Hook run result internal value parsing failed: '{}'", storeDesc);
        }
    }


}
