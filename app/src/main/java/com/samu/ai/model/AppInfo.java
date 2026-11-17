package com.samu.ai.model;

public class AppInfo {
    public final String label;
    public final String packageName;
    public boolean locked;

    public AppInfo(String label, String packageName, boolean locked) {
        this.label = label;
        this.packageName = packageName;
        this.locked = locked;
    }
}
