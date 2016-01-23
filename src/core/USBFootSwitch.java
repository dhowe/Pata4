package core;


import processing.core.PApplet;
import procontroll.ControllDevice;
import procontroll.ControllIO;

public class USBFootSwitch extends Joystick
{
  static final boolean PRINT_JOYSTICK_INFO = false;
  static final int NUM_BUTTONS = 1;
  static float originX, originY;  
  
  ControllIO controll;
  ControllDevice device;

  public USBFootSwitch()
  {
    this(0); 
  }
  
  public USBFootSwitch(int i)
  {
    super(i); 
  }
  
  public static ControllDevice getDevice(PApplet p)
  {
  	ControllDevice[] devs = null;
		try {
			devs = Joystick.getDevices(p, "DELCOM JS Foot Switch");
		} catch (Throwable e) {
			
			e.printStackTrace();
		}
  	return (devs != null && devs.length > 0) ? devs[0] : null;
  }
  
  public void setup(PApplet p, ControllDevice device)
  {
    this.app = p;   
    this.device = device;
    if (device != null) {
    	System.out.println("[INFO] USBFootSwitch: "+device.getName()+": "+id);    
    }
    else 
    	throw new RuntimeException("Unable to load footswitch");

    registerMethods();
  }

  private void registerMethods()
  {
    for (int i = 0; i < NUM_BUTTONS; i++) {
      //device.plug(this, "onButton"+i+"Press", ControllIO.WHILE_PRESS, i);
      device.plug(this, "onButton"+i+"Press",   ControllIO.ON_PRESS, i);
      device.plug(this, "onButton"+i+"Release", ControllIO.ON_RELEASE, i);
      //System.out.println("registered: "+"onButton"+i+"Press/Release");
    }
  }
  
  public void onButton0Press() {
  	
  	if (app instanceof Pataclysm) {
  		Pataclysm pata = (Pataclysm) app;
  		if (!pata.recording())
  			pata.recordStart();
  	}
  	else
  		System.out.println("USBFootSwitch.onButton0Press()");
  }
  
  public void onButton0Release() {
  	if (app instanceof Pataclysm) {
  		Pataclysm pata = (Pataclysm) app;
  		if (pata.recording())
  			pata.recordStop();
  	}
  	else
  		System.out.println("USBFootSwitch.onButton0Press()");
  }


}
