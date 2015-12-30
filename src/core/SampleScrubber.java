package core;

import java.awt.Rectangle;
import java.util.Properties;
import java.util.Random;

import pitaru.sonia.Sample;
import processing.core.*;

public class SampleScrubber implements SamplerConstants
{
  private static final int W = 194, H = 24;

  PApplet p;
  SampleUIControl parent;

  int x, y, w, h, stopFrame, sliderX, startFrame, waveformH;
  
  private boolean visible = true, pressed;
  private int lastFrame;
  private static Random rand = new Random();
  private float startX = 0, stopX = 0;

  private Rectangle bounds;

  public SampleScrubber(PApplet p, SampleUIControl sc, int xPos, int yPos) {
  	this(p, sc, xPos, yPos, W, H);
  }
  
  public SampleScrubber(PApplet p, int numFrames, int xPos, int yPos) { // only for testing
  	init(checkPApplet(p), numFrames, xPos, yPos, W, H);
  }
 
  public SampleScrubber(PApplet p, SampleUIControl  sc, int x, int y, int w, int h) {
  	
  	this.parent = sc;
  	this.init(p, sc.getSampleLength(), x, y, w, h);
  }
  
  private void init(PApplet p, int stopFrame, int x, int y, int w, int h) {

  	this.p = p;
    this.w = w;
    this.h = h;
    this.y = y;
    this.x = sliderX = x;
    this.waveformH = h+12;
    this.bounds = new Rectangle(x, y - h / 2, w, h);
    this.stopFrame = stopFrame;
  }

  public void draw() 
  {    
    boolean partial = updateScrubPos();
    
    if (!visible) return;

    // setup pen --------------------------
    p.fill(WAVEFORM_COL);
    p.stroke(WAVEFORM_COL);
    p.strokeWeight(1);
    p.rectMode(PConstants.CENTER);

    // horiz line -------------------------
    float x1 = partial ? x + startX : x;
    float x2 = partial ? x + stopX : x + w + 2;
    p.line(x1, y, x2, y);

    // waveform  ---------------------------
    if (waveform == null) 
      computeWaveForm(getSample(), w+4, waveformH);
    
    p.image(waveform, bounds.x, y - waveformH/2f);
    
    // red loop line -----------------------
    if (pressed & partial) {
      p.stroke(255, 20, 20);
      p.line(x + startX, y, x + stopX, y);
    }
    
    // slider rect -------------------------
    p.fill(255);
    p.noStroke();
    p.rect(x + sliderX + 1, y, 4, 16);
  }
  
  public void computeWaveForm(Sample s, int w, int h) 
  {
  	boolean partial = isPartial(s);
    float[] pts = computePoints(s, w);
    
    waveform = p.createGraphics(w, h, PApplet.JAVA2D);
    float center = waveform.height/2f;
    
    waveform.beginDraw();
    
    waveform.noStroke();
    waveform.fill(0,0);
    waveform.rect(0,0, waveform.width-1, waveform.height-1);
    
    waveform.smooth();
    waveform.noFill();
    
    for (int i = 0; i < pts.length-1; i++) {
    	
      float y1 = center+(pts[i] * h/2f);
      float y2 = center+(pts[i+1] * h/2f);
      
      int alpha = (partial && (i < startX || i+1 > stopX)) ? 32 : 255;
      
      waveform.stroke(WAVEFORM_COL, alpha);
      waveform.line(i, y1, i+1, y2);
      
      if (y1>center && y2>center) {
        waveform.stroke(WAVEFORM_COL*.75f, alpha);
        waveform.line(i, center+1, i, y1-1);
      }
      if (y1<center && y2<center) {
        waveform.stroke(WAVEFORM_COL*.75f, alpha);
        waveform.line(i, center-1, i, y1+1);
      }
    }

    waveform.endDraw();
    
    if (unprocessedWaveform == null) {
      try {
        unprocessedWaveform = (PGraphics) waveform.clone();
      }
      catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
    }
  }

	private float[] computePoints(Sample s, int w) {
		
		float[] pts = new float[w]; // 1 pt per pixel
    //int mod = s.getNumFrames() / w;
		int mod = parent.frames.length / w;
    
    float max = 0;
    try
    {
    	// find the max value
      for (int i = 0; i < pts.length; i++) {
      	int idx = i * mod;
      	if (idx >= parent.frames.length)
      		System.err.println("ERROR:"+idx +">="+ parent.frames.length);
        float f = Math.abs(parent.frames[idx]);
        if (f > max) max = f;
      }
      
      for (int i = 0; i < pts.length; i++) {
      	int idx = i * mod;
        pts[i] = (parent.frames[idx] / max);
      }
    }
    catch (Throwable e)
    {
      System.out.println("[WARN] Error in computeWaveForm() : "+e);
    }
    
    pts[0] = pts[pts.length-1] = 0; // first and last
    
		return pts;
	}
  
  PGraphics waveform, unprocessedWaveform;

  public boolean contains(int x, int y) {
    return bounds.contains(x, y);
  }

  public void shiftLeft() {
    if (startX <= 0) return;
    startX = Math.max(startX - 2, 0);
    if (stopX > 0)
      stopX = Math.max(stopX - 2, startX + 2);
    doPartialPlayback(getSample());
  }

  public void shiftRight() {
    if (stopX >= w) return;
    startX = Math.min(startX + 2, stopX - 2);
    stopX = Math.min(stopX + 2, w);
    doPartialPlayback(getSample());
  }

  public int maxYPos() {
    return y + h;
  }

  boolean updateScrubPos()  // return true if we have a partial loop, else false
  {    
  	Sample s = getSample();

    int cur = 0;
    float tot = Float.MAX_VALUE;
    try
    {
      cur = s.getCurrentFrame();
      tot = s.getNumFrames();
    }
    catch (Exception e1)
    {
      System.out.println("[WARN] (Caught) updateScrubPos-0: Error getting currentFrame!");
      System.out.println(Pataclysm.stackToString(e1));
      return false;
    }
    
    sliderX = (int) PApplet.lerp(0, w, cur / tot);

    if (!s.isPlaying())  // start looping if stopped
    {
      try
      {
        s.repeat(startFrame, stopFrame);
        computeWaveForm(s, w, waveformH); // recompute whenever we restart the loop? could be optimized...
      }
      catch (Exception e)
      {
        System.out.println("[WARN] updateScrubPos-1:\n"+Pataclysm.stackToString(e));
      }
      lastFrame = Integer.MAX_VALUE;
    }

    // ok, back at start

    try
    {
      cur = s.getCurrentFrame();
    }
    catch (Exception e)
    {
      System.out.println("[WARN] updateScrubPos-2:\n"+Pataclysm.stackToString(e));
    }
    if (cur < lastFrame) 
    {
      checkProbability(s);
      
      // flip pan if bouncing
      if (parent.isBouncing()) 
        s.setPan(s.getPan() * -1f); 
    }
    
    lastFrame = cur;
    
    return isPartial(s);
  }

  private void checkProbability(Sample s)
  {
    float ap = parent.getAggregateProb();
    if (ap < 1) 
    {
      if (rand.nextFloat() > ap) 
        s.setVolume(0); // no trigger
      else if (!parent.muted) 
        parent.resetGainFromLastPos();
    }
  }

  public void reset() {
    startFrame = 0;
    stopFrame = 0;
    startX = 0;
    stopX = 0;
  }

  static float[] multiples = { 1, 16, 12, 8, 6, 4, 3, 2, 3/2f, 4/3f  };

  public void playPartial(int oneToTen) {
    
    Sample s = getSample();
    if (s == null || oneToTen > 9 || oneToTen < 0)
      return;

    float mult = multiples[oneToTen];
    
    // we're playing a sub-section
    startX = w * (.5f - ((1 / mult) * .5f));
      
    stopX = w - startX;
    
    //System.out.println("SampleScrubber.playPartial: "+(stopX-startX)/w);
     
    doPartialPlayback(s);
  }

  void doPartialPlayback(Sample s) {
    if (s == null) return;
    // System.out.println("start: "+startX+" stop: "+stopX);
    int len = s.getNumFrames();
    startFrame = (int) ((startX / (float)w) * len); // spurious warning from eclipse, casts needed here
    stopFrame = (int) ((stopX / (float)w) * len); // spurious warning from eclipse, casts needed here
    if (startFrame >= stopFrame)
      throw new RuntimeException("startFrame=" + startFrame + " >= stopFrame=" + stopFrame+ "!");
    if (s != null) getSample().stop();
  }

  public void resetStartStopFrames() {
    Sample s = getSample();
    startX = stopX = 0;
    startFrame = stopFrame = 0;
    try {
      stopFrame = s.getNumFrames();
    } catch (RuntimeException e) {
      System.err.println("[WARN] SampleScrubber.resetStartStopFrames() : " + e.getMessage());
    }
  }

  private Sample getSample() {
    return parent.getSample();
  }

  public void setVisible(boolean showUI) {
    this.visible = showUI;
  }

  private String getTag(int i, int j) {
    return "bank" + i + ".control" + j + ".scrubSlider.";
  }

  public void mousePressed(int mx, int my, boolean right) {

    if (right) {
      rightClicked(mx, my);
      return;
    }
    
    pressed = true;
    startX = mx - x;
  }

  public void mouseDragged(int mx, int my, boolean right) {
    if (!pressed || getSample() == null) return;
    stopX = Math.min(w, mx - x);
  }

  public void mouseReleased(int mx, int my, boolean right) 
  {
    Sample s = getSample();
    if (!pressed || s == null) return;

    pressed = false;
    stopX = Math.min(w, mx - x);
    
    if (stopX < startX) {// swap
      float tmp = startX;
      startX = stopX;
      stopX = tmp;
    }
    
    if (stopX - startX > 2)
      doPartialPlayback(s);
    else
      parent.resetSample();
    
    //AudioUtils.declickifyEnds(s);
  }
  
  private void rightClicked(int mx, int my) {
    Sample s = getSample();
    if (s == null) return;
    sliderX = mx - x;
    try {
      s.play((int) ((sliderX / (float) w) * s.getNumFrames()), s.getNumFrames());
    } catch (RuntimeException e) {
      System.err.println("rightClicked.CAUGHT: " + e+e.getMessage());
    }
  }
  
  private boolean isPartial(Sample s)
  {
    if (s == null) return false;
    return (startX  > 0 || (stopX > 0 && stopX < s.getNumFrames()));
  }

  /* Restarts the sample based on its current position, as stored in sliderX */
  public void restart()
  {
    Sample s = getSample();
    try {
      int start = (int) ((sliderX / (float) w) * s.getNumFrames());
      s.play(start, stopFrame);
    } 
    catch (RuntimeException e) {
      System.err.println("restart: " + e+e.getMessage());
    }
  }

  public void toXml(Properties p, int i, int j) {
    String tag = getTag(i, j);
    p.setProperty(tag + "sliderX", sliderX + "");
    p.setProperty(tag + "startFrame", startFrame + "");
    p.setProperty(tag + "stopFrame", stopFrame + "");
    p.setProperty(tag + "startX", startX + "");
    p.setProperty(tag + "stopX", stopX + "");
  }
  
  private Properties toXml(int i, int j) {
    Properties p = new Properties();
    toXml(p, i, j);
    return p;
  }

  public void fromXml(Properties p, int i, int j) {
    String tag = getTag(i, j);
    sliderX = Integer.parseInt(p.getProperty(tag + "sliderX"));
    startFrame = Integer.parseInt(p.getProperty(tag + "startFrame"));
    stopFrame = Integer.parseInt(p.getProperty(tag + "stopFrame"));
    startX = Float.parseFloat(p.getProperty(tag + "startX"));
    stopX = Float.parseFloat(p.getProperty(tag + "stopX"));
  }

  void copyScrubberProperties(SampleScrubber scrubSlider)
  {
    Properties p = toXml(-1, -1);
    fromXml(p, -1, -1);
    //System.out.println(p);
  }
  
  private static PApplet checkPApplet(PApplet pa) {
  	if (pa instanceof Pataclysm)
  		throw new RuntimeException("Unexpected state");
  	return pa;
	}
}// end
