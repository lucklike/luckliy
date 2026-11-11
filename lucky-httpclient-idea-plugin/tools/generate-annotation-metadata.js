#!/usr/bin/env node
/**
 * 从 lucky-httpclient 框架源码生成 `src/main/resources/spel/annotations.json`。
 *
 * 扫描两类信息：
 * 1. 全部 `@interface` 定义（注解全限定名与属性名）；
 * 2. SpEL 入口调用点上的注解属性引用：
 *    - `parseExpression(ann.xxx(), ...)`
 *    - `analysisSpELAndFunc(context, ann.xxx(), ...)`
 *    - `autoExecuteSpELOrFunc(context, ann.xxx(), ...)`
 *    - `retryExpressionCheck(taskResult, ann.xxx(), ...)`
 *
 * 调用点默认记为 `TEMPLATE`，值种类与生命周期由 `annotation-overrides.json` 人工确认。
 * `@SpELImport` 的属性由框架反射读取，扫描不到调用点，按约定直接写入。
 *
 * 用法：node tools/generate-annotation-metadata.js
 */
const fs = require("fs");
const path = require("path");

const pluginRoot = path.resolve(__dirname, "..");
const sourceRoot = path.resolve(pluginRoot, "..", "lucky-httpclient", "src", "main", "java");
const outputFile = path.join(pluginRoot, "src", "main", "resources", "spel", "annotations.json");
const overrides = JSON.parse(
    fs.readFileSync(path.join(__dirname, "annotation-overrides.json"), "utf8")
);

function walk(dir, result = []) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) {
            walk(full, result);
        } else if (entry.name.endsWith(".java")) {
            result.push(full);
        }
    }
    return result;
}

const files = walk(sourceRoot);
const annotationIndex = new Map(); // simpleName -> fqName
const annotationSources = new Map(); // fqName -> source text

for (const file of files) {
    const text = fs.readFileSync(file, "utf8");
    const pkg = /package\s+([\w.]+)\s*;/.exec(text);
    if (!pkg) {
        continue;
    }
    for (const match of text.matchAll(/(?:public\s+)?@interface\s+(\w+)/g)) {
        const fqName = `${pkg[1]}.${match[1]}`;
        annotationIndex.set(match[1], fqName);
        annotationSources.set(fqName, text);
    }
}

function guessAnnotation(varName) {
    const normalized = varName.replace(/(Ann|Meta|Annotation)$/i, "");
    const candidates = [normalized, normalized.charAt(0).toUpperCase() + normalized.slice(1)];
    for (const candidate of candidates) {
        for (const [simpleName, fqName] of annotationIndex) {
            if (simpleName.toLowerCase() === candidate.toLowerCase()) {
                return fqName;
            }
        }
    }
    return null;
}

const found = new Map(); // fqName -> Set(attribute)
function record(fqName, attribute) {
    if (!found.has(fqName)) {
        found.set(fqName, new Set());
    }
    found.get(fqName).add(attribute);
}

const callPatterns = [
    /parseExpression\s*\(\s*(?:[\w.]+\.)?(\w+)\.(\w+)\s*\(\s*\)/g,
    /analysisSpELAndFunc\s*\([^,]+,\s*(\w+)\.(\w+)\s*\(\s*\)/g,
    /autoExecuteSpELOrFunc\s*\([^,]+,\s*(\w+)\.(\w+)\s*\(\s*\)/g,
    /retryExpressionCheck\s*\([^,]+,\s*(\w+)\.(\w+)\s*\(\s*\)/g,
];

for (const file of files) {
    const text = fs.readFileSync(file, "utf8");
    const varToType = new Map();
    for (const match of text.matchAll(
        /(\w+)\s+(\w+)\s*=\s*(?:[\w.]+\.)?(?:toAnnotation|getSameAnnotationCombined|getMergedAnnotationCheckParent|getMergedAnnotation)\(\s*(\w+)\.class/g
    )) {
        varToType.set(match[2], match[3]);
    }
    for (const pattern of callPatterns) {
        for (const match of text.matchAll(pattern)) {
            const varName = match[1];
            const attribute = match[2];
            const simpleName = varToType.get(varName);
            const fqName = simpleName
                ? annotationIndex.get(simpleName) || guessAnnotation(varName)
                : guessAnnotation(varName);
            if (fqName) {
                record(fqName, attribute);
            }
        }
    }
}

// @SpELImport 的属性由框架反射读取，扫描不到调用点
const spelImportFqName = "com.luckyframework.httpclient.proxy.spel.SpELImport";
if (!found.has(spelImportFqName)) {
    found.set(spelImportFqName, new Set());
}
for (const attribute of ["root", "var", "rootLit", "varLit", "value", "ifs", "pack", "separator"]) {
    found.get(spelImportFqName).add(attribute);
}

// overrides 中登记的属性即使扫描不到也保留
for (const key of Object.keys(overrides)) {
    const [fqName, attribute] = key.split("#");
    record(fqName, attribute);
}

const annotations = {};
for (const fqName of [...found.keys()].sort()) {
    const attributes = {};
    for (const attribute of [...found.get(fqName)].sort()) {
        const override = overrides[`${fqName}#${attribute}`];
        attributes[attribute] = override
            ? { kind: override.kind, lifecycle: override.lifecycle || "METHOD" }
            : { kind: "TEMPLATE", lifecycle: "UNKNOWN" };
    }
    annotations[fqName] = { attributes };
}

const table = {
    tableVersion: "1.0.0",
    frameworkVersion: "3.0.x",
    annotations,
};

fs.writeFileSync(outputFile, JSON.stringify(table, null, 2) + "\n", "utf8");

const attributeCount = Object.values(annotations).reduce(
    (sum, entry) => sum + Object.keys(entry.attributes).length, 0
);
console.log(`生成 ${outputFile}`);
console.log(`注解 ${Object.keys(annotations).length} 个，属性 ${attributeCount} 个`);
for (const key of [
    "com.luckyframework.httpclient.proxy.annotations.HttpRequest#url",
    "com.luckyframework.httpclient.proxy.annotations.ServerAddress#url",
    "com.luckyframework.httpclient.proxy.annotations.ServerAddress#path",
    "com.luckyframework.httpclient.proxy.annotations.Retryable#condition",
    "com.luckyframework.httpclient.proxy.spel.SpELImport#var",
    "com.luckyframework.httpclient.proxy.spel.SpELImport#root",
]) {
    const [fqName, attribute] = key.split("#");
    const entry = annotations[fqName] && annotations[fqName].attributes[attribute];
    if (!entry) {
        console.error(`缺少必要条目: ${key}`);
        process.exitCode = 1;
    }
}
