package com.leotesla.dao.http.data;

import com.alibaba.fastjson.JSONException;
import com.leotesla.httpclient.ClientErrorHandler;

import java.io.Serializable;

/**
 * Client使用的数据接收器，此处完成数据解析
 *
 * @version 1.0
 *
 * created by LeoTesla on 2017/10/6.
 */

public abstract class ClientCallback<T extends Serializable> extends CallbackBase<T> {

    private T entity;

    public ClientCallback(ClientErrorHandler errorHandler, Object tag) {
        super(errorHandler, tag);
    }

    @Override
    public final boolean onParseData(String data) throws JSONException, NumberFormatException {
        if (null == parsable) {
            throw new RuntimeException("泛型参数不能为空");
        }
        entity = parsable.parse(data);
        if (null != entity) {
            post(() -> onSuccess(entity));
        }

        return null != entity;
    }

    /**
     * 成功
     *
     * @param data 数据
     */
    public abstract void onSuccess(T data);

}
