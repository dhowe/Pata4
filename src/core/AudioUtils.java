package core;

import java.util.Arrays;
import java.util.Random;

import pitaru.sonia.LiveInput;
import pitaru.sonia.Sample;
import processing.core.PApplet;

import com.softsynth.jsyn.Synth;

public class AudioUtils
{
  static final float BASE_MIDI_C = 8.1758224f;
  static final int SAMPLE_RATE = 44100;
  static final int NUM_SMOOTHING_FRAMES = (int)(SAMPLE_RATE/1000f * 3f); // 3 MS
  static final float INPUT_SENSITIVITY = 1.5f;
  
  public static void drawInputMeterVertical(PApplet p, int x, int y, int w, int h)
  {
    float meterData = LiveInput.getLevel();
    p.fill(0, 100, 0);
    p.rect(x - w/2, y, w , Math.min(h, meterData * -h * INPUT_SENSITIVITY));
 }
  
  public static void drawInputMeter(PApplet p, int x, int y, int w, int h)
  {
    float meterData = LiveInput.getLevel();
    p.fill(0, 100, 0);
    p.rect(x, y, Math.min(w,(meterData*w) * INPUT_SENSITIVITY), h);
 }
  
  public static String getCpuPercentage()
  {
    double usage = Synth.getUsage();
    //System.out.println("AudioUtils.getCpuPercentage() -> "+usage);
    int percent = (int) (usage * 100.0); /* Chop trailing digits. */
    return Integer.toString(percent) + "%";
  }

  
  public static float getFrequencyFromMIDINote(double note)
  {
    return (float) (BASE_MIDI_C * Math.pow( 2.0f, (note * (1.0f/12.0f)) ));
  }

  public static float getMIDINoteFromFrequency(double freq)
  {
    return (float) (12f * Math.log(freq/BASE_MIDI_C) / Math.log( 2.0f ));
  }
  
  /**
   * Returns a sorted gaussian dist of size 'n', with values V1...Vn, 
   * where -.5 <= Vi <= .5  for all Vi.
   */
  public static float[] getGaussianDist(int n) {
    Random rand = new Random();
    float[] dist = new float[n];
    for (int i = 0; i < n; i++)
      dist[i] = (float)rand.nextGaussian();
    Arrays.sort(dist);
    float min = dist[0];float max = dist[n-1];
    float scale = Math.max(max, min*=-1)*2f;
    for (int i = 0; i < dist.length; i++)
      dist[i] = dist[i]/scale;
    Arrays.sort(dist);
    return dist;
  }

  private static void gaussianTest()
  {
    float sum = 0;
    float[] gd = getGaussianDist(10000);
    for (int i = 0; i < gd.length; i++) {
      float val = gd[(int)(Math.random()*gd.length)];
      sum += val;
    }
    System.out.println("Avg: "+(sum/(float)gd.length));
  }
  
  private static void declickifyEnds(float[] frames)
  {
     declickifyEnds(frames, true, 0, frames.length);
  }
  
  public static void declickifyEnds(float[] frames, int startFrame, int endFrame)
  {
     declickifyEnds(frames, true, startFrame, endFrame);
  }
  
  private static void declickifyEnds(final float[] frames, boolean doStart, int startFrame, int endFrame)
  {
    declickEnd(frames, NUM_SMOOTHING_FRAMES, endFrame);
    //if (frames == null) return frames;
    if (doStart) 
      declickStart(frames, NUM_SMOOTHING_FRAMES, startFrame);
  }
  
  private static float[] declickStart(float[] frames, int numFadeSamples)
  {
   return declickStart(frames, numFadeSamples, 0); 
  }
  
  /*
   *  Invariants: 
   *    -- no change to the # of non-zero frames
   *    -- no data is modified before startFrame (startFrame is first to be modified)
   */
  private static float[] declickStart(float[] frames, int numFadeSamples, int startFrame)
  {
    if (frames == null || startFrame > frames.length) 
      throw new RuntimeException("Invalid startFrames: "+startFrame);
    
    boolean dbug = false;
    
    // check we have enough room for the fade
    if (numFadeSamples > (frames.length/3f)) {
      System.out.println("[WARN] Short sample in declickStart(len="+frames.length+")");
      numFadeSamples = (int)(frames.length/3f);
    }

    // find the first (non-zero) frame to fade up to
    int firstValIdx = 0;
    for (int i = startFrame; i < frames.length; i++)
    {
      if (Math.abs(frames[i]) > 0)
      {
        firstValIdx = i;
        break;
      }
    }

    // compute ending value for the fade (where we fade to)
    int start = firstValIdx;
    if (dbug) System.err.println("Start: frames["+(start)+"] = "+frames[start]);
    float target = frames[(start+numFadeSamples-1)];
    if (dbug) System.err.println("Target: frames["+(start+numFadeSamples-1)+"] = "+target);
    if (dbug) System.err.println("----------------------------------------------------------------");
    
    // now do the lerp from '0' to 'target' in 'numFadeSamples' steps
    int j = 1;
    for (; j < numFadeSamples; j++) { 
      int idx = start+numFadeSamples-j-1;
      float old = frames[idx];
      frames[idx] = PApplet.lerp(target, 0, ((j)/(float)numFadeSamples));
      if (dbug) System.err.println("Adjust: frames["+idx+"] "+old+" -> "+frames[idx]); 
    }
    //frames[start-1] = 0; // first frame to zero
    return frames;
  }
  

  /*
   *  Invariants: 
   *    -- no change to the # of non-zero frames
   *    -- no data is modified after endFrame (endFrame is last to be modified)
   */
  private static float[] declickEnd(final float[] frames, int numFadeSamples, int endFrame)
  {
    //System.out.println("AudioUtils.declickEnd("+frames.length+", "+numFadeSamples+", "+endFrame+")");
    
    boolean dbug = false;

    if (frames == null) return new float[0];
    
    // check we have enough room for the fade
    if (numFadeSamples > (frames.length/3f)) {
      System.out.println("[WARN] Short sample in declickEnd(len="+frames.length+")");
      numFadeSamples = (int)(frames.length/3f);
    }
    
    // find the last (non-zero) frame to fade down from 
    int lastValIdx = 0;
    int last = Math.min(frames.length-1, endFrame);
    for (int i = last; i > 0; i--)
    {
      if (Math.abs(frames[i]) > 0)
      {
        lastValIdx = i;
        break;
      }
    }

    // compute starting value for the fade (where we fade from)
    if (dbug) System.err.println("Last:  frames["+lastValIdx+"] = "+frames[lastValIdx]);
    int targetIdx = Math.max(0, Math.min(frames.length-1, lastValIdx - numFadeSamples + 1));
    float fadingFrom = frames[targetIdx];
    if (dbug) System.err.println("Start: frames["+(lastValIdx-numFadeSamples+1)+"] = "+fadingFrom);
    if (dbug) System.err.println("----------------------------------------------------------------");
      
    // now do the lerp from 'fadingFrom' to '0' in 'numFadeSamples' steps
    int j = 1;
    try
    {
      for (; j < numFadeSamples; j++)
      {
        int idx = lastValIdx-j+1;
        if (idx >= frames.length) {
          System.err.println("[WARN] "+idx+ " > frames.length="+frames.length+"!!!!!!!!");
          return frames;
        }

        float old = frames[idx];
        frames[idx] = PApplet.lerp(0, fadingFrom, (j/(float)numFadeSamples));
        if (dbug) System.err.println("Adjust: frames["+idx+"] "+old+" -> "+frames[idx]); 
      }
    }
    catch (Exception e)
    {
      System.err.println("[WARN] Error in declickEnd()!!!");
      return frames;
    }
    
    //frames[start+j] = 0; // last frame to zero ??
    
    return frames;
  }
  
  private static void declickEnd(float[] frames, int numFadeSamples)
  {
     declickEnd(frames, numFadeSamples, frames.length); 
  }
  
  private static void declickifyEnds(Sample s)
  {
    if (s == null) return;
    declickifyEnds(s, 0, s.getNumFrames());
  }
  
  /** Returns an array of size 'newLength' with ends padded by zeros */
  public static float[] padArray(float[] current, int newLength)
  {
    // # of zeros to pad, 1/2 on each size
    int pad = newLength - current.length;

    // make an array for the new data
    float[] newdata = new float[newLength];
    
    System.arraycopy(current, 0, newdata, pad/2, current.length);
    
    return newdata;
  }
  
  private static void declickifyEnds(Sample s, int startFrame, int endFrame)
  {
    if (s == null) return;
    //System.out.println("SamplerFi.declickifyEnds(s, "+startFrame+","+endFrame+")");
    
    // write sample data to 'frames
    float[] tmp = new float[s.getNumFrames()];
    s.read(tmp);
    
    // write float[] data to back to the sample
    declickifyEnds(tmp, true, startFrame, endFrame);
    s.write(tmp);
  }
  
  // Note: doesn't always get all the way to 0, just very close 
  private static void declickTest(boolean start)
  {
    float[] test = new float[100];
    for (int i = 0; i < test.length; i++)
      test[i] = (float)(Math.random()/2f+.3f);
    
    //if (start) {
      float[] tmp = declickStart(test, 10, 0);
     // for (int i = 0; i < 100; i++)
       // System.out.println(i+")"+res[i]);
  /*  }
    else {*/
      float[] res = declickEnd(tmp, 10, 100);
      for (int i = 0; i < 100; i++)
        System.out.println(i+")"+res[i]);
    //}
  }
  
  public static void main(String[] args)
  {
    declickTest(false);
    //gaussianTest();
  }

}// end
