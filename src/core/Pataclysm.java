package core;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.SwingUtilities;

import mkv.MyGUI.MyGUILabel;
import mkv.MyGUI.MyGUIPinSlider;
import pitaru.sonia.Sample;
import processing.core.PApplet;
import processing.core.PConstants;
import procontroll.ControllDevice;

/*
 * NEW
 *  Probability-disable-button
 *  Tap-tempo
 *  Undo-last
 *  Automate slider
 * 	Check modified jar/src:Sample.instances
 * 
 * TODO: 
 *     -- re-add declick() as needed (always work from 'unprocessed')
 *     -- test mixDown
 *     -- add: setQuantsizeFromLength in q-menu
 *    
 *   BUGS:
 *     -- cut/paste keys are busted :(
 *     -- trem deletes startX, stopX
 *     -- trem restarts sample (instead of continuing it) 
 *     -- changing pitch ignores startX, stopX frames!
 *     -- copy-paste ignores shift (and delete)  
 *   
 *   NEXT:
 *      Remove pitch-shift, restore shift
 *      Add micro pitch-shift function
 *      Add switch for pitch-shift vs. sample rate
 *      Add menus w' state (boolean & pitchShift)
 *   
 *      show/check: micro-quantize times/prob
 *      add footswitch
 term should not unset partials
 ----
 *      Try removing extra computeWaveForm() call    
 *      Micro: variable capture-length?  
 *      Micro: make smart-sync of sliderX positions **
 * 
 *  NEW FEATURES:
 *    No-Attack mode
 *    Transpose-bank/transpose-all down 1-step, up a -3rd? up a 4th?
 *    Trem all
 *    
 *    **  declickify w' start/end-frames has to fadeIn/fadeOut otherwise it is adding clicks... ??? 
 *    **  add smart-quantize-mode (ignores levels close to zero on ends) ? or make all modes 'smart'
 *     
 *    Global Output Level Meter?
 *    
 *    PREFS:  quantize-size, quantize-mode, quantize-multiple, default-prob
 *    
 *    Add to sample: declickify-entire?
 *    Add to Global: trigger-all-on-next button, declickify-all controls
 *    
 *    Add partials to menu/sub-menu?
 *    quantize loops on select-partial w' mouse?
 *    enable dragging of (red) selected loop line
 *    
 *    Do multi-select 
 *    Do Mix-Down
 *    
 *    Fix CPU problem (profile!)
 *       Remove transparency of Ui-Controls?
 *    
 *    BUGS:
 *      after double(), revert (unprocessed) is also doubled (bug or feature?)
 *      'cut' is broken? yes, need to clone control?
 *      Deal with Sonia/JSyn bugs!
 *    
 *    ---------------------------------
 *    MENUS:
 *      Make open-dialog select folder-name when you click
 *      Add Save-as, and refigure save
 *          
 *    Add dynamic quantize based on 1st sample?  
 */
public class Pataclysm extends PApplet implements SamplerConstants {

	static final boolean IGNORE_PREFS = true, EXITING = false;
	static final String PROJECT_TO_LOAD = "proj/DandelionMessenger";
	static final boolean LOAD_CONFIG_FILE = true, LOAD_SAMPLE_DIR = false;
	static final int SAMPLE_RATE = AudioUtils.SAMPLE_RATE;
	static final String SAMPLE_DIR = "/Users/dhowe/Documents/Workspaces/eclipse-workspace/LiveSampler/";

	// PREFS
	static int quantizeMult = DEFAULT_QUANTIZE_MULT;
	static int quantizeMode = ADDITIVE_QUANTIZE;
	static int minQuantum = DEFAULT_MIN_QUANTUM;
	static int microDataSize = DEFAULT_MICRO_DATA_SIZE;
	static int microPadSize = DEFAULT_MICRO_PAD_SIZE;
	static float microProb = DEFAULT_MICRO_PROB;

	static String cpu = "";
	static int timestamp = -100000, masterControlsY = 0,
			currentControlBankIdx = 0;
	static float bg[] = new float[3], masterGain = 0, masterProb = 1;

	static boolean isExiting;
	static private boolean saving = false;

	static SampleUIControlBank[] controlBanks;
	static RecordBuffer recordBuffer;
	static MyGUIPinSlider gain, prob;
	static MyGUILabel gainLabel, probLabel;
	static USBFootSwitch footswitch;
	static UIManager uiMan;
	static Preferences prefs;

	static SliderType[] currentSliderTypes = { RATE_SLIDER, TREM_SLIDER,
			PROB_SLIDER, GAIN_SLIDER, };

	public void setup() {
		size(1280, 768);

		masterControlsY = height - 22;
		uiMan = new UIManager(this);

		this.loadPrefs();
		this.setCurrentBank(0);
		this.setMasterVolume(INITIAL_MASTER_VOL);
		this.preloadFiles(SAMPLE_DIR);
		this.initFootSwitch();

		// refresh the gain sliders
		for (int i = 0; i < controlBanks.length; i++)
			controlBanks[i].refreshControls();
	}

	private void loadPrefs() {

		quantizeMode = (DEFAULT_QUANTIZE_MODE);
		microDataSize = (DEFAULT_MICRO_DATA_SIZE);
		microPadSize = (DEFAULT_MICRO_PAD_SIZE);
		microProb = (DEFAULT_MICRO_PROB);

		Preferences prefs = getPrefs();

		if (!IGNORE_PREFS) {

			setQuantizeMode(prefs.get(QUANTIZE_MODE, ADDITIVE));
			microDataSize = prefs.getInt(MICRO_DATA, DEFAULT_MICRO_DATA_SIZE);
			microPadSize = prefs.getInt(MICRO_PAD, DEFAULT_MICRO_PAD_SIZE);
			microProb = prefs.getFloat(MICRO_PROB, DEFAULT_MICRO_PROB);

			System.out.println("[INFO] Loading prefs: " + " quantize=" + quantizeMode
					+ " micro-data=" + microDataSize + " micro-pad=" + microPadSize
					+ " micro-prob=" + microProb);
		} else {

			try {
				prefs.flush();
				System.out.println("[INFO] Flushing preferences...");
			} catch (BackingStoreException e) {
				System.err.println("[WARN] " + e.getMessage());
			}
		}
	}

	public void draw() {
		background(Pataclysm.bg[0], Pataclysm.bg[1], Pataclysm.bg[2]);

		setVisible(Switch.SHOW_UI.on);

		strokeWeight(1);

		// drawSwitches();

		if (saving) {
			fill(200, 100, 100);
			text("[saving]", width - 60, 15);
		}

		if (Switch.SHOW_UI.on)
			drawControlBanks();

		if (recording())
			drawOnRecord();

		drawMasterControls();
	}

	public void setVisible(boolean b) {
		gain._visible = b;
		prob._visible = b;
		for (int i = 0; i < controlBanks.length; i++) {
			SampleUIControlBank bank = controlBanks[i];
			if (bank != null)
				bank.setVisible(b);
		}
		super.setVisible(b);
	}

	private void drawMasterControls() {
		if (!Switch.SHOW_UI.on)
			return;

		noFill();
		strokeWeight(2);
		stroke(BG_R, BG_G, BG_B, 63);
		rectMode(PConstants.CORNER);

		int rectW = (NUM_BANKS * UI_BANK_SPACING) - NUM_BANKS * 3;
		rect(controlBanks[0].x - 3, masterControlsY - 18, rectW, 38);

		// update every 5 sec
		if (millis() - timestamp > 5000) {
			cpu = "CPU: " + AudioUtils.getCpuPercentage();// +"  Samples: "+Sample.instances.size();
			timestamp = millis();
		}

		// AudioUtils.inputMeterVertical(this, width-70, height-57, 30, 400);
		AudioUtils.drawInputMeter(this, controlBanks[2].x + 40, height - 30, 300,
				20);

		fill(255);

		text("IN:", controlBanks[2].x + 10, masterControlsY + 5);
		text("QUANTIZE:", controlBanks[NUM_BANKS - 1].x - 10, masterControlsY + 5);

		if (quantizeMode == MICRO_QUANTIZE)
			fill(200, 0, 0);

		text(getQuantizeMode(), controlBanks[NUM_BANKS - 1].x + 40,
				masterControlsY + 5);
		fill(255);
		text(cpu, controlBanks[NUM_BANKS - 1].x + 134, masterControlsY + 5);

		strokeWeight(1);
	}

	static String getQuantizeMode() {
		if (quantizeMode == ADDITIVE_QUANTIZE)
			return ADDITIVE;
		else if (quantizeMode == SUBTRACTIVE_QUANTIZE)
			return SUBTRACTIVE;
		else if (quantizeMode == MICRO_QUANTIZE)
			return MICRO;
		else
			return NONE;
	}

	public static String dataFolder() {
		return DATA_DIR;
	}

	private void initFootSwitch() {
		try {
			ControllDevice dev = USBFootSwitch.getDevice(this);
			footswitch = new USBFootSwitch(0);
			footswitch.setup(this, dev);
		} catch (Throwable e) {
			System.out.println("[WARN] No FootSwitch found...");
		}
	}

	private void drawSwitches() { // not used presently
		// if (!SHOW_UI) return;

		rectMode(PConstants.CORNER);
		int xOff = 13 + UI_XOFFSET;
		int yOff = 8;

		textSize(11);
		fill(TEXT_FILL);
		stroke(TEXT_FILL);
		for (int i = 0; i < Switch.ACTIVE.length; i++) {
			Switch.ACTIVE[i].draw(this);
			xOff += 45;
		}
	}

	public static SampleUIControl currentControl() {
		SampleUIControlBank currentBank = currentControlBank();
		return currentBank.selectedControl();
	}

	int incrControlBank() {
		// System.err.print("LiveTextMix.incrControlSet()  oldIdx="+currentControlBankIdx);
		if (++currentControlBankIdx == controlBanks.length)
			currentControlBankIdx = 0;
		updateBankView();
		return currentControlBankIdx;
	}

	int decrControlBank() {
		// System.out.println("LiveTextMix.decrControlSet() oldIdx="+currentControlBankIdx);
		if (--currentControlBankIdx < 0)
			currentControlBankIdx = controlBanks.length - 1;
		updateBankView();
		return currentControlBankIdx;
	}

	public void recordStart() {
		if (recordBuffer == null)
			recordBuffer = new RecordBuffer();
		recordBuffer.startRecord();
		/*
		 * testRecording = true; testSample = new Sample(44100*5);
		 * LiveInput.startRec(testSample);
		 */
	}

	void toggleRecord() {
		if (recording()) {
			recordBuffer.stopRecord();
		} else {
			recordStart();
		}
	}

	void recordStop() {
		/*
		 * testRecording = false; LiveInput.stopRec(testSample); Sample s =
		 * testSample;
		 */
		Sample s = recordBuffer.stopRecord();
		if (s == null) {
			System.out.println("IGNORING INVALID SAMPLE");
			return;
		}
		currentControl().setSample(s, true);
		currentControlBank().incrControl(false);
	}

	public int incrControl(SampleUIControlBank bank, boolean manual) {
		int current = bank.selectedControlIdx;
		if (++current > bank.uiControls.length - 1) {
			// change to next bank if not manual
			if (!manual && ++currentControlBankIdx > controlBanks.length - 1) {
				currentControlBankIdx = 0;
			}
			bank = setCurrentBank(currentControlBankIdx);
			current = 0;
		}
		int next = bank.setSelectedControl(current, true);
		return next;

	}

	public void mixDownOld(SampleUIControl dst, final SampleUIControl... srcs) {
		List srcList = new ArrayList();
		for (int i = 0; i < srcs.length; i++) {
			if (srcs[i].getSample() != null)
				srcList.add(srcs[i]);
		}
		SampleUIControl[] src = (SampleUIControl[]) srcList
				.toArray(new SampleUIControl[srcList.size()]);

		Sample[] rawSamples = new Sample[src.length];
		for (int i = 0; i < src.length; i++) {
			// need to quantize these !!!
			rawSamples[i] = src[i].getActualSample();
		}

		int[] lengths = new int[src.length];
		for (int i = 0; i < src.length; i++)
			lengths[i] = rawSamples[i].getNumFrames();
		int totalSize = LeastCommonMultiple.compute(lengths);

		System.out.println("MixDown.TOTAL_SIZE=" + totalSize);

		// sum the samples
		float[] data, result = new float[totalSize];

		for (int i = 0; i < src.length; i++) {
			Sample s = rawSamples[i];
			data = new float[s.getNumFrames()];
			s.read(data);
			float vol = s.getVolume();
			for (int j = 0; j < result.length; j++) {
				result[j] += data[j % data.length] * vol;
			}
		}

		// divide each amp by # of srcs
		for (int i = 0; false && i < result.length; i++)
			result[i] /= src.length;

		// assign our result to dst sample
		Sample s = new Sample(result.length);
		s.write(result);
		dst.scrubSlider.reset();
		dst.setSample(s, true);

		// remove all the old samples
		for (int i = 0; i < src.length; i++) {
			if (src[i] != dst)
				src[i].delete();
		}
	}

	public int decrControl(SampleUIControlBank bank, boolean manual) {
		int current = bank.selectedControlIdx;
		if (--current < 0) {
			// change to prev bank if not manual
			if (!manual && --currentControlBankIdx < 0)
				currentControlBankIdx = controlBanks.length - 1;
			bank = setCurrentBank(currentControlBankIdx);
			current = bank.uiControls.length - 1;
		}
		return bank.setSelectedControl(current, true);
	}

	public List getAllSamples() {
		SampleUIControl[] voiceControls = currentControlBank().uiControls;
		List l = new ArrayList();
		if (voiceControls == null)
			return l;
		for (int i = 0; i < voiceControls.length; i++) {
			if (voiceControls[i] == null)
				continue;
			Object o = voiceControls[i].getSample();
			if (o != null)
				l.add(o);
		}
		return l;
	}

	public static SampleUIControlBank currentControlBank() {
		// System.out.println("LiveTextMix.currentControlBank(): "+currentControlBankIdx);
		return controlBanks[currentControlBankIdx];
	}

	public void setSelectedControl(int bankIdx, int controlIdx) {
		setCurrentBank(bankIdx);
		currentControlBank().setSelectedControl(controlIdx, true);
	}

	void drawOnRecord() {
		// recording circle
		fill(220);
		rectMode(PConstants.CENTER);
		rect(width - 20, 20, 30, 30);
		fill(255, 55, 55);
		ellipse(width - 20, 20, 30, 30);
		/*
		 * 
		 * float curr = recordBuffer.update(); stroke(220); rect(width - 20, height
		 * - 20, 30, 60); fill(255, 55, 55); rect(width - 20, height - 20, 30, curr
		 * * (height - 40)); rectMode(PConstants.CORNER);
		 */
		// drawLiveSample(this, recordBuffer.levels, true, 50, width/2,
		// height/2, LIVE_SAMPLE_Z_OFFSET);
	}

	public boolean recording() {
		/* return testRecording; */
		if (recordBuffer == null)
			return false;
		return recordBuffer.isRecording();
	}

	private void drawControlBanks() {
		for (int i = 0; i < controlBanks.length; i++) {
			SampleUIControlBank bank = controlBanks[i];
			if (bank == null)
				continue;
			bank.draw();
		}
	}

	public void setMasterProb(float p) {
		masterProb = p;
		prob.setValue((int) (100 * p));
		for (int k = 0; k < controlBanks.length; k++) {
			if (controlBanks[k] != null)
				controlBanks[k].refreshControls();
			/*
			 * continue; SampleUIControl[] voiceControls = controlBanks[k].uiControls;
			 * for (int i = 0; i < voiceControls.length; i++) { if (voiceControls[i]
			 * != null) voiceControls[i].refresh(!voiceControls[i].isMuted()); //
			 * voiceControls[i].mapToSlider(PROB, //
			 * voiceControls[i].sliders[PROB].getValue()); }
			 */
		}
	}

	void setMasterVolume(float vol) // 0-1
	{
		masterGain = vol;
		gain.setValue((int) (100 * vol));
		for (int k = 0; k < controlBanks.length; k++) {
			if (controlBanks[k] != null)
				controlBanks[k].refreshControls();
		}
	}

	private SampleUIControlBank setCurrentBank(int currentIdx) {
		currentControlBankIdx = currentIdx;
		updateBankView();
		return currentControlBank();
	}

	void updateBankView() {
		controlBanks[currentControlBankIdx].selected = true;
		// if (SHOW_UI)controlBanks[currentControlBankIdx].setVisible(true);
		// System.out.println("ENABLING: "+currentControlBankIdx);
		for (int i = 0; i < controlBanks.length; i++) {
			if (controlBanks[i] != null) {
				if (i != currentControlBankIdx) {
					// System.out.println(i+") ControlSets.enabled=false");
					controlBanks[i].selected = false;
					// controlBanks[i].setVisible(true);// Switch.SHOW_ALL_CONTROLS.on);
				}
			}
		}
		// controlBanks[currentControlBankIdx].selectedControl().setSelected(true);
	}

	// UI Delegation
	public void mouseClicked() {
		uiMan.mouseClicked();
	}

	public void mousePressed() {
		uiMan.mousePressed();
	}

	public void mouseMoved() {
		uiMan.mouseMoved();
	}

	public void mouseReleased() {
		uiMan.mouseReleased();
	}

	public void mouseDragged() {
		uiMan.mouseDragged();
	}

	public void keyPressed() {
		uiMan.keyPressed(key, keyCode);
	}

	public void keyReleased() {
		uiMan.keyReleased(key, keyCode);
	}

	public void actionPerformed(ActionEvent e) {
		uiMan.actionPerformed(e);
	}

	public void saveToXml(final File projDir) {
		System.out.println("[INFO] " + getClass().getName() + ".saveToXml(projDir="
				+ projDir + ")");// "+parentDir+")");
		new Thread() {
			public void run() {
				try {
					saving = true;

					long ts = System.currentTimeMillis() / 1000;

					// mk save dir
					if (!projDir.exists()) // copy first...
					{
						if (!projDir.mkdirs())
							throw new RuntimeException("Unable to create proj dir: "
									+ projDir);
					}
					System.out.println("\n[INFO] Created directory: "
							+ projDir.getAbsolutePath());

					Properties p = toXml(projDir);
					p = sortProperties(p);
					File config = new File(projDir, projDir.getName() + ".xml");
					p.storeToXML(new FileOutputStream(config), new Date() + "");

					System.out.println("\n[INFO] Saved xml properties to: "
							+ config.getAbsolutePath());
				} catch (IOException e) {
					e.printStackTrace();
				}
				saving = false;
			}
		}.start();
	}

	// is this not working?
	public static Properties sortProperties(Properties p) {
		List list = new LinkedList(p.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
		Properties result = new Properties();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private void preloadFiles(String sampleDir) {
		if (LOAD_SAMPLE_DIR) {
			if (sampleDir == null)
				return;
			loadSamplesFromDir(sampleDir);
			return;
		} else if (LOAD_CONFIG_FILE) {
			String home = System.getProperty("user.dir");
			int idx = PROJECT_TO_LOAD.lastIndexOf('/');
			String projName = PROJECT_TO_LOAD;
			if (idx > -1)
				projName = PROJECT_TO_LOAD.substring(idx + 1);
			File xml = new File(home + "/" + PROJECT_TO_LOAD + "/" + projName
					+ ".xml");
			// System.out.println("Trying: "+xml+" proj="+projName);
			fromXml(xml);
		}
	}

	private void loadSamplesFromDir(String directory) { 
		
		File dir = new File(directory);
		if (!dir.exists())
			throw new RuntimeException("No sampleDir(" + directory + ") found in: "
					+ System.getProperty("user.dir"));

		// System.out.println(Arrays.asList(dir.listFiles()));

		FileFilter ff = new FileFilter() {
			public boolean accept(File pathname) {
				System.out.println("trying " + pathname.getName());
				return pathname.toString().endsWith(".wav")
						|| pathname.toString().endsWith(".aiff")
						|| pathname.toString().endsWith(".aif");
			}
		};

		File[] samps = dir.listFiles(ff);

		List l = Arrays.asList(samps);
		Collections.shuffle(l);
		samps = (File[]) l.toArray(new File[l.size()]);

		System.out.println("[INFO] Loading " + samps.length + " samples from: "
				+ directory);

		for (int i = 0; i < Math.min(samps.length, MAX_NUM_TO_PRELOAD); i++) {
			SampleUIControl sc = controlBanks[currentControlBankIdx]
					.selectedControl();

			sc.setSample(samps[i].getPath(), true, false);
			sc.setGain(.4f); // lower volume for default samples
		}
	}

	static boolean getBool(Properties p, String key) {
		return p.getProperty(key, "false").equals("true");
	}

	public void fromXml(File f) {

		Properties p = new Properties();

		try {
			p.loadFromXML(new FileInputStream(f));
			System.out.println("[INFO] Loaded: " + f); 
			//+ "\n       ("+ p.getProperty("data.dir") + ")");

		} catch (Exception e) {

			System.err.println("[WARN] No config file found: " + f);
			return;
		}

		try {
			gain.setValue(Integer.parseInt(p.getProperty("master.volume")));
		} catch (NumberFormatException e1) {
			gain.setValue((int) (INITIAL_MASTER_VOL * 100));
		}

		try {
			prob.setValue(Integer.parseInt(p.getProperty("master.prob")));
		} catch (NumberFormatException e) {
			prob.setValue(100);
		}

		Switch.SNIP.set(getBool(p, "master.snip"));
		for (int i = 0; i < controlBanks.length; i++)
			controlBanks[i].fromXml(p, i);
	}

	// save dir structure (move xml out of data dir & make dir invisible)
	// ===========================================
	// /proj-name
	// /proj-name.xml
	// /data
	// sample1234.wav
	// sample1235.wav
	private Properties toXml(File projDir) {
		System.out.println("SamplerFi.toXml(" + projDir + ")");
		Properties p = new Properties();
		p.setProperty("master.volume", gain.getValue() + "");
		p.setProperty("master.prob", prob.getValue() + "");
		p.setProperty("master.snip", Switch.SNIP.on + "");
		p.setProperty("data.dir", projDir.getAbsolutePath());
		for (int i = 0; i < controlBanks.length; i++)
			controlBanks[i].toXml(p, i);
		return p;
	}

	public static boolean isSaving() {
		return saving;
	}

	public static String stackToString(Throwable t) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		t.printStackTrace(printWriter);
		return result.toString();
	}

	protected void handleKeyEvent(KeyEvent event) {
		keyEvent = event;
		key = event.getKeyChar();
		keyCode = event.getKeyCode();

		keyEventMethods.handle(new Object[] { event });

		switch (event.getID()) {
		case KeyEvent.KEY_PRESSED:
			keyPressed = true;
			keyPressed();
			break;
		case KeyEvent.KEY_RELEASED:
			keyPressed = false;
			keyReleased();
			break;
		case KeyEvent.KEY_TYPED:
			keyTyped();
			break;
		}
	}

	public static void setQuantizeMode(String mode) {
		// System.out.println("setQuantizeMode("+mode+")");

		quantizeMode = NO_QUANTIZE;

		if (mode.equals(ADDITIVE))
			quantizeMode = ADDITIVE_QUANTIZE;
		else if (mode.equals(SUBTRACTIVE))
			quantizeMode = SUBTRACTIVE_QUANTIZE;
		else if (mode.equals(MICRO))
			quantizeMode = MICRO_QUANTIZE;
	}

	static Preferences getPrefs() {
		if (prefs == null)
			prefs = Preferences.userRoot().node(Pataclysm.class.getName());
		return prefs;
	}

	public void mixDown(SampleUIControl sampleUIControl,
			SampleUIControl[] uiControls) {
		System.err.println("Pataclysm.mixDown() :: not implemented!");
	}

	public static void main(String[] args) {
		// MyGUIButton.SHOW_BUTTON_ARROW = true;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new ApplicationFrame(new Pataclysm(), 1280, 768);
			}
		});
	}

}// end
