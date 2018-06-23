package com.leotesla.app;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.leotesla.dao.R;
import com.leotesla.dao.http.HttpClientBase;
import com.leotesla.httpclient.ClientError;

public class ActivityLogin extends ActivityBase {

    private EditText mEtUsername, mEtPassword;
    private Button btnLogin;

    private LoginHttpClient mHttpClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mHttpClient = new LoginHttpClient(getApplicationContext(), getHandler());
        setContentView(R.layout.act_login);
        mEtUsername = findViewById(R.id.et_username);
        mEtPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(v -> {
            final String username = mEtUsername.getText().toString().trim();
            final String password = mEtPassword.getText().toString().trim();
            if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
                mHttpClient.getLogin(username, password, new HttpClientBase.PojoCallback<User>() {
                    @Override
                    public void onSuccess(User data) {
                        Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public boolean onError(ClientError error) {
                        Toast.makeText(getApplicationContext(),
                                "登录失败-" + error.getPrettyMsg(), Toast.LENGTH_SHORT).show();
                        return super.onError(error);
                    }
                });
            }
        });
    }
}
