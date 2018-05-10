/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.novo.twilight;

import java.util.prefs.Preferences;

/**
 *
 * @author Eunovo
 */
public class Settings {
    
    private static final String pathName = "/com/novo/twilight";
    private static Preferences prefs = Preferences.userRoot();
    private static Preferences node = prefs.node(pathName);
    
    public static String get(String key, String def){
        return node.get(key, def);
    }
    
    public static void save(String key, String value){
        node.put(key, value);
        //System.out.printf(" key - %s, value - %s \n", key, value);
    }
    
}
