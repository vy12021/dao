package com.leotesla.dao.http;

import android.support.v4.util.ArrayMap;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 响应错误码对应表
 *
 * @version 1.0
 *
 * Created by Leo on 10/10/2017.
 */

public class HttpErrorBook {

    private static final Map<Integer, String> BOOK = new ArrayMap<>();

    /**
     * 更新错误表
     * @param data  错误列表
     */
    static void update(String data) {
        JSONObject errorMap = JSON.parseObject(data);
        if (null != errorMap) {
            for (String key : errorMap.keySet()) {
                BOOK.put(Integer.parseInt(key), errorMap.getString(key));
            }
        }
    }

    /**
     * 获取错误信息
     * @param code  错误码
     * @return      错误信息
     */
    public static String getErrorMsg(int code, String defaultMsg) {
        return BOOK.containsKey(code) ? BOOK.get(code) : defaultMsg;
    }

}
