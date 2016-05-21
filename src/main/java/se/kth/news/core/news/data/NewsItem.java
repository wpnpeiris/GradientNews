/**
 * 
 */
package se.kth.news.core.news.data;

/**
 * @author pradeeppeiris
 *
 */
public class NewsItem {
	private String id;

	private String news;

	public NewsItem(String id, String news) {
		this.id = id;
		this.news = news;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getNews() {
		return news;
	}

	public void setNews(String news) {
		this.news = news;
	}
}
