package se.kth.news.core.news.data;

import java.util.HashMap;
import java.util.Map;

public class NewsItemDAO {
	private Map<String, NewsItem> data = new HashMap<String, NewsItem>();
	private static NewsItemDAO instance;
	
	private NewsItemDAO() {
		
	}
	
	public static NewsItemDAO getInstance() {
		if(instance == null) {
			instance = new NewsItemDAO();
		}
		
		return instance;
	}
	
	public void add(NewsItem newsItem) {
		data.put(newsItem.getId(), newsItem);
	}
	
	public NewsItem get(String id) {
		return data.get(id);
	}
	
	public boolean isEmpty() {
		return data.isEmpty();
	}
	
}
