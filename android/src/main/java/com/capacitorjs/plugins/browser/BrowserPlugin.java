package com.capacitorjs.plugins.browser;

import android.graphics.Color;
import android.net.Uri;
import android.content.Context;
import android.content.Intent;
import com.getcapacitor.Logger;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.PluginRequestCodes;

@NativePlugin(name = "Browser", requestCodes = {PluginRequestCodes.BROWSER_OPEN_CHROME_TAB})
public class BrowserPlugin extends Plugin {

    private Browser implementation;

    @PluginMethod
    public void load() {
        implementation = new Browser(getContext());
        implementation.setBrowserEventListener(this::onBrowserEvent);
    }

    @PluginMethod
    public void open(PluginCall call) {
        // get the URL
        String urlString = call.getString("url");
        if (urlString == null) {
            call.reject("Must provide a URL to open");
            return;
        }
        if (urlString.isEmpty()) {
            call.reject("URL must not be empty");
            return;
        }
        Uri url;
        try {
            url = Uri.parse(urlString);
        } catch (Exception ex) {
            call.reject(ex.getLocalizedMessage());
            return;
        }

        // get the toolbar color, if provided
        String colorString = call.getString("toolbarColor");
        Integer toolbarColor = null;
        if (colorString != null) try {
            toolbarColor = Color.parseColor(colorString);
        } catch (IllegalArgumentException ex) {
            Logger.error(getLogTag(), "Invalid color provided for toolbarColor. Using default", null);
        }

        // open the browser and finish
        implementation.open(url, toolbarColor);
        call.resolve();
    }

    @PluginMethod
    public void close(PluginCall call) {
        Context context = getActivity().getBaseContext();
        Intent myIntent = new Intent(context, getActivity().getClass());
        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(myIntent);
        call.resolve();
    }

    @Override
    protected void handleOnResume() {
        if (!implementation.bindService()) {
            Logger.error(getLogTag(), "Error binding to custom tabs service", null);
        }
    }

    @Override
    protected void handleOnPause() {
        implementation.unbindService();
    }

    void onBrowserEvent(int event) {
        switch (event) {
            case Browser.BROWSER_LOADED:
                notifyListeners("browserPageLoaded", null);
                break;
            case Browser.BROWSER_FINISHED:
                notifyListeners("browserFinished", null);
                break;
        }
    }
}
