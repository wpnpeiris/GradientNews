/**
 * 
 */
package se.kth.news.core.news;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;
import se.kth.news.core.news.util.NewsView;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Handler;
import se.sics.kompics.network.Transport;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.util.identifiable.Identifier;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.KContentMsg;
import se.sics.ktoolbox.util.network.KHeader;
import se.sics.ktoolbox.util.network.basic.BasicContentMsg;
import se.sics.ktoolbox.util.network.basic.BasicHeader;

/**
 * @author pradeeppeiris
 *
 */
public class CroupierNewsComp extends NewsComp {
	
	private static final Logger LOG = LoggerFactory.getLogger(CroupierNewsComp.class);
    private String logPrefix = " ";
    
	public CroupierNewsComp(Init init) {
		super(new NewsComp.Init(init.selfAdr, init.gradientOId, init.newItemDAO));
		
		subscribe(handleCroupierSample, croupierPort);
		subscribe(handleNewsItem, networkPort);
	}
	
	Handler handleCroupierSample = new Handler<CroupierSample<NewsView>>() {
        @Override
        public void handle(CroupierSample<NewsView> castSample) {
        	if (castSample.publicSample.isEmpty()) {
                return;
            }
        	
        	floodNewsItems(castSample);
        }
    };
    
    private void floodNewsItems(CroupierSample<NewsView> castSample) {
    	Set<Identifier> croupierSet = castSample.publicSample.keySet();
    	
    	List<NewsItem> newsItems = newItemDAO.getAll();
		for(NewsItem newsItem : newsItems) {
			newsItem.reduceTtl();
			if(newsItem.getTtl() > 0) {
				for(Identifier key: croupierSet) {
					disseminateNewsItem(castSample.publicSample.get(key).getSource(), newsItem.copy());
				}
			}
		}
    }
    
    private void disseminateNewsItem(KAddress neighbour, NewsItem newsItem) {
    	KHeader header = new BasicHeader(selfAdr, neighbour, Transport.UDP);
		KContentMsg msg = new BasicContentMsg(header, newsItem);
		trigger(msg, networkPort);
    }
    
	ClassMatchedHandler handleNewsItem = new ClassMatchedHandler<NewsItem, KContentMsg<?, ?, NewsItem>>() {

		@Override
		public void handle(NewsItem newsItem, KContentMsg<?, ?, NewsItem> container) {
			LOG.info("{} received {} from:{}", logPrefix, newsItem, container.getHeader().getSource());
			updateGlobalNumMessagesView();
			if (newItemDAO.cotains(newsItem)) {
				LOG.debug("{} received {} already exists", logPrefix, newsItem);
			} else {
				newItemDAO.save(newsItem);
				updateGlobalNewsCoverageView();
				updateGlobalNodeKnowlegeView();
			}

		}
	};
	
	private void updateGlobalNewsCoverageView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Set<String> data = gv.getValue("simulation.news_coverage", HashSet.class) ;
        data.add(selfAdr.getId().toString());
        gv.setValue("simulation.news_coverage", data);
    }
    
    private void updateGlobalNodeKnowlegeView() {
    	GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
    	Map<String, Integer> data = gv.getValue("simulation.node_knowlege", HashMap.class) ;
    	data.put(selfAdr.getId().toString(), newItemDAO.size());
        gv.setValue("simulation.node_knowlege", data);
    }
    
	private void updateGlobalNumMessagesView() {
		GlobalView gv = config().getValue("simulation.globalview", GlobalView.class);
		gv.setValue("simulation.num_messages", gv.getValue("simulation.num_messages", Integer.class) + 1) ;
	}

	public static class Init extends se.sics.kompics.Init<CroupierNewsComp> {

        public final KAddress selfAdr;
        public final Identifier gradientOId;
        public final INewsItemDAO newItemDAO;

        public Init(KAddress selfAdr, Identifier gradientOId, INewsItemDAO newItemDAO) {
            this.selfAdr = selfAdr;
            this.gradientOId = gradientOId;
            this.newItemDAO = newItemDAO;
        }
    }
}
