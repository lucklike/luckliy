package com.luckyframework.annotations;

import com.luckyframework.environment.LuckyStandardEnvironment;
import com.luckyframework.scanner.ScannerUtils;
import org.springframework.core.env.Profiles;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static com.luckyframework.scanner.Constants.PROFILE_ANNOTATION_NAME;
import static com.luckyframework.scanner.Constants.VALUE;

/**
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/14 上午2:40
 */
public class ProfileCondition implements Condition{
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        if(ScannerUtils.annotationIsExist(metadata, PROFILE_ANNOTATION_NAME)){
            LuckyStandardEnvironment environment = (LuckyStandardEnvironment) context.getEnvironment();
            String[] strProfiles = (String[]) ScannerUtils.getAnnotationAttribute(metadata, PROFILE_ANNOTATION_NAME,VALUE);
            Profiles profiles = Profiles.of(strProfiles);
            return environment.acceptsProfiles(profiles);
        }
        return true;
    }
}
