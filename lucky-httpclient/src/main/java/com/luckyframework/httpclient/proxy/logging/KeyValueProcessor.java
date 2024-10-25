package com.luckyframework.httpclient.proxy.logging;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/9/22 13:38
 */
public interface KeyValueProcessor {

    KeyValueProcessor DEFAULT = new NotProcessor();

    KV process(Object key, Object value);


    class KV {
        private final Object key;
        private final Object value;

        public KV(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public Object getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }
    }

    class NotProcessor implements KeyValueProcessor {

        @Override
        public KV process(Object key, Object value) {
            return new KV(key, value);
        }
    }
}
