package core;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;

import javax.swing.JFileChooser;
import javax.swing.JPopupMenu;

import mkv.MyGUI.*;
import pitaru.sonia.Sample;
import processing.core.PApplet;

public class SampleUIControl implements SamplerConstants, LerpListener, Actionable
{
  private static final float MAX_PAN = .8f, MIN_PAN = -.8f;
  
  private static int ID;
  private static String sampleDir;
  private static float[] clipboard;
  private static SampleUIControl copiedControl;

  private float[] unprocessed;
  private boolean bounce;
  private float alpha, aggregateProb;
  public float frames[], levelsMax;
  public String sampleName;
  int lastSliderGainPos, id;

  boolean useFFT = true;
  boolean pressed, muted, disabled, soloing;
  private int r, g, b, tremSize;

  float currentShift = 0;

  private PApplet p;
  Sample sample;
  private SampleUIControlBank bank;
  public SampleScrubber scrubSlider;
  public MyGUIButton sampleMenuButton;
  private MyGUILabel levelLabel;
  public LabelSlider[] sliders;
  private Lerp sweeper;

  JPopupAppMenu sampleJContextMenu;
  Rectangle bounds, buttonBounds;


  public SampleUIControl(Pata4 p, SampleUIControlBank bank, MyGUI gui, String text, int idx, int x, int y)
  {
    this.p = p;
    this.id = ++ID;
    this.bank = bank;
    this.bounds = new Rectangle(x, y, W, H);
    sampleDir = Pata4.dataFolder();

    buttonBounds = new Rectangle(x + W - 8, y + 8, 10, 10); // x+7 for left-edge
    gui.add(sampleMenuButton = new SamplerButton(p, buttonBounds.x, buttonBounds.y, buttonBounds.height, buttonBounds.width));

    sliders = new LabelSlider[Pata4.currentSliderTypes.length];

    scrubSlider = new SampleScrubber(p, this, x, y + 132);

    int yOff = y + 11;
    gui.add(levelLabel = new MyGUILabel(p, "LEVEL", x + 9, y+10));
    for (int i = 0; i < sliders.length; i++) {
      sliders[i] = new LabelSlider(p, Pata4.currentSliderTypes[i], gui, this, i, x + 6, yOff+=23);
      if (Pata4.currentSliderTypes[i] == SHIFT_SLIDER)
        sliders[i].setContinuous(false); // hmm???
    }
    
    lastSliderGainPos = GAIN_SLIDER.defaultValue;
    
    randomColor(p);
  }

  public void draw()
  {
    int x = bounds.x, y = bounds.y;

    p.rectMode(PApplet.CORNER);

    // colored rect
    p.noStroke();
    alpha = (muted || disabled || sample == null ? 40 : 127);
    p.fill(r, g, b, alpha);
    p.rect(x, y, W, H);

    // selected rect
    if (this == Pata4.currentControl())
    {
      p.noFill();
      p.stroke(255);
      p.strokeWeight(2);
      p.rect(x - 2, y - 1, W + 3, H + 2);
    }

    if (sample == null)
      return;

    if (!muted) {
      drawLevels();
    }
    else {
      p.fill(100,50,50);
      p.text("[muted]", x+115,y+12);
    }

    if (sweeper != null && sample != null) 
      sample.setPan(sweeper.update());

    scrubSlider.draw(sample);
  }
  
  private void drawLevels()
  {
    if (sample == null || Pata4.isExiting) return;
    try
    {
      float[] fft = sample.getSpectrum(SPECTRUM_LENGTH * 2, sample.getCurrentFrame());
      fftMax = 0;
      for (int i = 0; i < fft.length; i++)
      {
        float f = (fft[i]);
        if (f > fftMax)
          fftMax = f;
      }
    }
    catch (Exception e) { return; }
    //p.println(fftMax);
    p.rectMode(PApplet.CORNER);
    p.fill(Pata4.BG[0], Pata4.BG[1], Pata4.BG[2]);
    //p.fill(255);
    p.noStroke();
    //p.rect(getSlider(RATE_SLIDER).label._x+38, getSlider(RATE_SLIDER).label._y - 28, getSlider(RATE_SLIDER).slider._width-15, getSlider(RATE_SLIDER).slider._height);
    p.image(UIManager.meterImg, sliders[0].label._x+38, sliders[0].label._y - 28);
   // p.fill(200);

    float gain = 0;
    try {
      gain = sample.getVolume();
    }
    catch (Throwable e) {
      System.out.println("SampleUIControl.drawLevels() error getting volume: "+e);
    }
    
    float level = (fftMax * gain * LEVEL_GAIN_SCALE) * (sliders[0].slider._width-15);
    //p.rect(sliders[0].label._x+38, sliders[0].label._y - 28, level, sliders[0].slider._height);
    p.rect(sliders[0].label._x+38+level, sliders[0].label._y - 28, Math.max(0, (sliders[0].slider._width-15)-level), sliders[0].slider._height);
  }
  
  float getX()
  {
    return (float) bounds.getX();
  }

  float getY()
  {
    return (float) bounds.getY();
  }

  public String toString()
  {
    return "Control#" + id;
  }

  private void randomColor(PApplet p)
  {
    r = (int) p.random(100, 200);
    g = (int) p.random(100, 200);
    b = (int) p.random(100, 200);
  }

  public boolean contains(int x, int y)
  {
    return bounds.contains(x, y);
  }
  
  LabelSlider getSlider(SliderType type) {
    for (int i = 0; i < sliders.length; i++)
    {
      if (sliders[i].type == type)
        return sliders[i];
    }
    return null;
  }

 
  float fftMax = -1000;
  private void drawFFT()
  {
    if (sample == null)
      return;

    try
    {
      float[] fft = sample.getSpectrum(SPECTRUM_LENGTH * 2, sample.getCurrentFrame());

      float portion = 3;
      if (fft == null)
        return;
      int num = (int) Math.min(fft.length, PIN_SLIDER_W / portion);

      // get max level
      for (int i = 0; i < num; i++)
      {
        float f = Math.abs(fft[i]);
        if (f > fftMax)
          fftMax = f;
      }

      p.fill(Pata4.BG[0], Pata4.BG[1], Pata4.BG[2]);
      p.noStroke();

      float xOff = bounds.x + 45;
      float yOff = getSlider(PROB_SLIDER).label._y + 10;

      float hvol = H * (sample == null ? 0 : sample.getVolume());

      for (int i = 0; i < num; i++)
      {
        float lev = (fft[i] / fftMax) * hvol;// hvol;
        p.rect(xOff + (i * portion), yOff - lev, portion, lev);
      }
    }
    catch (Exception e)
    {
      if (!Pata4.isExiting) {
        System.out.println("SampleUIControl.drawFFT-Error2" + e);
//if (Sample.THROW_JSYN_ERRORS)
//  System.out.println(Pataclysm.stackToString(e));
      }
    }
  }

  void resetSliders()
  {
    for (int i = 0; i < sliders.length; i++)
      sliders[i].reset();
    
    if (Pata4.quantizeMode==MICRO_QUANTIZE)
      getSlider(PROB_SLIDER).setValue((int)(Pata4.microProb*100));
  }

  public Sample getSample()
  {
    return this.sample;
  }

  /* refresh params based on current slider vals -- IS THIS WORKING???*/
  public void refresh(boolean forceGain)
  {
    for (int i = 0; i < sliders.length-1; i++)
    {
      mapToSlider(sliders[i].type, sliders[i].getValue());  
    }
    if (forceGain)
      mapToSlider(GAIN_SLIDER, getSlider(GAIN_SLIDER).getValue());
  }

/*  // not used...
  private void refreshGain(Sample sample)
  {
    if (sample == null) return;
    int rawVal = getSlider(GAIN_SLIDER).getValue();
    float val = (rawVal / 100f) * PataClysm.masterGain * bank.getGain() * GAIN_SCALE;
    sample.setVolume(val); // 0 -> 1
  }*/

  public void mapToSlider(SliderType type, float rawVal)
  {
    if (sample == null) return;

    float val = 0f;
    if (type == RATE_SLIDER)
    {
      val = rawVal * 882; // 0 -> 88200
      // System.out.println("Slider[RATE].val="+val);
      sample.setRate(val);
    }
    else if (type == PAN_SLIDER)
    {
      val = (rawVal / 50f) - 1; // -1 -> 1
      sample.setPan(val);
    }  
    else if (type == GAIN_SLIDER)
    {
      if (rawVal < 5)
        rawVal = 0; // make silent if close
      val = (rawVal / 100f) * (Pata4.masterGain * bank.getGain() * GAIN_SCALE);
      // System.out.println("SampleUIControl.mapToSlider() bank: "+bank.getGain()+" master: "+SamplerFi.masterGain+" raw="+rawVal);
      sample.setVolume(val); // 0 -> 1
    }
    
    else if (type == SHIFT_SLIDER)
    {
      val = (rawVal * .24f) - 12; // -12 -> 12
      doPitchShift(val);
    }
    else if (type == TREM_SLIDER)
    {
      val = (rawVal / 10); // 0 -> 10
      doTremolo(val);
    }
    else if (type == PROB_SLIDER)
    {
      aggregateProb = (rawVal / 100f) * Pata4.masterProb * bank.getProb();
    }
  }

  public void cut()
  {
    // System.out.println("SampleUIControl.cut()");
    copy();
    delete();
    useControlDefaults();
  }

  public void stop()
  {
    if (sample != null && sample.isPlaying())
      sample.stop(1);
  }

  public void delete()
  {
    setSolo(false);
    stop();
    if (sample != null)
      sample.delete();
    sample = null;
    frames = unprocessed = null;
  }

  public void copy()
  {
    // System.out.println("SampleUIControl.copy()");
    if (sample == null) return;
    copiedControl = this;
  }

  // this is problematic... should make an exact revertable copy (rate, gain, trem, shift, etc) 
  public void paste()
  {
    if (copiedControl == null || copiedControl.unprocessed == null || copiedControl == this) // can't copy onto self?
    {
      System.err.println("[WARN] Attempt to copy control onto self...");
      return;
    }
    
    clipboard = new float[copiedControl.unprocessed.length]; 
    System.arraycopy(copiedControl.unprocessed, 0, clipboard, 0, clipboard.length);
    
    transferControlProperties(copiedControl);
    
    Sample s = new Sample(clipboard.length);
    s.write(clipboard);
    
    // sample-params
    s.setVolume(copiedControl.sample.getVolume());
    s.setPan(-1*copiedControl.sample.getPan());
    s.setRate(copiedControl.sample.getRate());
    
    setSample(s, false); // does this need to be true??
    
    // this needs to be tested!!!!
    scrubSlider.copyScrubberProperties(copiedControl.scrubSlider);
    
    // Need only the visual (slider) representation, no???
    if (getSlider(PROB_SLIDER) != null)
      setProb(copiedControl.getProb());
    if (getSlider(SHIFT_SLIDER) != null)
      setShift(copiedControl.getShift());
    if (getSlider(TREM_SLIDER) != null)
      setTrem(copiedControl.getTrem());
    
    // just set the sliders, don't adjust the actual sample!
    LabelSlider gainSlider = getSlider(GAIN_SLIDER);
    if (gainSlider != null) {
      LabelSlider gainSliderCopied = copiedControl.getSlider(GAIN_SLIDER);
      if (gainSliderCopied != null)
        gainSlider.setValue(gainSliderCopied.getValue());
    }
    LabelSlider rateSlider = getSlider(RATE_SLIDER);
    if (rateSlider != null) {
      LabelSlider rateSliderCopied = copiedControl.getSlider(RATE_SLIDER);
      if (rateSliderCopied != null)
        rateSlider.setValue(rateSliderCopied.getValue());
    }

    bank.incrControl(false); // ?
  }

  private void transferControlProperties(SampleUIControl copyingFrom)
  {
    setBounce(isBouncing());
    setSweep(isSweeping());
    muted = copyingFrom.muted;
    tremSize = copyingFrom.tremSize;
    disabled = copyingFrom.disabled;
    currentShift = copyingFrom.currentShift;
  }

  public void setSolo(boolean b)
  {
    Pata4.uiMan.updateSoloMode(this, b);
  }
  
  public void mute()
  {
    if (sample == null) return;
    muted = true;
    sample.setVolume(0);
  }
  
  public void unmute()
  {
    muted = false;
    if (sample == null) return;
    resetGainFromLastPos();
  }

  void resetGainFromLastPos()
  {
    mapToSlider(GAIN_SLIDER, lastSliderGainPos);
  }

  private float getGain()
  { // 0 - 1;
    return sample.getVolume();
  }

  private void setPropertyWithSliderType(SliderType type, int raw)
  {
    LabelSlider ls = getSlider(type);
    if (ls != null) ls.setValue(raw);
    mapToSlider(type, raw);
  }
  
  void setGain(float vol) // 0 - 1
  {
    int raw = (int) (vol * 100f / (Pata4.masterGain * bank.getGain() * GAIN_SCALE));
    setPropertyWithSliderType(GAIN_SLIDER, raw);
  }
  
  private void setRate(int theRate) // 0 -> 88200
  {
    int raw = theRate / 882;
    setPropertyWithSliderType(RATE_SLIDER, raw);
  }
  
  private void setProb(float p) // 0 - 1
  {
    int raw = (int) (p * 100f);
    setPropertyWithSliderType(PROB_SLIDER, raw);
  }
  
  private void setShift(float shift) // -12 -> 12
  {
    int raw = (int) PApplet.map(shift, -12, 12, 0, 100);
    setPropertyWithSliderType(SHIFT_SLIDER, raw);
  }

  private void setTrem(float trem) // 0-9
  {
    int raw = (int) (trem * 10);
    setPropertyWithSliderType(TREM_SLIDER, raw);
  }

  private void setPan(float p) // -1 -> 1
  {
    int raw = ((int) (p * 50)) + 50;
    setPropertyWithSliderType(PAN_SLIDER, raw);
    /*
    LabelSlider pan = getSlider(PAN_SLIDER);
    if (pan != null)
    {
      int raw = ((int) (p * 50)) + 50;
      getSlider(PAN_SLIDER).setValue(raw);
      mapToSlider(PAN_SLIDER, raw);
    }
    else
      sample.setPan(p);*/
  }
  
  //   ------------------------------
  private float getProb()
  { // 0 - 1;
    return getSlider(PROB_SLIDER).getValue() / 100f;
  }

  private float getShift() { // -12 -> 12
    return (getSlider(SHIFT_SLIDER).getValue() * .24f) - 12;
  }
  
  private int getRate()
  { // 0 -> 88200
    return getSlider(RATE_SLIDER).getValue() * 882;
  }

  private float getTrem()
  { // 0-9
    float trem = getSlider(TREM_SLIDER).getValue() / 10f;
    // System.out.println("getTrem() -> "+trem);
    return trem;
  }

  private float getPan()
  {
    return sample.getPan();
  }

  private Sample cloneDuplicateSample(Sample newSample)
  {
    // We have a sample, check for a duplicate (why?)
    List l = ((Pata4) p).getAllSamples();
    if (l.contains(newSample))
    {
      // System.out.print("Cloning duplicate: "+newSample);
      newSample = cloneSample(newSample);
      // System.out.println(" -> "+newSample);
    }
    return newSample;
  }

  public void setSample(String sName, boolean useDefaults, boolean declickify)
  {
    // System.out.println("SampleUIControl.setSample("+sName+","+useDefaults+","+declickify+")");
    this.sampleName = sName;
    Sample samp = null;
    try
    {
      samp = new Sample(sName);
    }
    catch (RuntimeException e)
    {
      System.err.println("Unable to load: " + sName + " with" +
          " cwd='"+ System.getProperty("user.dir") + "'");
    }

    if (samp != null)
    {
      setSample(samp, useDefaults);
    }
  }

  void setSample(Sample s, boolean useDefaults)
  {
    // System.out.println("SampleUIControl.setSample
      // + (" + s + ", " + useDefaults + ")");

    this.stop();

    randomColor(p);

    this.scrubSlider.waveform = null; // clear waveform

    // update the sample
    Sample lastSample = sample; // previous

    // write sample data to 'frames
    this.frames = new float[s.getNumFrames()];
    s.read(frames);

    // save orig data to 'unprocessed'
    this.unprocessed = new float[frames.length];
    System.arraycopy(frames, 0, unprocessed, 0, frames.length);

    sample = s; // current

    if (useDefaults)
      useControlDefaults();

    // clear-out the old sample
    if (lastSample != null)
      lastSample.delete();
  }

  private void useControlDefaults()
  {
    resetSliders();

    // reset any pan effects
    sweeper = null;
    LabelSlider pan = getSlider(PAN_SLIDER);
    if (pan != null)
      pan.setValue((int) (randomPan() * 100));
    else if (sample != null)
      sample.setPan(randomPan()); // manual if no slider

    refresh(true);
    
    // do we want this here?
    if (sample != null)
      scrubSlider.resetStartStopFrames();
  }

  private float randomPan()
  {
    return Math.random() > .5 ? MIN_PAN/2f: MAX_PAN/2f;
  }
  
  void resetSample() {
    if (sample == null) return;
    scrubSlider.resetStartStopFrames();
    if (sample != null) sample.stop();
  }
  
  private Sample cloneSample(Sample s)
  {
    float[] data = new float[s.getNumFrames()];
    s.read(data);
    Sample cloned = new Sample(data.length);
    cloned.write(data);
    return cloned;
  }

  void setVisible(boolean visible)
  {
    // selectBox._visible = visible;
    // soloBox._visible = visible;
    this.levelLabel._visible = visible;
    this.sampleMenuButton._visible = visible;
    scrubSlider.setVisible(visible);
    for (int i = 0; i < sliders.length; i++)
      sliders[i].setVisible(visible);
  }

  public int rawSliderValue(SliderType type)
  {
    return getSlider(type).getValue();
  }

  public boolean isSolo()
  {
    return this.soloing;
  }

  class LabelSlider
  {
    SliderType type;
    MyGUILabel label;
    GUIPinSlider slider;
    SampleUIControl parent;
    int idx, startValue;

    public LabelSlider(PApplet p, SliderType type, MyGUI gui, SampleUIControl vui, int idx, int x, int y)
    {
      this.idx = idx;
      this.type = type;
      this.parent = vui;
      this.startValue = type.defaultValue;
      label = new MyGUILabel(p, type.label, x + 5, y);
      gui.add(label);
      slider = new GUIPinSlider(p, x + 115, y, PIN_SLIDER_W, PIN_SLIDER_H, 0, 100); // subclass
      slider.setValue(startValue);
      gui.add(slider);
    }
    
    public boolean isContinuous()
    {
      return slider.isContinuous();
    }

    public void setContinuous(boolean continuous)
    {
      slider.setContinuous(continuous);
    }

    public void setVisible(boolean visible)
    {
      label._visible = visible;
      slider._visible = visible;
    }

    public void setText(String s)
    {
      this.label.setText(s);
    }

    public String getXmlTag()
    {
      return "slider."+this.label._text.toLowerCase();
    }

    public int getValue()
    {
      return slider.getValue();
    }

    public void setValue(int v)
    {
      //System.out.println("SampleUIControl.LabelSlider.setValue("+label._text+"="+v+")");
      slider.setValue(v);
      if (parent != null)
        parent.mapToSlider(type, v);
    }

    public void reset()
    {
      // System.out.println("SampleUIControl.LabelSlider.reset("+label._text+"="+slider.getValue()+")");
      slider.setValue(this.startValue);
    }

    public void setX(int x)
    {
      this.label._x = x;
      this.slider._x = x + 115;
    }
  }

  public String getSampleName()
  {
    return sampleName;
  }

  private String getTag(int i, int j)
  {
    return "bank" + i + ".control" + j + ".";
  }

  private void setColor(String[] rgb)
  {
    r = Integer.parseInt(rgb[0]);
    g = Integer.parseInt(rgb[1]);
    b = Integer.parseInt(rgb[2]);
  }

  private boolean getBool(Properties p, String key)
  {
    return Pata4.getBool(p, key);
  }

  // mouse methods ---------------------

  public boolean isPressed()
  {
    return pressed;
  }

  public void doAction(int mx, int my, String item)
  {
    if (item.equals(REVERT))
      revert(true);
    
    else if (item.equals(REVERSE))
      reverse();

    else if (item.equals(DECLICK))
      manualDeclickify();

    else if (item.equals(CUT))
      cut();

    else if (item.equals(COPY))
      copy();

    else if (item.equals(PASTE))
      paste();

    else if (item.equals(OPEN))
      load();

    else if (item.equals(MUTE))
      setMuted(!isMuted());

    else if (item.equals(PAD))
      doubleLength();
    
    else if (item.equals(SOLO))
      setSolo(!isSolo());

    else if (item.equals(SWEEP))
      setSweep(!isSweeping());

    else if (item.equals(BOUNCE))
      setBounce(!isBouncing());
    
    else if (item.startsWith(SHIFT)) {
      String shiftStr = item.split("=")[1].trim();
      int shift = Integer.parseInt(shiftStr);
      doPitchShift(shift);
    }
  }

  void doubleLength()
  { 
    if (sample == null) return;
    
    int newLen = sample.getNumFrames() * 2;
    
    sample = padSample(sample, newLen);
    
    scrubSlider.unprocessedWaveform = null;
    
    scrubSlider.resetStartStopFrames();
    
    sample.stop();
  }
  
  private Sample padSample(Sample s, int newLength)
  {
    // get the current data in the sample
    float[] current = new float[s.getNumFrames()];
    s.read(current);
    
    // created a larger array with ends padded
    float[] newdata = AudioUtils.padArray(current, newLength);
    
    Sample lastSample = s; // previous

    Sample result = cloneSampleWith(newdata);

    lastSample.delete();
    
    return result;
  }
  
  public boolean isSweeping()
  {
    return sweeper != null;
  }

  void setSweep(boolean b)
  {
    // System.out.println("SampleUIControl.setSweep("+b+")");
    if (sample == null) return;
    
    if (b)
    {
      setBounce(false);
      if (sweeper == null)
        sweeper = new Lerp(this);
      setPan(getPan() < 0 ? MIN_PAN : MAX_PAN); // full pan
      lerpComplete(sample.getPan());
    }
    else
    {
      if (sweeper != null)
      {
        sweeper = null; // reset pan
        sample.setPan(randomPan());
      }
    }
  }

  public boolean isBouncing()
  {
    return bounce;
  }

  void setBounce(boolean b)
  {    if (b)
    {
      if (sample != null)
      {
        setSweep(false); // pan full
        setPan(getPan() < 0 ? MIN_PAN : MAX_PAN);
      }
    }
    else
    {
      if (this.bounce) // reset pan
        sample.setPan(randomPan());
    }

    this.bounce = b;
  }
  
/*  private void declickifyEnds()
  {
    AudioUtils.declickifyEnds(sample);
    sample.read(unprocessed);
  }
  */
  void manualDeclickify()//PartialEnds()
  {
//    if (sample == null) return;

    // create new data
    //float[] tmp = new float[frames.length];
    //System.arraycopy(frames, 0, tmp, 0, frames.length);
    //float[] proc = 
      
    AudioUtils.declickifyEnds(frames, scrubSlider.startFrame, scrubSlider.stopFrame);

    Sample lastSample = sample; // previous

    sample = cloneSampleWith(frames); // reset sample

    if (lastSample != null) lastSample.delete();
  }

  Sample cloneSampleWith(float[] proc)
  {
    // reset if float[] is a different size
/*    if (proc.length != sample.getNumFrames()) {
      unprocessed = new float[proc.length];
      System.arraycopy(proc, 0, unprocessed, 0, proc.length);
    }*/
    
    // write the changed data to new sample
    Sample s = new Sample(proc.length);
    s.write(proc);
    
    s.setRate(sample.getRate());
    s.setVolume(sample.getVolume());
    s.setPan(sample.getPan());

    // update 'frames'
    frames = new float[proc.length];
    s.read(frames);
    
    // and start it...
    s.play(scrubSlider.startFrame, scrubSlider.stopFrame);

    return s;
  }
  
  void reverse() 
  {
    Sample lastSample = sample; // previous

    if (sample == null) return;

    float[] proc = new float[frames.length];
    for (int j = 0; j < proc.length; j++)
      proc[j] = frames[frames.length-1-j];
   
    sample = cloneSampleWith(proc);

    lastSample.delete();
  }

  // need to do this in a thread...?
  void doPitchShift(float steps) // 12 <= x <= 12
  {
    if (steps == currentShift ) {
      //System.out.println("SampleUIControl.ignoring-shift="+steps);
      return;
    }
    
    System.out.println("SampleUIControl.doPitchShift("+steps+")");
    
    Sample lastSample = sample; // previous
    revert(false);

    if (sample == null) return;

    float[] proc = new float[frames.length];
    sample.read(proc);
   
    PitchShifter.shift(proc, steps);

    sample = cloneSampleWith(proc);
    
    currentShift = steps;

    lastSample.delete();
  }
  
  private void doTremolo(float trem) // 0 <= x < 10
  {
    if (((int) (trem * TREM_SCALE)) == tremSize)
      return;
    
    this.tremSize = (int) (trem * TREM_SCALE);

    Sample lastSample = sample; // previous
    revert(false);

    if (sample == null || tremSize < 1)
      return;

    //float[] proc = digitalTrem();
    float[] proc = TremoloUtil.doTremolo
      (frames, tremSize, tremSize, AudioUtils.NUM_SMOOTHING_FRAMES);
    
    sample = cloneSampleWith(proc);
    
    lastSample.delete();
    
    //declickify();  // this shouldnt be necessary!  
  }

  private float[] digitalTrem()
  {
    float[] proc = new float[frames.length];
    for (int i = 0; i < frames.length; i++)
    {
      if (i % (2 * tremSize) >= tremSize)
        proc[i] = frames[i];
      else
        proc[i] = 0;
    }
    return proc;
  }

  public void revert(boolean resetValues)
  {
    if (sample == null) return;

    // restore orig data from 'unprocessed' TODO: fix bug here
    System.arraycopy(unprocessed, 0, frames, 0, frames.length);
    sample.write(frames);
    
    if (resetValues) {
      bounce = false;
      sweeper = null;
      getSlider(TREM_SLIDER).setValue(0);
      scrubSlider.resetStartStopFrames();
      scrubSlider.waveform = scrubSlider.unprocessedWaveform;
    }

    // scrubSlider.waveform = null;

    sample.stop();
    
    // scrubSlider.waveform = scrubSlider.unprocessedWaveform;
  }

  private void load()
  {
    final JFileChooser fc = new JFileChooser(new File(sampleDir));
    new Thread()
    {
      public void run()
      {
        fc.showOpenDialog(p);
        File selFile = fc.getSelectedFile();
        if (selFile != null)
        {
          sampleDir = selFile.getParent();
          setSample(selFile.getPath(), true, true);
        }
      }
    }.start();
  }

  public boolean isMuted()
  {
    return (sample == null || sample.getVolume() <= 0);
  }

  private void setMuted(boolean b)
  {
    if (b)
      mute();
    else
      unmute();
  }

  public void lerpComplete(float value)
  {
    // System.out.println("SampleUIControl.lerpComplete("+value+") at "+p.millis());
    if (sweeper != null)
    {
      float target = (value > 0) ? MIN_PAN : MAX_PAN; // sweep over 2 * the length of the sample?
      
      // System.out.println("SampleUIControl.lerpComplete: val="+sample.getPan()+" targ="+target);
      sweeper.setTarget(sample.getPan(), target, 
        sample.getNumFrames() / (float) SAMPLE_RATE);
    }
  }

  public void mousePressed(MouseEvent me)
  {
    pressed = true;
    int mx = me.getX(), my = me.getY();
    boolean right = UIManager.rightMouseButton(me);
    if (scrubSlider.contains(mx, my))
    {
      scrubSlider.mousePressed(mx, my, right);
      return;
    }
  }

  public void mouseDragged(MouseEvent me)
  {
    int mx = me.getX(), my = me.getY();
    boolean right = UIManager.rightMouseButton(me);
    if (!UIManager.rightMouseButton(me))
      scrubSlider.mouseDragged(mx, my, right);
  }

  public void mouseReleased(MouseEvent me)
  {
    pressed = false;;
    if (!me.isPopupTrigger())
      scrubSlider.mouseReleased(me.getX(), me.getY(), UIManager.rightMouseButton(me));
  }

  public void mouseDoubleClick()
  {
    setSolo(!isSolo()); 
    // dumpProperties();
  }

  void dumpProperties()
  {
    Properties p = new Properties();
    toXml(p, id, -1, true);
    for (Iterator it = p.keySet().iterator(); it.hasNext();)
    {
      String k = (String) it.next();
      String v = (String) p.get(k);
      System.out.println(k + "=" + v);
    }
  }

  JPopupMenu getSampleJPopupMenu()
  {
    if (sampleJContextMenu == null)
    {
      sampleJContextMenu = new JPopupAppMenu(this, ApplicationFrame.sampleMenuNames, ApplicationFrame.sampleCbMenuNames, ApplicationFrame.nestedMenus);
      p.add(sampleJContextMenu);
    }
    return sampleJContextMenu;
  }

  // returns a sample holding the frames actually being triggered (startFrame->stopFrame)
  public Sample getActualSample()
  {
    int num = scrubSlider.stopFrame - scrubSlider.startFrame;
    float[] actual = new float[num];
    System.arraycopy(frames, scrubSlider.startFrame, actual, 0, num);
    Sample s = new Sample(num);
    s.write(actual);
    return s;
  }

  public float getAggregateProb()
  {
    return aggregateProb;
  }

  public static final int W = 198, H = 150;
  private static final float GAIN_SCALE = 2f;
  private final int PIN_SLIDER_W = 140, PIN_SLIDER_H=12;
  
  public void toXml(Properties p, int bankIdx, int controlIdx)
  {
    this.toXml(p, bankIdx, controlIdx, false);
  }

  public void toXml(Properties p, int bankIdx, int controlIdx, boolean debugOnly)
  {
    String tag = getTag(bankIdx, controlIdx);
    if (sample == null)
      return;

    // booleans ==============================
    p.setProperty(tag + "solo", isSolo() + "");
    p.setProperty(tag + "sweep", isSweeping() + "");
    p.setProperty(tag + "bounce", isBouncing() + "");

    // properties ==============================
    p.setProperty(tag + "color", (r + "," + g + "," + b));
    
    p.setProperty(tag + "prob",  aggregateProb + "");
    p.setProperty(tag + "dtrem", tremSize + "");
    p.setProperty(tag + "shift", currentShift + "");
    //System.out.println("Saving shift="+currentShift);
    p.setProperty(tag + "rate",  sample.getRate() + "");
    p.setProperty(tag + "pan",   sample.getPan() + "");
    p.setProperty(tag + "gain",  sample.getVolume() + "");

/*    // setup the sliders =======================
    for (int k = 0; k < sliders.length; k++)
    {
      p.setProperty(getTag(bankIdx, controlIdx) + "slider" + k 
          + ".value", sliders[k].slider.getValue() + "");
    }*/

    // gain, rate, pan =========================

    // create the scrubber =====================
    scrubSlider.toXml(p, bankIdx, controlIdx);

    // write sample to file ====================
    String dataDir = p.getProperty("data.dir");
    if (!debugOnly && dataDir == null)
      throw new RuntimeException("SampleUIControl.toXml() :: Null data dir!");

    // if its a new sample, assign a name (no ext)
    sampleName = dataDir + "/" + tag + "sample";
    p.setProperty(tag + "sample", sampleName);

    // un-trem and write to file
    Sample s = new Sample(unprocessed.length);
    s.write(unprocessed);
    if (!debugOnly)
      s.saveFile(sampleName);
    s.delete();
  }

  public void fromXml(Properties props, int i, int j)
  {
    String tag = getTag(i, j);

    String sampleName = props.getProperty(tag + "sample");
    if (sampleName == null)
    {
      sampleName = props.getProperty(tag + "sample");
      if (sampleName == null) // try old-name
        return;
    }

    scrubSlider.fromXml(props, i, j);

    setSample(sampleName + ".wav", false, true);

    if (sample == null)
    {
      System.err.println("[ERROR] Unable to load: " + sampleName);
      return;
    }

    // booleans ----------------------------
    setSolo(getBool(props, tag + "solo"));
    setSweep(getBool(props, tag + "sweep"));
    setBounce(getBool(props, tag + "bounce"));

    // properties --------------------------
    setPan(Float.parseFloat(props.getProperty(tag   + "pan")));
    setGain(Float.parseFloat(props.getProperty(tag  + "gain")));
    setProb(Float.parseFloat(props.getProperty(tag  + "prob")));
    setRate(Integer.parseInt(props.getProperty(tag  + "rate")));
    setTrem(Integer.parseInt(props.getProperty(tag  + "dtrem")) / TREM_SCALE);
    
    try
    {
      setShift(Float.parseFloat(props.getProperty(tag + "shift")));
    }
    catch (Exception e1)
    {
      System.err.println("[WARN] Problem parsing shift, using 0.0...");
      setShift(0);
    }
    
    /*catch (NumberFormatException e) {
      //System.out.println("Unable to parse dtrem: " + props.getProperty(tag + "dtrem"));
    }*/
    try {
      setColor(props.getProperty(tag + "color").split(","));
    }
    catch (Exception e)
    {
      System.err.println("[WARN] Problem parsing color, using random...");
      randomColor(p);
    }
    
    // assume we always have a gain slider...
    lastSliderGainPos = getSlider(GAIN_SLIDER).getValue();
    
    refresh(true);
    
    try {
      // restart sample where it stopped on save()
      scrubSlider.restart();
    }
    catch (RuntimeException e) {
      System.err.println("SampleScrubberfromXml() : " + e+"/"+e.getMessage());
    }
  }

}// end