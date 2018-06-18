package com.leotesla.dao.http.data;

import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.leotesla.httpclient.ClientError;
import com.leotesla.httpclient.ClientErrorHandler;
import com.leotesla.httpclient.HandlerCallback;
import com.leotesla.httpclient.internal.HttpRequest;
import com.leotesla.httpclient.internal.HttpResponse;

import java.io.Serializable;
import java.util.MissingFormatArgumentException;

/**
 * 数据响应基类
 *
 * @version 1.0
 *
 * Created by LeoTesla on 2017/10/8.
 */

public abstract class CallbackBase<T extends Serializable> extends HandlerCallback<T> {

    // 错误处理器
    private final ClientErrorHandler errorHandler;

    public CallbackBase(ClientErrorHandler errorHandler, Object tag) {
        this(null, errorHandler, tag);
    }

    public CallbackBase(Handler callbackHandler, ClientErrorHandler errorHandler) {
        this(callbackHandler, errorHandler, null);
    }

    @SuppressWarnings("unchecked")
    public CallbackBase(Handler callbackHandler, ClientErrorHandler errorHandler, Object tag) {
        super(callbackHandler, tag);
        this.errorHandler = errorHandler;
    }

    @Override
    @WorkerThread
    public final boolean onHttpSuccess(@NonNull HttpResponse response) {
        ClientError clientError = null;
        try {
            JSONObject jsonObject = JSON.parseObject(response.getContent());
            if (null != jsonObject && jsonObject.containsKey("error")) {
                // 解析数据状态码
                int code = jsonObject.getIntValue("error");
                if (code == 0) {
                    // 成功
                    if (jsonObject.containsKey("data")) {
                        // 解析数据区
                        if (!onParseData(jsonObject.getString("data"))) {
                            clientError = new ClientError(ClientError.EXCEPTION_CLIENT,
                                    ClientError.DATA_EXCEPTION, "Not match parse format");
                        }
                    } else {
                        clientError = new ClientError(ClientError.EXCEPTION_CLIENT,
                                ClientError.DATA_EXCEPTION, "Missing format specifier 'data'");
                    }
                } else {
                    // 其他错误
                    clientError = new ClientError(
                            ClientError.EXCEPTION_SERVER, code, jsonObject.getString("data"));
                }
            } else {
                clientError = new ClientError(
                        ClientError.EXCEPTION_SERVER, ClientError.DATA_EXCEPTION, "null data");
            }
        } catch (NumberFormatException e) {
            clientError = new ClientError(e, ClientError.EXCEPTION_CLIENT,
                    ClientError.DATA_EXCEPTION, "NumberFormatException: " + e.getLocalizedMessage());
        } catch (JSONException e) {
            clientError = new ClientError(e, ClientError.EXCEPTION_CLIENT,
                    ClientError.DATA_EXCEPTION, "JSONException: " + e.getLocalizedMessage());
        } catch (MissingFormatArgumentException e) {
            clientError = new ClientError(e, ClientError.EXCEPTION_CLIENT,
                    ClientError.DATA_EXCEPTION,
                    "MissingFormatArgumentException: " + e.getLocalizedMessage());
        }
        if (null != clientError) {
            dispatchError(clientError);
        }
        return null == clientError;
    }

    @WorkerThread
    @Override
    public final void onHttpFailed(@NonNull HttpResponse response) {
        dispatchError(this.errorHandler.onHttpFailed(response));
    }

    @WorkerThread
    @Override
    public final void onHttpCanceled(@NonNull HttpRequest request) {
        dispatchError(new ClientError(
                ClientError.EXCEPTION_CLIENT, ClientError.NET_EXCEPTION, "CanceledException"));
    }

    @Override
    @SuppressWarnings("all")
    protected void onPostActionError(Exception e) {
        ClientError error = new ClientError(e, ClientError.EXCEPTION_CLIENT,
                ClientError.CODE_EXCEPTION, e.getLocalizedMessage());
        if (!this.errorHandler.onDispatchError(error)) {
            if (onError(error)) error.closed();
            this.errorHandler.onPostError(error);
        }
    }

    /**
     * 转发错误信息
     */
    @WorkerThread
    private void dispatchError(@NonNull ClientError error) {
        if (!this.errorHandler.onDispatchError(error)) {
            post(() -> {
                if (onError(error)) error.closed();
                this.errorHandler.onPostError(error);
            });
        }
    }

    /**
     * 解析数据
     * @param data 数据区字符串
     */
    @SuppressWarnings("null")
    @WorkerThread
    public abstract boolean onParseData(String data) throws JSONException;

    /**
     * 客户端错误响应
     * @param error 错误信息
     * @return      true, 客户端错误处理; false, 默认处理
     */
    @MainThread
    public boolean onError(ClientError error) {
        return false;
    }

}
