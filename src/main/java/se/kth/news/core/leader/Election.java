/**
 * 
 */
package se.kth.news.core.leader;

/**
 * @author pradeeppeiris
 *
 */
public class Election {
	private int utility;
	
	public Election(int utility) {
		this.utility = utility;
	}

	public int getUtility() {
		return utility;
	}

	public void setUtility(int utility) {
		this.utility = utility;
	}

}
