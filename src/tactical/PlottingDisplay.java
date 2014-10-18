package tactical;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

import oscP5.OscMessage;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;

public class PlottingDisplay extends Display {
	PImage backgroundImage;
	
	ArrayList<MapNode> mapNodes = new ArrayList<MapNode>();
	MapNode currentNode;
	MapNode startNode;
	MapNode destNode;
	boolean routeComplete = false;
	
	
	//entered codes 
	ArrayList<MapNode> currentRoute = new ArrayList<MapNode>();
	String currentCode = "";

	private String failReason = "";
	
	//debug animation stuff
	long lastKeyTime = 0;
	
	
	public PlottingDisplay(PlayerConsole parent) {
		super(parent);
		backgroundImage = parent.loadImage("tacticalconsole/plottingBG.png");
		loadMap();
		
	}

	@Override
	public void draw() {
		parent.image(backgroundImage,0,0);
		// TODO Auto-generated method stub
		parent.fill(255);
		parent.textFont(font, 40);
		
		//current route
		int yOffset = 0;
		for(MapNode s : currentRoute){
			parent.text("> " + s.id, 39, 246+ yOffset);
			yOffset += 40;
		}
		//post-list text
		if(!routeComplete){
			String t = "> " + currentCode;
			if(parent.globalBlinker){
				t += "_";
			}
			parent.text(t, 39, 246+ yOffset);
		} else {
			if(parent.globalBlinker){
				parent.textFont(font, 22);
				parent.fill(0,255,0);
				parent.text("ROUTE OK, BEGIN JUMP SEQUENCE", 39, 246+ yOffset);
			}
		}
		
		//the failure reason
		if(failReason.equals("") == false){
			
			parent.fill(255,0,0);
			parent.text(failReason, 39, 246+ yOffset + 50);
		}
		parent.noStroke();
		if(routeComplete){
			if(parent.globalBlinker){
				parent.fill(0,255,0);
			} else {
				parent.fill(0,100,0);
				
			}
			parent.rect(725, 623, 251, 90);
			parent.fill(255);
			parent.textFont(font, 20);
			parent.text("ROUTE OK", 776, 673);
		} else {
			parent.fill(255,0,0);
			parent.rect(725, 623, 251, 90);
			parent.fill(255);
			parent.textFont(font, 20);
			parent.text("NO ROUTE", 776, 673);
		}
		
		//animated debug info
		parent.noFill();
		parent.stroke(255,255,0);
		//parent.rect(721, 204, 268, 340);
		
	}

	@Override
	public void oscMessage(OscMessage theOscMessage) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if (evt.event.equals("KEY")){
			if(evt.value >= KeyEvent.VK_0 && evt.value <= KeyEvent.VK_9){
				keyTyped(evt.value);
			} else if (evt.value == KeyEvent.VK_SPACE){
				codeEntered();
			}
		}
	}
			
	
	private void loadMap(){
		 String[] s = parent.loadStrings("tacticalconsole/map.txt");
		  ConsoleLogger.log(this, "loading " + s.length + " mapnodes");
		  
		  for(String it : s){
		    String[] parts = it.split(":");
		    int xp = Integer.parseInt(parts[0]);
		    int yp = Integer.parseInt(parts[1]);
		    
		    String id = parts[2];
		    MapNode n = new MapNode();
		    n.pos = new PVector(xp, yp);
		    n.id = id;
		    mapNodes.add(n);
		  }
	}
	
	private void codeFailed(String reason){
		currentCode = "";
		failReason = reason;
		
		
	}

	private void codeEntered() {
		if(routeComplete) return;
		
		if(currentCode.length() == 4){
			//check to see if code exists in map 
			boolean found = false;
			for(MapNode m : mapNodes){
				if(m.id.equals(currentCode)){
					
					found = true;
					testNode(m);
					
					break;
				}
			}
			if(!found){
				codeFailed("INVALID WAYPOINT");
			}
			
			
		} else {
			codeFailed("INVALID WAYPOINT");
			
		}
		
		
	}

	/* test a node to see if its in range of the current node
	 * if it is and hasnt been visited then add it to the waypoint list
	 * if it isnt then codefailed();
	 */
	private void testNode(MapNode m) {
		if(m.visited){
			codeFailed("INVALID WAYPOINT");
			return;
			
		} else {
			
			//check its distance
			PVector src = currentRoute.get(currentRoute.size() - 1).pos;
			PVector dst = m.pos;
			PVector diff = PVector.sub(src, dst);
			if(diff.mag() > 165){ // too far
				codeFailed("DISTANCE TOO GREAT");
				return;
			}
			m.visited = true;
			
			currentRoute.add(m);
			currentCode = "";
			
			//now do a quick check to see if we are in range of the destination node
			src = m.pos;
			dst = destNode.pos;
			diff = PVector.sub(src, dst);
			if(diff.mag() < 165){ // too far
				//tell the main game which route we're using
				OscMessage msg = new OscMessage("/system/jump/setRoute");
				msg.add(0);
				parent.getOscClient().send(msg, parent.getServerAddress());
				//fuck yeah, we're there!
				parent.getBannerSystem().setSize(700, 300);
				parent.getBannerSystem().setTitle("ROUTE OK");
				parent.getBannerSystem().setText("JUMP PLOTTING COMPLETE");
				parent.getBannerSystem().displayFor(2000);
				currentRoute.add(destNode);	
				routeComplete = true;
				
				return;
			}
		}
	}

	private void keyTyped(int value) {
		lastKeyTime = 50;
		if(currentCode.length() < 4){
			currentCode += (char)value;
			failReason = "";
		} else {
			currentCode = "" + (char)value;
		}
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub
		currentRoute = new ArrayList<MapNode>();
		if(parent.getShipState().currentScene == ShipState.SCENE_LAUNCH){
			startNode = findNodeById("STATION 13");
			destNode = findNodeById("TRAINING AREA");					
			currentRoute.add(startNode);
			
		} else if (parent.getShipState().currentScene == ShipState.SCENE_WARZONE){
			destNode = findNodeById("STATION 13");
			startNode = findNodeById("TRAINING AREA");					
			currentRoute.add(startNode);
		}
		for(MapNode m : mapNodes){
			m.visited = false;
		}
		currentCode = "";
		failReason = "";
		routeComplete = false;
		
		OscMessage msg = new OscMessage("/system/jump/clearRoute");
		parent.getOscClient().send(msg, parent.getServerAddress());
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
	
	private MapNode findNodeById(String id){
		for(MapNode m : mapNodes){
			if (m.id.equals(id)){
				return m;
			}
		}
		return null;
	}
	
	class MapNode{
		  public PVector pos;
		  public String id;
		  public boolean visited = false;
		  
		  public MapNode(){}
		}

}
