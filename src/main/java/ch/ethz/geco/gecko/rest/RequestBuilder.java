/*
 *  Stammbot - A Discord Bot for personal use
 *  Copyright (C) 2016 - 2017
 *  *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.ethz.geco.gecko.rest;

import ch.ethz.geco.gecko.ErrorHandler;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestBuilder {
    // TODO: Use static thread-safe HttpClients
    /**
     * An HttpClient which ignores SSL errors. Use this if you don't care about SSL but have to use https.
     */
    private HttpClient insecureClient = createSSLIgnore();

    /**
     * A default HttpClient which needs a correctly working SSL configuration on the server-side when using https.
     */
    private HttpClient secureClient = HttpClients.createDefault();

    /**
     * The request fields
     */
    private String requestURL;
    private Map<String, String> headers;
    private AbstractHttpEntity payload;
    private boolean ignoreSSL = false;

    public RequestBuilder(String requestURL) {
        this.requestURL = requestURL;
    }

    /**
     * Sets the headers of this request.
     *
     * @param headers the headers
     * @return this
     */
    public RequestBuilder setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    /**
     * Adds a header to this request
     *
     * @param name  the name of the header
     * @param value the value of the header
     * @return this
     */
    public RequestBuilder addHeader(String name, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }

        headers.put(name, value);
        return this;
    }

    /**
     * Sets the payload as string.
     *
     * @param payload the payload
     * @return this
     */
    public RequestBuilder setPayload(String payload) {
        this.payload = new StringEntity(payload, "UTF-8");
        return this;
    }

    /**
     * Sets the payload in URL encoded format.
     *
     * @param payload the payload
     * @return this
     */
    public RequestBuilder setPayload(Map<String, String> payload) {
        List<NameValuePair> paramList = new ArrayList<>();
        for (Map.Entry<String, String> param : payload.entrySet()) {
            paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }

        try {
            this.payload = new UrlEncodedFormEntity(paramList, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ErrorHandler.handleError(e);
        }

        return this;
    }

    /**
     * Sets this request to ignore SSL certificate errors.
     *
     * @return this
     */
    public RequestBuilder ignoreSSL() {
        this.ignoreSSL = true;
        return this;
    }

    /**
     * Performs a get request.
     *
     * @return the response
     * @throws IOException
     */
    public HttpResponse get() throws IOException {
        HttpGet httpGet = new HttpGet(requestURL);

        // Set headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            httpGet.setHeader(header.getKey(), header.getValue());
        }

        // Choose the right http client
        HttpClient currentClient;
        if (ignoreSSL) {
            currentClient = insecureClient;
        } else {
            currentClient = secureClient;
        }

        return currentClient.execute(httpGet);
    }

    /**
     * Performs a post request
     *
     * @return the response
     * @throws IOException
     */
    public HttpResponse post() throws IOException {
        HttpPost httpPost = new HttpPost(requestURL);

        // Set headers
        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                httpPost.setHeader(header.getKey(), header.getValue());
            }
        }

        // Set payload
        if (payload != null) {
            httpPost.setEntity(payload);
        }

        // Choose the right http client
        HttpClient currentClient;
        if (ignoreSSL) {
            currentClient = insecureClient;
        } else {
            currentClient = secureClient;
        }

        return currentClient.execute(httpPost);
    }

    /**
     * Creates an HttpClient which ignores certificate errors.
     *
     * @return an HttpClient which ignores SSL errors
     */
    private static HttpClient createSSLIgnore() {
        SSLContextBuilder sslBuilder = new SSLContextBuilder();
        try {
            sslBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
        SSLConnectionSocketFactory sslSF = null;
        try {
            sslSF = new SSLConnectionSocketFactory(sslBuilder.build());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        return HttpClients.custom()
                .setSSLSocketFactory(sslSF)
                .build();
    }
}
