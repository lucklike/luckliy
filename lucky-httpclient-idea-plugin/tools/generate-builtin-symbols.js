#!/usr/bin/env node
/**
 * 从 lucky-httpclient 框架源码生成 `src/main/resources/spel/builtin-symbols.json`。
 *
 * 扫描三类信息：
 * 1. `InternalRootVarName` 的常量 → Root 变量；
 * 2. `InternalVarName` 的常量 → 普通变量；
 * 3. `proxy/function` 包的 public static 方法 → 内置函数（跳过 `@FunctionFilter`，
 *    按 `@FunctionAlias` 改名，命名空间来自类上的 `@Namespace` 与 `MethodSpaceConstant`）。
 *
 * 变量类型、可用阶段与说明由 `builtin-overrides.json` 人工确认；
 * `HttpClientProxyObjectFactory` 中注册的函数类标记为 `registered=true`。
 *
 * 用法：node tools/generate-builtin-symbols.js
 */
const fs = require("fs");
const path = require("path");

const pluginRoot = path.resolve(__dirname, "..");
const sourceRoot = path.resolve(pluginRoot, "..", "lucky-httpclient", "src", "main", "java");
const outputFile = path.join(pluginRoot, "src", "main", "resources", "spel", "builtin-symbols.json");
const overrides = JSON.parse(
    fs.readFileSync(path.join(__dirname, "builtin-overrides.json"), "utf8")
);

const SPEL_PACKAGE = "com.luckyframework.httpclient.proxy.spel";
const FUNCTION_PACKAGE = "com.luckyframework.httpclient.proxy.function";

function readSource(relativePath) {
    return fs.readFileSync(path.join(sourceRoot, ...relativePath.split("/")), "utf8");
}

/** 解析 `public static final String NAME = "value";` */
function parseStringConstants(text) {
    const result = [];
    const pattern = /public\s+(?:static\s+final|final\s+static)\s+String\s+([\w$]+)\s*=\s*"([^"]*)"\s*;/g;
    for (const match of text.matchAll(pattern)) {
        result.push({ constant: match[1], value: match[2] });
    }
    return result;
}

function parseVariable(constantEntry, kind) {
    const override = overrides.variables[constantEntry.value];
    if (!override) {
        console.warn(`缺少变量元数据: ${constantEntry.value} (${constantEntry.constant})`);
    }
    return {
        name: constantEntry.value,
        constant: constantEntry.constant,
        kind,
        type: override ? override.type : "Object",
        stage: override ? override.stage : "REQUEST_INIT",
        description: override ? override.description : "内置变量",
    };
}

/** 从 [openIndex] 处的 `{` 开始，返回匹配 `}` 之后的位置（跳过注释与字符串） */
function findMatchingBrace(text, openIndex) {
    let depth = 0;
    for (let i = openIndex; i < text.length; i++) {
        const char = text[i];
        if (char === "/" && text[i + 1] === "/") {
            const end = text.indexOf("\n", i);
            i = end < 0 ? text.length : end;
            continue;
        }
        if (char === "/" && text[i + 1] === "*") {
            const end = text.indexOf("*/", i + 2);
            i = end < 0 ? text.length : end + 1;
            continue;
        }
        if (char === '"' || char === "'") {
            const quote = char;
            for (i++; i < text.length; i++) {
                if (text[i] === "\\") {
                    i++;
                } else if (text[i] === quote) {
                    break;
                }
            }
            continue;
        }
        if (char === "{") {
            depth++;
        } else if (char === "}") {
            depth--;
            if (depth === 0) {
                return i + 1;
            }
        }
    }
    return text.length;
}

/** 类体内每个成员声明的结束位置（跳过注释与字符串） */
function memberEnds(text, classBodyOpen) {
    const ends = [];
    let depth = 0;
    for (let i = classBodyOpen + 1; i < text.length; i++) {
        const char = text[i];
        if (char === "/" && text[i + 1] === "/") {
            const end = text.indexOf("\n", i);
            i = end < 0 ? text.length : end;
            continue;
        }
        if (char === "/" && text[i + 1] === "*") {
            const end = text.indexOf("*/", i + 2);
            i = end < 0 ? text.length : end + 1;
            continue;
        }
        if (char === '"' || char === "'") {
            const quote = char;
            for (i++; i < text.length; i++) {
                if (text[i] === "\\") {
                    i++;
                } else if (text[i] === quote) {
                    break;
                }
            }
            continue;
        }
        if (char === "{") {
            depth++;
        } else if (char === "}") {
            depth--;
            if (depth === 0) {
                ends.push(i + 1);
            }
        }
    }
    return ends;
}

/** 读取方法声明：从 `public` 开始，返回返回类型、方法名、参数、结束位置 */
function readMethodDeclaration(text, start) {
    const parenIndex = text.indexOf("(", start);
    if (parenIndex < 0) {
        return null;
    }
    const header = text.slice(start, parenIndex);
    const nameMatch = /(\w+)\s*$/.exec(header);
    if (!nameMatch) {
        return null;
    }
    const returnType = header
        .slice(0, nameMatch.index)
        .replace(/^\s*public\s+static\s+/, "")
        .replace(/^\s*<[^>]*>\s*/, "")
        .trim();

    let depth = 0;
    let cursor = parenIndex;
    for (; cursor < text.length; cursor++) {
        if (text[cursor] === "(") {
            depth++;
        } else if (text[cursor] === ")") {
            depth--;
            if (depth === 0) {
                cursor++;
                break;
            }
        }
    }
    const paramsText = text.slice(parenIndex + 1, cursor - 1);

    let end = cursor;
    let bodyStart = text.indexOf("{", cursor);
    const semicolon = text.indexOf(";", cursor);
    if (bodyStart < 0 || (semicolon >= 0 && semicolon < bodyStart)) {
        end = semicolon < 0 ? cursor : semicolon + 1;
    } else {
        end = findMatchingBrace(text, bodyStart);
    }

    return { returnType, name: nameMatch[1], paramsText, end };
}

function splitParameters(paramsText) {
    const params = [];
    let depth = 0;
    let current = "";
    for (const char of paramsText) {
        if (char === "<" || char === "(") {
            depth++;
        } else if (char === ">" || char === ")") {
            depth--;
        }
        if (char === "," && depth === 0) {
            params.push(current);
            current = "";
        } else {
            current += char;
        }
    }
    if (current.trim()) {
        params.push(current);
    }
    return params.map(simplifyParameterType).filter(Boolean);
}

function simplifyParameterType(parameter) {
    let text = parameter.replace(/@\w+(\.\w+)*(\([^)]*\))?/g, " ").trim();
    text = text.replace(/\bfinal\s+/g, "").trim();
    if (!text) {
        return "";
    }
    const nameMatch = /(\w+)\s*$/.exec(text);
    let type = nameMatch ? text.slice(0, nameMatch.index).trim() : text;
    type = type.replace(/\s+/g, " ");
    return type;
}

/** 取最近一个 Javadoc 的摘要（去掉标签与 HTML） */
function summarizeJavadoc(prefix) {
    let doc = null;
    for (const match of prefix.matchAll(/\/\*\*([\s\S]*?)\*\//g)) {
        doc = match[1];
    }
    if (!doc) {
        return "";
    }
    let lines = doc.split(/\r?\n/).map((line) => line.replace(/^\s*\* ?/, "").trim());
    const tagIndex = lines.findIndex((line) => /^@\w+/.test(line));
    if (tagIndex >= 0) {
        lines = lines.slice(0, tagIndex);
    }
    return lines
        .join(" ")
        .replace(/<pre>[\s\S]*?<\/pre>/gi, " ")
        .replace(/<[^>]+>/g, " ")
        .replace(/\{@(?:link|code)\s+([^}]+)\}/g, "$1")
        .replace(/\s+/g, " ")
        .trim();
}

/** 解析一个函数类的 public static 方法 */
function parseFunctionClass(className, namespace, registered) {
    const text = readSource(`${FUNCTION_PACKAGE.replace(/\./g, "/")}/${className}.java`);
    const methods = [];
    const classBodyOpen = text.indexOf("{", text.indexOf("class " + className));
    if (classBodyOpen < 0) {
        return methods;
    }
    const ends = memberEnds(text, classBodyOpen);
    const usedNames = new Set();

    const pattern = /\bpublic\s+static\s+/g;
    let match;
    while ((match = pattern.exec(text))) {
        const declaration = readMethodDeclaration(text, match.index);
        if (!declaration) {
            continue;
        }
        // 只取紧邻当前方法、且位于上一个成员声明结束之后的注释与注解
        let prefixStart = classBodyOpen + 1;
        for (const end of ends) {
            if (end <= match.index) {
                prefixStart = end;
            } else {
                break;
            }
        }
        const prefix = text.slice(prefixStart, match.index);

        if (/@FunctionFilter\b/.test(prefix)) {
            continue;
        }
        const aliasMatch = /@FunctionAlias\s*\(\s*(?:value\s*=\s*)?"([^"]*)"\s*\)/.exec(prefix);
        const name = aliasMatch ? aliasMatch[1] : declaration.name;
        // 同一类中同名重载无法注册为 SpEL 函数（ClassStaticElement 会抛异常），只保留首个
        if (usedNames.has(name)) {
            console.warn(`跳过同名重载函数: ${className}.${declaration.name} -> ${name}`);
            continue;
        }
        usedNames.add(name);
        const params = splitParameters(declaration.paramsText);

        methods.push({
            name,
            method: declaration.name,
            owner: `${FUNCTION_PACKAGE}.${className}`,
            namespace,
            signature: `${name}(${params.join(", ")})`,
            returnType: declaration.returnType,
            description: summarizeJavadoc(prefix),
            registered,
        });
    }
    return methods;
}

// ---------------------------------------------------------------------------
// 变量
// ---------------------------------------------------------------------------
const rootConstants = parseStringConstants(readSource(`${SPEL_PACKAGE.replace(/\./g, "/")}/InternalRootVarName.java`));
const internalConstants = parseStringConstants(readSource(`${SPEL_PACKAGE.replace(/\./g, "/")}/InternalVarName.java`));
const rootVariables = rootConstants.map((entry) => parseVariable(entry, "ROOT_VARIABLE"));
const variables = internalConstants.map((entry) => parseVariable(entry, "VARIABLE"));

// ---------------------------------------------------------------------------
// 函数
// ---------------------------------------------------------------------------
const spaceConstants = new Map(
    parseStringConstants(readSource(`${SPEL_PACKAGE.replace(/\./g, "/")}/MethodSpaceConstant.java`))
        .map((entry) => [entry.constant, entry.value])
);

const factoryText = readSource(
    "com/luckyframework/httpclient/proxy/HttpClientProxyObjectFactory.java"
);
const registeredClasses = new Set(
    [...factoryText.matchAll(/addSpringElFunctionClass\s*\(\s*(\w+)\.class\s*\)/g)].map(
        (match) => match[1]
    )
);

const functionDir = path.join(sourceRoot, ...FUNCTION_PACKAGE.split("."));
const functionClasses = fs
    .readdirSync(functionDir)
    .filter((file) => file.endsWith(".java"))
    .map((file) => file.slice(0, -".java".length))
    .sort();

const functions = [];
for (const className of functionClasses) {
    const text = readSource(`${FUNCTION_PACKAGE.replace(/\./g, "/")}/${className}.java`);
    const namespaceMatch = /@Namespace\s*\(\s*(\w+)\s*\)/.exec(text);
    const namespace = namespaceMatch ? spaceConstants.get(namespaceMatch[1]) || "" : "";
    functions.push(...parseFunctionClass(className, namespace, registeredClasses.has(className)));
}

// ---------------------------------------------------------------------------
// 输出
// ---------------------------------------------------------------------------
const table = {
    tableVersion: overrides.tableVersion || "1.0.0",
    frameworkVersion: overrides.frameworkVersion || "3.0.x",
    rootVariables,
    variables,
    functions,
};

fs.writeFileSync(outputFile, JSON.stringify(table, null, 2) + "\n", "utf8");

console.log(`生成 ${outputFile}`);
console.log(`Root 变量 ${rootVariables.length} 个，普通变量 ${variables.length} 个，函数 ${functions.length} 个`);
console.log(`已注册函数类: ${[...registeredClasses].join(", ")}`);

const checks = [
    ["Root 变量", rootVariables.length, 40],
    ["普通变量", variables.length, 17],
    ["函数", functions.length, 148],
];
for (const [label, actual, expected] of checks) {
    if (actual < expected) {
        console.error(`${label}数量不足: ${actual} < ${expected}`);
        process.exitCode = 1;
    }
}
for (const name of ["$status$", "$body$", "time", "md5"]) {
    const found =
        rootVariables.some((entry) => entry.name === name) ||
        variables.some((entry) => entry.name === name) ||
        functions.some((entry) => entry.name === name);
    if (!found) {
        console.error(`缺少必要条目: ${name}`);
        process.exitCode = 1;
    }
}
