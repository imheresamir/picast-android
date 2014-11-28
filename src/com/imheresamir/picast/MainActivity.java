package com.imheresamir.picast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.internal.XWalkViewInternal;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceResponse;


public class MainActivity extends Activity {
	XWalkView webview;
	XWalkView interceptor;
	boolean asyncTaskComplete;
	AudioManager am;
	
	class MyResourceClient extends XWalkResourceClient {
		boolean ranScript;
		
        MyResourceClient(XWalkView view) {
            super(view);
            ranScript = false;
        }
        
        @Override
        public void onLoadFinished(XWalkViewInternal view, String url) {
        	if(!ranScript) {
        		view.evaluateJavascript("jwplayer().play()", null);
        		ranScript = true;
        	}
        }

        @Override
		public WebResourceResponse shouldInterceptLoadRequest(XWalkView view, String url) {
        	if(url.contains("playlist.m3u8") && url.startsWith("http://dittotv")) {
	    		System.out.println("Got playlist! " + url);
	    		new sendZeetv().execute(url);
	    	} else {
	    	}
	    	
	    	return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        webview = (XWalkView) findViewById(R.id.webview);
        webview.load("http://192.168.1.4:8080/index.html", null);
        
        processExtras();
		
        
    }
    
    protected void onNewIntent(Intent intent) {
    	super.onNewIntent(intent);
    	
    	setIntent(intent);
    	
    	processExtras();
    }

	private void processExtras() {
		Bundle extras;
        String url;
		if ((extras = getIntent().getExtras()) != null) {
			if ((url = extras.getString(Intent.EXTRA_TEXT)) != null) {
				url = url.substring(url.indexOf("http"));
				if (url.contains("zeetv")) {
					interceptor = (XWalkView) findViewById(R.id.interceptor);
					interceptor
							.getSettings()
							.setUserAgentString(
									"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.59 Safari/537.36");
					interceptor.setResourceClient(new MyResourceClient(
							interceptor));

					interceptor.load(url, null);
					am = (AudioManager) this
							.getSystemService(MainActivity.AUDIO_SERVICE);
					am.setStreamMute(AudioManager.STREAM_MUSIC, true);

				} else {
					new sendUrl().execute(url);
				}
			}
		}
	}
    
    private class sendUrl extends AsyncTask<String, Void, Boolean> {
    	protected void onPostExecute(Boolean clean) {
    		if (clean) {
				interceptor.load("about:blank", null);
			}
			if(am != null) {
            	am.setStreamMute(AudioManager.STREAM_MUSIC, false);
            }
    	}
    	
		protected Boolean doInBackground(String... params) {
			// Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://192.168.1.7:8082/media/play");

            try {
                
                //passes the results to a string builder/entity
                StringEntity se;
                
                if(params.length == 1) {
                	se = new StringEntity("{\"Url\":\"" + params[0] + "\"}");
                } else {
                	se = new StringEntity("{\"Url\":\"" + params[0] + "\", \"Data\":\"" + params[1] + "\"}");
                	System.out.println("{\"Url\":\"" + params[0] + "\", \"Data\":\"" + params[1] + "\"}");
                }

                //sets the post request as the resulting string
                httppost.setEntity(se);
                //sets a request header so the page receiving the request
                //will know what to do with it
                httppost.setHeader("Accept", "application/json");
                httppost.setHeader("Content-Type", "application/json");

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
			return (params.length > 1);
		}
    }
    
    private class sendZeetv extends AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			// Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(params[0]);
            String outfile = "";

            try {
                
                HttpResponse response = httpclient.execute(httpget);
                
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                String line;
                while((line = in.readLine()) != null)
                	outfile += line + "\\n";
                outfile = outfile.replace("\"", "\\\"");
                
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }
            
            new sendUrl().execute("zeetv.com", outfile);
            
            
			return null;
		}
    }

   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
