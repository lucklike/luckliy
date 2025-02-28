package com.luckyframework.io;

@FunctionalInterface
public interface LineEventListener {

    void onNewline(String line, int lineNumber);
}
