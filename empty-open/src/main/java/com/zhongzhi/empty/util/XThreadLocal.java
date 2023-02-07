package com.zhongzhi.empty.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * 使用线程池等会缓存线程的组件情况下传递ThreadLocal
 * @author liuh
 * @date 2021年10月26日
 * @param <T>
 */
public class XThreadLocal<T> extends InheritableThreadLocal<T> {
    private static final Logger logger = LoggerFactory.getLogger(XThreadLocal.class);


    protected T copy(T parentValue) {
        return parentValue;
    }


    protected void beforeExecute() {
    }


    /**
     * Callback method after task object({@link XRunnable}/{@link XCallable} )
     * execute.
     */
    protected void afterExecute() {
    }


    @Override
    public final T get() {
        T value = super.get();
        if (null != value) {
            addValue();
        }
        return value;
    }


    @Override
    public final void set(T value) {
        super.set(value);
        if (null == value) { // may set null to remove value
            removeValue();
        } else {
            addValue();
        }
    }


    @Override
    public final void remove() {
        removeValue();
        super.remove();
    }


    void superRemove() {
        super.remove();
    }


    T copyValue() {
        return copy(get());
    }

    private static InheritableThreadLocal<Map<XThreadLocal<?>, ?>> holder = new InheritableThreadLocal<Map<XThreadLocal<?>, ?>>() {
        @Override
        protected Map<XThreadLocal<?>, ?> initialValue() {
            return new WeakHashMap<XThreadLocal<?>, Object>();
        }


        @Override
        protected Map<XThreadLocal<?>, ?> childValue(Map<XThreadLocal<?>, ?> parentValue) {
            return new WeakHashMap<XThreadLocal<?>, Object>(
                    parentValue);
        }
    };


    private void addValue() {
        if (!holder.get().containsKey(this)) {
            holder.get().put(this, null); // WeakHashMap supports null value.
        }
    }


    private void removeValue() {
        holder.get().remove(this);
    }


    private static void doExecuteCallback(boolean isBefore) {
        for (Map.Entry<XThreadLocal<?>, ?> entry : holder.get().entrySet()) {
            XThreadLocal<?> threadLocal = entry.getKey();

            try {
                if (isBefore) {
                    threadLocal.beforeExecute();
                } else {
                    threadLocal.afterExecute();
                }
            } catch (Throwable t) {
                logger.warn("exception when " + (isBefore ? "beforeExecute" : "afterExecute")
                        + ", cause: " + t.toString(), t);
            }
        }
    }


    /**
     * Debug only method!
     */
    static void dump(String title) {
        if (title != null && title.length() > 0) {
            System.out.printf("Start XThreadLocal[%s] Dump...\n", title);
        } else {
            System.out.println("Start XThreadLocal Dump...");
        }

        for (Map.Entry<XThreadLocal<?>, ?> entry : holder.get().entrySet()) {
            final XThreadLocal<?> key = entry.getKey();
            System.out.println(key.get());
        }
        System.out.println("XThreadLocal Dump end!");
    }


    /**
     * Debug only method!
     */
    static void dump() {
        dump(null);
    }

    public static class Transmitter {
        /**
         * Capture all {@link XThreadLocal} values in current thread.
         *
         * @return the captured {@link XThreadLocal} values
         * @since 2.3.0
         */
        public static Object capture() {
            Map<XThreadLocal<?>, Object> captured = new HashMap<XThreadLocal<?>, Object>();
            for (XThreadLocal<?> threadLocal : holder.get().keySet()) {
                captured.put(threadLocal, threadLocal.copyValue());
            }
            return captured;
        }


        /**
         *
         */
        public static Object replay(Object captured) {
            @SuppressWarnings("unchecked")
            Map<XThreadLocal<?>, Object> capturedMap = (Map<XThreadLocal<?>, Object>) captured;
            Map<XThreadLocal<?>, Object> backup = new HashMap<XThreadLocal<?>, Object>();

            for (Iterator<? extends Map.Entry<XThreadLocal<?>, ?>> iterator = holder.get()
                    .entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<XThreadLocal<?>, ?> next = iterator.next();
                XThreadLocal<?> threadLocal = next.getKey();

                // backup
                backup.put(threadLocal, threadLocal.get());

                // clear the value only in captured
                // avoid extra value in captured, when run task.
                if (!capturedMap.containsKey(threadLocal)) {
                    iterator.remove();
                    threadLocal.superRemove();
                }
            }

            // set value to captured
            for (Map.Entry<XThreadLocal<?>, Object> entry : capturedMap.entrySet()) {
                @SuppressWarnings("unchecked")
                XThreadLocal<Object> threadLocal = (XThreadLocal<Object>) entry.getKey();
                threadLocal.set(entry.getValue());
            }

            // call beforeExecute callback
            doExecuteCallback(true);

            return backup;
        }


        /**
         *
         */
        public static void restore(Object backup) {
            @SuppressWarnings("unchecked")
            Map<XThreadLocal<?>, Object> backupMap = (Map<XThreadLocal<?>, Object>) backup;
            // call afterExecute callback
            doExecuteCallback(false);

            for (Iterator<? extends Map.Entry<XThreadLocal<?>, ?>> iterator = holder.get()
                    .entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<XThreadLocal<?>, ?> next = iterator.next();
                XThreadLocal<?> threadLocal = next.getKey();

                // clear the value only in backup
                // avoid the extra value of backup after restore
                if (!backupMap.containsKey(threadLocal)) {
                    iterator.remove();
                    threadLocal.superRemove();
                }
            }

            // restore value
            for (Map.Entry<XThreadLocal<?>, Object> entry : backupMap.entrySet()) {
                @SuppressWarnings("unchecked")
                XThreadLocal<Object> threadLocal = (XThreadLocal<Object>) entry.getKey();
                threadLocal.set(entry.getValue());
            }
        }


        public static <R> R runSupplierWithCaptured(Object captured, Supplier<R> bizLogic) {
            Object backup = replay(captured);
            try {
                return bizLogic.get();
            } finally {
                restore(backup);
            }
        }


        public static <R> R runCallableWithCaptured(Object captured, Callable<R> bizLogic)
                throws Exception {
            Object backup = replay(captured);
            try {
                return bizLogic.call();
            } finally {
                restore(backup);
            }
        }


        private Transmitter() {
            throw new InstantiationError("Must not instantiate this class");
        }
    }
}
