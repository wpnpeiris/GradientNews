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
	
	private boolean stage;

	public NewsItem(String id, String news) {
		this.id = id;
		this.news = news;
	}
	
	public NewsItem(String id, String news, int ttl) {
		this.id = id;
		this.news = news;
		this.ttl = ttl;
	}
	
	public NewsItem(String id, String news, boolean stage) {
		this.id = id;
		this.news = news;
		this.stage = stage;
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
    
    
    public boolean isStage() {
		return stage;
	}

	public void setStage(boolean stage) {
		this.stage = stage;
	}

	@Override
    public String toString() {
        return "NewsItem<" + id + ", " + news + ", " + ttl + ">";
    }
}
