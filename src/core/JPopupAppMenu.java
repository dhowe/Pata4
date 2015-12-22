package core;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class JPopupAppMenu extends JPopupMenu implements ActionListener//, ItemListener
{
  private int mx, my;
  private Actionable parent;
  
  public JMenu createNestedMenu(String[] subs)
  {
    JMenuItem mi;
    String title = subs[0];
    JMenu m = new JMenu(title);
    ButtonGroup nGroup= new ButtonGroup();
    String defaultVal = subs[subs.length-1];
    for (int i = 1; i < subs.length-1; i++)
    {
      // m.add(subs[i]);
      m.add(mi = new JRadioButtonMenuItem(subs[i]));
      mi.addActionListener(this);
      mi.setActionCommand(title+'='+subs[i]);
      if (subs[i].equals(defaultVal))
        mi.setSelected(true);
      nGroup.add(mi);
    }
    return m;
  }

  public JPopupAppMenu(Actionable parent, String[] menus, String[] checkBoxMenus, String[][] nestedMenus)
  {
    this(parent, null, menus, checkBoxMenus, nestedMenus);
  }
  
  private JPopupAppMenu(Actionable parent, String title, String[] menus, String[] checkBoxMenus, String[][] nestedMenus)
  {
    super("context");
    this.parent = parent;
    JMenuItem mi;
    JCheckBoxMenuItem cmi;
    if (title != null) {
      add(new JMenuItem(title));
      addSeparator();
    }
    for (int i = 0; menus != null && i < menus.length; i++)
    {
      add(mi = new JMenuItem(menus[i]));
      //System.out.println("adding '"+menus[i]+"' list="+this);
      mi.addActionListener(this);
    }
    
    
    if (nestedMenus != null) 
    {
      addSeparator();
      for (int i = 0; i < nestedMenus.length; i++)
      {
        add(createNestedMenu(nestedMenus[i]));
      }
    }
    
    
    if (checkBoxMenus != null) 
    {
      addSeparator();
      for (int i = 0; i < checkBoxMenus.length; i++)
      {
        add(cmi = new JCheckBoxMenuItem(checkBoxMenus[i]));
        //System.out.println("adding '"+checkBoxMenus[i]+"' list="+this);
        cmi.setState(false);
        //cmi.addItemListener(this);
        cmi.addActionListener(this);
      }
    }
  }
  
  public void actionPerformed(ActionEvent e)
  {
    //System.out.println("JPopupAppMenu.actionPerformed("+e.getActionCommand()+")");
    parent.doAction(mx, my, e.getActionCommand());
  }

/*  public void itemStateChanged(ItemEvent e)
  {
    //System.out.println("JPopupAppMenu.itemStateChanged()");
    parent.doAction(mx, my, (String) e.getItem());
  }*/

  public void show(Component origin, int x, int y)
  {
    this.mx = x; this.my = y;
    super.show(origin, x-10, y-10);
  }

}