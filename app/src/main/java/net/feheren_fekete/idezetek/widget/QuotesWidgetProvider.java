package net.feheren_fekete.idezetek.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class QuotesWidgetProvider extends AppWidgetProvider {

    private static final String TAG = QuotesWidgetProvider.class.getSimpleName();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "Updating widgets");
        Intent updateWidgetIntent = new Intent(context, QuotesWidgetService.class);
        updateWidgetIntent.setAction(QuotesWidgetService.ACTION_UPDATE_WIDGETS);
        updateWidgetIntent.putExtra(QuotesWidgetService.EXTRA_WIDGET_IDS, appWidgetIds);
        context.startService(updateWidgetIntent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        Log.d(TAG, "Updating widget");
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        Intent updateWidgetIntent = new Intent(context, QuotesWidgetService.class);
        updateWidgetIntent.setAction(QuotesWidgetService.ACTION_UPDATE_WIDGETS);
        updateWidgetIntent.putExtra(QuotesWidgetService.EXTRA_WIDGET_IDS, new int[]{ appWidgetId });
        context.startService(updateWidgetIntent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.d(TAG, "Deleting widgets");
        super.onDeleted(context, appWidgetIds);

        Intent removeWidgetIntent = new Intent(context, QuotesWidgetService.class);
        removeWidgetIntent.setAction(QuotesWidgetService.ACTION_REMOVE_WIDGETS);
        removeWidgetIntent.putExtra(QuotesWidgetService.EXTRA_WIDGET_IDS, appWidgetIds);
        context.startService(removeWidgetIntent);
    }

}
