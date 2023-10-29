package com.luckyframework.httpclient.proxy;

import com.luckyframework.spel.ParamWrapper;
import com.luckyframework.spel.SpELImport;

import java.util.ArrayList;
import java.util.List;

/**
 * 支持{@link SpELImport}注解导入依赖包功能的接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/10 05:08
 */
public interface SupportSpELImport {

    default ParamWrapper getImportCompletedParamWrapper(Context context) {
        ParamWrapper paramWrapper = new ParamWrapper();
        List<Context> contextList = new ArrayList<>();
        Context tempContext = context;
        while (tempContext != null) {
            contextList.add(tempContext);
            tempContext = tempContext.getParentContext();
        }
        for (int i = contextList.size() - 1; i >= 0 ; i--) {
            SpELImport spELImportAnn = contextList.get(i).getMergedAnnotation(SpELImport.class);
            if (spELImportAnn != null) {
                paramWrapper.importPackage(spELImportAnn.value());
            }
        }
        return paramWrapper;
    }
}
