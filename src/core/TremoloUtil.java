package core;
import processing.core.PApplet;

public class TremoloUtil // combine with AudioUtils?
{
  public static float[] doTremolo(float[] frames, int tremSz)
  {
    return doTremolo(frames, tremSz, tremSz, 0);  
  }
  
  public static float[] doTremolo(float[] frames, int tremOn, int tremOff)
  {
    return doTremolo(frames, tremOn, tremOff, 0);  
  }
  
  public static float[] smoothTremolo(float[] frames, int tremOn, int tremOff)
  {  
    return doTremolo(frames, tremOn, tremOff, AudioUtils.NUM_SMOOTHING_FRAMES);  
  }
  
  public static float[] doTremolo(float[] frames, int tremOn, int tremOff, int smoothCount)
  { 
    boolean smoothing = smoothCount>0;
    float[] proc = new float[frames.length];
    
    int mult = 1, i = 0;
    int lastSmoothIdx = 0;
    for (; i < frames.length; i++)
    {
      int test = (tremOn * mult) + (tremOff * (mult - 1));
      if (i < test)
      {
        proc[i] = frames[i];
      }
      else
      {
        proc[i] = 0;
        if (i >= (tremOn + tremOff) * mult - 1) {
          if (smoothing) {
            lastSmoothIdx = i;
            doSmoothing(tremOn, tremOff, smoothCount, proc, mult);
          }
          mult++;
        }
      }
    }
    
    if (smoothing) {
      if (i > (lastSmoothIdx+smoothCount)) { // last time
        doSmoothing(tremOn, tremOff, smoothCount, proc, mult);
      }
      // rampDown(proc, proc.length-smoothCount, smoothCount);
    }
    
    return proc;
  }

  private static void doSmoothing(int tremOn, int tremOff, int smoothCount, float[] proc, int mult)
  {
    int start = ((tremOn * (mult-1)) + (tremOff * (mult-1)));
    //System.out.println("Trigger @ "+i+" start="+start);
    rampUp(proc, start-1, smoothCount);
    rampDown(proc, start + tremOn - smoothCount, smoothCount);
  }
  
  public static void rampDown(float[] frames, int startIdx, int numSamples) // orig alg
  {
    //ystem.err.println("rampDown("+startIdx+"-"+(startIdx+numSamples)+")");

    if (startIdx > frames.length-1) return;
    
    startIdx = Math.max(0, startIdx);
    
    float target = frames[startIdx];

    boolean dbug = false;
    if (dbug)
    {
      System.err.println("Start: frames[" + (startIdx) + "] = " + frames[startIdx]);
      System.err.println("Target: frames[" + (startIdx) + "] = " + target);
      System.err.println("---------------------------------------------------");
    }

    // do the lerp from '0' to 'target' in 'numFadeSamples' steps
    int j = 1;
    for (; j <= numSamples; j++)
    {
      int idx = startIdx + numSamples - j;
      float old = frames[idx];
      frames[idx] = PApplet.lerp(0, target, ((j) / (float) numSamples));
      if (dbug) 
        System.err.println("Adjust: frames[" + idx + "] " + old + " -> " + frames[idx]);
    }
  }

  public static void rampUp(float[] frames, int startIdx, int numSamples)
  {
    //System.err.println("rampUp("+startIdx+"-"+(startIdx+numSamples)+")");
    
    if (startIdx > frames.length-1) return;
    
    startIdx = Math.max(startIdx, 0);
    
    int targetIdx = Math.min(startIdx + numSamples, frames.length-1);
    float target = frames[targetIdx];

    boolean dbug = false;
    if (dbug)
    {
      System.err.println("Start: frames[" + (startIdx) + "] = " + frames[startIdx]);
      System.err.println("Target: frames[" + (startIdx + numSamples) + "] = "
          + target);
      System.err.println("---------------------------------------------------");
    }

    // now do the lerp from '0' to 'target' in 'numFadeSamples' steps
    int j = 0;
    for (; j <= numSamples - 1; j++)
    {
      int idx = startIdx + numSamples - j - 1;
      float old = frames[idx];
      frames[idx] = PApplet.lerp(target, 0, ((j + 1) / (float) numSamples));
      if (dbug)
        System.err.println("Adjust: frames[" + idx + "] " + old + " -> " + frames[idx]);
    }
  }


  public static void main(String[] args)
  {
    float[] test = new float[100];
    for (int i = 0; i < test.length; i++)
      test[i] = (float) ((Math.random() / 2.0) + .25);
    float[] res = doTremolo(test, 4, 10, 5);
    for (int i = 0; i < res.length; i++)
      System.out.println(i + ") " + res[i]);
  }
}
