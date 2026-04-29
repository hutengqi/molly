package cn.molly.document.config;

import cn.molly.document.email.DefaultMailService;
import cn.molly.document.email.MailService;
import cn.molly.document.properties.MollyDocumentProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.thymeleaf.TemplateEngine;

import java.util.concurrent.Executor;

/**
 * Email 子能力自动配置：仅当 classpath 存在 {@link JavaMailSender} 时生效。
 *
 * @author Ht7_Sincerity
 * @since 2026/4/28
 */
@AutoConfiguration(after = MollyDocumentAutoConfiguration.class)
@ConditionalOnClass(JavaMailSender.class)
public class MollyEmailAutoConfiguration {

    @Bean(name = "mollyMailAsyncExecutor")
    @ConditionalOnMissingBean(name = "mollyMailAsyncExecutor")
    public Executor mollyMailAsyncExecutor(MollyDocumentProperties properties) {
        MollyDocumentProperties.Async async = properties.getEmail().getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(async.getCorePoolSize());
        executor.setMaxPoolSize(async.getMaxPoolSize());
        executor.setQueueCapacity(async.getQueueCapacity());
        executor.setThreadNamePrefix(async.getThreadNamePrefix());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MailService mailService(JavaMailSender mailSender,
                                   MollyDocumentProperties properties,
                                   ObjectProvider<TemplateEngine> templateEngineProvider,
                                   @Qualifier("mollyMailAsyncExecutor") Executor mollyMailAsyncExecutor) {
        TemplateEngine engine = templateEngineProvider.getIfAvailable();
        return new DefaultMailService(mailSender, properties.getEmail(), engine, mollyMailAsyncExecutor);
    }
}
