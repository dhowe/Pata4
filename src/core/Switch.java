package core;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import processing.core.PApplet;

public final class Switch implements SamplerConstants
{
  static int MENU_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  
  public static final Switch SNIP  		= new Switch("Snip-Mode", false, KeyEvent.VK_D);
  public static final Switch SHOW_UI 	= new Switch("VisibleUI", true);
  
  // public static final Switch MUTE  = new Switch("Mute", false);

  //public static final Switch HALF_RACKS           = new Switch("half", false);
  //public static final Switch FFT_GRID           = new Switch("ffts",false);
  //public static final Switch ROTATE_Y           = new Switch("roty",false);
  //public static final Switch SHOW_GEOMETRY      = new Switch("geom",false);
  //public static final Switch COLOR              = new Switch("colr",false);
  //public static final Switch FALL_MODE          = new Switch("fall",LiveSampler.FALL_MODE);  
  //public static final Switch FFT_3D               = new Switch("fft3",false);
  //public static final Switch USE_FFT              = new Switch("fft*",false);
  //public static final Switch FFT_ROT              = new Switch("fftR",false);
  //public static final Switch QUANTIZE             = new Switch("quan",true);  
  //public static final Switch MODULATE_ALL         = new Switch("lock",false);
  //public static final Switch POSTER_MODE           = new Switch("post",false);
  
  static Switch[] ACTIVE = { SHOW_UI };
    //SNIPPET_MODE, //MUTE_MODE, SHOW_ALL_CONTROLS, /*ROTATE_Y, SHOW_GEOMETRY, FALL_MODE, HALF_RACKS  };
  
  private static final int SPACING = 65;
  private static final int RECT_PAD = 4;
  
  static { // set positions
    int xOff = (10 + UI_XOFFSET) - SPACING;
    for (int i = 0; i < Switch.ACTIVE.length; i++)
      Switch.ACTIVE[i].setPosition(xOff += SPACING, -4);
  }

  boolean on;
  String name;
  int key;
  Rectangle bounds;
  
  private Switch(String name, boolean val) {
    this(name, val, 0);
  }
  
  private Switch(String name, boolean val, int ke) {
    this.name = name;
    this.on = val;
    this.key = ke;
  }
  
  public void setPosition(int xOff, int yOff) {
    this.bounds = new Rectangle(xOff, yOff, -1, -1);
  }

  public void toggle() {
    this.on = !this.on;
  }

  public void set(boolean b) {
    this.on = b;
  }
  
  public boolean contains(int mx, int my) {
    return bounds.contains(mx, my);
  }

  public void draw(PApplet p) {
    if (bounds.width<0) 
      bounds.width = (int)p.textWidth(name)+(RECT_PAD*2);
    if (bounds.height<0) 
      bounds.height = (int)p.textAscent()+(RECT_PAD*2);
    p.noFill();
    p.rect(bounds.x, bounds.y, bounds.width, bounds.height);
    p.fill(on ? Pata4.TEXT_FILL : 200);
    if (on)
      p.fill(200, 100, 100);
    else
      p.fill(Pata4.TEXT_FILL);
    p.text(name, bounds.x+RECT_PAD, bounds.y+12);
  }
  
}// end