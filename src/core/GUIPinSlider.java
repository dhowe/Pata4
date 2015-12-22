package core;


import mkv.MyGUI.MyGUIActionEvent;
import mkv.MyGUI.MyGUIPinSlider;
import processing.core.PApplet;

/**
 * Extends MyGui v10 MyGUIPinSlider to allow continuous updates on sliders..
 */
public class GUIPinSlider extends MyGUIPinSlider 
{
  boolean continuous = true; // adjust during drags? yes by default
  
  public boolean isContinuous()
  {
    return continuous;
  }

  public void setContinuous(boolean continuous)
  {
    this.continuous = continuous;
  }

  public GUIPinSlider(PApplet root, int x, int y, int width, int height, int minValue, int maxValue) {
    super(root, x, y, width, height, minValue, maxValue);
  }

  public void mouseDragged() {
    if (continuous) {
      if (dragged) {
        MyGUIActionEvent a = new MyGUIActionEvent(this, _actionCommand);
        a.sendEvent(_root);      
      } 
    }
    super.mouseDragged();
  }
  
}
