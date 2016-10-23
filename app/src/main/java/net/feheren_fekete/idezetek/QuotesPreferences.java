package net.feheren_fekete.idezetek;

import android.content.Context;
import android.content.SharedPreferences;

public class QuotesPreferences {

    private static final String WIDGET_BOOK_TITLE = "WidgetBookTitle";
    private static final String WIDGET_BOOK_TITLE_DEFAULT = "Buddha";

    private static final String WIDGET_QUOTE_INDEX = "WidgetQuoteIndex";
    private static final int WIDGET_QUOTE_INDEX_DEFAULT = 0;

    private static final String WIDGET_DATE_OF_LAST_UPDATE = "WidgetDateOfLastUpdate";
    private static final String WIDGET_DATE_OF_LAST_UPDATE_DEFAULT = "";

    private SharedPreferences mSharedPreferences;

    public QuotesPreferences(Context context) {
        mSharedPreferences = context.getSharedPreferences("QuotesPreferences", Context.MODE_PRIVATE);
    }

    public String getWidgetBookTitle(int widgetId) {
        return mSharedPreferences.getString(
                createWidgetPreferenceKey(WIDGET_BOOK_TITLE, widgetId),
                WIDGET_BOOK_TITLE_DEFAULT);
    }

    public void setWidgetBookTitle(int widgetId, String bookTitle) {
        mSharedPreferences
                .edit()
                .putString(createWidgetPreferenceKey(WIDGET_BOOK_TITLE, widgetId), bookTitle)
                .apply();
    }

    public int getWidgetQuoteIndex(int widgetId) {
        return mSharedPreferences.getInt(
                createWidgetPreferenceKey(WIDGET_QUOTE_INDEX, widgetId),
                WIDGET_QUOTE_INDEX_DEFAULT);
    }

    public void setWidgetQuoteIndex(int widgetId, int quoteIndex) {
        mSharedPreferences
                .edit()
                .putInt(createWidgetPreferenceKey(WIDGET_QUOTE_INDEX, widgetId), quoteIndex)
                .apply();
    }

    public String getWidgetDateOfLastUpdate(int widgetId) {
        return mSharedPreferences.getString(
                createWidgetPreferenceKey(WIDGET_DATE_OF_LAST_UPDATE, widgetId),
                WIDGET_DATE_OF_LAST_UPDATE_DEFAULT);
    }

    public void setWidgetDateOfLastUpdate(int widgetId, String date) {
        mSharedPreferences
                .edit()
                .putString(createWidgetPreferenceKey(WIDGET_DATE_OF_LAST_UPDATE, widgetId), date)
                .apply();
    }

    public void removeWidgetPreferences(int widgetId) {
        mSharedPreferences
                .edit()
                .remove(createWidgetPreferenceKey(WIDGET_BOOK_TITLE, widgetId))
                .remove(createWidgetPreferenceKey(WIDGET_QUOTE_INDEX, widgetId))
                .remove(createWidgetPreferenceKey(WIDGET_DATE_OF_LAST_UPDATE, widgetId))
                .apply();
    }

    private String createWidgetPreferenceKey(String preference, int widgetId) {
        return preference + "_" + widgetId;
    }
}
