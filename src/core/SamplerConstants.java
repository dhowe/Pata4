package core;


public interface SamplerConstants 
{
  boolean DECLICKIFY_SAMPLES = true;
  boolean USE_FOOTSWITCH = true;
  boolean CONFIRM_ON_QUIT = false;
  boolean SHOW_GLOBAL_MENU = true;
  boolean SHOW_BANK_MENU = true; 
  boolean USE_JPOPUP_MENUS = true;
  
  String VERSION = "v011";
  String METER_IMG_12 = "meter125x12.png";
  String PROJ_DIR = "proj", DATA_DIR = "src/data/";
  String PAD = "Double", SOLO = "Solo", BOUNCE="Bounce", MIX_DOWN="MixDown", REVERSE="Reverse";
  String DTREM = "dTrem", REVERT = "Revert", DECLICK = "Declickify", CLEAR="Clear", SHIFT="Shift"; 
  String COPY = "Copy", PASTE= "Paste", CUT = "Cut", SEP="-";
  String OPEN = "Open...", MUTE = "Mute", SWEEP="Sweep";
  String FONT = "LucidaSans-Typewriter-48.vlw";
  String ADDITIVE = "Grow", SUBTRACTIVE="Shrink", MICRO="Micro", NONE="None";
  String[] QUANTIZE_MODES = { ADDITIVE, SUBTRACTIVE, MICRO, NONE };
  
  // PREF-KEYS
  String QUANTIZE_MODE = "quantize_mode", MICRO_PROB = "micro_prob";
  String MICRO_PAD = "micro_pad", MICRO_DATA = "micro_data";
  
  
  float INITIAL_MASTER_VOL = .8f;
  float MIN_RECORD_LEVEL = .03f;
  float LEVEL_GAIN_SCALE = 50f;
 
  
  int UI_XOFFSET = 100;
  int UI_BANK_SPACING = 218;
  int TEXT_FILL = 255;
  int NUM_BANKS = 5;
  int TREM_SCALE = 750;
  int SPECTRUM_LENGTH = 256;
  int NUM_CONTROLS_PER_BANK = 4;
  int SAMPLE_RATE = AudioUtils.SAMPLE_RATE;
  int DEFAULT_BANK_SLIDER_GAIN_POS = 70;
  int MAX_NUM_TO_PRELOAD = 20;
  int WAVEFORM_COL=220, WAVEFORM_ALPHA=32;
  int BG_R=255, BG_G=255, BG_B=255, BG_TINT=255; 
  int MAX_SAMPLE_LEN = 20;
  int MIN_SAMPLE_SIZE = SAMPLE_RATE/20; // 50ms 
  
  int NO_QUANTIZE = -1;
  int ADDITIVE_QUANTIZE = 1;
  int SUBTRACTIVE_QUANTIZE = 2;
  int MICRO_QUANTIZE = 3;
  int REALTIME_QUANTIZE = 4; // not used
  int LOOP_QUANTIZE = 5; // not used
  
  // DEFAULT_PREFS
  float DEFAULT_MICRO_PROB = .95f;
  int DEFAULT_QUANTIZE_MULT = 2; // double each quantize
  int DEFAULT_QUANTIZE_MODE = SUBTRACTIVE_QUANTIZE;
  int DEFAULT_MICRO_DATA_SIZE = 4000;
  int DEFAULT_MICRO_PAD_SIZE = 8800;
  int DEFAULT_MIN_QUANTUM = 44100/4; // 1/4 sec
  
  //int RATE=5, TREM=1, GAIN=2, PROB=3, PAN=4, SHIFT=0;
  //int[] DEFAULT_VALS = { 50, 0, 70, 95 };
  //String[] LABELS = { /*"RATE", */"SHFT", "TREM", "GAIN", "PROB"};
  
  SliderType SHIFT_SLIDER = new SliderType("SHFT", 50);
  SliderType TREM_SLIDER =  new SliderType("TREM", 0);
  SliderType GAIN_SLIDER =  new SliderType("GAIN", 70);
  SliderType PROB_SLIDER =  new SliderType("PROB", 95);
  SliderType RATE_SLIDER =  new SliderType("RATE", 50);
  SliderType PAN_SLIDER =   new SliderType("PAN", 50);
  
  //SliderType[] ALL_SLIDERS = {SHIFT_SLIDER, TREM_SLIDER, GAIN_SLIDER, PROB_SLIDER, RATE_SLIDER, PAN_SLIDER};
}

class SliderType {
  public SliderType(String name, int val)
  {
    this.label = name;
    this.defaultValue = val;
  }
  String label;
  int defaultValue;
  public String toString()
  {
    return "SliderType."+label.toLowerCase();
  }
  //int typeConst;
}
