package com.leotesla.dao.http;

import android.support.annotation.NonNull;

import com.leotesla.httpclient.ClientError;
import com.leotesla.httpclient.internal.HttpRequest;


/**
 * 客户端处理器
 *
 * @version 1.0
 *
 * Created by Leo on 10/10/2017.
 */
public interface HttpClientHandler {

    /**
     * 在发射前配置请求
     * @param request   请求体
     */
    void onPreRequest(@NonNull HttpRequest request);

    /**
     * 最终错误处理
     * @param clientError   错误体
     */
    void onHandleError(@NonNull ClientError clientError);

}