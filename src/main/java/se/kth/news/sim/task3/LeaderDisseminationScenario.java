/*
 * 2016 Royal Institute of Technology (KTH)
 *
 * LSelector is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.news.sim.task3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.kth.news.core.NewsComponentType;
import se.kth.news.core.news.NewsComp;
import se.kth.news.core.news.data.INewsItemDAO;
import se.kth.news.core.news.data.NewsItem;
import se.kth.news.core.news.util.NewsView;
import se.kth.news.sim.GlobalViewControler;
import se.kth.news.sim.ScenarioSetup;
import se.kth.news.sim.compatibility.SimNodeIdExtractor;
import se.kth.news.sim.task1.NewsFloodObserver;
import se.kth.news.system.HostMngrComp;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation2;
import se.sics.kompics.simulator.adaptor.distributions.IntegerUniformDistribution;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.SetupEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor;
import se.sics.kompics.simulator.run.LauncherComp;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.ktoolbox.croupier.event.CroupierSample;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapServerComp;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.overlays.id.OverlayIdRegistry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class LeaderDisseminationScenario {
	protected static final int NUM_NODES = 100;
	
    static Operation<SetupEvent> systemSetupOp = new Operation<SetupEvent>() {
        @Override
        public SetupEvent generate() {
            return new SetupEvent() {
                @Override
                public void setupSystemContext() {
                    OverlayIdRegistry.registerPrefix("newsApp", ScenarioSetup.overlayOwner);
                }

                @Override
                public IdentifierExtractor getIdentifierExtractor() {
                    return new SimNodeIdExtractor();
                }
                
                @Override
				public void setupGlobalView(GlobalView gv) {
                	GlobalViewControler.getInstance().setupGlobalView(gv);
				}
            };
        }
    };

    static Operation startObserverOp = new Operation<StartNodeEvent>() {
		@Override
        public StartNodeEvent generate() {
			return new StartNodeEvent() {
				KAddress selfAdr;

                {
                    selfAdr = ScenarioSetup.bootstrapServer;
                }
                
                @Override
                public Map<String, Object> initConfigUpdate() {
                    HashMap<String, Object> config = new HashMap<>();
                    config.put("newsflood.simulation.checktimeout", 2000);
                    return config;
                }
                
                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return LeaderDisseminationObserver.class;
                }
                
                @Override
                public Init getComponentInit() {
                    return new LeaderDisseminationObserver.Init(LeaderDisseminationObserver.PULL_MODE);
                }
			};
		}
	};
	
    static Operation<StartNodeEvent> startBootstrapServerOp = new Operation<StartNodeEvent>() {

        @Override
        public StartNodeEvent generate() {
            return new StartNodeEvent() {
                KAddress selfAdr;

                {
                    selfAdr = ScenarioSetup.bootstrapServer;
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return BootstrapServerComp.class;
                }

                @Override
                public BootstrapServerComp.Init getComponentInit() {
                    return new BootstrapServerComp.Init(selfAdr);
                }
            };
        }
    };

    static Operation2<StartNodeEvent, Integer, Integer> startNodeOp = new Operation2<StartNodeEvent, Integer, Integer>() {

        @Override
        public StartNodeEvent generate(final Integer nodeId, final Integer numNews) {
            return new StartNodeEvent() {
                KAddress selfAdr;

                {
                    selfAdr = ScenarioSetup.getNodeAdr(nodeId);
                }

                @Override
                public Address getNodeAddress() {
                    return selfAdr;
                }

                @Override
                public Class getComponentDefinition() {
                    return HostMngrComp.class;
                }

                @Override
                public HostMngrComp.Init getComponentInit() {
                    return new HostMngrComp.Init(selfAdr, ScenarioSetup.bootstrapServer, ScenarioSetup.newsOverlayId, new INewsItemDAO() {
                    	public void save(NewsItem newsItem) {
                    	}

                    	public NewsItem get(String id) {
                    		return new NewsItem(id, "Test News", 7);
                    	}
                    	
                    	public List<NewsItem> getAll() {
                    		return new ArrayList<NewsItem>();
                    	}
                    	
                    	public boolean isEmpty() {
                    		return false;
                    	}
                    	
                    	public boolean cotains(NewsItem newsItem) {
                    		return false;
                    		
                    	}
                    	
                    	public int getDataSize() {
                    		return 0;
                    	}
                    	
                    	public int size() {
//                    		Assume number of news items varies according to node id
//                    		i.e. node 10-20 has 10 news items, node 20-30 has 20 items, etc
//                    		And set last 10 nodes with varying number of items, so that always last
//                    		nodes centered in the gradient
                    		int count = 0;
                    		if(Integer.valueOf(selfAdr.getId().toString()) < (NUM_NODES - 10)) {
                    			count = (Integer.valueOf(selfAdr.getId().toString()) / 10) * 10;
                    		} else {
                    			count = (NUM_NODES - 10) + numNews;
                    		}
                    		
                    		return count;
                    	}
                    }, NewsComponentType.GRADIENT_NETWORK);
                }

                @Override
                public Map<String, Object> initConfigUpdate() {
                    Map<String, Object> nodeConfig = new HashMap<>();
                    nodeConfig.put("system.id", nodeId);
                    nodeConfig.put("system.seed", ScenarioSetup.getNodeSeed(nodeId));
                    nodeConfig.put("system.port", ScenarioSetup.appPort);
                    return nodeConfig;
                }
            };
        }
    };

    public static SimulationScenario scenario1() {
        SimulationScenario scen = new SimulationScenario() {
            {
                StochasticProcess systemSetup = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, systemSetupOp);
                    }
                };
                SimulationScenario.StochasticProcess observer = new SimulationScenario.StochasticProcess() {
                    {
                        raise(1, startObserverOp);
                    }
                };
                StochasticProcess startBootstrapServer = new StochasticProcess() {
                    {
                        eventInterArrivalTime(constant(1000));
                        raise(1, startBootstrapServerOp);
                    }
                };
                StochasticProcess startPeers = new StochasticProcess() {
                    {
                        eventInterArrivalTime(uniform(1000, 1100));
                        raise(NUM_NODES, startNodeOp, new BasicIntSequentialDistribution(1), new IntegerUniformDistribution(1, 3, new Random()));
                    }
                };

                systemSetup.start();
                observer.startAfterTerminationOf(1000, systemSetup);
                startBootstrapServer.startAfterTerminationOf(1000, observer);
                startPeers.startAfterTerminationOf(1000, startBootstrapServer);
                terminateAfterTerminationOf(1000*1000, startPeers);
            }
        };

        return scen;
    }
    
    public static void main(String[] args) {
        SimulationScenario.setSeed(ScenarioSetup.scenarioSeed);
        SimulationScenario simpleBootScenario = LeaderDisseminationScenario.scenario1();
        simpleBootScenario.simulate(LauncherComp.class);
    }
}
