package com.yunxian.extoolbar.menu;

import android.content.ComponentName;
import android.content.Intent;
import android.support.v4.internal.view.SupportMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.SubMenu;

/**
 * @author A Shuai
 * @email lishuai.ls@alibaba-inc.com
 * @date 2016/12/3 20:02
 */

public class MenuBuilder implements SupportMenu {

    @Override
    public MenuItem add(CharSequence charSequence) {
        return null;
    }

    @Override
    public MenuItem add(int i) {
        return null;
    }

    @Override
    public MenuItem add(int i, int i1, int i2, CharSequence charSequence) {
        return null;
    }

    @Override
    public MenuItem add(int i, int i1, int i2, int i3) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(CharSequence charSequence) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(int i) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(int i, int i1, int i2, CharSequence charSequence) {
        return null;
    }

    @Override
    public SubMenu addSubMenu(int i, int i1, int i2, int i3) {
        return null;
    }

    @Override
    public int addIntentOptions(int i, int i1, int i2, ComponentName componentName, Intent[] intents, Intent intent, int i3, MenuItem[] menuItems) {
        return 0;
    }

    @Override
    public void removeItem(int i) {

    }

    @Override
    public void removeGroup(int i) {

    }

    @Override
    public void clear() {

    }

    @Override
    public void setGroupCheckable(int i, boolean b, boolean b1) {

    }

    @Override
    public void setGroupVisible(int i, boolean b) {

    }

    @Override
    public void setGroupEnabled(int i, boolean b) {

    }

    @Override
    public boolean hasVisibleItems() {
        return false;
    }

    @Override
    public MenuItem findItem(int i) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public MenuItem getItem(int i) {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean performShortcut(int i, KeyEvent keyEvent, int i1) {
        return false;
    }

    @Override
    public boolean isShortcutKey(int i, KeyEvent keyEvent) {
        return false;
    }

    @Override
    public boolean performIdentifierAction(int i, int i1) {
        return false;
    }

    @Override
    public void setQwertyMode(boolean b) {

    }
}
