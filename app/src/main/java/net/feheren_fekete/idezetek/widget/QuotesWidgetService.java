package net.feheren_fekete.idezetek.widget;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.RemoteViews;

import net.feheren_fekete.idezetek.QuotesActivity;
import net.feheren_fekete.idezetek.QuotesPreferences;
import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.model.Quote;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class QuotesWidgetService extends Service {

    private static final String TAG = QuotesWidgetService.class.getSimpleName();

    public static final String ACTION_UPDATE_WIDGETS = QuotesWidgetService.class.getCanonicalName() + ".ACTION_UPDATE_WIDGETS";
    public static final String ACTION_REMOVE_WIDGETS = QuotesWidgetService.class.getCanonicalName() + ".ACTION_REMOVE_WIDGETS";
    public static final String EXTRA_WIDGET_IDS = QuotesWidgetService.class.getCanonicalName() + ".EXTRA_WIDGET_IDS";

    public static final String ACTION_SHOW_NEXT_QUOTE = QuotesWidgetService.class.getCanonicalName() + ".ACTION_SHOW_NEXT_QUOTE";
    public static final String ACTION_SHOW_PREVIOUS_QUOTE = QuotesWidgetService.class.getCanonicalName() + ".ACTION_SHOW_PREVIOUS_QUOTE";
    public static final String EXTRA_WIDGET_ID = QuotesWidgetService.class.getCanonicalName() + ".EXTRA_WIDGET_ID";

    private DataModel mDataModel;
    private QuotesPreferences mPreferences;
    private AppWidgetManager mAppWidgetManager;
    private Map<Integer, WidgetInfo> mWidgetInfos;

    private static final class WidgetInfo {
        int widgetId;
        String quotesTag;
        List<Quote> quotes;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDataModel = new DataModel(this);
        mPreferences = new QuotesPreferences(this);
        mAppWidgetManager = AppWidgetManager.getInstance(this);
        mWidgetInfos = new HashMap<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_UPDATE_WIDGETS.equals(action)) {
            updateWidgets(intent);
        } else if (ACTION_REMOVE_WIDGETS.equals(action)) {
            removeWidgets(intent);
        } else if (ACTION_SHOW_NEXT_QUOTE.equals(action)) {
            showNextQuote(intent);
        } else if (ACTION_SHOW_PREVIOUS_QUOTE.equals(action)) {
            showPreviousQuote(intent);
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateWidgets(Intent intent) {
        int[] widgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
        for (int widgetId : widgetIds) {
            updateWidget(widgetId);
        }
    }

    private void updateWidget(int widgetId) {
        if (mWidgetInfos.containsKey(widgetId)) {
            Log.d(TAG, "Updating widget " + widgetId);
            WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
            String quotesTag = mPreferences.getWidgetQuotesTag(widgetId);

            if (widgetInfo.quotesTag.equals(quotesTag)) {
                if (!widgetInfo.quotes.isEmpty()) {
                    int quoteIndex = mPreferences.getWidgetQuoteIndex(widgetId);
                    if (quoteIndex < 0 || quoteIndex >= widgetInfo.quotes.size()) {
                        quoteIndex = 0;
                    }

                    String widgetDate = mPreferences.getWidgetDateOfLastUpdate(widgetId);
                    Date currentDate = Calendar.getInstance().getTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String currentDateString = dateFormat.format(currentDate);
                    Log.d(TAG, "Date check: current " + currentDateString + " vs widget " + widgetDate);
                    if (!currentDateString.equals(widgetDate)) {
                        quoteIndex = quoteIndex + 1;
                        if (quoteIndex >= widgetInfo.quotes.size()) {
                            quoteIndex = 0;
                        }
                        mPreferences.setWidgetQuoteIndex(widgetId, quoteIndex);
                        mPreferences.setWidgetDateOfLastUpdate(widgetId, currentDateString);
                    }

                    Quote quote = widgetInfo.quotes.get(quoteIndex);
                    RemoteViews views = new RemoteViews(getPackageName(), R.layout.quotes_widget);
                    views.setTextViewText(R.id.quote_text, quote.getQuote());
                    views.setTextViewText(R.id.quote_author, quote.getAuthor());
                    views.setTextViewText(R.id.quote_index, String.valueOf(quoteIndex + 1));

                    Intent openQuotesIntent = new Intent(this, QuotesActivity.class);
                    PendingIntent openQuotesPendingIntent = PendingIntent.getActivity(this, 0, openQuotesIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    views.setOnClickPendingIntent(R.id.quote_content_layout, openQuotesPendingIntent);

                    Intent prevQuoteIntent = new Intent(this, QuotesWidgetService.class);
                    prevQuoteIntent.setAction(ACTION_SHOW_PREVIOUS_QUOTE);
                    prevQuoteIntent.putExtra(EXTRA_WIDGET_ID, widgetId);
                    PendingIntent prevQuotePendingIntent = PendingIntent.getService(this, 1, prevQuoteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    views.setOnClickPendingIntent(R.id.prev_button, prevQuotePendingIntent);

                    Intent nextQuoteIntent = new Intent(this, QuotesWidgetService.class);
                    nextQuoteIntent.setAction(ACTION_SHOW_NEXT_QUOTE);
                    nextQuoteIntent.putExtra(EXTRA_WIDGET_ID, widgetId);
                    PendingIntent nextQuotePendingIntent = PendingIntent.getService(this, 2, nextQuoteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                    views.setOnClickPendingIntent(R.id.next_button, nextQuotePendingIntent);

                    mAppWidgetManager.updateAppWidget(widgetId, views);
                }
            } else {
                Log.d(TAG, "Reloading quotes for widget " + widgetId);
                widgetInfo.quotesTag = quotesTag;
                widgetInfo.quotes.clear();
                loadQuotesAndUpdateWidget(widgetInfo);
            }
        } else {
            Log.d(TAG, "Creating new widget " + widgetId);
            WidgetInfo widgetInfo = new WidgetInfo();
            widgetInfo.widgetId = widgetId;
            widgetInfo.quotesTag = "buddha";
            widgetInfo.quotes = Collections.emptyList();
            mWidgetInfos.put(widgetId, widgetInfo);

            mPreferences.setWidgetQuotesTag(widgetId, widgetInfo.quotesTag);
            mPreferences.setWidgetQuoteIndex(widgetId, 0);
            Date currentDate = Calendar.getInstance().getTime();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String currentDateString = dateFormat.format(currentDate);
            mPreferences.setWidgetDateOfLastUpdate(widgetId, currentDateString);

            loadQuotesAndUpdateWidget(widgetInfo);
        }
    }

    private void loadQuotesAndUpdateWidget(final WidgetInfo widgetInfo) {
        Log.d(TAG, "Loading quotes for widget " + widgetInfo.widgetId);
        Task.callInBackground(new Callable<List<Quote>>() {
            @Override
            public List<Quote> call() {
                return mDataModel.loadQuotes(widgetInfo.quotesTag);
            }
        }).continueWith(new Continuation<List<Quote>, Void>() {
            @Override
            public Void then(Task<List<Quote>> task) {
                widgetInfo.quotes = task.getResult();
                updateWidget(widgetInfo.widgetId);
                return null;
            }
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

    private void showPreviousQuote(Intent intent) {
        int widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetInfos.containsKey(widgetId)) {
            WidgetInfo widgetInfo = mWidgetInfos.get(widgetId);
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
