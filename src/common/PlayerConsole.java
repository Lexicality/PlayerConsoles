package common;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.util.Hashtable;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;
import processing.core.PFont;
import ddf.minim.Minim;

public abstract class PlayerConsole extends PApplet {

	public static void main(String[] args) {
		System.out.println("Start PlayerConsole.....");
		// scan the args for params
		// console:pilot/tactical/engineer
		// testmode:true/false
		String consoleString = "";
		boolean testMode = false;
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].startsWith("console:")) {
					String[] parts = args[i].split(":");
					switch (parts[1]) {
					case "pilot":
						consoleString = "pilot.PilotConsole";
						break;
					case "tactical":
						consoleString = "tactical.TacticalConsole";
						break;
					case "engineer":
						consoleString = "engineer.EngineerConsole";
						break;
					default:
						System.out.println("Invalid conesole specified");
						showHelp();
						break;
					}

				} else if (args[i].equals("testMode")) {
					GlobalConfig.testMode = true;
					System.out.println("running in TEST MODE");
				}
			}
		} catch (Exception e) {
			showHelp();
		}
		if (consoleString.equals("")) {
			showHelp();
		}

		System.out.println("Running as : " + consoleString);
		PApplet.main(new String[] { consoleString });

	}

	public static void showHelp() {
		System.out
				.println("USAGE:\r\nPlayerConsole console:engineer/tactical/pilot\r\n\twhich console to start\r\ntestMode\r\n\tif present will start in test mode");

		System.exit(1);

	}

	public boolean testMode = false;

	// ----- global blink state ----
	public boolean globalBlinker;

	long blinkTime = 0;

	// ---banner overlay class---
	protected BannerOverlay bannerSystem;

	protected String serverIP = "127.0.0.1";
	// damage effects
	protected DamageEffect damageEffects;

	// ---- audio stuff
	protected Minim minim;
	protected ConsoleAudio consoleAudio;

	// -----OSC stuff--------
	protected OscP5 oscP5;

	protected NetAddress serverAddress;

	// ------ ship sate -----
	protected ShipState shipState = new ShipState(); // container for ship data
	// ----- common assets ----
	protected PFont globalFont; // default font for game

	// ----- display control
	protected Hashtable<String, Display> displayMap = new Hashtable<String, Display>();

	protected Display currentScreen; // screen that is currently being displayed

	protected String consoleName = "changeme";

	protected long heartBeatTimer = -1;

	/* switch to a new display */
	protected void changeDisplay(Display d) {
		if (currentScreen != null) {
			currentScreen.stop();
		}
		currentScreen = d;
		currentScreen.start();
	}

	@Override
	public void draw() {
		// common draw things
		if (blinkTime + 750 < millis()) {
			blinkTime = millis();
			globalBlinker = !globalBlinker;
		}
		// translate stuff
		damageEffects.startTransform();

		// call draw method
		drawConsole();

		// post-draw
		damageEffects.stopTransform();
		damageEffects.draw();
		damageEffects.drawCracks();
		
		//heartbeat effect
		if (heartBeatTimer > 0) {
			if (heartBeatTimer + 400 > millis()) {
				int a = (int) map(millis() - heartBeatTimer, 0, 400, 255, 0);
				fill(0, 0, 0, a);
				rect(0, 0, width, height);
			} else {
				heartBeatTimer = -1;
			}
		}

	}

	/* actual draw method for the console */
	public abstract void drawConsole();

	// --- getters ----
	public BannerOverlay getBannerSystem() {
		return bannerSystem;
	}

	public ConsoleAudio getConsoleAudio() {
		return consoleAudio;
	}

	public String getConsoleName() {
		return consoleName;
	}

	public PFont getGlobalFont() {
		return globalFont;
	}

	public OscP5 getOscClient() {
		return oscP5;
	}

	public String getServerIP() {
		return serverIP;
	}

	public ShipState getShipState() {
		return shipState;
	}

	public void hardwareEvent(HardwareEvent h) {
		// TODO Auto-generated method stub

	}

	/* hide the cursor by substituting it for an empty icon */
	protected void hideCursor() {
		BufferedImage cursorImg = new BufferedImage(16, 16,
				BufferedImage.TYPE_INT_ARGB);
		Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
				cursorImg, new Point(0, 0), "blank cursor");
		frame.setCursor(blankCursor);
	}

	protected void oscEvent(OscMessage theOscMessage) {
		if (theOscMessage.checkAddrPattern("/ship/damage") == true) {
			float damage = 0.0f;
			if (theOscMessage.arguments().length > 0) {
				damage = theOscMessage.get(0).floatValue();
			}

			damageEffects.startEffect(1000);
			shipDamaged(damage);
		} else if (theOscMessage.checkAddrPattern("/ship/effect/heartbeat") == true) {
			heartBeatTimer = millis();
		}

	}

	@Override
	public void setup() {
		// read config from the command line args
		testMode = GlobalConfig.testMode;

		size(1024, 768, P3D);
		frameRate(25);
		hideCursor();
		bannerSystem = new BannerOverlay(this);
		damageEffects = new DamageEffect(this);
		globalFont = loadFont("common/HanzelExtendedNormal-48.vlw");
	}

	protected abstract void shipDamaged(float amount);
}
