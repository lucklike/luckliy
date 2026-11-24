package com.luckyframework.idea.spel.component;

import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟平台分栏容器 `EditorsSplitters` 的图标写入入口。
 *
 * 方法名必须与 Kotlin `internal` 成员在 JVM 上的名称一致（带 `$` 后缀），
 * 这样测试才能固定反射约定，平台升级改名时立即暴露。
 */
public class FakeSplitters {

    public final List<VirtualFile> files = new ArrayList<>();
    public final List<Icon> icons = new ArrayList<>();

    public void updateFileIconImmediately$intellij_platform_ide_impl(VirtualFile file, Icon icon) {
        files.add(file);
        icons.add(icon);
    }
}
