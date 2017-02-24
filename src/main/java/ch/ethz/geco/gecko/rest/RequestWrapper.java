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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RequestWrapper {
    /**
     * An HttpClient which ignores SSL errors. Use this if you don't care about SSL but have to use https.
     */
    private static HttpClient insecureClient = createSSLIgnore();

    /**
     * A default HttpClient which needs a correctly working SSL configuration on the server-side when using https.
     */
    private static HttpClient secureClient = HttpClients.createDefault();

    /**
     * Performs a GET request without headers on the given URL.
     *
     * @param URL the url to send the request to
     * @return the HttpResponse of the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse getRequest(String URL) throws IOException {
        return getRequest(URL, Collections.emptyMap(), false);
    }

    /**
     * Performs a GET request without headers on the given URL.
     *
     * @param URL     the url to send the request to
     * @param headers the headers of the request
     * @return the HttpResponse of the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse getRequest(String URL, Map<String, String> headers) throws IOException {
        return getRequest(URL, headers, false);
    }

    /**
     * Performs a GET request on the given URL.
     *
     * @param URL       the url to send the request to
     * @param headers   the headers of the request
     * @param ignoreSSL if it should ignore SSL errors
     * @return the HttpResponse of the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse getRequest(String URL, Map<String, String> headers, boolean ignoreSSL) throws IOException {
        HttpGet httpGet = new HttpGet(URL);

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
     * Performs a POST request on the given URL without a header.
     *
     * @param URL       the url to send the request to
     * @param payload   the data to send
     * @return the HttpResponse of the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse postRequest(String URL, Map<String, String> payload) throws IOException {
        return postRequest(URL, Collections.emptyMap(), payload, false);
    }

    /**
     * Performs a POST request on the given URL.
     *
     * @param URL     the url to send the request to
     * @param headers the headers of the request
     * @param payload the data to send
     * @return the HttpResponse of the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse postRequest(String URL, Map<String, String> headers, Map<String, String> payload) throws IOException {
        return postRequest(URL, headers, payload, false);
    }

    /**
     * Performs a POST request on the given URL.
     *
     * @param URL       the url to send the request to
     * @param headers   the headers of the request
     * @param payload   the data to send
     * @param ignoreSSL if it should ignore SSL errors
     * @return the HttpResponse of the request
     * @throws IOException in case of a problem or the connection was aborted
     */
    public static HttpResponse postRequest(String URL, Map<String, String> headers, Map<String, String> payload, boolean ignoreSSL) throws IOException {
        HttpPost httpPost = new HttpPost(URL);

        // Set headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            httpPost.setHeader(header.getKey(), header.getValue());
        }

        // Set payload
        List<NameValuePair> paramList = new ArrayList<>();
        for (Map.Entry<String, String> param : payload.entrySet()) {
            paramList.add(new BasicNameValuePair(param.getKey(), param.getValue()));
        }

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(paramList, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
        SSLContextBuilder sshbuilder = new SSLContextBuilder();
        try {
            sshbuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            e.printStackTrace();
        }
        SSLConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(sshbuilder.build());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        return HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();
    }
}
