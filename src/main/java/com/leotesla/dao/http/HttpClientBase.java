package com.leotesla.dao.http;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.leotesla.dao.http.data.ClientArrayCallback;
import com.leotesla.dao.http.data.ClientCallback;
import com.leotesla.dao.http.data.ClientSidArrayCallback;
import com.leotesla.dao.http.data.ClientVoidCallback;
import com.leotesla.httpclient.ClientError;
import com.leotesla.httpclient.ClientErrorHandler;
import com.leotesla.httpclient.ClientModule;
import com.leotesla.httpclient.DefaultClientCallback;
import com.leotesla.httpclient.data.KeyValuePair;
import com.leotesla.httpclient.internal.CacheConfig;
import com.leotesla.httpclient.internal.HttpException;
import com.leotesla.httpclient.internal.HttpRequest;
import com.leotesla.httpclient.internal.HttpResponse;
import com.leotesla.httpclient.internal.SSLManager;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 项目Http客户端
 *
 * @version 1.0
 *
 * Created by LeoTesla on 2017/10/5.
 */

public class HttpClientBase {

    // 全局异常处理
    private static InternalClientErrorHandler ERROR_HANDLER;
    // 上下文
    protected final Context context;
    // 请求核心
    protected final ClientModule engine;
    // 默认事件回调主线程Handler
    private Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 初始化
     * @param application   应用上下文
     * @param httpClientHandler 错误处理器
     */
    public static void init(@NonNull Application application,
                            @NonNull HttpClientHandler httpClientHandler) {
        // 初始化错误处理器和错误码
        ERROR_HANDLER = new InternalClientErrorHandler(application.getResources(), httpClientHandler);
    }

    /**
     * 常量放到构造器中初始化防止加固泄露
     */
    public HttpClientBase(@NonNull Context context, Handler handler) {
        this.context = context.getApplicationContext();
        if (null != handler)
            this.handler = handler;
        this.engine = new InternalClientModule(this.handler);
    }

    /**
     * 可以在不同客户端实现自定义部分参数逻辑
     * @param request   当前请求
     */
    protected boolean onPreExecute(@NonNull HttpRequest request) {
        return false;
    }

    /**
     * 全局错误处理器
     */
    private final static class InternalClientErrorHandler implements ClientErrorHandler {

        private final Resources resources;
        private final HttpClientHandler httpClientHandler;

        private InternalClientErrorHandler(@NonNull Resources resources,
                                           HttpClientHandler httpClientHandler) {
            this.resources = resources;
            this.httpClientHandler = httpClientHandler;
        }

        @Override
        public ClientError onHttpFailed(HttpResponse response) {
            // http直接响应错误转化为客户端错误类型
            HttpException exception = response.getException();
            ClientError error = new ClientError(exception, ClientError.EXCEPTION_CLIENT,
                    ClientError.IO_EXCEPTION, exception.getLocalizedMessage());
            switch (exception.getType()) {
                case Unknown:
                case Route:
                case Host:
                case Connect:
                case Encode:
                case Url:
                    error = new ClientError(ClientError.EXCEPTION_CLIENT,
                            ClientError.IO_EXCEPTION, exception.getLocalizedMessage());
                    break;
                case Timeout:
                    error = new ClientError(ClientError.EXCEPTION_CLIENT,
                            ClientError.TIMEOUT_EXCEPTION, exception.getLocalizedMessage());
                    break;
                case Params:
                    error = new ClientError(ClientError.EXCEPTION_CLIENT,
                            ClientError.IO_EXCEPTION, exception.getLocalizedMessage());
                    break;
                case SSL:
                    error = new ClientError(ClientError.EXCEPTION_CLIENT,
                            ClientError.SSH_EXCEPTION, exception.getLocalizedMessage());
                    break;
                case NotFound:
                    error = new ClientError(ClientError.EXCEPTION_CLIENT,
                            ClientError.CODE_EXCEPTION + response.getStatusCode(),
                            exception.getLocalizedMessage());
                    break;
                case Server:
                    error = new ClientError(ClientError.EXCEPTION_SERVER,
                            ClientError.CODE_EXCEPTION + response.getStatusCode(),
                            exception.getLocalizedMessage());
                    break;
                case Service:
                    error = new ClientError(ClientError.EXCEPTION_SERVER,
                            response.getStatusCode(), exception.getLocalizedMessage());
                    break;
            }

            return error;
        }

        @Override
        public boolean onDispatchError(ClientError error) {
            String msg = error.getMsg();
            switch (error.getCode()) {
                case ClientError.NET_EXCEPTION:
                    msg = "没网";
                    break;
                case ClientError.TIMEOUT_EXCEPTION:
                    msg = "超市";
                    break;
                case ClientError.SSH_EXCEPTION:
                    msg = "非法ssh连接";
                    break;
                case ClientError.DATA_EXCEPTION:
                case ClientError.URL_EXCEPTION:
                case ClientError.IO_EXCEPTION:
                    msg = "连接失败";
                    break;
                case ClientError.SERVICE_EXCEPTION:
                    msg = "服务器拒绝服务";
                    break;
                default:
                    // 从错误表中查询注入错误信息
                    msg = "自定义错误";
            }
            error.setPrettyMsg(msg);

            return false;
        }

        @Override
        public void onPostError(ClientError error) {
            httpClientHandler.onHandleError(error);
        }

    }

    /**
     * 此类定义了请求的预处理（拦截器机制）注入了配置，注意Handler为模块所在Handler
     */
    private class InternalClientModule extends ClientModule {

        // 服务端头部信息中的etag资源状态标识，内容为Etag:W/"71f-5s1+guZP4QsgdPvsa5n8FPRee/I"
        private final static String RES_ETAG = "ETag";
        // 客户端请求时头部需要携带此标识，值为上次的该请求etag，比如：X-DOUPAI-ETAG-MATCH:W/"71f-5s1+guZP4QsgdPvsa5n8FPRee/I"
        private final static String REQ_ETAG = "X-DOUPAI-ETAG-MATCH";

        private InternalClientModule(Handler handler) {
            super(handler);
        }

        @Override
        public boolean filter(@NonNull HttpRequest request) {
            return true;
        }

        @Override
        public boolean onPreRequest(@NonNull HttpRequest request) {
            try {
                request.cache(true, 1, 1024 * 1024 * 8 * 10,
                        context.getExternalCacheDir() + "/http");
                // 防止重复初始化
                if (!SSLManager.isInitialed(SSLManager.DEFAULT_CERT_HOST)) {
                    request.getConfig().setCerties(new KeyValuePair<>(SSLManager.DEFAULT_CERT_HOST,
                                    context.getAssets().open(SSLManager.DEFAULT_CERT_HOST + ".cer")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ERROR_HANDLER.httpClientHandler.onPreRequest(request);

            return onPreExecute(request);
        }

        @Override
        public boolean onExecuteRequest(@NonNull HttpRequest request) {
            Map<String, List<String>> cacheHead = request.getResponse().getHeader(true);
            if (null != cacheHead) {
                // 缓存中获取到头部信息
                if (cacheHead.containsKey(RES_ETAG)) {
                    request.addHeader(REQ_ETAG, cacheHead.get(RES_ETAG).get(0));
                }
            }
            return false;
        }

        /**
         * 注意此处回调所在工作线程
         * @param response  响应
         */
        @Override
        public boolean onPostResponse(@NonNull HttpResponse response) {
            return false;
        }

    }

    /**
     * 生成api请求链接
     * @param path  相对地址
     */
    protected final String generateAPIUrl(@NonNull String path) {
        return "host" + path;
    }

    /**
     * 不获取任何响应数据请使用此回调类
     */
    public abstract static class VoidCallback extends ClientVoidCallback {

        public VoidCallback() {
            this(null);
        }

        public VoidCallback(Object tag) {
            super(ERROR_HANDLER, tag);
        }

    }

    /**
     * 简单Java对象请使用此回调类
     * @param <T>
     */
    public abstract static class PojoCallback<T extends Serializable> extends ClientCallback<T> {

        public PojoCallback() {
            this(null);
        }

        public PojoCallback(Object tag) {
            super(ERROR_HANDLER, tag);
        }

    }

    /**
     * 集合对象请使用此回调
     * @param <T>
     */
    public abstract static class ArrayCallback<T extends Serializable> extends ClientArrayCallback<T> {

        public ArrayCallback() {
            this(null);
        }

        public ArrayCallback(Object tag) {
            super(ERROR_HANDLER, tag);
        }

    }

    /**
     * Sid分页集合使用此回调
     * @param <T>
     */
    public abstract static class SidArrayCallback<T extends Serializable> extends ClientSidArrayCallback<T> {

        public SidArrayCallback() {
            this(null);
        }

        public SidArrayCallback(Object tag) {
            super(ERROR_HANDLER, tag);
        }

    }

    /**
     * 原数据对象使用此回调
     */
    public abstract static class DefaultCallback extends DefaultClientCallback {

        public DefaultCallback() {
            this(null);
        }

        public DefaultCallback(Object tag) {
            super(ERROR_HANDLER, tag);
        }

    }

    /**
     * 获取错误提示表
     */
    public void getErrorBook() {
        this.engine.get(CacheConfig.create(10, TimeUnit.MINUTES, true),
                generateAPIUrl("config/error_code"), null,
                new PojoCallback<String>() {
            @Override
            public void onSuccess(String data) {
                HttpErrorBook.update(data);
            }
        });
    }

}
