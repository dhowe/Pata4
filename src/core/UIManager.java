package core;

import java.awt.event.*;

import mkv.MyGUI.*;
import processing.core.PFont;
import processing.core.PImage;
import core.SampleUIControl.LabelSlider;

public class UIManager implements SamplerConstants, ActionListener
{
  public static String DEFAULT_SAMPLE_DIR = "src/data";

  private static boolean shiftDown;
  static PImage meterImg;

  MyGUI gui;
  PFont font;
  Pataclysm app;
  SampleUIControlBank[] banks;

  public UIManager(Pataclysm p)
  {
    this.app = p;

    p.smooth();
    font = p.loadFont(FONT);
    p.textFont(font, 32);
     
    meterImg = p.loadImage(METER_IMG_12); 

    gui = new MyGUI(p, 260);
    gui.getStyle().setFont(font, 10);
    gui.getStyle().buttonFace = p.color(0);
    gui.getStyle().buttonShadow = p.color(20);
    gui.getStyle().tintColor(p.color(BG_TINT)); // tint color
    gui.getStyle().buttonText = p.color(BG_R, BG_G, BG_B); // text color
    gui.getStyle().face = p.color(255); // slider color

    // global sliders ---------------------------

    p.gainLabel = new MyGUILabel(p, "GAIN", UI_XOFFSET + 5, Pataclysm.masterControlsY);
    p.gain = new GUIPinSlider(p, p.gainLabel._x + 110, p.gainLabel._y, 140, 12, 0, 100);
    gui.add(p.gain);
    gui.add(p.gainLabel);

    p.probLabel = new MyGUILabel(p, "PROB", p.gain._x + 108, Pataclysm.masterControlsY);
    p.prob = new GUIPinSlider(p, p.probLabel._x + 110, p.probLabel._y, 140, 12, 0, 100);
    p.prob.setValue(100);
    gui.add(p.prob);
    gui.add(p.probLabel);

    this.banks = new SampleUIControlBank[NUM_BANKS];
    for (int i = 0; i < banks.length; i++)
      banks[i] = new SampleUIControlBank(p, gui, UI_XOFFSET + (i * UI_BANK_SPACING), true);
    
    app.controlBanks = banks; // hack...
    app.updateBankView(); // needed?
  }

  public void keyReleased(char key, int keyCode)
  {
    if (keyCode == 16)
      shiftDown = false;
  }

  @SuppressWarnings("static-access")
  public void keyPressed(char key, int keyCode)
  {
    //System.out.println("keyPressed: "+key+"="+keyCode);
    
    SampleUIControlBank bank = app.currentControlBank();
    SampleUIControl control = app.currentControl();

    if (keyCode == 27)  // what should this do?
    {  
      // escape key  ???????????
      control.revert(true);
      control.resetSample();
      return;
    }
    else if (keyCode == 127)
    { 
      // forward-delete key
      control.delete();
      return;
    }
    else if (keyCode == 16)
    { 
      // shift key
      shiftDown = true;
      return;
    }
    // shift sample left
    else if (key == '<')
    {
      bank.selectedControl().scrubSlider.shiftLeft();
    }
    // shift sample right
    else if (key == '>')
    {
      bank.selectedControl().scrubSlider.shiftRight();
    }
    // key codes -------------------
    else if (keyCode == 32)
    {
      // space-bar
      app.toggleRecord();
    }
    else if (keyCode == 8)
    {
      // backspace
      if (app.recording())
        app.recordStop();
    }
    else if (keyCode == 40)
    { 
      // up-arrow
      bank.incrControl(true);
    }
    else if (keyCode == 38)
    { 
      // down-arrow
      bank.decrControl(true);
    }
    else if (keyCode == 37)
    {
      // left-arrow
      int idx = bank.selectedControlIdx;
      app.decrControlBank(); // reset selectedIdx for bank
      app.currentControlBank().selectedControlIdx = idx;
    }
    else if (keyCode == 39)
    {
      // right-arrow
      int idx = bank.selectedControlIdx;
      app.incrControlBank(); // reset selectedIdx for bank
      app.currentControlBank().selectedControlIdx = idx;
    }
    else if (keyCode > 111 && keyCode < 122) // F1-F10
    { 
      int i = keyCode - 111;
      control.getSlider(TREM_SLIDER).setValue(i * 10);
      control.mapToSlider(TREM_SLIDER, i * 10);
    }
    else if (keyCode < 58 && keyCode > 47 || keyCode == 192) // number keys (0-9)
    {
      // what is 192??? ~?s
      if (shiftDown())
      {
        control.scrubSlider.playPartial(keyCode - 48);
        //control.declickify();
        return;
      }
      
      LabelSlider ls = control.getSlider(SHIFT_SLIDER);
      if (ls != null)
      {
        adjustShift(keyCode, control);
      }
      else 
      {
        ls = control.getSlider(RATE_SLIDER);
        if (ls != null)
          adjustRate(keyCode, control);
      }
    }
  }

  // need to map these to nice values
  private void adjustShift(int keyCode, SampleUIControl control)
  {
    float sliderVal = (keyCode - 48) * 10;
    if (sliderVal == 0)  sliderVal = 100;
    control.getSlider(SHIFT_SLIDER).setValue((int) sliderVal);
    control.mapToSlider(SHIFT_SLIDER, sliderVal);
  }
  
  private void adjustRate(int keyCode, SampleUIControl control)
  {
    float sliderVal = (keyCode - 48) * 10;
    if (sliderVal == 10)
      sliderVal = 12.5f; // hack for 1/4 speed
      // control.playSampleQuarterLength();
    if (sliderVal == 20)
      sliderVal = 25; // hack for 1/2 speed
    if (sliderVal == 0)
      sliderVal = 100;
    control.getSlider(RATE_SLIDER).setValue((int) sliderVal);
    control.mapToSlider(RATE_SLIDER, sliderVal);
  }

  public void mousePressed()
  {
    if (banks == null)
      return;

    int mx = app.mouseX, my = app.mouseY;
    MouseEvent me = app.mouseEvent;

    for (int k = 0; k < banks.length; k++)
    {
      if (banks[k] == null)
        continue;

      // check each control
      for (int i = 0; i < banks[k].uiControls.length; i++)
      {
        if (banks[k].uiControls[i].contains(mx, my))
        {
          app.setSelectedControl(k, i);
          banks[k].uiControls[i].mousePressed(me);
          return;
        }
      }
    }
  }

  public void mouseDragged()
  {
    if (banks == null) return;

    for (int k = 0; k < banks.length; k++)
    {
      if (banks[k] == null) continue;
      SampleUIControl[] controls = banks[k].uiControls;
      for (int i = 0; i < controls.length; i++)
      {
        if (controls[i].isPressed())
          controls[i].mouseDragged(app.mouseEvent);
      }
    }
  }

  public void mouseReleased()
  {
    if (banks == null)
      return;

    for (int k = 0; k < banks.length; k++)
    {
      if (banks[k] == null)
        continue;
      SampleUIControl[] controls = banks[k].uiControls;
      for (int i = 0; i < controls.length; i++)
      {
        /* if (controls[i].isPressed()) */
        if (controls[i].contains(app.mouseX, app.mouseY))
          controls[i].mouseReleased(app.mouseEvent);
      }
    }
  }

  public void mouseClicked() // double-clicks only
  {
    if (banks == null) return;
    
    if (app.mouseEvent.getClickCount() != 2)
     return;
    
    if (UIManager.rightMouseButton(app.mouseEvent))
      return; // no right-clicks

    for (int k = 0; k < banks.length; k++)
    {
      if (banks[k] == null) continue;
      
      SampleUIControl[] controls = banks[k].uiControls;
      for (int i = 0; i < controls.length; i++)
      {
        if (controls[i].contains(app.mouseX, app.mouseY)) 
        {
          app.setSelectedControl(k, i);
          controls[i].mouseDoubleClick();
          return;
        }
      }
      
      if (banks[k].contains(app.mouseX, app.mouseY)) {
        banks[k].setSolo(!banks[k].isSolo());
        return;
      }
    }
  }

  public void mouseMoved()
  {      
  }
  
  public static boolean shiftDown()
  {
    return shiftDown;
  }

  public static boolean rightMouseButton(MouseEvent me)
  {
    boolean right = (me.getModifiers() == 4 || (me.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK);

    // if (right) System.out.println("RIGHT-MOUSE");

    return right;
  }

  // INPUT, SLIDERS, CHECK_BOXES ================
  public void actionPerformed(ActionEvent e)
  {
    Object source = e.getSource();

    // System.out.println("UIManager.actionPerformed("+source+")");

    // check the global sliders
    if (source == Pataclysm.gain)
    {
      app.setMasterVolume(Pataclysm.gain.getValue() / 100f);
      return;
    }
    else if (source == Pataclysm.prob)
    {
      app.setMasterProb(Pataclysm.prob.getValue() / 100f);
      return;
    }

    else if (source instanceof MyGUIButton)
    {
      if (banks == null)
        return;

      // check our menu buttons
      for (int k = 0; k < banks.length; k++)
      {
        if (source == banks[k].bankMenuButton)
        {
          for (int i = 0; i < 2; i++) // hack for swing crap
            banks[k].getBankJPopupMenu().show(app, (int) banks[k].x + 140, banks[k].clickBounds.y + 61);
          return;
        }

        SampleUIControl[] controls = banks[k].uiControls;
        for (int i = 0; i < controls.length; i++)
        {
          if (source == controls[i].sampleMenuButton)
          {
            for (int j = 0; j < 2; j++) // hack for swing crap
              controls[i].getSampleJPopupMenu().show
                (app, (int) controls[i].getX() + 130, (int) controls[i].getY() + 10);
            return;
          }
        }
      }
    }

    // check the bank sliders
    else if (source instanceof MyGUIPinSlider)
    {
      if (banks == null) return;

      for (int k = 0; k < banks.length; k++)
      {
        if (source == banks[k].volumeControl)
        {
          banks[k].refreshControls();
          banks[k].lastSliderGainPos = banks[k].volumeControl.getValue();
          return;
        }
        else if (source == banks[k].probControl)
        {
          banks[k].refreshControls();
          // System.out.println("GOT BANK VOLUME: "+controlBanks[k].getVolume());
          return;
        }

        // check the control sliders
        SampleUIControl[] uiControls = banks[k].uiControls;
        for (int i = 0; i < uiControls.length; i++)
        {
          for (int j = 0; j < uiControls[i].sliders.length; j++)
          {
            if (source == uiControls[i].sliders[j].slider)
            {
              SliderType sType = uiControls[i].sliders[j].type;
              int val = uiControls[i].rawSliderValue(sType);
              uiControls[i].mapToSlider(sType, val);
              if (sType == GAIN_SLIDER) 
                uiControls[i].lastSliderGainPos = val;
              return;
            }
          }
        }
      }
    }
  }

  void updateBankSoloMode(SampleUIControlBank bank, boolean isSolo)
  {
    // stop any control's soloing
    for (int j = 0; j < banks.length; j++) {
      for (int i = 0; i < banks[j].uiControls.length; i++)
      {
        SampleUIControl sc = banks[j].uiControls[i];
        if (sc.isSolo()) sc.setSolo(false);
      }
    }
    
    // unmute and unsolo all banks
    for (int j = 0; j < banks.length; j++) 
    {
      //
      banks[j].setMuted(false);
      banks[j].soloing = false;
    }
   
    // update the bank in question
    bank.soloing = isSolo;

    if (isSolo)  { // mute all the other banks
      for (int j = 0; j < banks.length; j++) {
        if (bank != banks[j])  
          banks[j].setMuted(true);
      }
    }
  }

  public void updateSoloMode(SampleUIControl clicked, boolean solo)
  {
    // always unmute the sample selected
    clicked.unmute();
    
    // now set its solo flag
    clicked.soloing = solo;

    // now update the other controls
    for (int i = 0; i < banks.length; i++)
    {
      if (banks[i] == null)
        continue;

      SampleUIControl[] voiceControls = banks[i].uiControls;
      // System.out.println(i+ ") controlBank");

      for (int j = 0; j < voiceControls.length; j++)
      {
        if (voiceControls[j] == null || voiceControls[j] == clicked)
          continue;

        // make sure all other solos are off
        voiceControls[j].soloing = false;

        if (solo)
          voiceControls[j].mute();
        else
          voiceControls[j].unmute();
      }
    }
  }


}// end
