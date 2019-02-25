package core.test;

import pitaru.sonia.Sample;
import processing.core.PApplet;
import core.SampleScrubber;

public class SampleScrubberTest extends PApplet {
	
	SampleScrubber scrubber;
	Sample s;

	public void setup() {
	
		size(600,600);
		s = new Sample("piano1.wav");
		scrubber = new SampleScrubber(this, s.getNumFrames(), 50, 50);
	}

	public void draw() {
		scrubber.draw(s);
	}
}
