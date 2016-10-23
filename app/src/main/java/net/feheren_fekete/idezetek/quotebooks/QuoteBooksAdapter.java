package net.feheren_fekete.idezetek.quotebooks;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.feheren_fekete.idezetek.R;
import net.feheren_fekete.idezetek.model.Book;
import net.feheren_fekete.idezetek.model.DataModel;

import java.util.ArrayList;
import java.util.List;

import bolts.Task;

public class QuoteBooksAdapter extends RecyclerView.Adapter<QuoteBooksAdapter.ItemViewHolder> {

    public interface Listener {
        void onItemsLoaded();
        void onItemClicked(Book book);
        void onItemLongClicked(Book book);
    }

    public class ItemViewHolder extends RecyclerView.ViewHolder {
        public LinearLayout rootLayout;
        public TextView title;
        public ItemViewHolder(View view) {
            super(view);
            rootLayout = (LinearLayout) view.findViewById(R.id.root_layout);
            title = (TextView) view.findViewById(R.id.book_title_text);
        }
    }

    private DataModel mDataModel;
    private List<Book> mBooks;
    private @Nullable Listener mListener;

    public QuoteBooksAdapter(DataModel dataModel) {
        mDataModel = dataModel;
        mBooks = new ArrayList<>();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void loadItems() {
        Task.callInBackground(() -> {
            mDataModel.initDefaultQuotes();
            return mDataModel.loadBooks();
        }).continueWith((task) -> {
            mBooks = task.getResult();
            notifyDataSetChanged();
            if (mListener != null) {
                mListener.onItemsLoaded();
            }
            return null;
        }, Task.UI_THREAD_EXECUTOR);
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.book_item, parent, false);
        return new ItemViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        final Book book = mBooks.get(position);
        holder.title.setText(book.getTitle());
        holder.rootLayout.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onItemClicked(book);
            }
        });
        holder.rootLayout.setOnLongClickListener((view) -> {
            if (mListener != null) {
                mListener.onItemLongClicked(book);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mBooks.size();
    }

}
