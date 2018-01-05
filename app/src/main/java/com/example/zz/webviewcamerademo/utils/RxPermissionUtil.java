package com.example.zz.webviewcamerademo.utils;

import android.app.Activity;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;

/**
 * 类描述：android 6.0动态权限管理类
 * 创建时间： 2018/1/4 16:00
 */


public class RxPermissionUtil {
        private boolean hasPermission = false;
    private static class InnerClass{
        private static final RxPermissionUtil instance = new RxPermissionUtil();
    }

    public static RxPermissionUtil getInstance(){
        return InnerClass.instance;
    }


    public boolean PermissionRequest(Activity activity,String...permissionName){
        RxPermissions rxPermissions = new RxPermissions(activity);
        rxPermissions.requestEach(permissionName)
                     .subscribe(new Consumer<Permission>() {
                         @Override
                         public void accept(Permission permission) throws Exception {
                             if(permission.granted){
                                 //应用授予权限
                                 return ;
                             }else if(permission.shouldShowRequestPermissionRationale){
                                 //拒绝权限但未点击不在提醒
                                 hasPermission = false;
                             }else {
                                 //拒绝并且提示不在提醒
                                 hasPermission = false;
                             }
                         }
                     });
        return hasPermission;
    }
}
