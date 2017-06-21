package edu.berkeley.cs.amplab.carat.android.protocol;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.map.ObjectMapper;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

public class ClickTracking {
    
    private static final String TAG = "ClickTracking";

    private static final String ADDRESS_OLD = "http://data-bakharzy.rhcloud.com/api/app/applications/70dff194-2871-4ad8-9795-3f27f0021713/actions";
    private static final String ADDRESS_VM = "http://86.50.18.40:8080/data/app/applications/f233a990-0421-4d84-b333-d0c93e7f171f/actions";

    public static void track(String user, String name, HashMap<String, String> options, Context c) {
        /*Track only on WIFI for now. See TODO below. */
        // These servers are off-line, so disable click tracking for now.

        /*
        String networkType = SamplingLibrary.getNetworkType(c);
        if (networkType != null && networkType.equals("WIFI")) {
        HttpAsyncTask task = new HttpAsyncTask(user, name, options);
        task.execute(ADDRESS_OLD, ADDRESS_VM);
        }*/
    }

    @SuppressLint("InlinedApi")
    private static String POST(String url, Action action) {
        InputStream inputStream = null;
        String result = "";
        try {

            // 1. create HttpClient
            URL u = new URL(url);
            HttpURLConnection client = (HttpURLConnection) u.openConnection();
            // 2. make POST request to the given URL
            client.setRequestMethod("POST");
            // 7. Set some headers to inform server about the type of the content
            client.setRequestProperty("Accept", "application/json");
            client.setRequestProperty("Content-type", "application/json; charset=UTF-8");

            // 3. build json String using Jackson
            String json = "";
            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(action);
            if (Constants.DEBUG)
                Logger.d(TAG, "JSON=\n" + json);
            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json, "UTF-8");


            // 6. set httpPost Entity
            client.setDoOutput(true);
            client.setDoInput(true);
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            out.writeUTF(se.toString());

            // 9. receive response as inputStream
            DataInputStream in = new DataInputStream(client.getInputStream());
            // 10. convert inputstream to string
            result = in.readUTF();
            in.close();
        } catch (Exception e) {
            String msg = e.getLocalizedMessage();
            if (msg != null)
                Logger.e(TAG, msg);
        }

        // 11. return result
        return result;
    }

    static class HttpAsyncTask extends AsyncTask<String, Void, String> {
        HashMap<String, String> options = null;
        private String name = null;
        private String user = null;

        public HttpAsyncTask(String user, String name, HashMap<String, String> options) {
            this.name = name;
            this.user = user;
            this.options = options;
            // Every event should have time, so add it here.
            options.put("time", System.currentTimeMillis() + "");
        }

        @Override
        protected String doInBackground(String... urls) {

            Action action = new Action();

            /*
             * Toast t = Toast.makeText(context, "execute for uuid="+user+" button="+name+" app="+options.get("app"), Toast.LENGTH_LONG);
             * t.show();
             */
            action.setName(name);
            action.setUsername(user);
            action.setOptions(options);
            String ret = null;
            for (String url : urls) {
                ret = POST(url, action);
            }
            return ret;
        }

        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            //Logger.e("InputStream", result);
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        StringBuilder result = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null)
            result.append(line + "\n");

        inputStream.close();
        return result.toString();
    }
}
