/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.google.maps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import com.google.maps.errors.OverQueryLimitException;
import com.google.maps.internal.ApiConfig;
import com.google.maps.internal.ApiResponse;
import com.google.maps.model.GeocodingResult;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import com.google.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(MediumTests.class)
public class GeoApiContextTest {

  private MockWebServer server;
  private GeoApiContext.Builder builder;

  @Before
  public void Setup() {
    server = new MockWebServer();
    builder = new GeoApiContext.Builder().apiKey("AIza...").queryRateLimit(500);
  }

  @After
  public void Teardown() {
    try {
      server.shutdown();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void setMockBaseUrl() {
    builder.baseUrlForTesting("http://127.0.0.1:" + server.getPort());
  }

  @Test
  public void testGetIncludesDefaultUserAgent() throws Exception {
    // Set up a mock request
    ApiResponse fakeResponse = mock(ApiResponse.class);
    String path = "/";
    Map<String, String> params = new HashMap<String, String>(1);
    params.put("key", "value");

    // Set up the fake web server
    server.enqueue(new MockResponse());
    server.play();
    setMockBaseUrl();

    // Build & execute the request using our context
    builder.build().get(new ApiConfig(path), fakeResponse.getClass(), params).awaitIgnoreError();

    // Read the headers
    server.shutdown();
    RecordedRequest request = server.takeRequest();
    List<String> headers = request.getHeaders();
    boolean headerFound = false;
    for (String header : headers) {
      if (header.startsWith("User-Agent: ")) {
        headerFound = true;
        assertTrue(
            "User agent not in correct format",
            header.matches("User-Agent: GoogleGeoApiClientJava/[^\\s]+"));
      }
    }

    assertTrue("User agent header not present", headerFound);
  }

  @Test
  public void testErrorResponseRetries() throws Exception {
    // Set up mock responses
    MockResponse errorResponse = createMockBadResponse();
    MockResponse goodResponse = createMockGoodResponse();

    server.enqueue(errorResponse);
    server.enqueue(goodResponse);
    server.play();

    // Build the context under test
    setMockBaseUrl();

    // Execute
    GeocodingResult[] result =
        builder.build().get(new ApiConfig("/"), GeocodingApi.Response.class, "k", "v").await();
    assertEquals(1, result.length);
    assertEquals(
        "1600 Amphitheatre Parkway, Mountain View, CA 94043, USA", result[0].formattedAddress);

    server.shutdown();
  }

  @Test(expected = IOException.class)
  public void testSettingMaxRetries() throws Exception {
    MockResponse errorResponse = createMockBadResponse();
    MockResponse goodResponse = createMockGoodResponse();

    // Set up the fake web server
    server.enqueue(errorResponse);
    server.enqueue(errorResponse);
    server.enqueue(errorResponse);
    server.enqueue(goodResponse);
    server.play();
    setMockBaseUrl();

    // This should limit the number of retries, ensuring that the success response is NOT returned.
    builder.maxRetries(2);

    builder.build().get(new ApiConfig("/"), GeocodingApi.Response.class, "k", "v").await();
  }

  private MockResponse createMockGoodResponse() {
    MockResponse response = new MockResponse();
    response.setResponseCode(200);
    response.setBody(
        "{\n"
            + "   \"results\" : [\n"
            + "      {\n"
            + "         \"address_components\" : [\n"
            + "            {\n"
            + "               \"long_name\" : \"1600\",\n"
            + "               \"short_name\" : \"1600\",\n"
            + "               \"types\" : [ \"street_number\" ]\n"
            + "            }\n"
            + "         ],\n"
            + "         \"formatted_address\" : \"1600 Amphitheatre Parkway, Mountain View, "
            + "CA 94043, USA\",\n"
            + "         \"geometry\" : {\n"
            + "            \"location\" : {\n"
            + "               \"lat\" : 37.4220033,\n"
            + "               \"lng\" : -122.0839778\n"
            + "            },\n"
            + "            \"location_type\" : \"ROOFTOP\",\n"
            + "            \"viewport\" : {\n"
            + "               \"northeast\" : {\n"
            + "                  \"lat\" : 37.4233522802915,\n"
            + "                  \"lng\" : -122.0826288197085\n"
            + "               },\n"
            + "               \"southwest\" : {\n"
            + "                  \"lat\" : 37.4206543197085,\n"
            + "                  \"lng\" : -122.0853267802915\n"
            + "               }\n"
            + "            }\n"
            + "         },\n"
            + "         \"types\" : [ \"street_address\" ]\n"
            + "      }\n"
            + "   ],\n"
            + "   \"status\" : \"OK\"\n"
            + "}");

    return response;
  }

  private MockResponse createMockBadResponse() {
    MockResponse response = new MockResponse();
    response.setStatus("HTTP/1.1 500 Internal server error");
    response.setBody("Uh-oh. Server Error.");

    return response;
  }

  @Test(expected = IOException.class)
  public void testRetryCanBeDisabled() throws Exception {
    // Set up 2 mock responses, an error that shouldn't be retried and a success
    MockResponse errorResponse = new MockResponse();
    errorResponse.setStatus("HTTP/1.1 500 Internal server error");
    errorResponse.setBody("Uh-oh. Server Error.");
    server.enqueue(errorResponse);

    MockResponse goodResponse = new MockResponse();
    goodResponse.setResponseCode(200);
    goodResponse.setBody(
        "{\n" + "   \"results\" : [],\n" + "   \"status\" : \"ZERO_RESULTS\"\n" + "}");
    server.enqueue(goodResponse);

    server.play();
    setMockBaseUrl();

    // This should disable the retry, ensuring that the success response is NOT returned
    builder.disableRetries();

    // We should get the error response here, not the success response.
    builder.build().get(new ApiConfig("/"), GeocodingApi.Response.class, "k", "v").await();
  }

  @Test
  public void testRetryEventuallyReturnsTheRightException() throws Exception {
    MockResponse errorResponse = new MockResponse();
    errorResponse.setStatus("HTTP/1.1 500 Internal server error");
    errorResponse.setBody("Uh-oh. Server Error.");

    // Enqueue some error responses.
    for (int i = 0; i < 10; i++) {
      server.enqueue(errorResponse);
    }
    server.play();

    // Wire the mock web server to the context
    setMockBaseUrl();
    builder.retryTimeout(5, TimeUnit.SECONDS);

    try {
      builder.build().get(new ApiConfig("/"), GeocodingApi.Response.class, "k", "v").await();
    } catch (IOException ioe) {
      // Ensure the message matches the status line in the mock responses.
      assertEquals("Server Error: 500 Internal server error", ioe.getMessage());
      return;
    }
    fail("Internal server error was expected but not observed.");
  }

  @Test
  public void testQueryParamsHaveOrderPreserved() throws Exception {
    // This test is important for APIs (such as the speed limits API) where multiple parameters
    // must be provided with the same name with order preserved.

    MockResponse response = new MockResponse();
    response.setResponseCode(200);
    response.setBody("{}");

    server.enqueue(response);
    server.play();

    setMockBaseUrl();
    builder
        .build()
        .get(new ApiConfig("/"), GeocodingApi.Response.class, "a", "1", "a", "2", "a", "3")
        .awaitIgnoreError();

    server.shutdown();
    RecordedRequest request = server.takeRequest();
    String path = request.getPath();
    assertTrue(path.contains("a=1&a=2&a=3"));
  }

  @Test
  public void testToggleIfExceptionIsAllowedToRetry() throws Exception {
    // Enqueue some error responses, although only the first should be used because the response's exception is not
    // allowed to be retried.
    MockResponse overQueryLimitResponse = new MockResponse();
    overQueryLimitResponse.setStatus("HTTP/1.1 400 Internal server error");
    overQueryLimitResponse.setBody(TestUtils.retrieveBody("OverQueryLimitResponse.json"));
    server.enqueue(overQueryLimitResponse);
    server.enqueue(overQueryLimitResponse);
    server.enqueue(overQueryLimitResponse);
    server.play();

    builder.retryTimeout(1, TimeUnit.MILLISECONDS);
    builder.maxRetries(10);
    builder.toggleifExceptionIsAllowedToRetry(OverQueryLimitException.class, false);

    setMockBaseUrl();

    try {
      builder
          .build()
          .get(new ApiConfig("/"), GeocodingApi.Response.class, "any-key", "any-value")
          .await();
    } catch (OverQueryLimitException e) {
      assertEquals(1, server.getRequestCount());
      return;
    }

    fail("OverQueryLimitException was expected but not observed.");
  }
}
