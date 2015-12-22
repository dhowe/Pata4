package core;

import java.util.*;

import pitaru.sonia.LiveInput;
import pitaru.sonia.Sample;

public class RecordBuffer implements SamplerConstants
{
  static int MIN_LEVELS_FOR_SNIP = 30;

  private Sample sample;
  private Sample buffer;
  private boolean recording;
  private List levels;

  public RecordBuffer()
  {
    this.levels = new ArrayList();
  }

  public float update()
  {
    float lev = LiveInput.getLevel();
    levels.add(lev);
    return lev;
  }

  public void startRecord()
  {
    this.levels.clear();
    this.recording = true;
    this.buffer = new Sample(Pata4.SAMPLE_RATE * MAX_SAMPLE_LEN);
    LiveInput.startRec(buffer);
  }

  public Sample stopRecord()
  {
    recording = false;
    LiveInput.stopRec(buffer);
    float[] frames = new float[buffer.getNumFrames()];
    this.buffer.read(frames);
    buffer.delete();

    float SILENCE_THRESHOLD = 0;//.00001f;
    
    float maxFrameVal = 0;
    int zeroCounter = 0, maxFrameIdx = 0;
    int lastRealFrame = frames.length;
    boolean gotData = false;
    
    for (int i = 0; i < frames.length; i++)
    {
      float f = Math.abs(frames[i]);
      
      if (gotData && f == 0) {
        if (zeroCounter++ == 100)
          lastRealFrame = i;
      }
      else
        zeroCounter = 0; 
      
      if (f > SILENCE_THRESHOLD)
        gotData = true;
      
      if (f > maxFrameVal)
      {
        maxFrameVal = frames[i];
        maxFrameIdx = i;
      }
    }
    
    if (lastRealFrame < MIN_SAMPLE_SIZE) {
      System.out.println("lastFrame="+lastRealFrame+
          " < MIN_SAMPLE_SIZE="+MIN_SAMPLE_SIZE+" samples!\n Verify that you are recieving input?");
      return null;
    }
      

    // get the sample frames
    float[] data = null;
    
    if (Pata4.quantizeMode == NO_QUANTIZE)
    {
      // just strip off the excess
      data = new float[lastRealFrame];
      System.arraycopy(frames, 0, data, 0, lastRealFrame); 
    }
    else if (Pata4.quantizeMode == MICRO_QUANTIZE)
    {
      data = createMicroSample(frames, lastRealFrame, maxFrameIdx, Pata4.minQuantum);
      if (data == null) {
        System.err.println("[WARN] #6 Null data!");
        return null;
      }
    }
    else {
      data = new float[lastRealFrame];
      System.arraycopy(frames, 0, data, 0, lastRealFrame); 
      data = quantizeSampleData(data, Pata4.minQuantum);
      if (data == null) {
        System.err.println("[WARN] #7 Null data!");
        return null;
      }
    }
    
    /*
    //float[] newdata = data;
    if (DECLICKIFY_SAMPLES) {
      newdata = AudioUtils.declickifyEnds(data);
      if (newdata == null) {
        System.err.println("[WARN] Unable to complete declickifyEnds()...");
        newdata = data;
      }
    }
*/
    // write to new sample
    this.sample = new Sample(data.length);
    sample.write(data);

    return sample;
  }
  
  

  private float[] quantizeSampleData(float[] data, int minQuantum)
  {
    //System.out.println("RecordBuffer.quantizeSampleData(mode="+SamplerFi.quantizeMode+", minQuantum="+minQuantum+")");
    switch(Pata4.quantizeMode) 
    {
      case NO_QUANTIZE:
        return data;
      case SUBTRACTIVE_QUANTIZE: 
        return subtractiveQuantize(data, minQuantum);
      default: // additive & micro
        return additiveQuantize(data, minQuantum);
    }
  }

  private float[] additiveQuantize(float[] data, int minQuantum)
  {
    int q = minQuantum;
    for (int i = 1;; i *= 2)
    {
      int quantum = i * minQuantum;
      if (data.length < quantum) { 
        q = quantum;
        break;
      }
    }
    //System.out.println("additiveQuantize: "+data.length +" -> "+q+" = "+q/(float)SAMPLE_RATE+"s");
    return AudioUtils.padArray(data, q);
  }

  private float[] subtractiveQuantize(float[] data, int minQuantum)
  {
    int q = minQuantum;
    for (int i = 2;; i *= 2)
    {
      int quantum = i * minQuantum;
      if (data.length < quantum) {
        q = (i/2) * minQuantum;
          break;
      }
    }
    
    int preStrip = 0;
    int strip = data.length - q;
    float[] sdata = new float[q];
    preStrip = strip / 2;
    if (strip > 0) {
      try {
        System.arraycopy(data, preStrip + 1, sdata, 0, sdata.length);
      }
      catch (Exception e) {
        System.err.println("[WARN] "+e+"\nSystem.arraycopy(data(len="+data.length+"), "+(preStrip + 1)+", sdata(len="+sdata.length+"), 0, "+sdata.length+")");
        //e.printStackTrace();
      }
    }
    else // data is < than minQuantum, so pad 
    {
      System.err.println("[WARN] Padding micro-sample smaller than minQuantum="+minQuantum);       
      System.arraycopy(data, 0, sdata, -preStrip, data.length);
    }
    return sdata;
  }
  
  private float[] createMicroSample(float[] frames, int endframe, int maxFrameIdx, int minQuantum)
  {
    int startClipIdx = Math.max(0, maxFrameIdx - Pata4.microDataSize);
    int endClipIdx = Math.min(endframe, maxFrameIdx + Pata4.microDataSize);
    float[] data = new float[(endClipIdx - startClipIdx)+(Pata4.microPadSize)];
    System.arraycopy(frames, startClipIdx, data, Pata4.microPadSize/2, data.length-Pata4.microPadSize);
    return quantizeSampleData(data, minQuantum);
  }

  private int getMaxLevelIdx(List l)
  {
    int idx = 0;
    float max = -1;
    int maxIdx = -1;
    for (Iterator i = l.iterator(); i.hasNext(); idx++)
    {
      Float f = (Float) i.next();
      float level = f.floatValue();
      if (level > max)
      {
        max = level;
        maxIdx = idx;
      }
    }
    return maxIdx;
  }

  public boolean isRecording()
  {
    return recording;
  }

  public Sample getSample()
  {
    return buffer;
  }

} // end RecordBuffer 