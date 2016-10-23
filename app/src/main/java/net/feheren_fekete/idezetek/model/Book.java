package net.feheren_fekete.idezetek.model;

public class Book {
    private String mTitle;
    private String mFileName;

    public Book(String title, String fileName) {
        mTitle = title;
        mFileName = fileName;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Book)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Book other = (Book) obj;
        return mTitle.equals(other.mTitle);
    }

}
