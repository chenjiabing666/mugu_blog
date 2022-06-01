package com.mugu.blog.async;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableScheduling
@Configuration
@EnableAsync
public class AsyncAutoConfig {
    @Bean
    @Primary
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor poolTaskExecutor = new ThreadPoolTaskExecutor();
        poolTaskExecutor.initialize();
        poolTaskExecutor.setCorePoolSize(4);
        poolTaskExecutor.setMaxPoolSize(6);
        // 设置线程活跃时间（秒）
        poolTaskExecutor.setKeepAliveSeconds(120);
        // 设置队列容量
        poolTaskExecutor.setQueueCapacity(40);
        // 设置拒绝策略
        //AbortPolicy策略：该策略会直接抛出异常，阻止系统正常工作。
        //
        //CallerRunsPolicy 策略：只要线程池未关闭，该策略直接在调用者线程中，运行当前的被丢弃的任务。
        //
        //DiscardOleddestPolicy策略： 该策略将丢弃最老的一个请求，也就是即将被执行的任务，并尝试再次提交当前任务。
        //
        //DiscardPolicy策略：该策略默默的丢弃无法处理的任务，不予任何处理。
        poolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        poolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);


        //这里对创建的ThreadPoolTaskExecutor使用TtlExecutors进行了包装
        return TtlExecutors.getTtlExecutor(poolTaskExecutor);
    }
}
