package net.feheren_fekete.idezetek.utils;

import android.content.SharedPreferences;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import net.feheren_fekete.idezetek.AppPreferences;

public class StringUtils {

    public static CharSequence prepareTextWithMarkup(String textWithMarkup, SharedPreferences appPreferences) {
        Spanned spanned = Html.fromHtml(textWithMarkup);
        if (appPreferences.getBoolean(AppPreferences.KEY_APP_ENABLE_MARKUP, false)) {
            return spanned;
        } else {
            char[] chars = new char[spanned.length()];
            TextUtils.getChars(spanned, 0, spanned.length(), chars, 0);
            return new String(chars);
        }
    }

}
