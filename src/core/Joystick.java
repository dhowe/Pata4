package core;


import java.util.*;

import processing.core.PApplet;
import procontroll.ControllDevice;
import procontroll.ControllIO;

public class Joystick
{
  int id;
  PApplet app;
  
  public Joystick(int i)
  {
    this.id = i; 
  }
  
  public static void dumpDeviceInfo(ControllDevice device)
  {
    System.out.println(device.getName()+" has:");
    System.out.println(" " + device.getNumberOfSliders() + " sliders");
    System.out.println(" " + device.getNumberOfButtons() + " buttons");
    System.out.println(" " + device.getNumberOfSticks()  + " sticks");
    
    device.printSliders();
    device.printButtons();
    device.printSticks();
  }
  
  public static void dumpDevices(PApplet p)
  {
	  List joysticks = new ArrayList();
	  ControllIO controll = ControllIO.getInstance(p);
	  int num = controll.getNumberOfDevices();
	  for (int i = 0; i < num; i++) {
	    ControllDevice device = controll.getDevice(i); 
	    joysticks.add(device);
	  }
	  System.out.println(Arrays.asList(joysticks));
  }
  
  public static ControllDevice[] getDevices(PApplet p, String name)
  {
    List joysticks = new ArrayList();
    ControllIO controll = ControllIO.getInstance(p);
    int num = controll.getNumberOfDevices();
    for (int i = 0; i < num; i++) {
      ControllDevice device = controll.getDevice(i); 
      if (device.getName().startsWith(name))
        joysticks.add(device);
    }
    return (ControllDevice[]) joysticks.toArray
      (new ControllDevice[joysticks.size()]);
  }
}
