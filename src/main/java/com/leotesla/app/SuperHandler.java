package com.leotesla.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.util.ArrayMap;

import com.leotesla.httpclient.data.KeyValuePair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 增强型Handler，隔离不同的处理任务
 *
 * @version 1.0
 * <p>
 * Created by TeslaLiu on 2015/11/23.
 */
public class SuperHandler<T> extends Handler {

    private static final int IDLE_FLAG = -1111;

    // 内部Handler,双层设计
    private WeakReference<SuperHandler> mInternalHandlerRef;
    // 上下文实例
    private final WeakReference<T> mWeakReference;
    // 当前是否活动状态
    private boolean mAlive = true, mExited;
    // 是否消息恢复模式
    private boolean mPending = true;
    // 积攒的未发送msg
    private List<Message> mPendingMsg = new ArrayList<>();

    public interface ExtraHandler {
        void handle(Message msg);
    }

    private List<KeyValuePair<Integer, ExtraHandler>> extraHandlers = new ArrayList<>();
    private Map<ExtraHandler, HashSet<Integer>> locateMap = new ArrayMap<>();
    private Set<Integer> whatSet = new HashSet<>();

    public SuperHandler(T t) {
        this((Looper) null, t);
    }

    public SuperHandler(Looper looper, T t) {
        super(null == looper ? Looper.getMainLooper() : looper);
        this.mWeakReference = new WeakReference<>(t);
    }

    public SuperHandler(SuperHandler handler, T t) {
        super(handler.getLooper());
        this.mInternalHandlerRef = new WeakReference<>(handler);
        this.mWeakReference = new WeakReference<>(t);
    }

    public SuperHandler(SuperHandler<T> handler) {
        super(handler.getLooper());
        this.mInternalHandlerRef = new WeakReference<>(handler);
        this.mWeakReference = new WeakReference<>(handler.get());
    }

    public T get() {
        return mWeakReference.get();
    }

    @Override
    public void dispatchMessage(Message msg) {
        if (this.mAlive && (null == mInternalHandlerRef
                || null == mInternalHandlerRef.get()
                || mInternalHandlerRef.get().isAlive())) {
            // 该Handler没有挂起时, 如果没有外部Handler或者外部Handler被释放了或者外部Handler没被释放且没挂起时, 直接

            // 当前Handler处于活动模式时
            //
            // 如果内部Handler不存在或内部Handler被回收了, 或者内部Handler也是活动模式时. 大概意思是: 内部Handler要不就没有, 有的话必须要是处于活动模式
            // 直接分发消息
            super.dispatchMessage(msg);
        } else if (!this.mExited) {
            // 当前Handler没有退出时
            // 首先, 如果处于恢复模式, 则将该消息存储到集合中
            // 其次, 移除掉消息中的回调
            if (this.mPending) {
                this.mPendingMsg.add(Message.obtain(msg));
            }
            removeCallbacks(msg.getCallback());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (!this.mExited && this.mAlive && (null == mInternalHandlerRef
                || null == mInternalHandlerRef.get() || mInternalHandlerRef.get().isAlive())) {
            super.handleMessage(msg);
            for (KeyValuePair<Integer, ExtraHandler> pair : extraHandlers) {
                if (IDLE_FLAG != pair.key) {
                    if (pair.key == msg.what) {
                        pair.value.handle(msg);
                    }
                } else {
                    pair.value.handle(msg);
                }
            }
        }
    }

    /**
     * 注册处理器，可收到所有what
     *
     * @param handler 处理器
     * @see #registerHandler(int, ExtraHandler)
     * @see #registerHandler(ExtraHandler, int...)
     * @see #registerSingle(int, ExtraHandler)
     * @deprecated instead of {{@link #registerHandler(int, ExtraHandler)}}
     */
    public void addHandler(ExtraHandler handler) {
        extraHandlers.add(new KeyValuePair<>(IDLE_FLAG, handler));
        locateMap.put(handler, new HashSet<>());
        whatSet.add(IDLE_FLAG);
    }

    /**
     * 注册指定what处理器
     *
     * @param what    标记
     * @param handler 处理器
     */
    public SuperHandler<T> registerHandler(int what, ExtraHandler handler) {
        if (what == IDLE_FLAG) {
            throw new IllegalArgumentException("arg what can not equals to " + IDLE_FLAG);
        }
        HashSet<Integer> posSet = new HashSet<>(1);
        extraHandlers.add(new KeyValuePair<>(what, handler));
        posSet.add(extraHandlers.size() - 1);
        locateMap.put(handler, posSet);
        whatSet.add(what);
        return this;
    }

    /**
     * 注册指定what,
     * Note: 只能注册单一what，不允许重复
     *
     * @param what    what
     * @param handler 处理器
     */
    public SuperHandler<T> registerSingle(int what, ExtraHandler handler) {
        if (what == IDLE_FLAG) {
            throw new IllegalArgumentException("arg what can not equals to " + IDLE_FLAG);
        }
        if (whatSet.contains(what)) {
            throw new IllegalArgumentException("arg what '" + what + "' is existed");
        }
        HashSet<Integer> posSet = new HashSet<>(1);
        extraHandlers.add(new KeyValuePair<>(what, handler));
        posSet.add(extraHandlers.size() - 1);
        locateMap.put(handler, posSet);
        whatSet.add(what);
        return this;
    }

    /**
     * 注册指定what, 如果该what已经存在则覆盖
     * Note: 只能注册单一what，不允许重复
     *
     * @param what    what
     * @param handler 处理器
     */
    public SuperHandler<T> registerOverride(int what, ExtraHandler handler) {
        if (what == IDLE_FLAG) {
            throw new IllegalArgumentException("arg what can not equals to " + IDLE_FLAG);
        }
        unregisterHandler(what);
        HashSet<Integer> posSet = new HashSet<>(1);
        extraHandlers.add(new KeyValuePair<>(what, handler));
        posSet.add(extraHandlers.size() - 1);
        locateMap.put(handler, posSet);
        whatSet.add(what);
        return this;
    }

    /**
     * 一个处理器处理多种what标记
     *
     * @param handler 处理器
     * @param whats   whats
     */
    public SuperHandler<T> registerHandler(ExtraHandler handler, int... whats) {
        HashSet<Integer> posSet = new HashSet<>(whats.length);

        if (checkDuplicate(whats)) {
            throw new IllegalArgumentException("Argument whats can not duplicate");
        }

        for (int what : whats) {
            if (what == IDLE_FLAG) {
                throw new IllegalArgumentException("Argument what can not equals to " + IDLE_FLAG);
            }
            extraHandlers.add(new KeyValuePair<>(what, handler));
            posSet.add(extraHandlers.size() - 1);
            whatSet.add(what);
        }
        locateMap.put(handler, posSet);
        return this;
    }

    /**
     * 反注册whats
     *
     * @param whats whats
     */
    public SuperHandler<T> unregisterHandler(int... whats) {
        Iterator<KeyValuePair<Integer, ExtraHandler>> iterator = extraHandlers.iterator();
        while (iterator.hasNext()) {
            KeyValuePair<Integer, ExtraHandler> pair = iterator.next();
            for (int what : whats)
                if (pair.key == what) {
                    iterator.remove();
                }
        }
        return this;
    }

    /**
     * 清除所有处理器
     */
    public SuperHandler<T> clearHandlers() {
        extraHandlers.clear();
        return this;
    }

    /**
     * 清除指定处理器
     *
     * @param handlers 处理器集合
     */
    public SuperHandler<T> clearHandlers(ExtraHandler... handlers) {
        clearHandlers(Arrays.asList(handlers));
        return this;
    }

    /**
     * 清除指定处理器
     *
     * @param handlers 处理器集合
     */
    public SuperHandler<T> clearHandlers(List<ExtraHandler> handlers) {
        for (ExtraHandler handler : handlers) {
            Set<Integer> posSet = locateMap.get(handler);
            for (int index : posSet) {
                if (index < extraHandlers.size() && handler == extraHandlers.get(index)) {
                    extraHandlers.remove(index);
                }
            }
        }
        return this;
    }

    public synchronized void setPending(boolean pending) {
        this.mPending = pending;
        if (!this.mPending) {
            this.mPendingMsg.clear();
        }
    }

    /**
     * 挂起，不处理任务
     */
    public synchronized void suspend() {
        this.mAlive = false;
    }

    /**
     * 再次唤醒
     */
    public synchronized boolean awake() {
        if (null != mWeakReference.get()) {
            this.mAlive = true;
            for (Message message : mPendingMsg) {
                sendMessage(message);
            }
        }
        this.mPendingMsg.clear();

        return this.mAlive;
    }

    /**
     * 完全退出，清除所有处理器
     */
    public synchronized void exit() {
        this.mExited = true;
        suspend();
        this.mPendingMsg.clear();
        clearHandlers();
        removeCallbacksAndMessages(null);
        mWeakReference.clear();
    }

    /**
     * 判断是否活动状态
     */
    public boolean isAlive() {
        return this.mAlive;
    }

    /**
     * 检查样本重复性
     * @param samples   样本
     * @return
     */
    public static <T> boolean checkDuplicate(T... samples) {
        Set<T> set = new HashSet<>(samples.length);
        for (T sample : samples) {
            if (set.contains(sample)) {
                return true;
            }
        }
        return false;
    }

}
