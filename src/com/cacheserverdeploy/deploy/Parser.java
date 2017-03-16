package com.cacheserverdeploy.deploy;

/**
 * 网络构建
 * @author mindw
 * @date 2017年3月10日
 */
public final class Parser {
	
	public static int edgeIndex = 0;
	
	/** 构建图 */
	public static void buildNetwork(String[] graphContent){
		 
		// 多少个网络节点，多少条链路，多少个消费节点
		String[] line0 = graphContent[0].split(" ");
		/** 网络节点数：不超过1000个 */
		int nodeNum = Integer.parseInt(line0[0]);
		Global.nodeNum = nodeNum;
		Global.graph = new Edge[nodeNum][nodeNum];
	
		/** 链路数：每个节点的链路数量不超过20条，推算出总共不超过20000 */
		int edgeNum = Integer.parseInt(line0[1]);
		Global.edges = new Edge[edgeNum*2];
		
		/** 消费节点数：不超过500个 */
		Global.consumerNum = Integer.parseInt(line0[2]);
		
		// 空行
		
		// 每台部署的成本
		String line2 = graphContent[2];
		Global.depolyCostPerServer = Integer.parseInt(line2);
		
		// 空行
		int lineIndex  = 4;
		String line = null;
		while(!(line=graphContent[lineIndex++]).isEmpty()){	
			buildEdge(line);
		}
		
		// 空行
		
		// 消费节点，相连网络节点ID，视频带宽消耗需求
		for (int index = lineIndex; index < graphContent.length; ++index) {
			line = graphContent[index];
			buildConsumer(line);
		}
		
	}

	/** 
	 * 构建条边 <br>
	 * @param line 每行：链路起始节点ID 链路终止节点ID 总带宽大小 单位网络租用费
	 */
	private static void buildEdge(String line){
		
		String[] strs = line.split(" ");
		
		// 链路起始节点
		int fromNode = Integer.parseInt(strs[0]);
		//  链路终止节点
		int toNode = Integer.parseInt(strs[1]);
		
		// 总带宽大小 
		int bandwidth = Integer.parseInt(strs[2]);
		// 单位网络租用费
		int cost = Integer.parseInt(strs[3]);
		
		// 为每个方向上都建立一条边，创建过程中负责相关连接
		Edge goEdege = new Edge(bandwidth, cost);
		Global.edges[edgeIndex++] = goEdege;
		Global.graph[fromNode][toNode] = goEdege;
		
		Edge backEdge = new Edge(bandwidth, cost);
		Global.edges[edgeIndex++] = backEdge;
		Global.graph[toNode][fromNode] = backEdge;
	
	}
	
	/**
	 * 构建消费节点
	 * @param line 消费节点 相连网络节点ID 视频带宽消耗需求 
	 */
	private static void buildConsumer(String line){
		String[] strs = line.split(" ");
		Integer consumerId = Integer.parseInt(strs[0]);
		int nodeId = Integer.parseInt(strs[1]);
		int demand = Integer.parseInt(strs[2]);
		Global.servers.add(new Server(consumerId,nodeId,demand));
	}
}
