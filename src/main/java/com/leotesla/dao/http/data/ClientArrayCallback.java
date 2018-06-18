package com.leotesla.dao.http.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.leotesla.httpclient.ClientErrorHandler;

import java.io.Serializable;
import java.util.List;
import java.util.MissingFormatArgumentException;

/**
 * Client使用的数据接收器
 *
 * @version 1.0
 *
 * Created by LeoTesla on 2017/10/6.
 */

public abstract class ClientArrayCallback<T extends Serializable> extends CallbackBase<T> {

    private List<T> entity;
    private String extra = "";

    public ClientArrayCallback(ClientErrorHandler errorHandler, Object tag) {
        super(errorHandler, tag);
    }

    @Override
    public final boolean onParseData(String data) throws JSONException, NumberFormatException,
            MissingFormatArgumentException {
        if (null == parsable) {
            throw new RuntimeException("泛型参数不能为空");
        }
        Object object = JSON.parse(data);

        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.containsKey("results")) {
                entity = parsable.parseList(jsonObject.getString("results"));
                jsonObject.remove("results");
                extra = jsonObject.toJSONString();
            } else {
                throw new MissingFormatArgumentException("results");
            }
        } else {
            entity = parsable.parseList(data);
        }
        if (null != entity) {
            post(() -> onSuccess(entity, extra));
        }

        return null != entity;
    }

    /**
     * 成功
     * @param data   主数据集
     * @param extra  附加数据
     */
    public abstract void onSuccess(@NonNull List<T> data, @Nullable String extra);

}
