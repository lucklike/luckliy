package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.common.ContainerUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 分片文件处理结果集
 */
public class HandleResults<R> {

    /**
     * 处理结果集
     */
    private final List<HandleResult<R>> results;

    public HandleResults(List<HandleResult<R>> results) {
        this.results = ContainerUtils.isEmptyCollection(results) ? Collections.emptyList() : results;
    }

    /**
     * 结果集是否为null
     *
     * @return 结果集是否为null
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * 遍历结果集
     *
     * @param consumer 元素处理逻辑
     */
    public void forEach(Consumer<HandleResult<R>> consumer) {
        results.forEach(consumer);
    }

    /**
     * 是否全部成功
     *
     * @param successFunction 结果成功与否的判断逻辑
     * @return 是否全部成功
     */
    public boolean isAllSuccess(Function<R, Boolean> successFunction) {
        for (HandleResult<R> result : results) {
            if (!successFunction.apply(result.getResult())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将结果集按照成功与否进行分类
     *
     * @param successFunction 结果成功与否的判断逻辑
     * @return 分类好的结果集
     */
    public ResultClassify<R> classify(Function<R, Boolean> successFunction) {
        List<HandleResult<R>> success = new ArrayList<>();
        List<HandleResult<R>> failure = new ArrayList<>();

        // 进行分类
        for (HandleResult<R> result : results) {
            if (successFunction.apply(result.getResult())) {
                success.add(result);
            } else {
                failure.add(result);
            }
        }

        return new ResultClassify<>(new HandleResults<>(success), new HandleResults<>(failure));
    }

    /**
     * 获取所有成功的结果集
     *
     * @param successFunction 结果成功与否的判断逻辑
     * @return 所有成功的结果集
     */
    public HandleResults<R> getSuccessResults(Function<R, Boolean> successFunction) {
        return classify(successFunction).getSuccess();
    }

    /**
     * 获取所有失败的结果集
     *
     * @param successFunction 结果成功与否的判断逻辑
     * @return 所有失败的结果集
     */
    public HandleResults<R> getFailureResults(Function<R, Boolean> successFunction) {
        return classify(successFunction).getFailure();
    }
}
