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
  	ControllDevice[] devs = Joystick.getDevices(p,"DELCOM JS Foot Switch");
  	ControllDevice fs = devs[0];
  	//System.out.println(fs);
  	return fs;
  }
  
  public void setup(Pataclysm p, ControllDevice device)
  {
    this.app = p;   
    this.device = device;
    System.out.print("[INFO] USBFootSwitch: ");
    if (device!= null) {
        System.out.println(device.getName()+": "+id);    
    }
    else 
    	System.out.println("[INFO] USBFootSwitch not found...");  

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
  	//System.out.println("USBFootSwitch.onButton0Press()");
    if (!app.recording())
      app.recordStart();
  }
  
  public void onButton0Release() {
  	//System.out.println("USBFootSwitch.onButton0Release()");
    if (app.recording()) {
      app.recordStop();
    }
  }


}
