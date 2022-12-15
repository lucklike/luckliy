package com.luckyframework.jdbc.datasource;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;

import java.util.Map;

public class ConnectedSynchronizationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConnectedSynchronizationManager.class);

    private static final ThreadLocal<Map<Object, Object>> resources =
            new NamedThreadLocal<>("Transactional resources");

}
