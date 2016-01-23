package core;

import processing.core.PApplet;
import procontroll.ControllDevice;

public class FootSwitchTest extends PApplet {
	
	public void setup() {
		try {
			ControllDevice dev = USBFootSwitch.getDevice(this);
			USBFootSwitch footswitch = new USBFootSwitch(0);
			footswitch.setup(this, dev);
		} catch (Throwable e) {
			System.out.println("[WARN] No FootSwitch found...");
			//e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		PApplet.main(new String[]{FootSwitchTest.class.getName()});
	}
}
