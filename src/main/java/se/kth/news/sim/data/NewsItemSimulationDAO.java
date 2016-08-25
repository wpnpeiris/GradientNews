/**
 * 
 */
package se.kth.news.sim.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;

/**
 * @author pradeeppeiris
 *
 */
public class NewsItemSimulationDAO implements INewsItemDAO {
	private Map<String, NewsItem> data = new HashMap<String, NewsItem>();
	
	public void save(NewsItem newsItem) {
		data.put(newsItem.getId(), newsItem);
	}

	public NewsItem get(String id) {
		return data.get(id);
	}

	public boolean isEmpty() {
		return data.isEmpty();
	}
	
	public int size() {
		return data.size();
	}
	
	public int getDataSize() {
		return data.size();
	}
	
	@Override
	public List<NewsItem> getAll() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean cotains(NewsItem newsItem){
		return false;
	}
}
