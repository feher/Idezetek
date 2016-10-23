package net.feheren_fekete.idezetek;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.feheren_fekete.idezetek.model.DataModel;
import net.feheren_fekete.idezetek.model.Quote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;

public class QuotesAdapter extends RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder> {

    public interface Listener {
        void onItemsLoaded();
    }

    public class QuoteViewHolder extends RecyclerView.ViewHolder {
        public TextView quoteText;
        public TextView quoteAuthor;
        public QuoteViewHolder(View view) {
            super(view);
            quoteText = (TextView) view.findViewById(R.id.quote_text);
            quoteAuthor = (TextView) view.findViewById(R.id.quote_author);
        }
    }

    private DataModel mDataModel;
    private List<Quote> mQuotes;
    private List<String> mAuthors;
    private @Nullable Listener mListener;

    public QuotesAdapter(DataModel dataModel) {
        mDataModel = dataModel;
        mQuotes = new ArrayList<>();
        mAuthors = new ArrayList<>();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void loadItems(final String bookTitle) {
        mAuthors.clear();
        Task.callInBackground(() -> {
            mDataModel.initDefaultQuotes();
            return mDataModel.loadQuotes(bookTitle);
        }).continueWith((task) -> {
            mQuotes = task.getResult();
            mAuthors = extractAuthors(mQuotes);
            notifyDataSetChanged();
            if (mListener != null) {
                mListener.onItemsLoaded();
            }
            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    public List<String> getAuthors() {
        return mAuthors;
    }

    @Override
    public QuoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.quote_item, parent, false);
        return new QuoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(QuoteViewHolder holder, int position) {
        Quote quote = mQuotes.get(position);
        holder.quoteText.setText(quote.getQuote());
        if (mAuthors.size() == 1) {
            holder.quoteAuthor.setVisibility(View.GONE);
        } else {
            holder.quoteAuthor.setText(quote.getAuthor());
        }
    }

    @Override
    public int getItemCount() {
        return mQuotes.size();
    }

    private List<String> extractAuthors(List<Quote> quotes) {
        Set<String> authors = new HashSet<>();
        for (Quote quote : quotes) {
            authors.add(quote.getAuthor());
        }
        return new ArrayList<>(authors);
    }
}
