package edu.uwm.twee.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class TweeWhitespaceDetector implements IWhitespaceDetector {

	@Override
	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
