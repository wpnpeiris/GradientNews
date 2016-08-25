/**
 * 
 */
package se.kth.news.sim.task3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.news.core.NewsComponentType;
import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;
import se.kth.news.system.HostMngrComp;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.timer.Timer;
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
public class NewsGradientClientComp extends ComponentDefinition {
	private static final Logger LOG = LoggerFactory.getLogger(NewsGradientClientComp.class);
    private String logPrefix = " ";
    
    //*****************************CONNECTIONS**********************************
    Positive<Timer> timerPort = requires(Timer.class);
    Positive<Network> networkPort = requires(Network.class);
    //***************************EXTERNAL_STATE*********************************
	private KAddress selfAdr;
    private KAddress bootstrapServer;
    private Identifier overlayId;
    private INewsItemDAO newItemDAO;
    private boolean initNewsDisseminate;
    
	private Component hostMngrComp;
	
	
	public  NewsGradientClientComp(Init init) {
		selfAdr = init.selfAdr;
        logPrefix = "<nid:" + selfAdr.getId() + ">";
        LOG.info("{}initiating...", logPrefix);
        
        selfAdr = init.selfAdr;
        bootstrapServer = init.bootstrapServer;
        overlayId = init.overlayId;
        newItemDAO = init.newItemDAO;
        initNewsDisseminate = init.initNewsDisseminate;
        
        subscribe(handleStart, control);
	}
	
	Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("{}starting...", logPrefix);
            connectHostMngr();
            
            trigger(Start.event, hostMngrComp.control());
            
            if(initNewsDisseminate) {
            	startNewsDisseminate();
            }
        }
	};
	
	private void startNewsDisseminate() {
		LOG.info("{} starting news disseminate", logPrefix);
		String nodeId = selfAdr.getId().toString();
		KHeader header = new BasicHeader(selfAdr, selfAdr, Transport.UDP);
		KContentMsg msg = new BasicContentMsg(header, new NewsItem("news" + nodeId, "News from " + nodeId));
		trigger(msg, networkPort);
	}
	
	private void connectHostMngr() {
		hostMngrComp = create(HostMngrComp.class, new HostMngrComp.Init(selfAdr, bootstrapServer, overlayId, newItemDAO, NewsComponentType.GRADIENT_NETWORK));
        connect(hostMngrComp.getNegative(Timer.class), timerPort, Channel.TWO_WAY);
        connect(hostMngrComp.getNegative(Network.class), networkPort, Channel.TWO_WAY);
        
        
	}
	public static class Init extends se.sics.kompics.Init<NewsGradientClientComp> {

        public final KAddress selfAdr;
        public final KAddress bootstrapServer;
        public final Identifier overlayId;
        public final INewsItemDAO newItemDAO;
        public final boolean initNewsDisseminate;

        public Init(KAddress selfAdr, KAddress bootstrapServer, Identifier overlayId, INewsItemDAO newItemDAO, boolean initNewsDisseminate) {
            this.selfAdr = selfAdr;
            this.bootstrapServer = bootstrapServer;
            this.overlayId = overlayId;
            this.newItemDAO = newItemDAO;
            this.initNewsDisseminate = initNewsDisseminate;
        }
    }
}
