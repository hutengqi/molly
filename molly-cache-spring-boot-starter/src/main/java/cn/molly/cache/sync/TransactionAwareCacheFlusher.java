package cn.molly.cache.sync;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务感知的缓存动作执行器。
 * <p>
 * 当处于活动事务中时，将缓存失效/回填动作挂入事务提交后阶段执行，
 * 避免事务回滚导致缓存与数据源不一致；无事务环境下退化为同步执行。
 *
 * @author Ht7_Sincerity
 * @since 2026/04/27
 */
public class TransactionAwareCacheFlusher {

    /**
     * 执行给定动作：若存在活动事务则在提交后执行，否则立即执行。
     *
     * @param action 缓存动作
     */
    public void runAfterCommit(Runnable action) {
        if (action == null) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
