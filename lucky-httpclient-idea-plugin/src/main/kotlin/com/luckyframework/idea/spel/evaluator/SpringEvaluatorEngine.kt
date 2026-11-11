package com.luckyframework.idea.spel.evaluator

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy

/**
 * 基于项目 classpath 中 `spring-expression` 的真实求值引擎（design D11）。
 *
 * 全部通过反射调用，插件不依赖 Spring 编译期 API：
 * - 解析器与上下文按需创建，类由 [loader] 提供；
 * - `TypeLocator` 与 `MethodResolver` 用动态代理换成白名单实现，
 *   被拒绝的访问抛出 [EvaluatorForbiddenException]；
 * - 反射边界异常由上层统一解包。
 */
class SpringEvaluatorEngine(private val loader: ClassLoader) : EvaluatorEngine {

    private val parserClass = loadClass("org.springframework.expression.spel.standard.SpelExpressionParser")
    private val expressionClass = loadClass("org.springframework.expression.Expression")
    private val contextClass =
        loadClass("org.springframework.expression.spel.support.StandardEvaluationContext")
    private val evaluationContextClass = loadClass("org.springframework.expression.EvaluationContext")
    private val typeLocatorClass = loadClass("org.springframework.expression.TypeLocator")
    private val methodResolverClass = loadClass("org.springframework.expression.MethodResolver")
    private val methodExecutorClass = loadClass("org.springframework.expression.MethodExecutor")
    private val typedValueClass = loadClass("org.springframework.expression.TypedValue")

    private val parseExpression = parserClass.getMethod("parseExpression", String::class.java)
    private val expressionGetValue = expressionClass.getMethod("getValue", evaluationContextClass)
    private val contextSetVariable = contextClass.getMethod("setVariable", String::class.java, Any::class.java)
    private val contextSetTypeLocator = contextClass.getMethod("setTypeLocator", typeLocatorClass)
    private val contextSetMethodResolvers =
        contextClass.getMethod("setMethodResolvers", java.util.List::class.java)

    override fun evaluate(expression: String, variables: Map<String, Any?>): Any? {
        val parser = parserClass.getConstructor().newInstance()
        val parsed = parseExpression.invoke(parser, expression)
        val context = contextClass.getConstructor().newInstance()

        for ((name, value) in variables) {
            contextSetVariable.invoke(context, name, value)
        }
        contextSetTypeLocator.invoke(context, newTypeLocator())
        contextSetMethodResolvers.invoke(context, listOf(newMethodResolver()))

        return expressionGetValue.invoke(parsed, context)
    }

    /** `T(...)` 类型引用的白名单代理。 */
    private fun newTypeLocator(): Any = Proxy.newProxyInstance(
        loader,
        arrayOf(typeLocatorClass)
    ) { proxy, method, args ->
        when (method.name) {
            "findType" -> resolveType(args?.getOrNull(0) as? String)
            else -> proxyDefault(proxy, method, args)
        }
    }

    private fun resolveType(typeName: String?): Any? {
        if (typeName.isNullOrBlank()) {
            return null
        }
        if (!EvaluatorPolicy.isTypeAllowed(typeName)) {
            throw EvaluatorForbiddenException(EvaluatorPolicy.typeDenialMessage(typeName))
        }
        return try {
            Class.forName(typeName, false, loader)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("无法加载类型 $typeName")
        }
    }

    /** `MethodResolver` 白名单代理；具体方法在调用时按实参类型解析。 */
    private fun newMethodResolver(): Any = Proxy.newProxyInstance(
        loader,
        arrayOf(methodResolverClass)
    ) { proxy, method, args ->
        when (method.name) {
            "resolve" -> newMethodExecutor(args?.getOrNull(2) as? String ?: "")
            else -> proxyDefault(proxy, method, args)
        }
    }

    private fun newMethodExecutor(name: String): Any = Proxy.newProxyInstance(
        loader,
        arrayOf(methodExecutorClass)
    ) { proxy, method, args ->
        when (method.name) {
            "execute" -> executeProperty(args?.getOrNull(1), name, args?.getOrNull(2))
            else -> proxyDefault(proxy, method, args)
        }
    }

    /** 执行方法：先做白名单检查，再反射调用；只暴露 public 方法。 */
    private fun executeProperty(target: Any?, name: String, arguments: Any?): Any {
        val values = arguments as? Array<*> ?: emptyArray<Any?>()
        val holder = target as? Class<*>
        val targetClass = holder ?: target?.javaClass
            ?: throw IllegalStateException("无法确定方法 $name 的接收者类型")

        val candidate = findMethod(targetClass, name, values)
            ?: throw IllegalStateException("找不到方法 $name(${values.joinToString { it?.javaClass?.simpleName ?: "null" }})")

        if (!EvaluatorPolicy.isMethodAllowed(candidate.declaringClass.name, candidate.name)) {
            throw EvaluatorForbiddenException(
                EvaluatorPolicy.methodDenialMessage(candidate.declaringClass.name, candidate.name)
            )
        }
        if (!Modifier.isPublic(candidate.modifiers)) {
            throw EvaluatorForbiddenException("方法 ${candidate.declaringClass.name}.${candidate.name} 不是 public")
        }

        val result = try {
            candidate.isAccessible = true
            val invokeArgs = if (candidate.isVarArgs) packVarArgs(candidate, values) else values
            if (Modifier.isStatic(candidate.modifiers) || holder != null) {
                candidate.invoke(null, *invokeArgs)
            } else {
                candidate.invoke(target, *invokeArgs)
            }
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
        return typedValueClass.getConstructor(Any::class.java).newInstance(result)
    }

    private fun findMethod(targetClass: Class<*>, name: String, arguments: Array<*>): Method? {
        val candidates = targetClass.methods.filter { it.name == name }
        if (candidates.isEmpty()) {
            return null
        }
        if (arguments.isEmpty()) {
            return candidates.firstOrNull { it.parameterCount == 0 } ?: candidates.first()
        }
        val exact = candidates.firstOrNull { method ->
            val parameters = method.parameterTypes
            parameters.size == arguments.size &&
                parameters.indices.all { index ->
                    boxed(parameters[index]).isAssignableFrom(boxed(argumentClass(arguments[index])))
                }
        }
        return exact
            ?: candidates.firstOrNull { method -> matchesVarArgs(method, arguments) }
            ?: candidates.firstOrNull { it.parameterCount == arguments.size }
    }

    /** 可变参数方法：固定参数逐个匹配，剩余实参可以放入末尾数组。 */
    private fun matchesVarArgs(method: Method, arguments: Array<*>): Boolean {
        if (!method.isVarArgs || arguments.size < method.parameterCount - 1) {
            return false
        }
        val fixed = method.parameterTypes.dropLast(1)
        if (!fixed.withIndex().all { (index, parameter) ->
                boxed(parameter).isAssignableFrom(boxed(argumentClass(arguments[index])))
            }
        ) {
            return false
        }
        val component = boxed(method.parameterTypes.last().componentType)
        return arguments.drop(fixed.size).all { value ->
            component.isAssignableFrom(boxed(argumentClass(value)))
        }
    }

    /** 把多余的实参打包进末尾的数组参数。 */
    private fun packVarArgs(method: Method, arguments: Array<*>): Array<Any?> {
        val fixedCount = method.parameterCount - 1
        if (arguments.size < fixedCount) {
            return arguments.toList().toTypedArray()
        }
        val component = method.parameterTypes.last().componentType
        val rest = arguments.drop(fixedCount)
        val array = java.lang.reflect.Array.newInstance(component, rest.size)
        rest.forEachIndexed { index, value -> java.lang.reflect.Array.set(array, index, value) }
        return (arguments.take(fixedCount) + array).toTypedArray()
    }

    private fun argumentClass(value: Any?): Class<*> = value?.javaClass ?: Any::class.java

    private fun boxed(type: Class<*>): Class<*> = when (type) {
        java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
        java.lang.Byte.TYPE -> java.lang.Byte::class.java
        java.lang.Character.TYPE -> java.lang.Character::class.java
        java.lang.Short.TYPE -> java.lang.Short::class.java
        java.lang.Integer.TYPE -> java.lang.Integer::class.java
        java.lang.Long.TYPE -> java.lang.Long::class.java
        java.lang.Float.TYPE -> java.lang.Float::class.java
        java.lang.Double.TYPE -> java.lang.Double::class.java
        java.lang.Void.TYPE -> java.lang.Void::class.java
        else -> type
    }

    private fun proxyDefault(proxy: Any, method: Method, args: Array<Any?>?): Any? = when (method.name) {
        "equals" -> proxy === args?.getOrNull(0)
        "hashCode" -> System.identityHashCode(proxy)
        "toString" -> "SpelEvaluatorProxy"
        else -> null
    }

    private fun loadClass(name: String): Class<*> =
        Class.forName(name, false, loader)
}
