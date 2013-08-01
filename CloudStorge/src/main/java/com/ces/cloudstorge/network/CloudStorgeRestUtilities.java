package com.ces.cloudstorge.network;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * Created by MichaelDai on 13-7-23.
 */
public class CloudStorgeRestUtilities {
    private static final String TAG = "CloudStorgeRestUtilities";

    public static final String PARAM_USERNAME = "username";

    public static final String PARAM_PASSWORD = "password";

    public static final int HTTP_REQUEST_TIMEOUT_MS = 10 * 1000;

    public static final String BASE_URL = "http://rd.114.chinaetek.com:18081";
    //public static final String BASE_URL = "http://172.17.10.61:8081";

    public static final String AUTH_URI = BASE_URL + "/user/login";

    public static final String CREATE_FOLDER_URL = BASE_URL + "/folder/create";

    public static final String ALL_CONTENT_URL = BASE_URL + "/list/all_files";

    public static final String TENANTS_URL = BASE_URL + "/tenants";


    public static HttpClient getHttpClient() {
        HttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        return httpClient;
    }

    public static String authenticate(String username, String password) {
        final HttpResponse resp;
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new IllegalStateException(e);
        }
        Log.i(TAG, "Authenticating to: " + AUTH_URI);
        final HttpPost post = new HttpPost(AUTH_URI);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        try {
            resp = getHttpClient().execute(post);
            String authToken = null;
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                InputStream istream = (resp.getEntity() != null) ? resp.getEntity().getContent()
                        : null;
                if (istream != null) {
                    BufferedReader ireader = new BufferedReader(new InputStreamReader(istream));
                    authToken = ireader.readLine().trim();
                }
            }
            if ((authToken != null) && (authToken.length() > 0)) {
                Log.v(TAG, "Successful authentication");
                return authToken;
            } else {
                Log.e(TAG, "Error authenticating" + resp.getStatusLine());
                return null;
            }
        } catch (final IOException e) {
            Log.e(TAG, "IOException when getting authtoken", e);
            return null;
        } finally {
            Log.v(TAG, "getAuthtoken completing");
        }
    }

    public static JSONObject syncAllContent(String username) {
        try {
            HttpClient resp;
            resp = getHttpClient();
            HttpGet get = new HttpGet(ALL_CONTENT_URL);
            HttpParams params = get.getParams();
            params.setParameter(PARAM_USERNAME, username);
            get.setParams(params);
            HttpResponse response = resp.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                JSONObject result = new JSONObject(EntityUtils.toString(entity));
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int commitAddFolder(int parent_folder_id, String folder_name) {
        final HttpResponse resp;
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("parent_folder_id", parent_folder_id + ""));
        params.add(new BasicNameValuePair("new_folder_name", folder_name));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        final HttpPost post = new HttpPost(CREATE_FOLDER_URL);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        try {
            resp = getHttpClient().execute(post);
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity resultEntity = resp.getEntity();
                JSONObject result = new JSONObject(EntityUtils.toString(resultEntity));
                if (result.getInt("result") != 0)
                    return -1;
                return result.getInt("new_folder_id");
            }
        } catch (final IOException e) {
            return -1;
        } catch (JSONException e) {
            return -1;
        }
        return -1;
    }

    public static JSONObject getTenants() {
        try {
            HttpClient resp;
            resp = getHttpClient();
            HttpGet get = new HttpGet(TENANTS_URL);
            HttpResponse response = resp.execute(get);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                JSONObject result = new JSONObject(EntityUtils.toString(entity));
                return result;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
