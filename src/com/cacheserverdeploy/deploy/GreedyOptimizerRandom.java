package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 防止在局部最优中出不来
 * 
 * @author mindw
 * @date 2017年3月23日
 */
public class GreedyOptimizerRandom extends GreedyOptimizerMiddle{
	
	private final class NODE implements Comparable<NODE>{
    	int id;
    	int bandWidth;
    	int costPath;
    	int freq;
		@Override
		public int compareTo(NODE o) {
			return o.freq-freq;
		}
    }
	
	public GreedyOptimizerRandom(){
		init();
	}
	

	private ArrayList<Integer> nodes = new ArrayList<Integer>();
	
	private Random random = new Random(47);
	
	private ArrayList<Integer> randomSelectServers() {
		
		ArrayList<Integer> nextServerNodes = new ArrayList<Integer>(Global.consumerNum);
		
		boolean[] selected = new boolean[nodes.size()];
		Arrays.fill(selected, false);
		int leftNum = Global.consumerNum;
		while(leftNum>0){
			int index = random.nextInt(nodes.size());
			if(!selected[index]){
				selected[index] = true;
				nextServerNodes.add(nodes.get(index));
				leftNum--;
			}
		}

		return nextServerNodes;
	}

	
	@Override
	void optimize() {

		if (Global.IS_DEBUG) {
			System.out.println("");
			System.out.println(this.getClass().getSimpleName() + " 开始接管 ");
		}

		if (Global.isTimeOut()) {
			return;
		}

		long t = System.currentTimeMillis();

			
		ArrayList<Integer> oldGlobalServers = randomSelectServers();
		
		int lastCsot = Global.INFINITY;
		
		while (true) {
				
			if (Global.IS_DEBUG) {
				for(int server : oldGlobalServers){
					System.out.print(server+" ");
				}
				System.out.println();
			}

			
			// 可选方案
			int minCost = Global.INFINITY;
			int bestFromNode = -1;
			int bestToNode = -1;

			for (int fromNode : oldGlobalServers) {
				for (int toNode : oldGlobalServers) {
					// 防止自己到自己
					if (fromNode == toNode) {
						continue;
					}

					if (Global.isTimeOut()) {
						return;
					}

					ArrayList<Server> nextGlobalServers = moveInRandom(oldGlobalServers, fromNode, toNode);
					int cost = Global.getTotalCost(nextGlobalServers);
					if (cost < minCost) {
						minCost = cost;
						bestFromNode = fromNode;
						bestToNode = toNode;
					}
				}
			}

			if (minCost == Global.INFINITY) {
				break;
			}

			if (Global.isTimeOut()) {
				return;
			}
			
			// 移动
			ArrayList<Server> nextGlobalServers = moveInRandom(oldGlobalServers, bestFromNode, bestToNode);
			int cost = Global.getTotalCost(nextGlobalServers);
			//boolean better = Global.updateSolution(nextGlobalServers);

			if (cost<lastCsot) {
				Global.updateSolution(nextGlobalServers);
				oldGlobalServers.clear();
				for(Server server : nextGlobalServers){
					oldGlobalServers.add(server.node);
				}
				lastCsot = cost;
			}else{ // not better
				lastCsot = Global.INFINITY;
				oldGlobalServers = randomSelectServers();
			}
			
		}

		if (Global.IS_DEBUG) {
			System.out.println(this.getClass().getSimpleName() + " 禁忌搜索结束，耗时: " + (System.currentTimeMillis() - t));
		}
		
	}
	
	
	/** 进行一步移动 */
	private ArrayList<Server> moveInRandom(ArrayList<Integer> oldGlobalServers, int fromServerNode, int toServerNode) {
		
		Map<Integer, Server> newServers = new HashMap<Integer, Server>();
		for (int serverNode : oldGlobalServers) {
			if (serverNode != fromServerNode) {
				newServers.put(serverNode, new Server(serverNode));
			}
		}
		newServers.put(toServerNode, new Server(toServerNode));

		Server[] consumerServers = Global.getConsumerServer();

		Global.resetEdgeBandWidth();

		return transferServers(consumerServers, newServers);
	}

	
	
	/////////////////////////////


	//private final int N =1010;
	private int nearNodeId;
	private int[] limitBandWidth;
	
	private void init() {
		limitBandWidth = new int[Global.nodeNum];
		NODE[] nodeFreq = new NODE[Global.nodeNum];

		for(int i=0;i<Global.nodeNum;i++){
			nodeFreq[i]= new NODE();
		}
		List<Integer> clientList = new ArrayList<Integer>(Global.consumerNodes.length);
		for(int client: Global.consumerNodes){
			clientList.add(client);
		}
		
		for(int client: Global.consumerNodes){
			nearNodeId=-1;
			int[] pre = dijkstra(client,clientList);
			for(int i=nearNodeId;i!=client;i=pre[i]){
				nodeFreq[i].id=i;
				nodeFreq[i].freq++;
			}
			nodeFreq[client].id=client;
			nodeFreq[client].freq++;
		}
		
		//Arrays.sort(nodeFreq);
		
		
		for(NODE node : nodeFreq){
			if(node.freq>0){
				nodes.add(node.id);
			}
		}
		
	}

	private  int[] dijkstra(int s,List<Integer> clientList){
		boolean [] vis = new boolean[Global.nodeNum];
		int[] dis = new int[Global.nodeNum];
		int[] pre = new int[Global.nodeNum];
		Arrays.fill(pre, -1);
		for(int i=0;i<Global.nodeNum;i++){
			dis[i]=Integer.MAX_VALUE;
		}
		dis[s]=0;
		limitBandWidth[s]=Integer.MAX_VALUE;
		for(int i=0;i<Global.nodeNum;i++){
			int u=-1,Min=Integer.MAX_VALUE,limit=-1;
			for(int j=0;j<Global.nodeNum;j++){
				if(vis[j]==false&&(dis[j]<Min||dis[j]==Min&&limitBandWidth[j]>limit)){
					u=j;
					Min=dis[j];
					limit=limitBandWidth[j];
				}
			}
			if(u==-1){
				break;
			}
			if(u!=s&&clientList.contains(u)){
				nearNodeId=u;
				return pre;
			}
			vis[u]=true;
			for(int v=0;v<Global.nodeNum;v++){
				if(vis[v]==false&&Global.graph[u][v]!=null&&
						dis[v]>dis[u]+Global.graph[u][v].cost){
					dis[v]=dis[u]+Global.graph[u][v].cost;
					pre[v]=u;
					limitBandWidth[v]=Math.min(limitBandWidth[u], Global.graph[u][v].initBandWidth);
				}
			}
		}
		return pre;
	}
	
	



}
