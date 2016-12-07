package com.hidecontacts4;

/**
 * Created by Administrator on 2016/11/27.
 */

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import static de.robv.android.xposed.XposedHelpers.findClass;

public class HideContactsModule implements IXposedHookLoadPackage {
    private static HashMap<String, String> procMap = new HashMap<String, String>();
    private static HashSet<String> susPackagesForFake = new HashSet<String>();
    private static HashSet<String> susPackagesForNull = new HashSet<String>();
    ArrayList<String> results = new ArrayList<>();
    static {

        procMap.put("Shanshan Zhang", "Dummy2");
        procMap.put("Zan", "Dummy3");
        procMap.put("1", "0");   //same for numbers
        procMap.put("6","111");
        procMap.put("3","222");
         //ditto
        procMap.put("(301)385-7492", "(222)222-2222");   //ditto

        susPackagesForFake.add("com.shanshan.simplecontacts"); //
//   like this:     susPackagesForFake.add("")

//        susPackagesForNull.add("")  // TODO: add these
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        //if (!lpparam.packageName.equals("com.android.providers.contacts")) {
        //XposedBridge.log("Not Loaded app: " + lpparam.packageName);
        // return;
        //}
        //XposedBridge.log("Loaded app: " + lpparam.packageName);
        String pacName = lpparam.packageName;
        if (!susPackagesForFake.contains(pacName) &&
                !susPackagesForNull.contains(pacName)) {
            XposedBridge.log(pacName + "Not in our suspicious lists, real data");
            return;
        }

        try {
            String[] allCursorClazzes = {

                    "android.database.MatrixCursor",
                    "android.database.CursorWrapper"
            };
            if (susPackagesForFake.contains(pacName)) {
                hookQuery(lpparam);
                // TODO: decide the real parameters e.g. Name, phone_number;
                // Holy interface Cursor
                for (String cursorClazz : allCursorClazzes) {
                    hookCursor(lpparam, cursorClazz, "getString");
                    hookCursor(lpparam, cursorClazz, "getInt");

                }

            } else { // even no fake data, directly null
                // TODO: directly set null in hookQueryForNull(lpparam);
                hookQueryForNull(lpparam);
                for (String cursorClazz : allCursorClazzes) {
                    hookCursorForNull(lpparam, cursorClazz, "getString");
                    hookCursorForNull(lpparam, cursorClazz, "getInt");
                }
            }


        } catch (Throwable t) {
            throw t;
        }


    }

    private void hookQuery(XC_LoadPackage.LoadPackageParam lpparam) {
        final Class<?> cResolver = findClass("android.content.ContentResolver",
                lpparam.classLoader);
        // if (lpparam.packageName.equals("com.skype.raider") || lpparam.equals("com.google.dialer") || lpparam.equals("com.chinatelecom.pim"))
        XposedBridge.log("<--------ENTER ContentResolver----------->: " + lpparam.packageName);
        XposedBridge.hookAllMethods(cResolver, "query", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                String uri = param.args[0].toString().toLowerCase();
                XposedBridge.log("Intercepted uri is = " + uri);
                for (int i = 0; i < param.args.length; i++) {
                    //XposedBridge.log(i + "-th arg: " + param.args[i]);
                }
                if (uri.contains("com.android.contacts") && !uri.contains("profile")) {
                    Uri URI = (Uri) param.args[0];

                    Cursor cursor = (Cursor) param.getResult();
                    int ColumeIndex_ID = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    int ColumeIndex_DISPLAY_NAME = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    int ColumeIndex_HAS_PHONE_NUMBER = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER);
                    XposedBridge.log("Cursor number is = " + cursor.getCount());
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        while (cursor.moveToNext()) {
                            List<String> listColumn = new ArrayList<>();

                            listColumn.addAll(Arrays.asList(cursor.getColumnNames()));
                            String id = cursor.getString(ColumeIndex_ID);
                            String name = cursor.getString(ColumeIndex_DISPLAY_NAME);
                            String has_phone = cursor.getString(ColumeIndex_HAS_PHONE_NUMBER);

                            XposedBridge.log("id is "+ id+ "+name is" + name + "+has phone number is" + has_phone);


                        }
                    } else {
                        XposedBridge.log("Cursor is empty");
                    }

                    List<String> listColumn = new ArrayList<>();
                    listColumn.addAll(Arrays.asList(cursor.getColumnNames()));
                    // XposedBridge.log(result.toString());
                    for (int i = 0; i <= listColumn.size() - 1; i++) {
                        // XposedBridge.log("column is" + listColumn.get(i).toString());
                    }
                }
            }
        });
    }

    private void hookQueryForNull(XC_LoadPackage.LoadPackageParam lpparam) {
        // TODO:
    }

    /*private void hookCursor(XC_LoadPackage.LoadPackageParam lpparam,
                            String methodName, String getParam, String fakeValue) {*/
    private void hookCursor(XC_LoadPackage.LoadPackageParam lpparam, final String clazz, final String methodName) {

        findAndHookMethod(clazz, lpparam.classLoader, methodName, int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                // TODO: use MainActivity to extend, use name or index to check
                String realName = param.getResult().toString();
                if (procMap.containsKey(realName)) {
                    XposedBridge.log(String.format("method %s get called, let's protect real value %s",
                            methodName, realName));
                    XposedBridge.log(String.format("replaced real value %s by %s",
                            realName, procMap.get(realName)));
                    if (methodName.equalsIgnoreCase("getString")) {

                        param.setResult(procMap.get(realName));
                    } else if (methodName.equalsIgnoreCase("getInt")) {
                        param.setResult(Integer.parseInt(procMap.get(realName)));
                    }
                }else{

                    XposedBridge.log(String.format("method %s get called, the real value does not need to be protected %s", methodName, realName));
                }
            }
        });
    }

    private void hookCursorForNull(XC_LoadPackage.LoadPackageParam lpparam, final String clazz, final String methodName) {
        // TODO:
    }
}


