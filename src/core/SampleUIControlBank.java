package core;

import java.awt.Rectangle;
import java.util.Properties;

import javax.swing.*;

import mkv.MyGUI.*;
import processing.core.PApplet;
import processing.core.PConstants;

public class SampleUIControlBank implements SamplerConstants, Actionable
{
  private static final int BANK_WIDTH = 203;

  static final int BANK_Y_SPACING = 155, BANK_Y_OFFSET = 15;
  
  boolean soloing, selected;
  int id, x, y, lastSliderGainPos, selectedControlIdx;
  MyGUIPinSlider volumeControl, probControl;
  MyGUILabel volumeLabel, probLabel;
  SampleUIControl[] uiControls;
  Pataclysm app;
  MyGUI gui;
   
  JPopupAppMenu bankJContextMenu;
  MyGUIButton bankMenuButton;

  Rectangle buttonBounds, clickBounds;
  private static int ID;
  

  SampleUIControlBank(Pataclysm p, MyGUI gui, int xPos)
  {
    this(p, gui, xPos, false);
  }

  public SampleUIControlBank(Pataclysm p, MyGUI gui, int x, boolean enabled)
  {
    this.app = p;
    this.x = x;
    this.id = ID++;
    this.gui = gui;
    this.selected = enabled;

    this.lastSliderGainPos = DEFAULT_BANK_SLIDER_GAIN_POS;

    this.createSampleControls();
    
    this.clickBounds = new Rectangle(x-3, 
      uiControls[uiControls.length-1].bounds.y + SampleUIControl.H +5, 203, 71);
    
    this.volumeControl = new MyGUIPinSlider(p, x+115, this.maxYPos() + 42, 140, 12, 0, 100);
    volumeControl.setValue(DEFAULT_BANK_SLIDER_GAIN_POS);
    
    this.probControl = new MyGUIPinSlider(p, volumeControl._x, volumeControl._y -30, 140, 12, 0, 100);
    probControl.setValue(100);
    
    this.volumeLabel = new MyGUILabel(p, "GAIN", x+5, volumeControl._y);
    this.probLabel = new MyGUILabel(p,   "PROB", volumeLabel._x, volumeLabel._y-30);
    //this.soloBox = new MyGUICheckBox(p, x + 12, volumeControl._y, "", 10, 10);

    gui.add(volumeLabel);
    gui.add(probLabel);
    
    gui.add(probControl);
    gui.add(volumeControl);
    
    buttonBounds = new Rectangle(x+SampleUIControl.W-6, app.height - 66, 10, 10);
    gui.add(bankMenuButton = new SamplerButton(p, 
      buttonBounds.x,buttonBounds.y,buttonBounds.height,buttonBounds.width));    

    this.lastSliderGainPos = DEFAULT_BANK_SLIDER_GAIN_POS;
    //updateVolumes();
  }

  public void draw()
  { 
    for (int i = 0; i < uiControls.length; i++)
      uiControls[i].draw();

    if (volumeControl.getValue() <= 0) {
      app.fill(200, 100, 100);
      app.text("[muted]", clickBounds.x+106,clickBounds.y+70);
    }
  
    // rectangle for the bank
    app.rectMode(PApplet.CORNER);
    app.noFill();
    if (volumeControl.getValue()==0)
      app.fill(0, 127);
    app.strokeWeight(2);
    app.stroke(BG_R, BG_G, BG_B, 63);
    app.rect(x - 3, BANK_Y_OFFSET-3, BANK_WIDTH, app.height - 70);
    
    // line separating the bank controls 
    app.line(clickBounds.x+6, clickBounds.y, clickBounds.x+195, clickBounds.y);
    app.strokeWeight(1);
  }
  
  public String toString() { return "Bank#"+id; }
  
  JPopupMenu getBankJPopupMenu()
  {
    if (bankJContextMenu == null) {
      bankJContextMenu = new JPopupAppMenu(this, ApplicationFrame.bankMenuNames, null, ApplicationFrame.nestedMenus);
      app.add(bankJContextMenu);
    }
    return bankJContextMenu;
  }
  
  // NOTE: this is only the very lower rect, below all the controls
  boolean contains(int mx, int my) {
    return clickBounds.contains(mx, my);
  }

  private int maxYPos()
  {
    return uiControls[NUM_CONTROLS_PER_BANK - 1].scrubSlider.maxYPos();
  }

  private void createSampleControls()
  {
    int y = BANK_Y_OFFSET;
    this.uiControls = new SampleUIControl[NUM_CONTROLS_PER_BANK];
    for (int j = 0; j < NUM_CONTROLS_PER_BANK; j++)
    {
      uiControls[j] = new SampleUIControl(app, this, gui, null, j, x, y);
      y += BANK_Y_SPACING;
    }
    setSelectedControl(selectedControlIdx, true);
  }

  public int incrControl(boolean manual)
  {
    return app.incrControl(this, manual);
  }
  
  public int decrControl(boolean manual)
  {
    return app.decrControl(this, manual);
  }

  public SampleUIControl selectedControl()
  {
    return uiControls[selectedControlIdx];
  }

  public int setSelectedControl(int idx, boolean manual)
  {
    selectedControlIdx = idx;
    return selectedControlIdx;
  }

  public SampleUIControl getControl(int idx)
  {
    return uiControls[idx];
  }

  private static void drawInfo(Pataclysm p, SampleUIControlBank[] controlSets)
  {
    //if (!PataClysm.SHOW_UI) return;

    int xoff = 65, yoff = 15;
    p.strokeWeight(2);
    p.fill(200);// p.guiR,p.guiG,p.guiB);
    p.textAlign(PConstants.CENTER);
    p.textSize(12);
    p.text("RACK:", xoff - 37, yoff + 5);
    for (int i = 0; i < controlSets.length; i++)
    {
      if (controlSets[i] == null)
      {
        p.fill(40);
        p.stroke(70);
      }
      else if (controlSets[i].selected)
      {
        p.fill(100);
        p.stroke(200);
      }
      else
      {
        p.fill(100);
        p.stroke(p.BG_R, p.BG_G, p.BG_B);
      }
      p.ellipse(xoff + i * 30, yoff, 20, 20);
      p.fill(200);
      p.text(i, xoff + i * 30, yoff + 5);
    }
    p.strokeWeight(1);
  }

  public float getProb()
  {
    if (probControl == null) return 0;
    return probControl.getValue() / 100f;    
  }
  
  public float getGain()
  {
    if (volumeControl == null) return 0;
    return volumeControl.getValue() / 100f;
  }

  public void setProb(float prob)
  {
    probControl.setValue((int) (prob * 100));
  }
  
  public void setVolume(float volume)
  {
    volumeControl.setValue((int) (volume * 100));
  }

/*  public void updateGains()
  {
    for (int i = 0; i < uiControls.length; i++)
      uiControls[i].mapToSlider(GAIN, uiControls[i].sliders[GAIN].getValue());
  }
  
  public void updateProbs()
  {
    for (int i = 0; i < uiControls.length; i++)
      uiControls[i].mapToSlider(PROB, uiControls[i].sliders[PROB].getValue());
  }*/

  public boolean isSolo()
  {
    return this.soloing;
  }

  public void setSolo(boolean b)
  {
    Pataclysm.uiMan.updateBankSoloMode(this, b);
  }

  private void mute()
  {
    //System.out.println(this+".mute()");
    volumeControl.setValue(0);  
    for (int i = 0; i < uiControls.length; i++) {
      int val = uiControls[i].rawSliderValue(GAIN_SLIDER);
      uiControls[i].mapToSlider(GAIN_SLIDER, val);
    }
  }
  
  private void unmute()
  {
    //System.out.println(this+".unmute()");
    volumeControl.setValue(lastSliderGainPos);
    for (int i = 0; i < uiControls.length; i++) {
      int val = uiControls[i].rawSliderValue(GAIN_SLIDER);
      uiControls[i].mapToSlider(GAIN_SLIDER, val);
    }
  }
  
  public void setMuted(boolean b)
  {
    if (b) mute();
    else unmute();
  }
  
  public void toXml(Properties p, int bankIdx)
  {
    p.setProperty("bank" + bankIdx + ".volume", getGain() + "");
    p.setProperty("bank" + bankIdx + ".prob", getProb() + "");
    p.setProperty("bank" + bankIdx + ".selectedControlIdx", selectedControlIdx + "");
    p.setProperty("bank" + bankIdx + ".solo", isSolo() + "");
    
    for (int controlIdx = 0; controlIdx < uiControls.length; controlIdx++)
      uiControls[controlIdx].toXml(p, bankIdx, controlIdx);
  }

  public void fromXml(Properties p, int i)
  {
    selectedControlIdx = (Integer.parseInt(p.getProperty("bank" + i + ".selectedControlIdx")));
    setVolume(Float.parseFloat(p.getProperty("bank" + i + ".volume")));
    setProb(Float.parseFloat(p.getProperty("bank" + i + ".prob")));
    try {
      setSolo(Pataclysm.getBool(p, "bank" + i +".solo"));
    }
    catch (Exception e) {
      System.out.println("[WARN] No property: 'solo' found for "+this);
    }
   
    //setSolo(p.getProperty("bank" + i + ".solo").equals("true"));
    for (int j = 0; j < uiControls.length; j++)
      uiControls[j].fromXml(p, i, j);
  }

  public void doAction(int mx, int my, String action)
  {
    //System.out.println(this+".doAction("+action+")");
    
    if (action.equals(MUTE)) {
      setMuted(!isMuted());
    }
    else if (action.equals(SOLO)) { 
      setSolo(!isSolo()); 
    }
    else if (action.equals(MIX_DOWN))  {
      app.mixDown(uiControls[uiControls.length-1], uiControls);
      selectedControlIdx = 0;
    }
    else if (action.equals(REVERT)) { 
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].revert(true);
      ((JCheckBoxMenuItem)ApplicationFrame.getSampleMenuItem(BOUNCE)).setSelected(false);
      ((JCheckBoxMenuItem)ApplicationFrame.getSampleMenuItem(SWEEP)).setSelected(false);
      ((JCheckBoxMenuItem)ApplicationFrame.getSampleMenuItem(REVERSE)).setSelected(false);
    }
    else if (action.equals(BOUNCE)) {
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].setBounce(true);
    }
    else if (action.equals(SWEEP)) {
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].setSweep(true);
    }
    else if (action.equals(REVERSE)) { 
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].reverse();
    }
    else if (action.equals(CLEAR)) { 
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].delete();
    }
    else if (action.equals(DECLICK)) { 
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].manualDeclickify();
    }
    else if (action.equals(DOUBLE)) { 
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].doDoubleLength();
    }
    else if (action.startsWith(SHIFT)) {
      int shift = Integer.parseInt(action.split("=")[1]);
      for (int i = 0; i < uiControls.length; i++)
        uiControls[i].doPitchShift(shift);
    }
    JMenuItem  jmi = ApplicationFrame.getSampleMenuItem(action);
    if (jmi instanceof JCheckBoxMenuItem)
      jmi.setSelected(!jmi.isSelected());
  }

  public boolean isMuted()
  {
    return volumeControl.getValue() <= 0;
  }

  public void refreshControls()
  {
    for (int i = 0; i < uiControls.length; i++)
      uiControls[i].refresh(!uiControls[i].isMuted());
  }
  
  public void setVisible(boolean b) {
    for (int i = 0; i < uiControls.length; i++)
      uiControls[i].setVisible(b);
  }

}// end
