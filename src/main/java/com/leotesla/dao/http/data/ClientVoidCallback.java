package com.leotesla.dao.http.data;

import com.leotesla.httpclient.ClientErrorHandler;

/**
 * 不解析数据区结构的接收器
 *
 * @version 1.0
 *
 * Created by LeoTesla on 2017/10/6.
 */

public abstract class ClientVoidCallback extends CallbackBase {

    public ClientVoidCallback(ClientErrorHandler errorHandler, Object tag) {
        super(errorHandler, tag);
    }

    @Override
    public final boolean onParseData(String data) throws NumberFormatException {
        post(this::onSuccess);
        return true;
    }

    /**
     * 成功
     */
    public abstract void onSuccess();

}
