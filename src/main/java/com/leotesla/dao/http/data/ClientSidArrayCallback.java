package com.leotesla.dao.http.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.leotesla.httpclient.ClientErrorHandler;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Sid数据结构使用的数据接收器
 *
 * @version 1.0
 *
 * Created by LeoTesla on 2017/10/6.
 */

public abstract class ClientSidArrayCallback<T extends Serializable> extends CallbackBase<T> {

    private String sid = "";
    private List<T> entity;
    private String extra = "";

    public ClientSidArrayCallback(ClientErrorHandler errorHandler, Object tag) {
        super(errorHandler, tag);
    }

    @Override
    public final boolean onParseData(String data) throws JSONException, NumberFormatException {
        if (null == parsable) {
            throw new RuntimeException("泛型参数不能为空");
        }
        Object object = JSON.parse(data);

        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.isEmpty()) {
                entity = Collections.emptyList();
                // fixme 一种兼容后端正确返回结构
            } else if (jsonObject.containsKey("sid")) {
                sid = jsonObject.getString("sid");
                if (jsonObject.containsKey("results")) {
                    // 解析数据区
                    entity = parsable.parseList(jsonObject.getString("results"));
                    // 去除结果集，其余为额外信息
                    jsonObject.remove("results");
                    jsonObject.remove("sid");
                    extra = jsonObject.toJSONString();
                }
            }
        } else if (object instanceof JSONArray) {
            // fixme 兼容后端在数据集为空时直接返回空数组
            if (((JSONArray) object).isEmpty()) {
                entity = Collections.emptyList();
            }
        }
        if (null != sid && null != entity) {
            post(() -> onSuccess(sid, entity, extra));
        }

        return null != entity;
    }

    /**
     * 成功
     * @param sid   分页标记
     * @param data  主数据集
     * @param extra 可能包含的额外数据信息
     */
    public abstract void onSuccess(@NonNull String sid, @NonNull List<T> data, @Nullable String extra);

}
