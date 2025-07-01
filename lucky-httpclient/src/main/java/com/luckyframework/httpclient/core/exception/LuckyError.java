package com.luckyframework.httpclient.core.exception;

import com.luckyframework.exception.LuckyRuntimeException;

public class LuckyError extends LuckyRuntimeException {
  public LuckyError(String message) {
    super(message);
  }

  public LuckyError(Throwable ex) {
    super(ex);
  }

  public LuckyError(String message, Throwable ex) {
    super(message, ex);
  }

  public LuckyError(String messageTemplate, Object... args) {
    super(messageTemplate, args);
  }

  public LuckyError(Throwable ex, String messageTemplate, Object... args) {
    super(ex, messageTemplate, args);
  }
}
