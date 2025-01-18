package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.NanoIdUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.io.FileUtils;
import com.luckyframework.io.RepeatableReadStreamUtil;
import com.luckyframework.io.StorageMediumStream;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.lang.NonNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("all")
public class RepeatableReadStreamFunction {

    public static Object handlingObject(ValueUnpackContext context, Object object, StreamType type, String storeDir) {
        if (object == null || (object instanceof StorageMediumStream)) {
            return object;
        }
        if (object instanceof InputStream) {
            return toRepeatableReadStream(context, ((InputStream) object), type, storeDir);
        }
        if (object instanceof Map) {
            return handlingMap(context, ((Map) object), type, storeDir);
        }
        if (ContainerUtils.isArray(object)) {
            return handlingArray(context, object, type, storeDir);
        }
        if (ContainerUtils.isCollection(object)) {
            return handlingCollection(context, (Collection<?>) object, type, storeDir);
        }
        return object;
    }

    private static Object handlingArray(ValueUnpackContext context, @NonNull Object array, StreamType type, String storeDir) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object item = Array.get(array, i);
            Array.set(array, i, handlingObject(context, item, type, storeDir));
        }
        return array;
    }

    private static Object handlingCollection(ValueUnpackContext context, @NonNull Collection<?> collection, StreamType type, String storeDir) {
        if (collection instanceof List) {
            ListIterator listIterator = ((List) collection).listIterator();
            while (listIterator.hasNext()) {
                Object item = listIterator.next();
                listIterator.set(handlingObject(context, item, type, storeDir));
            }
            return collection;
        }
        Set resultSet = (Set) ClassUtils.createObject(collection.getClass(), HashSet::new);
        for (Object o : collection) {
            resultSet.add(handlingObject(context, o, type, storeDir));
        }
        return collection;
    }

    private static Object handlingMap(ValueUnpackContext context, @NonNull Map<Object, Object> map, StreamType type, String storeDir) {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            entry.setValue(handlingObject(context, value, type, storeDir));
        }
        return map;
    }

    private static InputStream toRepeatableReadStream(ValueUnpackContext context, InputStream sourceIn, StreamType type, String storeDir) {
        try {
            // byte数组存储
            if (type == StreamType.BYTE_ARRAY) {
                return RepeatableReadStreamUtil.useByteStore(sourceIn);
            }

            // 文件存储介质
            if (!StringUtils.hasText(storeDir)) {
                return RepeatableReadStreamUtil.useFileStore(sourceIn);
            }

            // 指定文件存储的目录
            storeDir = context.parseExpression(storeDir);
            File storeFile = new File(storeDir, NanoIdUtils.randomNanoId());
            FileUtils.createSaveFolder(storeFile.getParentFile());
            return RepeatableReadStreamUtil.useFileStore(storeFile, sourceIn);
        } catch (IOException e) {
            throw new ContextValueUnpackException(e);
        }
    }


    //-----------------------------------------------------------------------------------------------
    //                                  Release resources
    //-----------------------------------------------------------------------------------------------

    public static void releaseByObject(Object object) {
        if (object instanceof StorageMediumStream) {
            ((StorageMediumStream) object).deleteStorageMedium();
        }
        if (object instanceof Closeable) {
            FileUtils.closeIgnoreException((Closeable) object);
        }

        if (object instanceof Map) {
            releaseByMap((Map) object);
        } else if (ContainerUtils.isArray(object)) {
            releaseByArray(object);
        } else if (ContainerUtils.isCollection(object)) {
            releaseByCollection((Collection<?>) object);
        }
    }

    private static void releaseByMap(Map<Object, Object> map) {
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            releaseByObject(entry.getValue());
        }
    }

    private static void releaseByArray(Object array) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            releaseByObject(Array.get(array, i));
        }
    }

    private static void releaseByCollection(Collection<?> collection) {
        for (Object element : collection) {
            releaseByObject(element);
        }
    }

}
