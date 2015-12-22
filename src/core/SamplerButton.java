package core;

import processing.core.PApplet;
import mkv.MyGUI.MyGUIButton;
import mkv.MyGUI.MyGUIStyle;

public class SamplerButton extends MyGUIButton {

	public static final boolean SHOW_BUTTON_ARROW = true;

  private boolean fixedSize = true;

	public SamplerButton(PApplet p, int i, int j) {
		super(p, i, j);
	}

	public SamplerButton(PApplet p, int i, int j, int k, int l) {
		super(p, i, j, k, l);
	}

	public void drawStates() {
		MyGUIStyle myguistyle = _style;
		if (_style == null)
			myguistyle = _parent.getStyle();
		else
			myguistyle = _style;
		if (!fixedSize) {
			_root.textFont(myguistyle.font, myguistyle.fontSize);
			_width = (PApplet.ceil(_root.textWidth(_text)) + 4 + myguistyle.padding);
			if (_width % 2 == 1)
				_width++;
			_height = myguistyle.fontSize + 2 + myguistyle.padding;
			if (_height % 2 == 1)
				_height++;
		}
		int i = PApplet.round(_width / 2.0F + myguistyle.padding);
		int i_9_ = PApplet.round(_height / 2.0F + myguistyle.padding);
		int i_10_ = 0;
		int i_11_ = 0;
		if (_icon != null) {
			i_10_ = PApplet.constrain(_icon.width, 2, _width - 2) / 2;
			i_11_ = PApplet.constrain(_icon.height, 2, _height - 2) / 2;
		}
		int i_12_ = myguistyle.fontSize / 2 - myguistyle.padding / 2 - 1;
		int i_13_ = 0;
		hover = checkForHit();
		_root.pushMatrix();
		_root.translate(_x, _y);
		_root.scale(_scale);
		_root.rotate(PApplet.radians(_rotation));
		_root.rectMode(1);
		_root.strokeWeight(myguistyle.strokeWeight);
		_root.strokeJoin(8);
		_root.textFont(myguistyle.font, myguistyle.fontSize);
		_root.textAlign(3);
		_root.imageMode(1);
		if (isDisabled()) {
			_root.stroke(myguistyle.buttonShadow);
			_root.fill(myguistyle.disabled);
			_root.rect( -i,  -i_9_,  i,  i_9_);
			if (_text.length() > 0) {
				_root.fill(myguistyle.buttonText);
				_root.text(_text,  i_13_,  i_12_);
			}
			if (_icon != null)
				_root.image(_icon,  -i_10_,  -i_11_,  i_10_,
						 i_11_);
		} else if (dragged) {
			_root.stroke(myguistyle.buttonShadow);
			_root.fill(myguistyle.buttonFace);
			_root.rect( -i,  -i_9_,  i,  i_9_);
			_root.stroke(myguistyle.buttonShadow);
			_root.noFill();
			_root.strokeWeight(myguistyle.strokeWeight * 1.2f);
			_root.quad( (-i + 1),  (-i_9_ + 1),  (i - 1),
					 (-i_9_ + 1),  (i - 1),  (i_9_ - 1),
					 (-i + 1),  (i_9_ - 1));
			if (_text.length() > 0) {
				_root.fill(myguistyle.buttonText);
				_root.text(_text,  i_13_,  i_12_);
			}
			if (_icon != null)
				_root.image(_icon,  -i_10_,  -i_11_,  i_10_,
						 i_11_);
		} else if (hover) {
			_root.stroke(myguistyle.buttonShadow);
			if (hasFocus()) {
				_root.fill(myguistyle.buttonHighlight);
				_root.rect( -i,  -i_9_,  i,  i_9_);
			} else {
				_root.fill(myguistyle.buttonFace);
				_root.rect( -i,  -i_9_,  i,  i_9_);
				_root.stroke(myguistyle.buttonHighlight);
				_root.noFill();
				_root.strokeWeight(myguistyle.strokeWeight * 1.2f);
				_root.quad( (-i + 1),  (-i_9_ + 1),  (i - 1),
						 (-i_9_ + 1),  (i - 1),  (i_9_ - 1),
						 (-i + 1),  (i_9_ - 1));
			}
			if (_text.length() > 0) {
				_root.fill(myguistyle.buttonText);
				_root.text(_text,  i_13_,  i_12_);
			}
			if (_icon != null)
				_root.image(_icon,  -i_10_,  -i_11_,  i_10_,
						 i_11_);
		} else if (hasFocus()) {
			_root.stroke(myguistyle.buttonShadow);
			_root.fill(myguistyle.buttonFace);
			_root.rect( -i,  -i_9_,  i,  i_9_);
			_root.stroke(myguistyle.buttonHighlight);
			_root.noFill();
			_root.quad( (-i + 1),  (-i_9_ + 1),  (i - 1),
					 (-i_9_ + 1),  (i - 1),  (i_9_ - 1),
					 (-i + 1),  (i_9_ - 1));
			if (_text.length() > 0) {
				_root.fill(myguistyle.buttonText);
				_root.text(_text,  i_13_,  i_12_);
			}
			if (_icon != null)
				_root.image(_icon,  -i_10_,  -i_11_,  i_10_,
						 i_11_);
		} else {
			_root.stroke(myguistyle.buttonShadow);
			_root.fill(myguistyle.buttonFace);
			_root.rect( -i,  -i_9_,  i,  i_9_);
			if (_text.length() > 0) {
				_root.fill(myguistyle.buttonText);
				_root.text(_text,  i_13_,  i_12_);
			}
			if (_icon != null)
				_root.image(_icon,  -i_10_,  -i_11_,  i_10_,
						 i_11_);
		}

		if (SHOW_BUTTON_ARROW) {
			
			// draw an arrow
			_root.pushMatrix();
			_root.fill(40);
			_root.rotate(PApplet.PI / 2F);
			_root.text('>', 2, 1);
			_root.popMatrix();
		}

		_root.popMatrix();
	}
}
