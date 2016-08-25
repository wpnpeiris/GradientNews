/**
 * 
 */
package se.kth.news.core.news.data;

import java.util.List;

/**
 * @author pradeeppeiris
 *
 */
public interface INewsItemDAO {

	public void save(NewsItem newsItem);
	
	public NewsItem get(String id);
	
	public List<NewsItem> getAll();
	
	public boolean isEmpty();
	
	public boolean cotains(NewsItem newsItem);
	
	public int size();
	
	public int getDataSize();
}
