package com.leotesla.app;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.leotesla.dao.http.HttpClientBase;

import java.util.HashMap;
import java.util.Map;

public class LoginHttpClient extends HttpClientBase {

    public LoginHttpClient(@NonNull Context context, Handler handler) {
        super(context, handler);
    }

    public void getLogin(@NonNull String username, @NonNull String password,
                         @NonNull PojoCallback<User> callback) {
        Map<String, String> params = new HashMap<>(2);
        params.put("username", username);
        params.put("password", password);
        this.engine.get(generateAPIUrl("login"), params, callback);
    }

}
