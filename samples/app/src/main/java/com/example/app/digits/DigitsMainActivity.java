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

package com.example.app.digits;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.Digits;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsSession;

import com.example.app.R;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.User;

public class DigitsMainActivity extends Activity {
    private AuthCallback callback;
    private DigitsAuthButton digitsAuthButton;
    private Button findFriendsButton;
    private Button clearSessionButton;
    private Button verifyCredentialsButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.digits_activity_main);
        final TextView userIdView = findViewById(R.id.user_id);
        final TextView tokenView = findViewById(R.id.token);
        final TextView secretView =  findViewById(R.id.secret);
        clearSessionButton = findViewById(R.id.clear_session_button);
        clearSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Digits.getSessionManager().clearActiveSession();
                userIdView.setText("");
                tokenView.setText("");
                secretView.setText("");
            }
        });

        callback = new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                Toast.makeText(DigitsMainActivity.this,
                        "Authentication Successful for " + phoneNumber, Toast.LENGTH_SHORT).show();
                userIdView.setText(getString(R.string.user_id, session.getId()));
                if (session.getAuthToken() instanceof TwitterAuthToken) {
                    final TwitterAuthToken authToken = (TwitterAuthToken) session.getAuthToken();
                    tokenView.setText(getString(R.string.token, authToken.token));
                    secretView.setText(getString(R.string.secret, authToken.secret));
                }
            }

            @Override
            public void failure(DigitsException error) {
                Toast.makeText(DigitsMainActivity.this, error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        //Example of how the Digits button works
        digitsAuthButton = (DigitsAuthButton) findViewById(R.id.signup_button);
        digitsAuthButton.setCallback(callback);
        digitsAuthButton.setAuthTheme(R.style.LightTheme);

        verifyCredentialsButton =  findViewById(R.id.verify_credentials_button);
        verifyCredentialsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final DigitsSession session = Digits.getSessionManager().getActiveSession();
                if (session != null) {
                    TwitterCore.getInstance().getApiClient(session).getAccountService()
                            .verifyCredentials(null, null,
                                    new Callback<User>() {

                                        @Override
                                        public void success(Result<User> result) {
                                            Toast.makeText(DigitsMainActivity.this,
                                                    "Successfully verified credentials",
                                                    Toast.LENGTH_LONG).show();
                                        }

                                        @Override
                                        public void failure(TwitterException exception) {
                                            Toast.makeText(DigitsMainActivity.this,
                                                    "Failed to verify credentials",
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                }
            }
        });

        findFriendsButton = (Button) findViewById(R.id.find_your_friends_button);
        findFriendsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Digits.getInstance().getContactsClient().startContactsUpload();
            }
        });
    }
}
