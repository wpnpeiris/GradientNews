/**
 * 
 */
package se.kth.news.core.news.data;

import se.kth.news.core.news.util.NewsView;

/**
 * @author pradeeppeiris
 *
 */
public class NewsItem {
	private String id;

	private String news;
	
	private int ttl;

	public NewsItem(String id, String news) {
		this.id = id;
		this.news = news;
	}
	
	public NewsItem(String id, String news, int ttl) {
		this.id = id;
		this.news = news;
		this.ttl = ttl;
	}
	
	
	
	public String getId() {
		return id;
	}


	public String getNews() {
		return news;
	}


	public int getTtl() {
		return ttl;
	}


	public void reduceTtl() {
		ttl--;
	}
	
    public NewsItem copy() {
        return new NewsItem(id, news, ttl);
    }
    
    @Override
    public String toString() {
        return "NewsItem<" + id + ", " + news + ", " + ttl + ">";
    }
}
