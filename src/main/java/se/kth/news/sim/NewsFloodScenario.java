/**
 * 
 */
package se.kth.news.sim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import se.kth.news.sim.compatibility.SimNodeIdExtractor;
import se.kth.news.system.HostMngrComp;
import se.sics.kompics.Init;
import se.sics.kompics.network.Address;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.simulator.adaptor.Operation;
import se.sics.kompics.simulator.adaptor.Operation1;
import se.sics.kompics.simulator.adaptor.distributions.extra.BasicIntSequentialDistribution;
import se.sics.kompics.simulator.events.system.SetupEvent;
import se.sics.kompics.simulator.events.system.StartNodeEvent;
import se.sics.kompics.simulator.network.identifier.IdentifierExtractor;
import se.sics.kompics.simulator.run.LauncherComp;
import se.sics.kompics.simulator.util.GlobalView;
import se.sics.ktoolbox.omngr.bootstrap.BootstrapServerComp;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.overlays.id.OverlayIdRegistry;

/**
 * @author pradeeppeiris
 *
 */
public class NewsFloodScenario {

	static Operation<SetupEvent> systemSetupOp = new Operation<SetupEvent>() {
		@Override
		public SetupEvent generate() {
			return new SetupEvent() {
				@Override
				public void setupSystemContext() {
					OverlayIdRegistry.registerPrefix("newsFloodApp", ScenarioSetup.overlayOwner);
				}

				@Override
				public IdentifierExtractor getIdentifierExtractor() {
					return new SimNodeIdExtractor();
				}
				
				@Override
				public void setupGlobalView(GlobalView gv) {
					gv.setValue("simulation.news", new HashSet<String>());
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
                    return NewsFloodObserver.class;
                }
                
                @Override
                public Init getComponentInit() {
                    return new NewsFloodObserver.Init(100, 2);
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
                    selfAdr = ScenarioSetup.observerServer;
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
	
	static Operation1<StartNodeEvent, Integer> startNodeOp = new Operation1<StartNodeEvent, Integer>() {
		@Override
        public StartNodeEvent generate(final Integer nodeId) {
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
					return new HostMngrComp.Init(selfAdr, ScenarioSetup.bootstrapServer, ScenarioSetup.newsOverlayId,
							nodeId == 1 ? true : false);
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

		 
	public static SimulationScenario newsCoverage() {
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
                        raise(100, startNodeOp, new BasicIntSequentialDistribution(1));
                    }
                };
                
	            systemSetup.start();
	            observer.startAfterTerminationOf(0, systemSetup);
	            startBootstrapServer.startAfterTerminationOf(1000, observer);
	            startPeers.startAfterTerminationOf(1000, startBootstrapServer);
                terminateAfterTerminationOf(1000*1000, startPeers);
			}
		};

		return scen;
	}

	public static void main(String[] args) {
		SimulationScenario.setSeed(ScenarioSetup.scenarioSeed);
		SimulationScenario simpleBootScenario = NewsFloodScenario.newsCoverage();
		simpleBootScenario.simulate(LauncherComp.class);
	}
}
