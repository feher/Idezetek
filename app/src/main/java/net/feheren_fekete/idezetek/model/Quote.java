package net.feheren_fekete.idezetek.model;

public class Quote {
    private String mAuthor;
    private String mQuote;

    public Quote(String author, String quote) {
        mAuthor = author;
        mQuote = quote;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String author) {
        mAuthor = author;
    }

    public String getQuote() {
        return mQuote;
    }

    public void setQuote(String quote) {
        mQuote = quote;
    }
}
