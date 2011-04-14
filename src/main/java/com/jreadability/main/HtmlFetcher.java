/*
 *  Copyright 2011 Peter Karich jetwick_@_pannous_._info
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.jreadability.main;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Peter Karich, jetwick_@_pannous_._info
 */
public class HtmlFetcher {

    private static final Logger logger = LoggerFactory.getLogger(HtmlFetcher.class);

    static {
        Helper.enableCookieMgmt();
        Helper.enableUserAgentOverwrite();
    }

    public static JResult fetchAndExtract(String url, int timeout, boolean resolve) throws Exception {
        if (resolve) {
            // TODO remove time taken to resolve from timeout!
            String resUrl = getResolvedUrl(url, timeout);
            // if resolved url is longer: use it!
            if (resUrl != null && resUrl.trim().length() > url.length())
                url = resUrl;
        }

        JResult result = new ArticleTextExtractor().extractContent(fetchAsString(url, timeout));
        result.setUrl(url);
        String domain = Helper.extractDomain(url, false);

        // some images are relative to root and do not include the url :/
        if (result.getImageUrl().startsWith("/"))
            result.setImageUrl("http://" + domain + result.getImageUrl());

        // some websites do not store favicon links within the page
        if (result.getFaviconUrl().isEmpty())
            result.setFaviconUrl(Helper.getDefaultFavicon(url));

        return result;
    }

    public static String fetchAsString(String urlAsString, int timeout) {
        try {
            URL url = new URL(urlAsString);
            //using proxy may increase latency
            HttpURLConnection hConn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            hConn.setRequestProperty("User-Agent", "Mozilla/5.0 Gecko/20100915 Firefox/3.6.10");

            boolean goose = true;
            if (goose) {
                hConn.setRequestProperty("Accept-Language", "en-us");
                hConn.setRequestProperty("content-charset", "UTF-8");
                hConn.addRequestProperty("Referer", "http://jetwick.com/s");
                // why should we avoid the cache?
                hConn.setRequestProperty("Cache-Control", "max-age=0");
            }

            // on android we got problems because of this
            // so do not allow gzip compression for now
//            hConn.setRequestProperty("Accept-Encoding", "gzip, deflate");
            hConn.setConnectTimeout(timeout);
            hConn.setReadTimeout(timeout);
            InputStream is = hConn.getInputStream();

            if ("gzip".equals(hConn.getContentEncoding()))
                is = new GZIPInputStream(is);

            String enc = Converter.extractEncoding(hConn.getContentType());
//            logger.info("header encoding:" + enc);
            return new Converter().streamToString(is, enc);
        } catch (Exception ex) {
        }
        return "";
    }

    /**
     * On some devices we have to hack:
     * http://developers.sun.com/mobility/reference/techart/design_guidelines/http_redirection.html
     * @return the resolved url if any. Or null if it couldn't resolve the url
     * (within the specified time) or the same url if response code is OK
     */
    public static String getResolvedUrl(String urlAsString, int timeout) {
        try {
            URL url = new URL(urlAsString);
            //using proxy may increase latency
            HttpURLConnection hConn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
            // force no follow

            hConn.setInstanceFollowRedirects(false);
            // the program doesn't care what the content actually is !!
            // http://java.sun.com/developer/JDCTechTips/2003/tt0422.html
            hConn.setRequestMethod("HEAD");
            // default is 0 => infinity waiting
            hConn.setConnectTimeout(timeout);
            hConn.setReadTimeout(timeout);
            hConn.connect();
            int responseCode = hConn.getResponseCode();
            hConn.getInputStream().close();
            if (responseCode == HttpURLConnection.HTTP_OK)
                return urlAsString;

            String loc = hConn.getHeaderField("Location");
            if (responseCode / 100 == 3 && loc != null)
                return loc.replaceAll(" ", "+");

        } catch (Exception ex) {
        }
        return "";
    }
}