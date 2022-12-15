package com.luckyframework.common;

import com.luckyframework.exception.LuckyRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 计时器，用于统计计算程序的运行时间
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/27 05:57
 */
public class StopWatch {

    private final static Logger logger = LoggerFactory.getLogger(StopWatch.class);

    /** 总任务ID*/
    private final static String TOTAL_TASK_ID = "@total";

    /** 任务ID与任务实例组成的Map*/
    private final ThreadLocal<Map<String, TaskInfo>> taskInfoMap = ThreadLocal.withInitial(LinkedHashMap::new);

    /** 当前注册的一批任务组*/
    private final ThreadLocal<List<TaskInfo>> currentTasks = ThreadLocal.withInitial(ArrayList::new);

    /** 总任务实例*/
    private final ThreadLocal<TaskInfo> totalTime = new ThreadLocal<>();

    public StopWatch(String totalTaskId){
        long startTime = getSystemTime();
        totalTime.set(new TaskInfo(totalTaskId, startTime));
        start(startTime);
    }

    public StopWatch(){
        this(TOTAL_TASK_ID);
    }

    /***
     * 开始一组任务
     * @param taskIds 任务ID集合
     */
    public void start(String ...taskIds){
        long startTime = getSystemTime();
        start(startTime, taskIds);
    }

    private void reset(String ...tasks){
        Map<String, TaskInfo> taskMap = getTaskMap();
        for (String task : tasks) {
            TaskInfo taskInfo = taskMap.get(task);
            if(taskInfo == null){
                start(task);
            }else{
                taskInfo.reset();
            }
        }

    }

    /***
     * 停止一组任务
     * @param taskIds 任务ID集合
     */
    public void stop(String ...taskIds){
        Map<String, TaskInfo> taskMap = getTaskMap();
        for (String taskId : taskIds) {
            TaskInfo taskInfo = taskMap.get(taskId);
            if(taskInfo == null){
                throw new StopwatchException("Can't stop StopWatch: The task with id '{}' does not exist.", taskId).printException(logger);
            }
            if(!taskInfo.isStop()){
                taskInfo.stop();
            }
        }
    }

    public void stopWatch(){
        Map<String, TaskInfo> taskMap = getTaskMap();
        for (Map.Entry<String, TaskInfo> entry : taskMap.entrySet()) {
            TaskInfo task = entry.getValue();
            if(!task.isStop()){
                task.stop();
            }
        }
        TaskInfo totalTask = totalTime.get();
        if (!totalTask.isStop()){
            totalTask.stop();
        }
    }

    /***
     * 停止当前组中的所有任务
     */
    public void stopCurrentGroup(){
        List<TaskInfo> taskInfos = currentTasks.get();
        for (TaskInfo taskInfo : taskInfos) {
            if(!taskInfo.isStop()){
                taskInfo.stop();
            }
        }
    }

    /***
     * 停止当前组中最后注册的那个任务
     */
    public void stopLast(){
        TaskInfo taskInfo = getLastTask();
        if(!taskInfo.isStop()){
            taskInfo.stop();
        }
    }

    /**
     * 获取当前组所有任务实例
     * @return  当前组所有任务实例
     */
    public List<TaskInfo> getCurrentTasks(){
        return currentTasks.get();
    }

    /***
     * 获取当前组中最后注册的那个任务实例
     * @return 当前组中最后注册的那个任务实例
     */
    public TaskInfo getLastTask(){
        List<TaskInfo> taskInfos = currentTasks.get();
        return taskInfos.get(taskInfos.size() - 1);
    }

    /***
     * 判断任务是否存在
     * @param taskId 任务ID
     * @return 判断任务是否存在
     */
    public boolean taskExists(String taskId){
        return getTaskMap().containsKey(taskId);
    }

    /***
     * 根据ID获取一个任务实例
     * @param taskId 任务ID
     * @return 任务实例
     */
    @Nullable
    public TaskInfo getTaskInfo(String taskId){
        return getTaskMap().get(taskId);
    }

    /***
     * 获取当前注册的所有任务实例
     * @return 当前注册的所有任务实例
     */
    @NonNull
    public TaskInfo[] getAllTasks(){
        return getTaskMap().values().toArray(new TaskInfo[0]);
    }

    /***
     * 获取已经注册得任务数量
     * @return 已经注册得任务数量
     */
    public int getTaskCount(){
        return getTaskMap().size();
    }

    /**
     * 以毫秒为单位获取某个任务的执行时间
     * @param taskId 任务ID
     * @return 某个任务的执行时间
     */
    public long getTimeMillis(String taskId){
        TaskInfo taskInfo = getTaskInfo(taskId);
        if(taskInfo != null){
            return taskInfo.getElapsedTimeMillis();
        }
        throw new StopwatchException("Unable to get the runtime of task {} because the task does not exist.", taskId).printException(logger);
    }

    /**
     * 以纳秒为单位获取某个任务的执行时间
     * @param taskId 任务ID
     * @return 某个任务的执行时间
     */
    public long getTimeNano(String taskId){
        TaskInfo taskInfo = getTaskInfo(taskId);
        if(taskInfo != null){
            return taskInfo.getElapsedTimeNano();
        }
        throw new StopwatchException("Unable to get the runtime of task {} because the task does not exist.", taskId).printException(logger);
    }

    /**
     * 以毫秒为单位获取当前组中最后注册的那个任务的执行时间
     * @return 任务的执行时间
     */
    public long getLastTaskTimeMillis(){
        return getLastTask().getElapsedTimeMillis();
    }

    /***
     * 以毫秒为单位获取所有任务的执行时间
     * @return 所有任务的执行时间
     */
    public Map<String, Long> getAllTimeMillis(){
        Map<String, Long> map = new LinkedHashMap<>();
        TaskInfo[] allTasks = getAllTasks();
        for (TaskInfo task : allTasks) {
            map.put(task.getTaskId(), task.getElapsedTimeMillis());
        }
        return map;
    }

    /**
     * 以秒为单位获取某个任务的执行时间
     * @param taskId 任务ID
     * @return 某个任务的执行时间
     */
    public double getTimeSeconds(String taskId){
        return getTimeMillis(taskId) / 1000.0D;
    }

    /**
     * 以秒为单位获取当前组中最后注册的那个任务的执行时间
     * @return 任务的执行时间
     */
    public double getLastTimeSeconds(String taskId){
        return getLastTask().getElapsedTimeSeconds();
    }

    /***
     * 以秒为单位获取所有任务的执行时间
     * @return 所有任务的执行时间
     */
    public Map<String, Double> getAllTimeSeconds(){
        Map<String, Double> map = new LinkedHashMap<>();
        TaskInfo[] allTasks = getAllTasks();
        for (TaskInfo task : allTasks) {
            map.put(task.getTaskId(), task.getElapsedTimeSeconds());
        }
        return map;
    }

    /***
     * 获取格式化的任务执行统计报告
     * @return 格式化的任务执行统计报告
     */
    public String prettyPrint(){
        Table table = new Table();
        table.styleThree();
        table.setHeader("TASK-ID", "ELAPSED-TIME(ns)","PROPORTION(%)");

        TaskInfo total = this.totalTime.get();
        TaskInfo[] allTasks = getAllTasks();

        Long totalElapsedTime = total.getElapsedTimeNano();
        table.addDataRow(total.toArray(totalElapsedTime, true));

        for (TaskInfo task : allTasks) {
            table.addDataRow(task.toArray(totalElapsedTime, true));
        }
        return table.format();
    }

    /***
     * 获取格式化的任务执行统计报告
     * @return 格式化的任务执行统计报告
     */
    public String prettyPrintMillis(){
        Table table = new Table();
        table.styleThree();
        table.setHeader("TASK-ID", "ELAPSED-TIME(ms)","PROPORTION(%)");

        TaskInfo total = this.totalTime.get();
        TaskInfo[] allTasks = getAllTasks();

        Long totalElapsedTime = total.getElapsedTimeMillis();
        table.addDataRow(total.toArray(totalElapsedTime, false));

        for (TaskInfo task : allTasks) {
            table.addDataRow(task.toArray(totalElapsedTime, false));
        }
        return table.format();
    }

    private void start(Long startTime, String ...taskIds){
        List<TaskInfo> currentTaskList = currentTasks.get();
        currentTaskList.clear();
        Map<String, TaskInfo> taskMap = getTaskMap();
        for (String taskId : taskIds) {
            if(taskMap.containsKey(taskId)){
                throw new StopwatchException("Can't start StopWatch: Task with id '{}' already exists.", taskId).printException(logger);
            }
            TaskInfo taskInfo = new TaskInfo(taskId, startTime);
            taskMap.put(taskId, taskInfo);
            currentTaskList.add(taskInfo);
        }
    }

    private Map<String, TaskInfo> getTaskMap(){
        return taskInfoMap.get();
    }


    public static long getSystemTime(){
        return System.nanoTime();
    }




    static final class TaskInfo{
        private final String taskId;
        private long startTime;

        private long stopTime = -1L;
        private long elapsedTime = -1L;
        
        public TaskInfo(String taskId){
            this(taskId, getSystemTime());
        }

        public TaskInfo(String taskId, Long startTime) {
            this.taskId = taskId;
            this.startTime = startTime;
        }

        public Object[] toArray(Long totalTime, boolean isNano){
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            long et = isNano ? getElapsedTimeNano() : getElapsedTimeMillis();
            return new Object[]{
                    getTaskId(),  String.valueOf(et),
                    String.valueOf(StringUtils.decimalToPercent(et / (totalTime*1D)))
            };
        }

        public String getTaskId() {
            return taskId;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getStopTime() {
            return stopTime == -1L ? getSystemTime() : stopTime;
        }
        
        public boolean isStop(){
            return this.stopTime != -1L;
        }

        public void stop(){
            this.stopTime = getSystemTime();
            this.elapsedTime = stopTime - startTime;
        }

        public void reset(){
            this.startTime = getSystemTime();
            this.stopTime = -1L;
            this.elapsedTime = -1L;
        }

        public Long getElapsedTimeNano(){
            return isStop() ? elapsedTime : getSystemTime() - startTime;
        }


        public Long getElapsedTimeMillis(){
            return getElapsedTimeNano() / (1000 * 1000);
        }

        public double getElapsedTimeSeconds(){
            return getElapsedTimeMillis() / 1000.0D;
        }
        
    }

    static class StopwatchException extends LuckyRuntimeException {


        public StopwatchException(String message) {
            super(message);
        }

        public StopwatchException(Throwable ex) {
            super(ex);
        }

        public StopwatchException(String message, Throwable ex) {
            super(message, ex);
        }

        public StopwatchException(String messageTemplate, Object... args) {
            super(messageTemplate, args);
        }

        public StopwatchException(Throwable ex, String messageTemplate, Object... args) {
            super(ex, messageTemplate, args);
        }
    }

}
