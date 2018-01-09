package edu.berkeley.cs.amplab.carat.android.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class JsonParser {

    private static String TAG = "CaratJSP";
	
	public static String getJSONFromUrl(String url) {
	    String result = null;
		
		try {
            // Set up HTTP post
            HttpURLConnection httpPost = (HttpURLConnection) new URL(url).openConnection();
            httpPost.setRequestMethod("GET");
            httpPost.setDoInput(true);

            final int bufferSize = 1024;
            final char[] buffer = new char[bufferSize];
            final StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(httpPost.getInputStream(), "UTF-8");
            while (true) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
            in.close();
            result = out.toString();
        } catch (UnsupportedEncodingException e) {
            Util.printStackTrace(TAG, e);
        } catch (ProtocolException e) {
            Util.printStackTrace(TAG, e);
        } catch (MalformedURLException e) {
            Util.printStackTrace(TAG, e);
        } catch (IOException e) {
            Util.printStackTrace(TAG, e);
        }

        return result;
	}
}
