/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org>
 */

package ch.ethz.geco.gecko.rest;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
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
    private Map<String, String> payload;
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
     * Sets the payload of this request.
     *
     * @param payload the payload
     * @return this
     */
    public RequestBuilder setPayload(Map<String, String> payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Sets this request to ignore SSL certificate errors.
     *
     * @return this
     */
    public RequestBuilder ignoreSSL() {
        ignoreSSL = true;
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
            List<NameValuePair> paramList = new ArrayList<>();
            for (Map.Entry<String, String> param : payload.entrySet()) {
                paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }

            try {
                httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
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
