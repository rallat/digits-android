/*
 * Copyright (C) 2015 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.digits.sdk.android;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.SessionManager;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.internal.oauth.OAuth2Service;
import com.twitter.sdk.android.core.internal.oauth.OAuth2Token;

public class DigitsClient {
    public static final String EXTRA_PHONE = "phone_number";
    public static final String EXTRA_RESULT_RECEIVER = "receiver";
    public static final String EXTRA_REQUEST_ID = "request_id";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String THIRD_PARTY_CONFIRMATION_CODE = "third_party_confirmation_code";
    public static final String EXTRA_FALLBACK_REASON = "fallback_reason";
    public static final String EXTRA_TOS_UPDATED = "tos_updated";

    private final OAuth2Service authService;
    private final Digits digits;
    private final SessionManager<DigitsSession> sessionManager;
    private final TwitterCore twitterCore;
    protected DigitsApiProvider digitsApiProvider;


    DigitsClient() {
        this(Digits.getInstance(), TwitterCore.getInstance(), Digits.getSessionManager(),
                new OAuth2Service(TwitterCore.getInstance(), TwitterCore.getInstance()
                        .getSSLSocketFactory(), new DigitsApi()), null);
    }

    DigitsClient(Digits digits, TwitterCore twitterCore, SessionManager<DigitsSession>
            sessionManager, OAuth2Service authService, DigitsApiProvider digitsApiProvider) {

        if (twitterCore == null) {
            throw new IllegalArgumentException("twitter must not be null");
        }
        if (digits == null) {
            throw new IllegalArgumentException("digits must not be null");
        }
        if (sessionManager == null) {
            throw new IllegalArgumentException("sessionManager must not be null");
        }
        if (authService == null) {
            throw new IllegalArgumentException("authService must not be null");
        }

        this.twitterCore = twitterCore;
        this.digits = digits;
        this.sessionManager = sessionManager;
        this.authService = authService;
        this.digitsApiProvider = digitsApiProvider;
    }

    protected void authDevice(Context context, DigitsController controller,
            final String phoneNumber, final Callback<AuthResponse> callback) {

        authService.requestGuestOrAppAuthToken(new DigitsCallback<OAuth2Token>(context,
                controller) {

            @Override
            public void success(Result<OAuth2Token> result) {
                final DigitsSession session = setSession(result);
                digitsApiProvider = new DigitsApiProvider(session, twitterCore.getAuthConfig(),
                        twitterCore.getSSLSocketFactory(), digits.getExecutorService(),
                        new DigitsUserAgent(digits.getVersion(), Build.VERSION.RELEASE));
                digitsApiProvider.getSdkService().auth(phoneNumber, callback);
            }

        });
    }

    private DigitsSession setSession(Result<OAuth2Token> result) {
        final DigitsSession session = new DigitsSession(result.data);
        sessionManager.setSession(DigitsSession.LOGGED_OUT_USER_ID, session);
        return session;
    }

    protected void createAccount(String pin, String phoneNumber, Callback<DigitsUser> listener) {
        digitsApiProvider.getSdkService().account(phoneNumber, pin, listener);
    }

    protected void startSignUp(AuthCallback callback) {
        final DigitsSession session = sessionManager.getActiveSession();
        digits.getScribeService().dailyPing();
        if (session != null && !session.isLoggedOutUser()) {
            callback.success(session, null);
        } else {
            startPhoneNumberActivity(callback, twitterCore.getContext());
        }
    }

    private void startPhoneNumberActivity(AuthCallback callback, Context context) {
        final Intent intent = new Intent(context, digits.getActivityClassManager()
                .getPhoneNumberActivity());
        final Bundle bundle = new Bundle();
        bundle.putParcelable(DigitsClient.EXTRA_RESULT_RECEIVER,
                new LoginResultReceiver(callback, sessionManager));
        intent.putExtras(bundle);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    protected void loginDevice(String requestId, long userId, String code,
            Callback<DigitsSessionResponse> digitsCallback) {
        digitsApiProvider.getSdkService().login(requestId, userId, code, digitsCallback);
    }

    protected void registerDevice(String phoneNumber, Callback<DeviceRegistrationResponse>
            listener) {
        digitsApiProvider.getDeviceService().register(phoneNumber, THIRD_PARTY_CONFIRMATION_CODE,
                true, listener);
    }

    protected void verifyPin(String requestId, long userId, String pin,
            Callback<DigitsSessionResponse> digitsCallback) {
        digitsApiProvider.getSdkService().verifyPin(requestId, userId, pin, digitsCallback);
    }
}
