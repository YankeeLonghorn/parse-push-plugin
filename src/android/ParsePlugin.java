package org.apache.cordova.core;

import java.util.List;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParsePush;

import android.util.Log;

public class ParsePlugin extends CordovaPlugin {
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_GET_INSTALLATION_ID = "getInstallationId";
    public static final String ACTION_GET_INSTALLATION_OBJECT_ID = "getInstallationObjectId";
    public static final String ACTION_GET_SUBSCRIPTIONS = "getSubscriptions";
    public static final String ACTION_SUBSCRIBE = "subscribe";
    public static final String ACTION_UNSUBSCRIBE = "unsubscribe";

    private static CordovaWebView gWebView;
    private static String gECB;
    private static String gPushOpen;
	private static JSONObject pushData = null;

    public static final String LOGTAG = "ParsePlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	if (action.equals(ACTION_REGISTER)) {
            this.registerDevice(callbackContext, args);
            return true;
        }
        if (action.equals(ACTION_GET_INSTALLATION_ID)) {
            this.getInstallationId(callbackContext);
            return true;
        }

        if (action.equals(ACTION_GET_INSTALLATION_OBJECT_ID)) {
            this.getInstallationObjectId(callbackContext);
            return true;
        }
        if (action.equals(ACTION_GET_SUBSCRIPTIONS)) {
            this.getSubscriptions(callbackContext);
            return true;
        }
        if (action.equals(ACTION_SUBSCRIBE)) {
            this.subscribe(args.getString(0), callbackContext);
            return true;
        }
        if (action.equals(ACTION_UNSUBSCRIBE)) {
            this.unsubscribe(args.getString(0), callbackContext);
            return true;
        }
        return false;
    }

    private void registerDevice(final CallbackContext callbackContext, final JSONArray args) {
    	try {
        	JSONObject jo = args.getJSONObject(0);
            String appId = jo.getString("appId");
            String server = jo.getString("server");

        	//
        	// initialize Parse
            try {
                Parse.initialize(new Parse.Configuration.Builder(cordova.getActivity())
                    .applicationId(appId)
                    .clientKey(null)
                    .server(server)
                    .build()
                );
            } catch (IllegalStateException e) { }
            ParseInstallation.getCurrentInstallation().saveInBackground();

            //
            // register callbacks for notification events
            gECB = jo.optString("ecb");
            gPushOpen = jo.optString("pushOpen");
			if(pushData != null )
			{
				ParsePlugin.javascriptPushOpen( pushData );
			}
            callbackContext.success();
        } catch (JSONException e) {
            callbackContext.error("JSONException: " + e.toString());
        } catch(Exception e){
        	callbackContext.error(e.toString());
        }
    }

    private void getInstallationId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                String installationId = ParseInstallation.getCurrentInstallation().getInstallationId();
                callbackContext.success(installationId);
            }
        });
    }

    private void getInstallationObjectId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                String objectId = ParseInstallation.getCurrentInstallation().getObjectId();
                callbackContext.success(objectId);
            }
        });
    }

    private void getSubscriptions(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
            	List<String> subscriptions = ParseInstallation.getCurrentInstallation().getList("channels");
                callbackContext.success(new JSONArray(subscriptions));
            }
        });
    }

    private void subscribe(final String channel, final CallbackContext callbackContext) {
    	ParsePush.subscribeInBackground(channel);
        callbackContext.success();
    }

    private void unsubscribe(final String channel, final CallbackContext callbackContext) {
    	ParsePush.unsubscribeInBackground(channel);
        callbackContext.success();
    }

    /*
    * Use the cordova bridge to call the jsCB and pass it _json as param
    */
    public static void javascriptECB(JSONObject _json){
    	String snippet = "javascript:" + gECB + "(" + _json.toString() + ")";
    	Log.v(LOGTAG, "javascriptCB: " + snippet);

    	if (gECB != null && !gECB.isEmpty() && gWebView != null) gWebView.sendJavascript(snippet);
    }

    public static void javascriptPushOpen(JSONObject _json) {
		
        String snippet = "javascript:" + gPushOpen + "(" + _json.toString() + ")";
    	Log.v(LOGTAG, "javascriptCB: " + snippet);
		
    	if (gPushOpen != null && !gPushOpen.isEmpty() && gWebView != null) {
			gWebView.sendJavascript(snippet);
			pushData = null;
		}
		else{
			pushData = _json;
		}
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        gECB = null;
        gPushOpen = null;
        gWebView = this.webView;
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	gECB = null;
    	gPushOpen = null;
    	gWebView = null;
    }
}
