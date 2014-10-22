package tactical;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
/*


 */





import javax.sound.sampled.TargetDataLine;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import common.ConsoleLogger;
import common.Display;
import common.HardwareEvent;
import common.PlayerConsole;
import common.ShipState;

public class WeaponsConsole extends Display {

	public class TargetObject implements Comparable<TargetObject> {
		public int hashCode = 0;
		public PVector pos = new PVector(0, 0, 0);
		public PVector lastPos = new PVector(0, 0, 0);
		public long lastUpdateTime = 0;
		public int scanId = 0;
		public boolean trackingPlayer = false;
		public boolean targetted = false;
		public int scanCountDown = -1;
		public boolean beingFiredAt = false;
		public boolean dead = false;
		public String name = "missile";
		public float[] stats = new float[2];
		public String[] statNames = new String[2];
		public PVector screenSpacePos = new PVector(0,0);

		
		
		
		public float randomAngle;
		protected HashMap<String, Float> statMap = new HashMap<String, Float>();

		public TargetObject() {
			// 4.2 - 5.23
			randomAngle = PApplet.map(parent.random(100), 0, 100, 4.2f, 5.23f);
		}

		public void clearStats() {
			statMap.clear();
		}

		@Override
		public int compareTo(TargetObject other) {
			return (int) (this.pos.mag() - other.pos.mag());
		}

		public Float getStat(String name) {
			Float f = statMap.get(name);
			return f;
		}

		public void setStat(String name, float val) {
			Float f = new Float(val);
			// println("setting stat: " + name);
			statMap.put(name, val);
		}
	}

	public static final int MODE_SCANNER = 0;

	// images etc
	PImage[] banners = new PImage[3];
	PImage bgImage;
	PImage scannerImage, titleImage, hullStateImage;
	PImage decoyButton, beamButton, decoyButtonD, beamButtonD;
	PImage grappleButton, grappleButtonD;

	PImage launchDetected;

	float firingTime = 0; // time a laser firing started

	// current screen mode
	int mode = MODE_SCANNER;
	// targetting crap
	List<TargetObject> targets = Collections
			.synchronizedList(new ArrayList<TargetObject>());

	TargetObject currentTarget;

	int sensorPower = 0;
	int beamPower = 0;
	boolean weaponsDeployed = false;
	
	long smartBombFireTime = 0;
	long missileStartTime = 0;
	float scannerAngle = 0;

	String scanString = "";
	int scanTimeout = 100;
	int scanTargetIndex;

	int numMissiles = 0;
	float maxBeamRange = 1300;

	int radarTicker = 175;
	
	// states
	boolean flareEnabled = false;
	boolean offline = false;
	boolean fireEnabled = false;
	boolean blinkenBool = false;

	long blinkenBoolTimer = 0;
	public static final int SCAN_TYPING = 0;
	public static final int SCAN_SCANNING = 1;
	public static final int SCAN_FAILED = 2;
	public static final int SCAN_OK = 3;

	int scanningState = SCAN_TYPING;

	public boolean hookArmed = false;

	// sensor power to range mapping
	//int[] sensorRanges = { 370, 580, 900, 1300 };
	OscP5 osc;

	String serverIP = "";

	
	public WeaponsConsole(PlayerConsole parent) {
		super(parent);
		osc = parent.getOscClient();
		serverIP = parent.getServerIP();
		ConsoleLogger.log(this, "wep console ip:" + serverIP);
		// load resources
		bgImage = parent.loadImage("tacticalconsole/tacticalscreen3.png");
		titleImage = parent.loadImage("tacticalconsole/weaponsTitle.png");

		scannerImage = parent.loadImage("tacticalconsole/radarscanner.png");

		launchDetected = parent.loadImage("tacticalconsole/launchDetected.png");
		beamButton = parent.loadImage("tacticalconsole/firebeam.png");
		decoyButton = parent.loadImage("tacticalconsole/firedecoy.png");
		beamButtonD = parent.loadImage("tacticalconsole/firebeamD.png");
		decoyButtonD = parent.loadImage("tacticalconsole/firedecoyD.png");

		grappleButton = parent.loadImage("tacticalconsole/grappleFireOn.png");
		grappleButtonD = parent.loadImage("tacticalconsole/grappleFireOff.png");
		hullStateImage = parent
				.loadImage("tacticalconsole/hulldamageoverlay.png");
		
	
	}

	@Override
	public void draw() {
	    sensorPower = parent.getShipState().powerStates[ShipState.POWER_SENSORS];
	    beamPower = parent.getShipState().powerStates[ShipState.POWER_WEAPONS];
	    int sensorRange = (int) parent.map(sensorPower, 0f, 12f, 270,1300);
	    maxBeamRange = (1000 + (beamPower - 1) * 300);
		
		parent.background(0, 0, 0);
		parent.noStroke();
		if (mode == MODE_SCANNER) {
			parent.fill(0, 128, 0, 100);
			int sensorSize = (int) parent.map(sensorPower, 0f, 12f, 270,1300) ;
			parent.ellipse(364, 707, sensorSize, sensorSize);
			radarTicker += 10;
			//parent.noFill();
			parent.noStroke();
			//parent.stroke(0, 255, 0);
			int alpha = (int) PApplet.map(radarTicker, 0, sensorSize, 45f, 0f );
			parent.fill(0,255,0, alpha);
			parent.arc(364f, 707f, radarTicker, radarTicker, 4.2f, 5.23f);
			if (radarTicker > sensorRange) {
				radarTicker = 15;
			}

			parent.image(bgImage, 0, 0);
			drawTargets();
		}
		drawSideBar();
		parent.stroke(255);
		parent.fill(255);
	}

	void drawSideBar() {
		parent.image(titleImage, 7, 5);
		// draw sidebar stuff
		parent.fill(255, 255, 255);
		parent.textFont(font, 56);
		parent.text(parent.getShipState().smartBombsLeft, 212, 706);
		// power gauges in bottom left
		parent.noStroke();
		int f = (int) PApplet.map(beamPower, 0f, 12f, 255f, 0f);
		parent.fill(f, 255-f, 0);
		parent.rect(106, 742, 25, PApplet.map(beamPower, 0f, 12f, 0f, -90));
		f = (int) PApplet.map(sensorPower, 0f, 12f, 255f, 0f);
		parent.fill(f, 255-f, 0);
		parent.rect(47, 742, 25, PApplet.map(sensorPower, 0f, 12f, 0f, -90));

		// the target list on the right hand side
		parent.textFont(font, 14);
		int sensorRange = (int) PApplet.map(sensorPower, 0f, 12f, 270,1300); //sensorRanges[sensorPower / 4];
		synchronized (targets) {
			Collections.sort(targets); // sorted by distance from ship
			int ypos = 144;
			for (TargetObject t : targets) {
				if (t.targetted) {
					parent.fill(255, 0, 0);
				} else {
					if (t.pos.mag() < sensorRange) {
						parent.fill(0, 255, 0);
					} else {
						parent.fill(100, 100, 100);
					}
				}
				if (t.pos.mag() > sensorRange) {
					parent.text("???", 710, ypos);
				} else {
					parent.text(t.scanId, 710, ypos);
				}
				String h = String.format("%.0f", t.pos.mag());
				parent.text(h, 780, ypos);

				String name = t.name;
				if (name.length() > 12) {
					name = name.substring(0, 12) + "..";
				}
				if (t.pos.mag() > sensorRange) {
					name = "???";
				}
				parent.text(name, 855, ypos);

				if (ypos + 20 > 400) {
					break;
				} else {
					ypos += 20;
				}
			}
		}

		// text in the scanning ID field
		// set its colour based on what its doing
		parent.fill(255);
		parent.textFont(font, 30);
		switch (scanningState) {
		case SCAN_TYPING:

			parent.fill(255);
			break;
		case SCAN_SCANNING:
			parent.text("LOCKING", 758, 737);
			parent.fill(0, 190, 0);
			break;

		case SCAN_OK:
			if (parent.globalBlinker) {
				parent.text("LOCKED", 758, 737);
			}

			parent.fill(0, 255, 0);
			break;
		case SCAN_FAILED:
			parent.text("NO TARGET", 730, 737);

			parent.fill(255, 0, 0);
			break;
		}
		parent.textFont(font, 60);
		parent.text(scanString, 720, 675);
		parent.fill(255);

		if (blinkenBool && fireEnabled) {
			parent.image(beamButton, 714, 431);
		}

		
/*
		// draw hull damage
		parent.tint((int) PApplet.map(parent.getShipState().hullState, 0, 100,
				255, 0), (int) PApplet.map(parent.getShipState().hullState, 0,
				100, 0, 255), 0);
		parent.image(hullStateImage, 486, 620);
		parent.textFont(font, 23);
		parent.text((int) parent.getShipState().hullState + "%", 463, 646);
		parent.noTint();
		*/ 
	}

	protected void drawTargets() {
		int sensorRange = (int) parent.map(sensorPower, 0f, 12f, 270,1300);
		parent.textFont(font, 12);
		fireEnabled = false;
		parent.strokeWeight(1);
		synchronized (targets) {
			for (int i = targets.size() - 1; i >= 0; i--) {
				TargetObject t = targets.get(i);
				
				float distanceToTarget = t.pos.mag();
				float lastDistanceToTarget = t.lastPos.mag();
				// update logic bits
				// if no update received for 280ms then remove this target
				if (parent.millis() - t.lastUpdateTime > 300) {
					if (t.targetted) {
						parent.getConsoleAudio().playClip("targetDestroyed");
						scanningState = SCAN_TYPING;
						scanString = "";
					}
					targets.remove(i);
				}

				float lerpX = PApplet.lerp(t.lastPos.x, t.pos.x,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				float lerpY = PApplet.lerp(t.lastPos.z, t.pos.z,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				float lerpZ = PApplet.lerp(t.lastPos.y, t.pos.y,
						(parent.millis() - t.lastUpdateTime) / 250.0f);

				// float x = 352 + map(lerpX, -2000, 2000, -352, 352);
				// float y = 426 + map(lerpY, -2000, 2000, -426, 426);
				// 364,707
				PVector p = PVector.fromAngle(t.randomAngle);
				p.mult(75 + distanceToTarget / 3.0f); // new pos
				PVector lp = PVector.fromAngle(t.randomAngle);
				lp.mult(75 + lastDistanceToTarget / 3.0f);

				float x = 364 + PApplet.lerp(lp.x, p.x,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				;
				float y = 707 + PApplet.lerp(lp.y, p.y,
						(parent.millis() - t.lastUpdateTime) / 250.0f);
				;

				// set target colour
				float scaleFactor = 1.2f;
				if (distanceToTarget > sensorRange * scaleFactor) {
					parent.fill(100, 100, 100);
					x += parent.random(-5, 5);
					y += parent.random(-5, 5);
				} else if (distanceToTarget < 200) {

					parent.fill(255, 0, 0);
				} else if (distanceToTarget < 500) {
					parent.fill(255, 255, 0);
				} else {
					parent.fill(0, 255, 0);
				}

				// draw the target on the radar
				parent.noStroke();
				parent.ellipse(x, y, 10, 10);
				String scanCode = "" + t.scanId;
				if (t.scanId < 1000) {
					scanCode = "0" + scanCode;
				}
				if (distanceToTarget < sensorRange * scaleFactor) { // grey it
																	// out if
																	// its
																	// outside
																	// of sensor
																	// range, if
																	// not then
																	// draw
					parent.textFont(font, 14);

					String h = String.format("%.2f", t.stats[0] * 100);
					parent.text(t.name + ": " + h + "%", x + 30, y + 5);
					parent.textFont(font, 20);
					parent.text(scanCode, x + 30, y-10);
					parent.stroke(255,255,0);
					parent.line(x+30, y-15, x+20, y-15);
					parent.line(x, y, x+20, y-15);
					// are there any extended stats on this?
					Float f = t.getStat("scanning");
					if (f != null && f > 0.0f) {
						// draw a scanning effect around the target
						int maxSize = 70;
						int size = (int) PApplet.map(parent.millis() % 2000, 0,
								2000, 0, maxSize);
						parent.noFill();
						parent.strokeWeight(2);
						parent.stroke(0, 128, 255,
								PApplet.map(size, 0, maxSize, 255, 0));
						parent.ellipse(x, y, size, size);
						size = (int) PApplet.map(
								(parent.millis() + 1000) % 2000, 0, 2000, 0,
								maxSize);
						parent.stroke(0, 128, 255,
								PApplet.map(size, 0, maxSize, 255, 0));
						parent.ellipse(x, y, size, size);
					}
					f = t.getStat("chargingWeapons");
					if (f != null && f > 0.0f) {
						// warn the player that the target is charging its
						// weapons
					}
				} else {
					parent.fill(128);
					StringBuilder s = new StringBuilder(t.name);
					for (int c = 0; c < (int) parent.random(3, s.length()); c++) {
						s.setCharAt((int) parent.random(0, s.length()),
								(char) parent.random(0, 255));
					}
					parent.text(s.toString(), x + 10, y);
				}

				if (t.dead) {
					if (t == currentTarget) {
						scanningState = SCAN_TYPING;
					}
					targets.remove(i);

				}

				// scanning stuff
				if (t.scanCountDown > 0) {
					if (t.scanCountDown - 1 > 0) {
						t.scanCountDown--;
					} else {
						// target this motherfucker
						t.scanCountDown--;
						OscMessage myMessage = new OscMessage(
								"/system/targetting/targetObject");
						myMessage.add(t.hashCode);
						osc.send(myMessage, new NetAddress(serverIP, 12000));
						currentTarget = t;
						parent.getConsoleAudio().playClip("targetLocked");
						scanningState = SCAN_OK;
					}
					parent.pushMatrix();
					parent.translate(x, y);
					parent.rotate(PApplet.radians((parent.millis() / 10.0f) % 360));
					parent.noFill();
					parent.stroke(255, 255, 0);
					float scale = PApplet.map(t.scanCountDown, 100, 0, 10, 1);
					parent.rect(-15 * scale, -15 * scale, 30 * scale,
							30 * scale);
					parent.popMatrix();
						
					
					//parent.text("scanning: " + t.scanCountDown, x + 10, y + 10);
				}

				if (t.targetted) {
					parent.stroke(0, 255, 0);
					parent.noFill();
					// rect(x-10, y-10, 20, 20);

					

					parent.pushMatrix();
					parent.translate(x, y);
					parent.rotate(PApplet.radians((parent.millis() / 10.0f) % 360));
					parent.noFill();
					parent.stroke(255, 255, 0);
					float scale = PApplet.map(t.scanCountDown, 100, 0, 10, 1);
					parent.rect(-15, -15, 30, 30);
					parent.popMatrix();
				}

				// draw a beam out to the target if we are firing
				if (t.beingFiredAt && firingTime + 400 > parent.millis()) {
					parent.stroke(255, 255, 0);
					parent.strokeWeight(2);
					parent.line(364, 707, x, y);
				}
				// draw a beam to the ship if the target is firing at us
				Float f = t.getStat("firing");
				if (f != null && f > 0.0f) {
					parent.stroke(255, 0, 0);
					parent.strokeWeight(4);
					parent.line(x, y, 364, 707);
				}
			}
		}
	}
	

	// find a target by hashcode
	protected TargetObject findTargetById(int id) {
		for (TargetObject t : targets) {
			if (t.hashCode == id) {
				return t;
			}
		}
		return null;
	}

	private void fireDecoy(){
		parent.getShipState().smartBombsLeft--;
		smartBombFireTime = parent.millis();
		
	}
	
	
	
	@Override
	public void oscMessage(OscMessage theOscMessage) {

		if (theOscMessage
				.checkAddrPattern("/tactical/weapons/targetUpdate")) {

			updateTarget(theOscMessage);
		} else if (theOscMessage
				.checkAddrPattern("/tactical/weapons/targetRemove")) {
			removeTarget(theOscMessage);
		} else if (theOscMessage
				.checkAddrPattern("/tactical/weapons/firingAtTarget")) {
			firingAtTarget(theOscMessage);
		} else if (theOscMessage.checkAddrPattern("/system/targetting/smartBombOk")){
			fireDecoy();
			
		} else if (theOscMessage.checkAddrPattern("/ship/weaponState")){
			int state = theOscMessage.get(0).intValue();
			switch(state){
			case ShipState.WEAPON_STOWED:
				parent.getConsoleAudio().playClip("weaponsRetracted");
				break;
			case ShipState.WEAPON_DEPLOYED: 
				parent.getConsoleAudio().playClip("weaponsDeployed");
				break;
			}
			
		}
				
	}

	protected void firingAtTarget(OscMessage theOscMessage) {
		synchronized (targets) {
			int tgtHash = theOscMessage.get(0).intValue();
			TargetObject t = findTargetById(tgtHash);
			if (t != null) {
				t.beingFiredAt = true;
				firingTime = parent.millis();
			}
		}
		
	}

	protected void removeTarget(OscMessage theOscMessage) {
		synchronized (targets) {
			int tgtHash = theOscMessage.get(0).intValue();
			TargetObject t = findTargetById(tgtHash);
			if (t != null) {
				t.dead = true;
				t.targetted = false;
				ConsoleLogger.log(this, "target removed");
				if (t.targetted) {
					parent.getConsoleAudio().playClip("targetDestroyed");
					scanningState = SCAN_TYPING;
				}
				if (t.scanCountDown >= 0) {
					scanningState = SCAN_TYPING;
				}
			}
		}
		
	}

	protected void updateTarget(OscMessage theOscMessage) {
		int tgtHash = theOscMessage.get(0).intValue();
		synchronized (targets) {
			TargetObject t = findTargetById(tgtHash);
			boolean newTarget = false;
			if (t == null) {
				ConsoleLogger.log(this, "new target: " + tgtHash);
				t = new TargetObject();
				t.hashCode = tgtHash;
				newTarget = true;
				targets.add(t);
				parent.getConsoleAudio().playClip("newTarget");
			}
			t.scanId = theOscMessage.get(1).intValue();
			t.trackingPlayer = theOscMessage.get(2).intValue() == 1 ? true
					: false;
			t.targetted = theOscMessage.get(3).intValue() == 1 ? true
					: false;
			float x = theOscMessage.get(4).floatValue();
			float y = theOscMessage.get(5).floatValue();
			float z = theOscMessage.get(6).floatValue();
			if (newTarget) {
				t.lastPos.x = x;
				t.lastPos.y = y;
				t.lastPos.z = z;
				t.randomAngle = PApplet.map(parent.random(100), 0, 100,
						4.2f, 5.23f);
			} else {

				t.lastPos.x = t.pos.x;
				t.lastPos.y = t.pos.y;
				t.lastPos.z = t.pos.z;
			}
			t.lastUpdateTime = parent.millis();
			t.pos = new PVector(x, y, z);
			
			t.stats[0] = theOscMessage.get(7).floatValue();
			// t.stats[1] = theOscMessage.get(8).floatValue();
			t.statNames[0] = theOscMessage.get(8).stringValue();
			// t.statNames[1] = theOscMessage.get(10).stringValue();
			t.name = theOscMessage.get(9).stringValue();

			// now unpack the stat string
			String statString = theOscMessage.get(10).stringValue();
			String[] pairs = statString.split(",");
			for (String p : pairs) {
				String[] vals = p.split(":");
				t.setStat(vals[0], Float.parseFloat(vals[1]));
			}
		}
		
	}

	void scanTarget() {

		currentTarget = null;
		ConsoleLogger.log(this, "scan start");

		int sId = 0;
		try {
			sId = Integer.parseInt(scanString);
			// find what were scanning
			boolean targetFound = false;
			synchronized (targets) {
				for (TargetObject t : targets) {
					if (sId == t.scanId) {
						t.scanCountDown = (13 - sensorPower) * 10;
						targetFound = true;
					} else {
						if (t.targetted) {
							t.scanCountDown = -1;
							t.targetted = false;
							t.beingFiredAt = false;
							OscMessage myMessage = new OscMessage(
									"/system/targetting/untargetObject");
							myMessage.add(t.hashCode);
							osc.send(myMessage, new NetAddress(serverIP,
									12000));
						}
					}
				}

				if (targetFound) {
					parent.getConsoleAudio().playClip("targetting");
					scanningState = SCAN_SCANNING;
				} else {
					parent.getConsoleAudio().playClip("outOfRange");
					scanningState = SCAN_FAILED;
				}
			}
		} catch (NumberFormatException e) {
		}
		// scanString = "";
	}

	@Override
	public void serialEvent(HardwareEvent evt) {
		if(evt.event.equals("KEYPAD")){

			int action = evt.id;
			if (action == TacticalHardwareController.KP_LASER) {
				fireLaser();
			}
	
			if (action == TacticalHardwareController.KP_DECOY) {
				fireSmartBomb();
				
				return;
			}
	
			if (action == TacticalHardwareController.KP_SCAN) {
				scanTarget();
			} else {
				//keypad only types 0-9
				keyTyped(evt.id);
			
			}
		} else if (evt.event.equals("KEY")){
			if(evt.value == KeyEvent.VK_F){
				fireLaser();
			} else if (evt.value == KeyEvent.VK_M){
				fireSmartBomb();
			} else if (evt.value == KeyEvent.VK_SPACE){
				scanTarget();
			}
			//KEY events are keycodes
			int code = evt.value - 48;
			
			keyTyped(code);
		}
	}

	private void fireLaser() {
		OscMessage myMessage = new OscMessage(
				"/system/targetting/fireAtTarget");
		osc.send(myMessage, new NetAddress(serverIP, 12000));
		if (currentTarget != null && currentTarget.pos.mag() < maxBeamRange) {
			parent.getConsoleAudio().playClip("firing");
		} else {
			parent.getConsoleAudio().playClip("outOfRange");
		}

		ConsoleLogger.log(this, "Fire at target");
		return;
		
	}

	private void fireSmartBomb() {
		if (parent.getShipState().smartBombsLeft > 0) {
			if (smartBombFireTime + 1000 < parent.millis()) {

				OscMessage myMessage = new OscMessage(
						"/system/targetting/fireFlare");
				osc.send(myMessage, new NetAddress(serverIP, 12000));

				
			}
		} else {
			// warn we have no flares left
		}
		
	}

	//value should be 0-9
	private void keyTyped(int value) {
		if (value >= 0 && value <= 9) {
			if (scanningState != SCAN_TYPING) {
				scanString = "";
			}
			scanString = scanString + value;

			scanningState = SCAN_TYPING;
			if (scanString.length() >= 4) {
				scanTarget();
			}
		}
		
	}

	@Override
	public void start() {
		offline = false;
		targets = new ArrayList<TargetObject>();
		OscMessage msg = new OscMessage("/system/ship/getPowerLevels");
		OscP5.flush(msg, new NetAddress(serverIP, 12000));
	}

	@Override
	public void stop() {
		offline = false;
	}
}
