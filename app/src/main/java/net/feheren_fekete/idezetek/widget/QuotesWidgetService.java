package net.feheren_fekete.idezetek.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.transition.Visibility;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import net.feheren_fekete.idezetek.QuotesPreferences;
import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.model.Quote;
import net.feheren_fekete.idezetek.quotebooks.QuoteBooksActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import bolts.Task;

public class QuotesWidgetService extends Service implements DataModel.Listener {

    private static final String TAG = QuotesWidgetService.class.getSimpleName();

    public static final String ACTION_UPDATE_ALL_WIDGETS = QuotesWidgetService.class.getCanonicalName() + ".ACTION_UPDATE_ALL_WIDGETS";
    public static final String ACTION_UPDATE_WIDGETS = QuotesWidgetService.class.getCanonicalName() + ".ACTION_UPDATE_WIDGETS";
    public static final String ACTION_REMOVE_WIDGETS = QuotesWidgetService.class.getCanonicalName() + ".ACTION_REMOVE_WIDGETS";
    public static final String EXTRA_WIDGET_IDS = QuotesWidgetService.class.getCanonicalName() + ".EXTRA_WIDGET_IDS";

    public static final String ACTION_ADD_WIDGET = QuotesWidgetService.class.getCanonicalName() + ".ACTION_ADD_WIDGET";
    public static final String ACTION_SHOW_NEXT_QUOTE = QuotesWidgetService.class.getCanonicalName() + ".ACTION_SHOW_NEXT_QUOTE";
    public static final String ACTION_SHOW_PREVIOUS_QUOTE = QuotesWidgetService.class.getCanonicalName() + ".ACTION_SHOW_PREVIOUS_QUOTE";
    public static final String ACTION_SET_QUOTE = QuotesWidgetService.class.getCanonicalName() + ".ACTION_SET_QUOTE";
    public static final String EXTRA_WIDGET_ID = QuotesWidgetService.class.getCanonicalName() + ".EXTRA_WIDGET_ID";
    public static final String EXTRA_BOOK_TITLE = QuotesWidgetService.class.getCanonicalName() + ".EXTRA_BOOK_TITLE";
    public static final String EXTRA_QUOTE_INDEX = QuotesWidgetService.class.getCanonicalName() + ".EXTRA_QUOTE_INDEX";

    private Calendar mCalendar;
    private DataModel mDataModel;
    private QuotesPreferences mQuotesPreferences;
    private SharedPreferences mAppPreferences;
    private AppWidgetManager mAppWidgetManager;
    private Map<Integer, WidgetInfo> mWidgetInfos;

    private static final class WidgetInfo {
        int widgetId;
        String bookTitle;
        List<Quote> quotes;
        boolean areQuotesLoaded;
        WidgetInfo(int widgetId, String bookTitle) {
            this.widgetId = widgetId;
            this.bookTitle = bookTitle;
            this.quotes = Collections.emptyList();
            this.areQuotesLoaded = false;
        }
        void setQuotes(List<Quote> quotes) {
            this.quotes = quotes;
            this.areQuotesLoaded = true;
        }
        void clearQuotes() {
            this.quotes.clear();
            this.areQuotesLoaded = false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mCalendar = Calendar.getInstance();
        mDataModel = new DataModel(this);
        mDataModel.setListener(this);
        mQuotesPreferences = new QuotesPreferences(this);
        mAppPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mWidgetInfos = new HashMap<>();

        int[] widgetIds = mAppWidgetManager.getAppWidgetIds(new ComponentName(this, QuotesWidgetProvider.class));
        for (int widgetId : widgetIds) {
            WidgetInfo widgetInfo = new WidgetInfo(widgetId, mQuotesPreferences.getWidgetBookTitle(widgetId));
            mWidgetInfos.put(widgetId, widgetInfo);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataModel.close();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_ADD_WIDGET.equals(action)) {
                addWidget(intent);
            } else if (ACTION_UPDATE_ALL_WIDGETS.equals(action)) {
                updateAllWidgets();
            } else if (ACTION_UPDATE_WIDGETS.equals(action)) {
                updateWidgets(intent);
            } else if (ACTION_REMOVE_WIDGETS.equals(action)) {
                removeWidgets(intent);
            } else if (ACTION_SHOW_NEXT_QUOTE.equals(action)) {
                showNextQuote(intent);
            } else if (ACTION_SHOW_PREVIOUS_QUOTE.equals(action)) {
                showPreviousQuote(intent);
            } else if (ACTION_SET_QUOTE.equals(action)) {
                setQuote(intent);
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onQuotesChanged(String bookTitle) {
        for (WidgetInfo widgetInfo : mWidgetInfos.values()) {
            if (widgetInfo.bookTitle.equals(bookTitle)) {
                widgetInfo.clearQuotes();
            }
        }
        updateAllWidgets();
    }

    private void updateAllWidgets() {
        int[] widgetIds = mAppWidgetManager.getAppWidgetIds(new ComponentName(this, QuotesWidgetProvider.class));
        Intent intent = new Intent();
        intent.putExtra(EXTRA_WIDGET_IDS, widgetIds);
        updateWidgets(intent);
    }

    private void updateWidgets(Intent intent) {
        int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
        for (int widgetId : widgetIds) {
            updateWidget(widgetId);
        }
    }

    private void addWidget(Intent intent) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        String bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE).trim();
        if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && !bookTitle.isEmpty()) {
            Log.d(TAG, "Creating new widget " + widgetId);
            WidgetInfo widgetInfo = new WidgetInfo(widgetId, bookTitle);
            mWidgetInfos.put(widgetId, widgetInfo);

            mQuotesPreferences.setWidgetBookTitle(widgetId, widgetInfo.bookTitle);
            mQuotesPreferences.setWidgetQuoteIndex(widgetId, 1);
            Date currentDate = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String currentDateString = dateFormat.format(currentDate);
            mQuotesPreferences.setWidgetDateOfLastUpdate(widgetId, currentDateString);

            loadQuotesAndUpdateWidget(widgetInfo);
        }
    }

    private void updateWidget(int widgetId) {
        if (!mWidgetInfos.containsKey(widgetId)) {
            return;
        }

        Log.d(TAG, "Updating widget " + widgetId);
        WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
        String bookTitle = mQuotesPreferences.getWidgetBookTitle(widgetId);

        if (widgetInfo.bookTitle.equals(bookTitle)) {
            if (!widgetInfo.areQuotesLoaded) {
                Log.d(TAG, "Loading quotes for widget " + widgetId);
                loadQuotesAndUpdateWidget(widgetInfo);
            } else if (!widgetInfo.quotes.isEmpty()) {
                int quoteIndex = mQuotesPreferences.getWidgetQuoteIndex(widgetId);
                if (quoteIndex < 0 || quoteIndex >= widgetInfo.quotes.size()) {
                    quoteIndex = 0;
                }

                String widgetDate = mQuotesPreferences.getWidgetDateOfLastUpdate(widgetId);
                String currentDate = getCurrentDate();
                Log.d(TAG, "Date check: current " + currentDate + " vs widget " + widgetDate);
                if (!currentDate.equals(widgetDate)) {
                    quoteIndex = quoteIndex + 1;
                    if (quoteIndex >= widgetInfo.quotes.size()) {
                        quoteIndex = 0;
                    }
                    mQuotesPreferences.setWidgetQuoteIndex(widgetId, quoteIndex);
                    mQuotesPreferences.setWidgetDateOfLastUpdate(widgetId, currentDate);
                }

                Quote quote = widgetInfo.quotes.get(quoteIndex);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.quotes_widget);
                views.setTextViewText(R.id.quote_text, Html.fromHtml(quote.getQuote()));

                boolean showAuthor = mAppPreferences.getBoolean(
                        getResources().getString(R.string.settings_key_show_author), false);
                boolean showQuoteNumber = mAppPreferences.getBoolean(
                        getResources().getString(R.string.settings_key_show_quote_number), false);
                boolean showControlButtons = mAppPreferences.getBoolean(
                        getResources().getString(R.string.settings_key_show_control_buttons), false);

                if (showAuthor) {
                    views.setViewVisibility(R.id.quote_author, View.VISIBLE);
                    views.setTextViewText(R.id.quote_author, quote.getAuthor());
                } else {
                    views.setViewVisibility(R.id.quote_author, View.GONE);
                }

                if (showQuoteNumber) {
                    views.setViewVisibility(R.id.quote_index, View.VISIBLE);
                    views.setTextViewText(R.id.quote_index, String.valueOf(quoteIndex + 1));
                } else {
                    views.setViewVisibility(R.id.quote_index, View.GONE);
                }

                if (showControlButtons) {
                    views.setViewVisibility(R.id.next_button, View.VISIBLE);
                    views.setViewVisibility(R.id.prev_button, View.VISIBLE);

                    Intent prevQuoteIntent = new Intent(this, QuotesWidgetService.class);
                    prevQuoteIntent.setAction(QuotesWidgetService.ACTION_SHOW_PREVIOUS_QUOTE);
                    prevQuoteIntent.putExtra(QuoteBooksActivity.EXTRA_WIDGET_ID, widgetId);
                    PendingIntent prevQuotePendingIntent = PendingIntent.getActivity(
                            this, widgetId + 1, prevQuoteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    views.setOnClickPendingIntent(R.id.prev_button, prevQuotePendingIntent);

                    Intent nextQuoteIntent = new Intent(this, QuotesWidgetService.class);
                    nextQuoteIntent.setAction(QuotesWidgetService.ACTION_SHOW_NEXT_QUOTE);
                    nextQuoteIntent.putExtra(QuoteBooksActivity.EXTRA_WIDGET_ID, widgetId);
                    PendingIntent nextQuotePendingIntent = PendingIntent.getActivity(
                            this, widgetId + 2, nextQuoteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    views.setOnClickPendingIntent(R.id.next_button, nextQuotePendingIntent);
                } else {
                    views.setViewVisibility(R.id.next_button, View.GONE);
                    views.setViewVisibility(R.id.prev_button, View.GONE);
                }

                Intent setQuoteIntent = new Intent(this, QuoteBooksActivity.class);
                setQuoteIntent.setAction(QuoteBooksActivity.ACTION_SET_WIDGET_QUOTE);
                setQuoteIntent.putExtra(QuoteBooksActivity.EXTRA_WIDGET_ID, widgetId);
                PendingIntent openQuotesPendingIntent = PendingIntent.getActivity(
                        this, widgetId, setQuoteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                views.setOnClickPendingIntent(R.id.quote_content_layout, openQuotesPendingIntent);

                mAppWidgetManager.updateAppWidget(widgetId, views);
            }
        } else {
            Log.d(TAG, "Reloading quotes for widget " + widgetId);
            widgetInfo.bookTitle = bookTitle;
            widgetInfo.clearQuotes();
            loadQuotesAndUpdateWidget(widgetInfo);
        }
    }

    private String getCurrentDate() {
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        Date currentDate = mCalendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(currentDate);
    }

    private void loadQuotesAndUpdateWidget(final WidgetInfo widgetInfo) {
        Log.d(TAG, "Loading quotes for widget " + widgetInfo.widgetId);
        Task.callInBackground(() -> {
            return mDataModel.loadQuotes(widgetInfo.bookTitle);
        }).continueWith((task) -> {
            widgetInfo.setQuotes(task.getResult());
            updateWidget(widgetInfo.widgetId);
            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    private void removeWidgets(Intent intent) {
        int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
        for (int widgetId : widgetIds) {
            removeWidget(widgetId);
        }
    }

    private void removeWidget(int widgetId) {
        Log.d(TAG, "Removing widget preferences for widget " + widgetId);
        mQuotesPreferences.removeWidgetPreferences(widgetId);
        mWidgetInfos.remove(widgetId);
    }

    private void showNextQuote(Intent intent) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetInfos.containsKey(widgetId)) {
            WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
            if (widgetInfo.quotes != null) {
                int quoteIndex = mQuotesPreferences.getWidgetQuoteIndex(widgetId);
                int nextQuoteIndex = quoteIndex + 1;
                if (nextQuoteIndex >= widgetInfo.quotes.size()) {
                    nextQuoteIndex = 0;
                }
                mQuotesPreferences.setWidgetQuoteIndex(widgetId, nextQuoteIndex);
                Log.d(TAG, "Next quote " + nextQuoteIndex);
                updateWidget(widgetId);
            }
        }
    }

    private void showPreviousQuote(Intent intent) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetInfos.containsKey(widgetId)) {
            WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
            if (widgetInfo.quotes != null) {
                int quoteIndex = mQuotesPreferences.getWidgetQuoteIndex(widgetId);
                int prevQuoteIndex = quoteIndex - 1;
                if (prevQuoteIndex < 0) {
                    prevQuoteIndex = widgetInfo.quotes.size() - 1;
                }
                mQuotesPreferences.setWidgetQuoteIndex(widgetId, prevQuoteIndex);
                Log.d(TAG, "Prev quote " + prevQuoteIndex);
                updateWidget(widgetId);
            }
        }
    }

    private void setQuote(Intent intent) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetInfos.containsKey(widgetId)) {
            WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
            String bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE);
            int quoteIndex = intent.getIntExtra(EXTRA_QUOTE_INDEX, -1);
            if (!TextUtils.isEmpty(bookTitle) && quoteIndex != -1) {
                if (!widgetInfo.bookTitle.equals(bookTitle)) {
                    widgetInfo.clearQuotes();
                }
                widgetInfo.bookTitle = bookTitle;
                mQuotesPreferences.setWidgetBookTitle(widgetId, bookTitle);
                mQuotesPreferences.setWidgetQuoteIndex(widgetId, quoteIndex);
                updateWidget(widgetId);
            }
        }
    }

}
