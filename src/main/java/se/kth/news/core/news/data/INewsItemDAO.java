/**
 * 
 */
package se.kth.news.core.news.data;

/**
 * @author pradeeppeiris
 *
 */
public interface INewsItemDAO {

	public void add(NewsItem newsItem);
	
	public NewsItem get(String id);
	
	public boolean isEmpty();
	
	public int getCount();
}
