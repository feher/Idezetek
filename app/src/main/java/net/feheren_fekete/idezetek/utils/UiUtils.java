package net.feheren_fekete.idezetek.utils;

import android.content.Context;
import android.support.annotation.StringRes;
import android.view.Gravity;
import android.widget.Toast;


public class UiUtils {

    public static void showToastAtCenter(Context context, @StringRes int message, int duration) {
        Toast toast = Toast.makeText(context, message, duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

}
