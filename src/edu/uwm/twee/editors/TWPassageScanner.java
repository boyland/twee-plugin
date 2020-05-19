package edu.uwm.twee.editors;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;

import edu.uwm.eclipse.util.ColorManager;

public class TWPassageScanner extends RuleBasedScanner {

	public TWPassageScanner(ColorManager manager) {
		IToken string = new Token(new TextAttribute(manager.getColor(IXMLColorConstants.STRING)));
		IToken tag = new Token(new TextAttribute(manager.getColor(IXMLColorConstants.TAG)));
		IRule[] rules = new IRule[4];

		// Add rule for double quotes
		rules[0] = new SingleLineRule("\"", "\"", string, '\\');
		// Add a rule for single quotes
		rules[1] = new SingleLineRule("'", "'", string, '\\');
		// Add generic whitespace rule.
		rules[2] = new WhitespaceRule(new TweeWhitespaceDetector());
		rules[3] = new SingleLineRule("[","]",tag);

		setRules(rules);
	}
}
