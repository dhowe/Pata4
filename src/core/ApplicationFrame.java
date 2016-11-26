package core;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Enumeration;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import pitaru.sonia.LiveInput;
import pitaru.sonia.Sonia;
import processing.core.PApplet;

public class ApplicationFrame extends JFrame implements SamplerConstants, ActionListener// ChangeListener
{ 
  static boolean OSX = (System.getProperty("os.name").toLowerCase().startsWith("mac os x"));      
  
  static File sampleDir, projDir;
  
  static protected JButton savePrefsButton;
  static protected JDialog aboutBox, prefsBox;

  static protected JMenu fileMenu, helpMenu, globalMenu, sampleMenu, bankMenu, quantizeMenu;
  static protected JMenuItem openMI, optionsMI, quitMI, saveMI;
  static protected JMenuItem docsMI, supportMI, aboutMI;
  
  static JSpinner microDataSpinner,microPadSpinner,microProbSpinner;
  static JComboBox modeList;
  
  static private PApplet p;
  static private UIManager uiMan;
  
  static String[] sampleMenuNames = { OPEN, CUT, COPY, PASTE, REVERT, REVERSE, DOUBLE, DECLICK };
  static String[] sampleCbMenuNames = { SOLO, MUTE, SWEEP, BOUNCE, }; 
  static String[][] nestedMenus = {{ SHIFT, "-12", "-7", "-5", "-2", "  0", "  2", "  3", "  5", "  12", "  0" }};
  static String[] bankMenuNames = { SOLO, MUTE, CLEAR, DOUBLE, SWEEP, BOUNCE, REVERT, REVERSE, DOUBLE, DECLICK };
  static ButtonGroup qGroup;

  public ApplicationFrame(PApplet sketch, int w, int h)
  {
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    
    p = sketch;
    
    startSonia(p);
    
    sampleDir = new File(DATA_DIR);
    projDir = new File(PROJ_DIR);
    
    addMenus();
    
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        quit();
    }});
    
    JPanel panel = createSketchPanel(sketch, w, h);

    aboutBox = createAbout();
    prefsBox = createPrefs();
    
    registerForMacOSXEvents();

    sketch.init(); // start the sketch
    
    doLayout(w, h);
    
    panel.requestFocus();
  }

	private JPanel createSketchPanel(PApplet sketch, int w, int h) {
		
		JPanel panel = new javax.swing.JPanel();
    Color purple = new Color(40);
    Pataclysm.bg = new float[] { purple.getRed(), purple.getGreen(), purple.getBlue() };
    panel.setBackground(purple);
    panel.setBounds(20, 0, w, h + 40);
    panel.add(sketch);
    this.add(panel);
		return panel;
	}
  
  private JDialog createPrefs()
  {
    JDialog prefsDialog = new JDialog(this, "Preferences");
    prefsDialog.setModal(true);
    prefsDialog.addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        System.out.println("prefsDialog.windowOpened...");
        refreshPrefsData();
    }});
    
    JPanel prefsPanel = new JPanel();
    prefsPanel.setLayout(null);
    prefsPanel.setSize(500, 400);
    Insets is = prefsPanel.getInsets();
    
    // QUANTIZE_PANEL ==============================
    
    JPanel quantizePanel = new JPanel();
    quantizePanel.setLayout(null);
    quantizePanel.setBorder(new TitledBorder("Quantize"));
    quantizePanel.setBounds(0, 0, prefsPanel.getWidth(), 100);
    is = quantizePanel.getInsets();
    int x1 = is.left+20; 
    int y1 = is.top+20;
    
    JLabel modeLabel = new JLabel("Initial Quantize Mode:");
    Dimension d = modeLabel.getPreferredSize();
    modeLabel.setBounds(x1, y1, d.width, d.height);
  
    modeList = new JComboBox(QUANTIZE_MODES);
    d = modeList.getPreferredSize();
    modeList.setBounds(x1+170, modeLabel.getY()-4, d.width, d.height);

    quantizePanel.add(modeLabel);
    quantizePanel.add(modeList);
  
    // MICRO_PANEL ==============================
    
    JPanel microPanel = new JPanel();
    microPanel.setLayout(null);
    microPanel.setBorder(new TitledBorder("Micro-mode"));
    microPanel.setBounds(0, 100, prefsPanel.getWidth(), 150);
    is = microPanel.getInsets();
    
    y1 = is.top + 20;
    
    JLabel l1 = new JLabel("Data(ms):");
    d = l1.getPreferredSize();
    l1.setBounds(x1, y1, d.width, d.height);
    
    microDataSpinner = new JSpinner(new SpinnerNumberModel(    
        Pataclysm.microDataSize, // initial
        100,                                                              // min
        SAMPLE_RATE/2,                                                    // max
        100));                                                            // step     
    
    microDataSpinner.setName(MICRO_DATA);
    //s1.addChangeListener(this);
    d = microDataSpinner.getPreferredSize();
    microDataSpinner.setBounds(l1.getX()+l1.getWidth()+20, l1.getY()-5, d.width, d.height);

    JLabel l2 = new JLabel("Pad(ms):");
    d = l2.getPreferredSize();
    l2.setBounds(microDataSpinner.getX()+microDataSpinner.getWidth()+40, y1, d.width, d.height);
    
    microPadSpinner = new JSpinner(new SpinnerNumberModel
        (Pataclysm.getPrefs().getInt(MICRO_PAD, DEFAULT_MICRO_PAD_SIZE), 1000, SAMPLE_RATE, 200));      
    microPadSpinner.setName(MICRO_PAD);
    //s2.addChangeListener(this);
    d = microPadSpinner.getPreferredSize();
    microPadSpinner.setBounds(l2.getX()+l2.getWidth()+20, l2.getY()-5, d.width, d.height);
    
    y1 += 50;
    
    JLabel l3 = new JLabel("Default-probability:");
    d = l3.getPreferredSize();
    l3.setBounds(x1, y1, d.width, d.height);
    
    microProbSpinner = new JSpinner(new SpinnerNumberModel(Pataclysm.microProb, 0.1, 1.0, .05)); 
    microProbSpinner.setName(MICRO_PROB);
    //s3.addChangeListener(this);
    d = microProbSpinner.getPreferredSize();
    microProbSpinner.setBounds(l3.getX()+l3.getWidth()+20, l3.getY()-5, d.width, d.height);

    microPanel.add(l1);
    microPanel.add(microDataSpinner);
    microPanel.add(l2);
    microPanel.add(microPadSpinner);
    microPanel.add(l3);
    microPanel.add(microProbSpinner);
    
    JPanel buttPanel = new JPanel();
    savePrefsButton = new JButton("save");
    savePrefsButton.addActionListener(this);
    d = savePrefsButton.getPreferredSize();
    savePrefsButton.setBounds(l3.getX()+l3.getWidth()+20, l3.getY()+25, d.width, d.height);
    buttPanel.setBounds(0, 300, prefsPanel.getWidth(), 150);
    buttPanel.add(savePrefsButton);

    prefsPanel.add(quantizePanel);
    prefsPanel.add(microPanel);
    prefsPanel.add(buttPanel);
        
    prefsDialog.getContentPane().add(prefsPanel);
    prefsDialog.setSize(500, 400);
    prefsDialog.setResizable(false);
    
    return prefsDialog;
  }

  void startSonia(PApplet p)
  {
		Sonia.setInputDevice(Pataclysm.INPUT_DEVICE_ID);
		Sonia.setOutputDevice(Pataclysm.OUTPUT_DEVICE_ID);
		
    Sonia.start(p);
    LiveInput.start(SPECTRUM_LENGTH);
    LiveInput.useEqualizer(false);
  }

  // Generic registration with the Mac OS X application menu
	public void registerForMacOSXEvents()
  {
    if (OSX) {
      try {
        // Generate and register the OSXAdapter, passing the methods we wish to
        // use as delegates for various com.apple.awt.ApplicationListener methods
        OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod("quit", (Class[]) null));
        OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod("about", (Class[]) null));
        OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod("preferences", (Class[]) null));
        //OSXAdapter.setFileHandler(this, getClass().getDeclaredMethod("loadImageFile", new Class[] { String.class }));
      }
      catch (Exception e)
      {
        System.err.println("Error while loading OSXAdapter code...");
        e.printStackTrace();
      }
    }
  }
  
  private JDialog createAbout()
  {
    JDialog ab = new JDialog(this, "About");
    ab.getContentPane().setLayout(new BorderLayout());
    ab.getContentPane().add(new JLabel("SamplerFi["+VERSION+"]", JLabel.CENTER));
    ab.getContentPane().add(new JLabel("\u00A920010 Daniel Howe", JLabel.CENTER), BorderLayout.SOUTH);
    ab.setSize(160, 120);
    ab.setResizable(false);
    return ab;
  }

  private void doLayout(int w, int h)
  {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int yoff= (screenSize.height - h)/2; // the window Dimensions
    setBounds((screenSize.width - w)/2, yoff, w, h + 40); 
    setUndecorated(yoff < 20);
    setVisible(true);
  }

  public void addMenus()
  {
    JMenu fileMenu = new JMenu("File");
    
    JMenuBar mainMenuBar = new JMenuBar();
    
    // FILE_MENU ----------------------
    mainMenuBar.add(fileMenu = new JMenu("File"));
    setShortcut(fileMenu, KeyEvent.VK_F);

    fileMenu.add(openMI = new JMenuItem("Open..."));
    setShortcut(openMI, KeyEvent.VK_O);
    openMI.addActionListener(this);
    
    fileMenu.addSeparator();
    
    fileMenu.add(saveMI = new JMenuItem("Save"));
    setShortcut(saveMI, KeyEvent.VK_S);
    saveMI.addActionListener(this);
    
      
    // Quit/prefs menu items are provided on Mac OS X; only add your own on other platforms
    if (!OSX)
    {
      fileMenu.addSeparator();
      fileMenu.add(optionsMI = new JMenuItem("Options"));
      optionsMI.addActionListener(this);
      
      fileMenu.addSeparator();
      
      fileMenu.add(quitMI = new JMenuItem("Quit"));
      setShortcut(quitMI, KeyEvent.VK_Q);
      quitMI.addActionListener(this);
    }
    
    // GLOBAL_MENU ---------------------
    if (SHOW_GLOBAL_MENU) {
    	
      mainMenuBar.add(globalMenu = new JMenu("Global"));
      setShortcut(fileMenu, KeyEvent.VK_G);
      
      JMenuItem nextMI = null;
      for (int i = 0; i < Switch.ACTIVE.length; i++) {
        globalMenu.add(nextMI = new JCheckBoxMenuItem(Switch.ACTIVE[i].name));
        nextMI.setSelected(Switch.ACTIVE[i].on);
        setShortcut(nextMI, Switch.ACTIVE[i].key);
        nextMI.addActionListener(this);
      }
    }
    
    // BANK_MENU ---------------------
    if (SHOW_BANK_MENU) {
    	
      mainMenuBar.add(bankMenu = new JMenu("Bank"));
      setShortcut(bankMenu, KeyEvent.VK_B);
      
      JMenuItem jmi = null;
      for (int i = 0; i < bankMenuNames.length; i++)
      {
        if (bankMenuNames[i].equals(SEP))
          bankMenu.addSeparator();
        else {
          bankMenu.add(jmi = new JCheckBoxMenuItem(bankMenuNames[i]));
          jmi.addActionListener(this);
        }
      }
    }
    
    // SAMPLE_MENU ---------------------
    mainMenuBar.add(sampleMenu = new JMenu("Sample"));
    
    JMenuItem jmi = null;
    for (int i = 0; i < sampleMenuNames.length; i++)
    {
        sampleMenu.add(jmi = new JMenuItem(sampleMenuNames[i]));
        jmi.addActionListener(this);
        // keyboard shortcuts
        if (sampleMenuNames[i].equals(PASTE)) { 
          setShortcut(jmi, KeyEvent.VK_X);
        }
        else if (sampleMenuNames[i].equals(CUT))
          //jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, MENU_MASK));
        	setShortcut(jmi, KeyEvent.VK_X);
        else if (sampleMenuNames[i].equals(COPY))
          //jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, MENU_MASK));
        	setShortcut(jmi, KeyEvent.VK_C);
    }
    
    
    // QUANTIZE_MENU ---------------------
    int[] accelerators = { KeyEvent.VK_G, KeyEvent.VK_K, KeyEvent.VK_M, KeyEvent.VK_N };
    mainMenuBar.add(quantizeMenu = new JMenu("Quantize"));
    qGroup = new ButtonGroup();
    for (int i = 0; i < QUANTIZE_MODES.length; i++)
    {
      JRadioButtonMenuItem rmi = new JRadioButtonMenuItem(QUANTIZE_MODES[i]);
      //rmi.setAccelerator(KeyStroke.getKeyStroke(accelerators[i], MENU_MASK));
      setShortcut(rmi, accelerators[i]);
      rmi.addActionListener(this);
      rmi.setSelected(i==0);
      quantizeMenu.add(rmi);
      qGroup.add(rmi);
    }
    
    if (false) { // modal menus
      for (int i = 0; i < sampleCbMenuNames.length; i++)
      {
        if (i == 0) sampleMenu.addSeparator();
        sampleMenu.add(jmi = new JCheckBoxMenuItem(sampleCbMenuNames[i]));
        jmi.addActionListener(this);
      }
    }

    // help & about menus ---------------------
    
    mainMenuBar.add(helpMenu = new JMenu("Help"));
    helpMenu.add(docsMI = new JMenuItem("Online Documentation"));
    helpMenu.addSeparator();
    helpMenu.add(supportMI = new JMenuItem("Technical Support"));
    
    // About menu item is provided on Mac OS X; only add your own on other platforms
    if (!OSX)
    {
      helpMenu.addSeparator();
      helpMenu.add(aboutMI = new JMenuItem("About SamplerFi"));
      aboutMI.addActionListener(this);
    }

    setJMenuBar(mainMenuBar);
  }

	private void setShortcut(JMenuItem jmi, int keyEvent) {
		if (!(jmi instanceof JMenu))
			jmi.setAccelerator(KeyStroke.getKeyStroke(keyEvent, KeyEvent.META_DOWN_MASK));
		jmi.setMnemonic(keyEvent);
	}

  public void actionPerformed(ActionEvent e)
  {
    Object src = e.getSource();
    String cmd = e.getActionCommand();
    
    //System.out.println("ApplicationFrame.actionPerformed("+src.getClass().getName()+","+cmd+")");

    // global-switch menu
    for (int i = 0; i < Switch.ACTIVE.length; i++)
    {
       if (Switch.ACTIVE[i].name.equals(cmd)) {
         Switch.ACTIVE[i].toggle();
         return;
       }
    }
    
    if (uiMan == null) uiMan = Pataclysm.uiMan;
    
    // quantize menu
    for (int i = 0; i < quantizeMenu.getItemCount(); i++)
    {
      JMenuItem mi = quantizeMenu.getItem(i);
      if (src == mi) {
        Pataclysm.setQuantizeMode(mi.getActionCommand());
        return;
      }
    }
    
    // sample menu
    for (int i = 0; i < sampleMenu.getItemCount(); i++)
    {
      JMenuItem mi = sampleMenu.getItem(i);
      if (src == mi) {
        Pataclysm.currentControl().doAction(-1, -1, mi.getActionCommand());
        return;
      }
    }
    
    // bank menu
    for (int i = 0; i < bankMenu.getItemCount(); i++)
    {
      JMenuItem mi = bankMenu.getItem(i);
      if (src == mi) {
        Pataclysm.currentControlBank().doAction(-1, -1, mi.getActionCommand());
        return;
      }
    }
    
    // File, Help, Global menus -----------------------------
    
    if (src == quitMI)
    {
      quit();
    }
    else if (src == optionsMI)
    {
      preferences();
    }
    else if (src == aboutMI)
    {
      about();
    }
    else if (src == saveMI) 
    {
      saveProject();
    }
    else if (src == savePrefsButton) 
    {
      savePrefs();
    }
    else if (src == openMI) // open proj config file
    {
      loadProject();
    } 
  }

  private void savePrefs() 
  {
    Preferences prefs = Pataclysm.getPrefs();
    
    Pataclysm.microDataSize = ((Integer)microDataSpinner.getValue()).intValue();
    prefs.putInt(MICRO_DATA, Pataclysm.microDataSize);
    
    Pataclysm.microPadSize = ((Integer)microPadSpinner.getValue()).intValue();
    prefs.putInt(MICRO_PAD, Pataclysm.microPadSize);
    
    Pataclysm.microProb = ((Float)microProbSpinner.getValue()).floatValue();
    prefs.putFloat(MICRO_PROB, Pataclysm.microProb);
    
    String mode = QUANTIZE_MODES[modeList.getSelectedIndex()];
    Pataclysm.setQuantizeMode(mode);
    prefs.put(QUANTIZE_MODE, mode);
    
    prefsBox.setVisible(false);
    
   /* System.out.println("ApplicationFrame.savePrefs(): " +
    		" data="+SamplerFi.microDataSize+
        " pad="+SamplerFi.microPadSize +
        " prob="+SamplerFi.microProb +
        " mode="+SamplerFi.quantizeMode+"/"+mode);*/
    
    Enumeration e = qGroup.getElements();
    while (e.hasMoreElements())
    {
      JRadioButtonMenuItem rmi = (JRadioButtonMenuItem) e.nextElement();
      if (rmi.getActionCommand().equals(mode)) {
        rmi.setSelected(true);
        return;
      }
    }
  }

  private void loadProject()
  {
    String s = System.getProperty("user.dir");
    File def = new File(s + "/" + projDir);

    final JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setFileFilter(new ExampleFileFilter("xml"));
    fc.setCurrentDirectory(def);
    
    int result = fc.showOpenDialog(p);
    if (result == JFileChooser.APPROVE_OPTION) {
    	
    	File chosen = fc.getSelectedFile();
    	
    	if (chosen.isDirectory()) // check for xml inside
    		chosen = new File(chosen, chosen.getName()+".xml");
    	
    	if (chosen != null) ((Pataclysm)p).fromXml(chosen);
    }
  }

  public void about()
  {
    aboutBox.setLocation((int) this.getLocation().getX() + 22, (int) this.getLocation().getY() + 22);
    aboutBox.setVisible(true);
  }

  public void preferences()
  {
    prefsBox.setLocation(getWidth()/2-150,getHeight()/2-150);
    prefsBox.setVisible(true);
  }

  public void saveProject() 
  {
    final JFileChooser fc = new JFileChooser(PROJ_DIR);
    new Thread() {
      public void run() {
        fc.showSaveDialog(p);
        File f = fc.getSelectedFile();
        if (f != null)  {
          projDir = f;
          System.out.println("Selected: "+projDir);
          ((Pataclysm)p).saveToXml(projDir);
        }
      }
    }.start();
  }
  
  public boolean quit()
  {
    int option = JOptionPane.YES_OPTION;
    
    if (CONFIRM_ON_QUIT) { 
      option = JOptionPane.showConfirmDialog(this, 
        "Are you sure you want to quit?", "Quit?", JOptionPane.YES_NO_OPTION);
    }
    
    Sonia.stop();
    
    if (option == JOptionPane.YES_OPTION) {
    	
      Pataclysm.isExiting = true;
      
      System.out.print("[INFO] Exiting...");
      
      for (int i = 0; Pataclysm.isSaving(); i++) {
        try {
          Thread.sleep(100);
        }
        catch (InterruptedException e1) {
          System.err.println("[WARN] "+e1.getMessage());
        }
        System.out.print(".");
        if (i%50==49) 
          System.out.println();
      }      
      System.out.println("OK");

      System.exit(1);
    }
    
    return (option == JOptionPane.YES_OPTION);
  }

  @SuppressWarnings("boxing")
	private void refreshPrefsData()
  {
    microDataSpinner.setValue(Pataclysm.microDataSize);
    microPadSpinner.setValue(Pataclysm.microPadSize);
    microProbSpinner.setValue(Pataclysm.microProb);
    String mode = Pataclysm.getQuantizeMode();
    for (int i = 0; i < QUANTIZE_MODES.length; i++) {
      if (mode.equals(QUANTIZE_MODES[i]))
        modeList.setSelectedIndex(i);
    }
  }

  public static JMenuItem getSampleMenuItem(String text)
  {
    Component[] c = sampleMenu.getMenuComponents();
    for (int i = 0; i < c.length; i++)
    {
      JMenuItem jmi = (JMenuItem)c[i];
      if (jmi.getActionCommand().equals(text))
        return jmi; 
    }
    return null;
  }

  /*public void stateChanged(ChangeEvent e)
  {
    //System.out.println("ApplicationFrame.stateChanged("+e+")");
    Object o = e.getSource();
    if (o instanceof JSpinner) {
      JSpinner js = (JSpinner)o;
      String name = js.getName();
      Preferences prefs = SamplerFi.getPrefs();
      if (name.equals(MICRO_DATA)) { 
        SamplerFi.microDataSize = ((Integer)js.getValue()).intValue();
        prefs.putInt(MICRO_DATA, SamplerFi.microDataSize);
      }
      else if (name.equals(MICRO_PAD)) { 
        SamplerFi.microPadSize = ((Integer)js.getValue()).intValue();
        prefs.putInt(MICRO_PAD, SamplerFi.microPadSize);
      }
      else if (name.equals(MICRO_PROB)) { 
        SamplerFi.microProb = (float)((Double)js.getValue()).doubleValue();
        prefs.putFloat(MICRO_PROB, SamplerFi.microProb);
      }
    }*/
  
  
}