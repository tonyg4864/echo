/*
 * Copyright 2025 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.echo.config;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CustomOkHttpClient {

  public static OkHttpClient create() {
    return new OkHttpClient.Builder().addInterceptor(new RetryInterceptor()).build();
  }

  static class RetryInterceptor implements Interceptor {
    private static final int MAX_RETRIES = 6;

    @Override
    public Response intercept(Chain chain) throws IOException {
      Request request = chain.request();
      Response response = chain.proceed(request);
      int tryCount = 0;

      while (!response.isSuccessful()
          && response.code() == 503
          && tryCount < MAX_RETRIES
          && tryCount * 10 < 60) {
        tryCount++;
        response.close();
        try {
          Thread.sleep(10000); // Wait for 10 seconds before retrying
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Retry interrupted", e);
        }
        response = chain.proceed(request);
      }
      return response;
    }
  }
}
