package end.the.supl;

import android.support.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

class ListItem {
    private int id;
    private String url;
    private Date date;
    private String page;

    @Override
    public int hashCode() {
        return ((((((url == null ? 0 : url.hashCode())) * 31 +
                (date == null ? 0 : date.hashCode())) * 31 +
                (page == null ? 0 : page.hashCode())) * 31 +
                id) * 31);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        boolean equals = obj == this;
        if (!equals && obj != null && obj.getClass() == this.getClass())
        {
            final ListItem item = (ListItem)obj;
            equals = item.id == this.id &&
                    (item.url == null ? this.url == null : item.url.equals(this.url)) &&
                    (item.page == null ? this.page == null : item.page.equals(this.page)) &&
                    item.date.compareTo(this.date) == 0;
        }
        return equals;
    }

    public ListItem(int id, String url, Date date, String page) {
        this.id = id;
        this.url = url;
        this.date = date;
        this.page = page;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    @NotNull
    @Override
    public String toString() {
        return "ListItem{" + "id=" + id + ", url='" + url + '\'' + ", date=" + date + ", page=" + page.substring(0, 100) + "...}";
    }
}
