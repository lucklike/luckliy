package com.luckyframework.httpclient.generalapi.chunk;

import com.luckyframework.common.ContainerUtils;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 分片处理结果集。
 * <pre>
 *     一次分片处理得到的全部{@link ChunkResult}，顺序与分片索引顺序一致。
 *
 *     本类提供两个正交的判断维度，使用时不要混淆：
 *     1.<b>执行维度</b>——分片任务是否跑完，依据{@link ChunkResult#getStatus()}区分：
 *       {@link #hasExecutionFailure()}、{@link #getUncompletedChunks()}、{@link #getResults(ChunkResultStatus)}；
 *     2.<b>业务维度</b>——跑完的分片返回值合不合预期，由调用方给出判断逻辑：
 *       {@link #isAllSuccess(Function)}只判断是否全部成功，
 *       {@link #classify(Function)}按成功与否拆成两组，便于对失败分片做重试等操作。
 *
 *     业务维度方法的取值范围：只有{@link ChunkResultStatus#SUCCESS}形态的结果会传入判断函数
 *     （FAILED与SKIPPED形态的{@link ChunkResult#getResult()}恒为null，传给业务判断函数会造成空指针），
 *     它们会被直接归入失败组／让{@code isAllSuccess}返回false；
 *     因此{@code asyncHandle}的结果集（全是SUCCESS形态）与改造前行为完全一致。
 *
 *     空集合语义：不存在失败即视为全部成功，因此空结果集的{@code isAllSuccess}返回true，
 *     该语义与{@link ChunkResultClassify#isAllSuccess()}保持一致；但空结果集同时意味着
 *     「没有任何分片被执行完毕」，存在失败分片时应结合{@link #hasExecutionFailure()}判断。
 * </pre>
 *
 * @param <R> 结果类型
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/20 05:05
 * @see ChunkResult
 * @see ChunkResultClassify
 * @see ChunkResultStatus
 */
public class ChunkResults<R> {

    /**
     * 处理结果集的不可修改视图；传入null或空集合时为空集合
     */
    private final List<ChunkResult<R>> results;

    /**
     * 构造分片处理结果集
     * <pre>
     *     入参列表会被拷贝一份再对外暴露，因此构造完成后再修改入参列表（或把它传给别处继续累加）
     *     都不会影响本次分片处理已落定的结果。
     * </pre>
     *
     * @param results 处理结果集合，为null或空集合时视为空结果集
     */
    public ChunkResults(List<ChunkResult<R>> results) {
        this.results = ContainerUtils.isEmptyCollection(results)
                ? Collections.<ChunkResult<R>>emptyList()
                : Collections.unmodifiableList(new ArrayList<>(results));
    }

    /**
     * 结果集是否为空
     * <pre>
     *     空结果集可能是「文件没有任何分片」，也可能是「所有分片都失败或被跳过」，
     *     需要区分时需结合分片总数或{@link #hasExecutionFailure()}判断。
     * </pre>
     *
     * @return 结果集是否为空
     */
    public boolean isEmpty() {
        return results.isEmpty();
    }

    /**
     * 获取结果数量
     *
     * @return 结果数量
     */
    public int size() {
        return results.size();
    }

    /**
     * 遍历结果集
     *
     * @param consumer 元素处理逻辑
     */
    public void forEach(Consumer<ChunkResult<R>> consumer) {
        results.forEach(consumer);
    }

    /**
     * 是否全部成功
     * <pre>
     *     不存在被判定为失败的分片时返回true，因此空结果集返回true。
     *     存在{@link ChunkResultStatus#FAILED}或{@link ChunkResultStatus#SKIPPED}形态的结果时直接返回false，
     *     不会将它们的null返回值传入判断函数。
     * </pre>
     *
     * @param successFunction 结果成功与否的判断逻辑，返回true表示该分片成功
     * @return 是否全部成功
     */
    public boolean isAllSuccess(Function<R, Boolean> successFunction) {
        for (ChunkResult<R> result : results) {
            if (!result.isSuccess()) {
                return false;
            }
            if (!successFunction.apply(result.getResult())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将结果集按照成功与否进行分类
     * <pre>
     *     一次遍历同时产出成功组与失败组，两组内部均保持原有的分片顺序；
     *     执行失败或被跳过的分片直接归入失败组（其{@link ChunkResult#getResult()}为null），
     *     判断函数只会被作用于执行完毕的分片。
     * </pre>
     *
     * @param successFunction 结果成功与否的判断逻辑，返回true表示该分片成功
     * @return 分类好的结果集
     */
    public ChunkResultClassify<R> classify(Function<R, Boolean> successFunction) {
        List<ChunkResult<R>> success = new ArrayList<>();
        List<ChunkResult<R>> failure = new ArrayList<>();

        // 进行分类
        for (ChunkResult<R> result : results) {
            if (result.isSuccess() && successFunction.apply(result.getResult())) {
                success.add(result);
            } else {
                failure.add(result);
            }
        }

        return new ChunkResultClassify<>(new ChunkResults<>(success), new ChunkResults<>(failure));
    }

    /**
     * 获取所有成功的结果集
     *
     * @param successFunction 结果成功与否的判断逻辑，返回true表示该分片成功
     * @return 所有成功的结果集
     */
    public ChunkResults<R> getSuccessResults(Function<R, Boolean> successFunction) {
        return classify(successFunction).getSuccess();
    }

    /**
     * 获取所有失败的结果集
     * <pre>
     *     失败分片可通过{@link ChunkResult#getFileChunk()}定位到具体区间，用于重新提交。
     * </pre>
     *
     * @param successFunction 结果成功与否的判断逻辑，返回true表示该分片成功
     * @return 所有失败的结果集
     */
    public ChunkResults<R> getFailureResults(Function<R, Boolean> successFunction) {
        return classify(successFunction).getFailure();
    }

    /**
     * 获取结果集合的不可修改视图
     * <pre>
     *     需要批量流式处理时可直接使用该方法，避免再次遍历包装。
     * </pre>
     *
     * @return 结果集合的不可修改视图
     */
    public List<ChunkResult<R>> getResults() {
        return results;
    }

    /**
     * 获取指定执行状态的结果集
     * <pre>
     *     传入{@link ChunkResultStatus#SUCCESS}即可取到「全部执行完毕」的分片结果，
     *     不受业务判断逻辑影响；与按业务判据取子集的
     *     {@link #getSuccessResults(Function)}是两个维度的方法。
     * </pre>
     *
     * @param status 目标执行状态，不可为null
     * @return 该状态的结果集，顺序与分片顺序一致
     * @throws IllegalArgumentException status为null时抛出
     */
    public ChunkResults<R> getResults(ChunkResultStatus status) {
        Assert.notNull(status, "Status must not be null");
        List<ChunkResult<R>> filtered = new ArrayList<>();
        for (ChunkResult<R> result : results) {
            if (result.getStatus() == status) {
                filtered.add(result);
            }
        }
        return new ChunkResults<>(filtered);
    }

    /**
     * 是否存在未执行完毕的分片
     *
     * @return 存在{@link ChunkResultStatus#FAILED}或{@link ChunkResultStatus#SKIPPED}形态的结果时返回true
     */
    public boolean hasExecutionFailure() {
        for (ChunkResult<R> result : results) {
            if (!result.isSuccess()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取需要重新处理的分片
     * <pre>
     *     包含执行异常（{@link ChunkResultStatus#FAILED}）与被止损跳过（{@link ChunkResultStatus#SKIPPED}）两类，
     *     顺序与分片顺序一致；调用方可直接将返回的分片交给下一轮重试，
     *     异常原因则从{@link ChunkResult#getError()}中获取。
     * </pre>
     *
     * @return 未执行完毕的分片集合，全部执行完毕时为空集合
     */
    @NonNull
    public List<FileChunk> getUncompletedChunks() {
        List<FileChunk> uncompleted = new ArrayList<>();
        for (ChunkResult<R> result : results) {
            if (!result.isSuccess()) {
                uncompleted.add(result.getFileChunk());
            }
        }
        return uncompleted;
    }
}
