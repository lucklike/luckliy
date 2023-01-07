package com.luckyframework.context.event;

import com.luckyframework.bean.factory.BeanFactory;
import com.luckyframework.bean.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**支持线程池和异常处理的事件多播器
 * @author fukang
 * @version 1.0.0
 * @date 2023/1/7 19:12
 */
@SuppressWarnings("all")
public class SimpleApplicationEventMulticaster extends DefaultApplicationEventMulticaster implements DisposableBean {

    @Nullable
    private Executor taskExecutor;

    @Nullable
    private ErrorHandler errorHandler;

    @Nullable
    public Executor getTaskExecutor() {
        return taskExecutor;
    }

    public void setTaskExecutor(@Nullable Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Nullable
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
        super(beanFactory);
    }

    @Override
    protected void invokeListener(ApplicationListener listener, ApplicationEvent event) {
        Executor executor = getTaskExecutor();
        if(executor != null){
            executor.execute(() -> doInvokeListener(listener, event));
        }
        else{
            doInvokeListener(listener, event);
        }
    }


    private void doInvokeListener(ApplicationListener listener, ApplicationEvent event){
        ErrorHandler handler = getErrorHandler();
        if(handler != null){
            try {
                super.invokeListener(listener, event);
            }catch (Throwable e){
                handler.handleError(e);
            }
        }
        else{
            super.invokeListener(listener, event);
        }

    }

    @Override
    public void destroy() throws Exception {
        if(this.taskExecutor != null){
            if(this.taskExecutor instanceof ExecutorService){
                ((ExecutorService)this.taskExecutor).shutdown();
            }
        }
    }
}
