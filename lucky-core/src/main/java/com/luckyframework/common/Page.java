package com.luckyframework.common;

import com.luckyframework.exception.PageParameterException;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 分页工具
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/6 11:48
 */
public class Page<T> {

    /** 当前页码*/
    private long currentPage;
    /** 总页数*/
    private long totalPages;

    /** 是否是最后一页*/
    private boolean lastPage;
    /** 是否是第一页*/
    private boolean firstPage;

    /** 当前页展示的数据条数*/
    private long pageSize;
    /** 每页最大的展示条数*/
    private long maxPageSize;

    /** 数据总数*/
    private long totalDataSize;

    /** 当前页的数据集*/
    private List<T> list;


    private Page(){}

    public long getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(long currentPage) {
        this.currentPage = currentPage;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isLastPage() {
        return lastPage;
    }

    public void setLastPage(boolean lastPage) {
        this.lastPage = lastPage;
    }

    public boolean isFirstPage() {
        return firstPage;
    }

    public void setFirstPage(boolean firstPage) {
        this.firstPage = firstPage;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public long getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(long maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    public void setTotalDataSize(long totalDataSize) {
        this.totalDataSize = totalDataSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    /**
     * 计算某一页开始位置的索引
     * @param page 页码
     * @param size 每页展示的数据数量
     * @return 某一页开始位置的索引
     */
    public static long calculateStartIndex(long page, long size){
        if(page < 1){
            throw new PageParameterException("Page number cannot be less than 1: page='{}'", page);
        }
        if(size < 1 ){
            throw new PageParameterException("The number of data displayed per page cannot be less than 1 : size='{}'", size);
        }
        return (page - 1) * size;
    }

    /**
     * 计算总页数
     * @param totalDataSize 数据总量
     * @param pageSize      每页展示的数量
     * @return              总页数
     */
    public static long calculateTotalPage(long totalDataSize, long pageSize){
        if(totalDataSize < 0){
            throw new PageParameterException("The total amount of data cannot be less than 0: totalDataSize='{}'", totalDataSize);
        }
        if(pageSize < 0){
            throw new PageParameterException("The number of impressions per page cannot be less than 0: totalDataSize='{}'", totalDataSize);
        }
        return (totalDataSize & pageSize) == 0
               ? totalDataSize / pageSize
               : totalDataSize / pageSize + 1;
    }


    public static <T> Page<T> create(List<T> currentPageDataList, long totalDataSize, long currentPage, long maxPageSize ){
        Page<T> page = new Page<>();
        page.setCurrentPage(currentPage);
        page.setTotalDataSize(totalDataSize);
        page.setMaxPageSize(maxPageSize);
        page.setList(currentPageDataList);
        page.setPageSize(currentPageDataList.size());
        page.setTotalPages(calculateTotalPage(totalDataSize, maxPageSize));
        page.setFirstPage(currentPage <= 1);
        page.setLastPage(currentPage >= page.getTotalPages());
        return page;
    }

    public static <T> Page<T> create(Supplier<List<T>> currentPageDataListSupp, LongSupplier totalDataSizeSupp,  LongSupplier currentPageSupplier, LongSupplier maxPageSupp){
        return create(currentPageDataListSupp.get(), totalDataSizeSupp.getAsLong(), currentPageSupplier.getAsLong(), maxPageSupp.getAsLong());
    }

    public static <T> Page<T> create( Supplier<List<T>> currentPageDataListSupp, LongSupplier totalDataSizeSupp, long currentPage, long maxPageSize){
        return create(currentPageDataListSupp.get(), totalDataSizeSupp.getAsLong(), currentPage, maxPageSize);
    }

}
