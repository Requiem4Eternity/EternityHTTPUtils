package com.beenvip.bvpassengergd.EternityHTTPUtil;

import org.json.JSONObject;

/**
 * Created by Administrator on 2017/2/23 0023.
 */

public interface IJSONCallback {
    public void onSuccess(JSONObject json);
    public void onError(int reason);
    public void onNetworkCrashed();
}
