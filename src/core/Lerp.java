package core;
import java.util.*;


public class Lerp
{
  static long TS = System.currentTimeMillis();
  
  List listeners = new ArrayList();
  float data, targetData, initialData;
  long lerpStart, lerpElapsed, lerpTotal;
  boolean completed, stopped;
  int startTimeStamp;
  
  public Lerp(float curr, float target, float duration) {
    setTarget(curr, target, duration);
  }  

  public Lerp() {}

  public Lerp(LerpListener lerpListener)
  {
    addListener(lerpListener);
  }

  public boolean setTarget
    (float start, float target, float duration)
  {  
//    System.out.println("Lerp.setTarget("+target+")");
    data = start;
    initialData = start;
    targetData = target;
            
    this.lerpStart = -1;
    this.lerpElapsed = 0; 
    this.lerpTotal = (int)(duration*1000);  ;  
    this.completed = false;
    
    return true;
  } 
 
  public void addListener(LerpListener ll) 
  { 
    listeners.add(ll);
  }
  
  public float update() 
  {        
    int time = millis();
    
    // if we're not started and not in the future, start
    if (lerpStart < 0) lerpStart = time;
    
    // get the elapsed time for this lerp
    lerpElapsed = time - lerpStart;    
    
    float amt = Math.min(1, (lerpElapsed/(float)lerpTotal));
    
    if (amt == 1) { // out of time
      data = targetData;
      if (!completed) {
        //System.out.println("Lerp.completed....");
        completed = true;
      }
      notifyListeners();
    } 
    else {  
        this.data = lerp(initialData, targetData, amt);
        //System.out.println(amt+" -> "+data);
        //System.out.println(time+" -> "+lerpStart);
    }       
    //System.out.println();
    return data;
  }
  
  public void notifyListeners() {
    for (Iterator it = listeners.iterator(); it.hasNext();)
    {
      LerpListener ll = (LerpListener) it.next();
      ll.lerpComplete(data);
    }
  }
  
  private final float lerp(float start, float stop, float amt) {
    return start + (stop-start) * amt;
  }


  public boolean isCompleted() {
    return completed;
  }
  
  private int millis() {
    return (int)(System.currentTimeMillis()-TS);
  }
  
  public float getValue() {
    return data;
  }
  
  public static void main(String[] args) throws InterruptedException
  {
    Lerp l = new Lerp(/*8, -1, 3*/);
    l.setTarget(1, -1, 3);
    while (!l.isCompleted()) {
      l.update(/*l.millis()*/);
      //System.out.println(l.getValue());
      Thread.sleep(5);
    }
    System.out.println(l.getValue()+" @ "+l.millis());
    l.setTarget(-1, 1, 3);
    Thread.sleep(1000);
    while (!l.isCompleted()) {
      l.update(/*l.millis()*/);
      //System.out.println(l.getValue());
      Thread.sleep(5);
    }
    System.out.println(l.getValue()+" @ "+l.millis());
    l.setTarget(1, -1, 3);
    while (!l.isCompleted()) {
      l.update(/*l.millis()*/);
      //System.out.println(l.getValue());
      Thread.sleep(5);
    }
    System.out.println(l.getValue()+" @ "+l.millis());
  }
  
}// end