package core;

import java.awt.Rectangle;
import java.util.Properties;
import java.util.Random;

import pitaru.sonia.Sample;
import processing.core.*;

public class SampleScrubber implements SamplerConstants
{
  private static final int DEFAULT_SCRUB_W = 194, DEFAULT_SCRUB_H = 24;

  PApplet p;
  SampleUIControl parent;

  int x, y, w, h, stopFrame;
  private boolean visible = true, pressed;
  private int lastFrame;
  int sliderX;
  int startFrame, waveformH;
  private static Random rand = new Random();
  private float startX = 0, stopX = 0;

  private Rectangle bounds;

  /* for testing only...
  SampleScrubber(PApplet p, String sampleName, int xPos, int yPos, int w, int h) {
    this(p, (SampleUIControl) null, xPos, yPos, w, h);
    this.testSample = new Sample(sampleName);
    this.stopFrame = testSample.getNumFrames();
  }*/

  public SampleScrubber(Pata4 p, SampleUIControl svui, int xPos, int yPos) {
    this(p, svui, xPos, yPos, DEFAULT_SCRUB_W, DEFAULT_SCRUB_H);
  }

  private SampleScrubber
    (PApplet p, SampleUIControl sc, int x, int y, int w, int h) {
    // System.out.println("SampleSlider("+xPos+","+yPos+","+w+","+h+")");
    this.parent = sc;
    if (parent != null) {
      this.p = p;
      Sample s = getSample();
      if (s != null)
        this.stopFrame = s.getNumFrames();
    }
    this.w = w;
    this.h = h;
    this.y = y;
    this.x = sliderX = x;
    this.waveformH = h+12;
    this.bounds = new Rectangle(x, y - h / 2, w, h);
  }

  public void draw(Sample s) 
  {
    //parent.refreshGain(s);
    
    updateScrubPos(s);
    
    boolean partial = isPartial(s);

    if (!visible) return;

    // setup pen --------------------------
    p.fill(WAVEFORM_COL);
    p.stroke(WAVEFORM_COL);
    p.strokeWeight(1);
    p.rectMode(PConstants.CENTER);

    // horiz line -------------------------
    //p.stroke(SamplerFi.BG[0], SamplerFi.BG[1], SamplerFi.BG[2]);
    if (partial) {
      p.stroke(WAVEFORM_COL);
      p.line(x + startX, y, x + stopX, y); 
    }
    else {
      p.stroke(WAVEFORM_COL);
      p.line(x, y, x + w + 2, y);
    }
    
    // waveform  ---------------------------
    if (waveform == null) 
      computeWaveForm(s, w+4, waveformH);
    p.image(waveform, bounds.x, y - waveformH/2f);
    
    // red loop line -----------------------
    if (pressed & partial) {
     /// p.strokeWeight(2);
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
    //System.out.println("SampleScrubber."+parent.id+".computeWaveForm()");
    
    float[] pts = new float[w]; // 1 pt per pixel
    boolean partial = isPartial(s);
    int mod = s.getNumFrames() / w;
    
    float max = 0;
    try
    {
      for (int i = 0; i < pts.length; i++) {
        float f = Math.abs(parent.frames[i * mod]);
        if (f > max) max = f;
      }
      for (int i = 0; i < pts.length; i++)
        pts[i] = (parent.frames[i * mod] / max);
    }
    catch (Throwable e)
    {
      System.out.println("[WARN] Error in computeWaveForm() : "+e);
    }
    
    pts[0] = pts[pts.length-1] = 0; // first and last
    
    waveform = p.createGraphics(w, h, PApplet.JAVA2D);
    float center = waveform.height/2f;
    
    waveform.beginDraw();
    
      waveform.noStroke();//255,0,0);
      waveform.fill(0,0);
      waveform.rect(0,0, waveform.width-1, waveform.height-1);
      
      waveform.smooth();
      waveform.noFill();
      
      for (int i = 0; i < pts.length-1; i++) {
        float y1 = center+(pts[i] * h/2f);
        float y2 = center+(pts[i+1] * h/2f);
        
        int alpha = 255;

        if (partial && (i < startX || i+1 > stopX))
          alpha = 32;
        
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
    
    if (unprocessedWaveform == null)
      try {
        unprocessedWaveform = (PGraphics) waveform.clone();
      }
      catch (CloneNotSupportedException e) {
        e.printStackTrace();
      }
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

  void updateScrubPos(Sample s) 
  {
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
      System.out.println(Pata4.stackToString(e1));
      return;
    }
    
    sliderX = (int) PApplet.lerp(0, w, cur / (float) tot);

    if (!s.isPlaying())  // start looping if stopped
    {
      try
      {
        s.repeat(startFrame, stopFrame);
        //System.out.println("SampleScrubber."+parent.id+".updateScrubPos().computeWaveForm");
        
        
        // really?? every loop? try without this
        computeWaveForm(s, w, waveformH);
      }
      catch (Exception e)
      {
        System.out.println("[WARN] updateScrubPos-1:\n"+Pata4.stackToString(e));
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
      System.out.println("[WARN] updateScrubPos-2:\n"+Pata4.stackToString(e));
    }
    if (cur < lastFrame) 
    {
      checkProbability(s);
      
      // flip pan if bouncing
      if (parent.isBouncing()) 
        s.setPan(s.getPan() * -1f); 
    }
    lastFrame = cur;
  }

  private void checkProbability(Sample s)
  {
    float ap = parent.getAggregateProb();
    if (ap < 1) 
    {
      if (rand.nextFloat() > ap) 
      {
        s.setVolume(0); // no trigger
      }
      else if (!parent.muted) 
      {
        parent.resetGainFromLastPos();
      }
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
    startFrame = (int) ((startX / (float) w) * len);
    stopFrame = (int) ((stopX / (float) w) * len);
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
    //System.out.println("SampleScrubber.resetStartStopFrames().startFrame="+startFrame+" stopFrame="+stopFrame);
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

}// end
