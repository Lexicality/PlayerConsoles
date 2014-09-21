package common;

import java.awt.event.KeyEvent;

import processing.serial.Serial;

/* interface to hardware connected to machine */
public class HardwareController {

	protected Serial serialPort;
	protected boolean isKeyboard = false;
	protected String interfaceName = "";

	protected PlayerConsole parent;

	protected char[] serialBuffer = new char[20];
	protected int bufPtr = 0;

	public HardwareController(String interfaceName, String port, int rate,
			PlayerConsole parent) {

		if (port.equals("Keyboard")) {
			isKeyboard = true;
		} else {
			if (parent.testMode == false) {
				serialPort = new Serial(parent, port, rate);
			}

		}

		this.interfaceName = interfaceName;
		this.parent = parent;

	}

	/*
	 * take contents of buffer and convert to hardwareevent This needs to be
	 * overriden for each stations hardware as each thing produces its own mess
	 * of data
	 */
	protected void bufferComplete() {
		if (isKeyboard) {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEY";
			h.data = "" + serialBuffer[0];
			h.fromDevice = interfaceName;
			parent.hardwareEvent(h);
		}
	}

	public void keyPressed(KeyEvent ke) {
		if (isKeyboard) {
			HardwareEvent h = new HardwareEvent();
			h.event = "KEY";
			h.data = ke;
			h.fromDevice = interfaceName;
			parent.hardwareEvent(h);
		}

	}

	protected void sendSerial(String toSend) {
		if (parent.testMode == false && serialPort != null) {
			for (int i = 0; i < toSend.length(); i++) {
				serialPort.write(toSend.charAt(i));
			}
			serialPort.write(',');

		} else {
			Messages.println("sending {0} to serial", toSend);
		}
	}

	public void update() {
		if (!isKeyboard && !parent.testMode) {
			while (serialPort.available() > 0) {
				char c = serialPort.readChar();

				if (c == ',') {
					bufferComplete();
					bufPtr = 0;
				} else {
					serialBuffer[bufPtr] = c;
					bufPtr++;
				}
			}
		}
	}

}
