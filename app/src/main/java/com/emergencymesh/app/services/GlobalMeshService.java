package com.emergencymesh.app.services;

import android.content.Context;
import com.emergencymesh.app.services.BluetoothMeshService;

/**
 * Singleton manager to maintain a single BluetoothMeshService instance
 * across all activities. This prevents connections from being lost when
 * switching between screens.
 */
public class GlobalMeshService {
    private static GlobalMeshService instance;
    private BluetoothMeshService meshService;
    private Context appContext;

    private GlobalMeshService(Context context) {
        this.appContext = context.getApplicationContext();
        this.meshService = new BluetoothMeshService(appContext);
    }

    public static synchronized GlobalMeshService getInstance(Context context) {
        if (instance == null) {
            instance = new GlobalMeshService(context);
        }
        return instance;
    }

    public BluetoothMeshService getMeshService() {
        return meshService;
    }

    public void startService() {
        if (meshService != null && meshService.isBluetoothEnabled()) {
            meshService.startServer();
        }
    }

    public void stopService() {
        if (meshService != null) {
            meshService.stopServer();
        }
    }

    public void cleanup() {
        if (meshService != null) {
            meshService.cleanup();
            meshService = null;
        }
        instance = null;
    }
}