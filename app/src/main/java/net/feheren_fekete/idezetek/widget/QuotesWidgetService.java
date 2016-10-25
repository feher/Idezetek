package net.feheren_fekete.idezetek.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
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
    private QuotesPreferences mPreferences;
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
        mPreferences = new QuotesPreferences(this);
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mWidgetInfos = new HashMap<>();

        int[] widgetIds = mAppWidgetManager.getAppWidgetIds(new ComponentName(this, QuotesWidgetProvider.class));
        for (int widgetId : widgetIds) {
            WidgetInfo widgetInfo = new WidgetInfo(widgetId, mPreferences.getWidgetBookTitle(widgetId));
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

            mPreferences.setWidgetBookTitle(widgetId, widgetInfo.bookTitle);
            mPreferences.setWidgetQuoteIndex(widgetId, 1);
            Date currentDate = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String currentDateString = dateFormat.format(currentDate);
            mPreferences.setWidgetDateOfLastUpdate(widgetId, currentDateString);

            loadQuotesAndUpdateWidget(widgetInfo);
        }
    }

    private void updateWidget(int widgetId) {
        if (!mWidgetInfos.containsKey(widgetId)) {
            return;
        }

        Log.d(TAG, "Updating widget " + widgetId);
        WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
        String bookTitle = mPreferences.getWidgetBookTitle(widgetId);

        if (widgetInfo.bookTitle.equals(bookTitle)) {
            if (!widgetInfo.areQuotesLoaded) {
                Log.d(TAG, "Loading quotes for widget " + widgetId);
                loadQuotesAndUpdateWidget(widgetInfo);
            } else if (!widgetInfo.quotes.isEmpty()) {
                int quoteIndex = mPreferences.getWidgetQuoteIndex(widgetId);
                if (quoteIndex < 0 || quoteIndex >= widgetInfo.quotes.size()) {
                    quoteIndex = 0;
                }

                String widgetDate = mPreferences.getWidgetDateOfLastUpdate(widgetId);
                String currentDate = getCurrentDate();
                Log.d(TAG, "Date check: current " + currentDate + " vs widget " + widgetDate);
                if (!currentDate.equals(widgetDate)) {
                    quoteIndex = quoteIndex + 1;
                    if (quoteIndex >= widgetInfo.quotes.size()) {
                        quoteIndex = 0;
                    }
                    mPreferences.setWidgetQuoteIndex(widgetId, quoteIndex);
                    mPreferences.setWidgetDateOfLastUpdate(widgetId, currentDate);
                }

                Quote quote = widgetInfo.quotes.get(quoteIndex);
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.quotes_widget);
                views.setTextViewText(R.id.quote_text, Html.fromHtml(quote.getQuote()));
                views.setTextViewText(R.id.quote_author, quote.getAuthor());
                views.setTextViewText(R.id.quote_index, String.valueOf(quoteIndex + 1));

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
        mPreferences.removeWidgetPreferences(widgetId);
        mWidgetInfos.remove(widgetId);
    }

    private void showNextQuote(Intent intent) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetInfos.containsKey(widgetId)) {
            WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
            if (widgetInfo.quotes != null) {
                int quoteIndex = mPreferences.getWidgetQuoteIndex(widgetId);
                int nextQuoteIndex = quoteIndex + 1;
                if (nextQuoteIndex >= widgetInfo.quotes.size()) {
                    nextQuoteIndex = 0;
                }
                mPreferences.setWidgetQuoteIndex(widgetId, nextQuoteIndex);
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
                int quoteIndex = mPreferences.getWidgetQuoteIndex(widgetId);
                int prevQuoteIndex = quoteIndex - 1;
                if (prevQuoteIndex < 0) {
                    prevQuoteIndex = widgetInfo.quotes.size() - 1;
                }
                mPreferences.setWidgetQuoteIndex(widgetId, prevQuoteIndex);
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
                mPreferences.setWidgetBookTitle(widgetId, bookTitle);
                mPreferences.setWidgetQuoteIndex(widgetId, quoteIndex);
                updateWidget(widgetId);
            }
        }
    }

}
