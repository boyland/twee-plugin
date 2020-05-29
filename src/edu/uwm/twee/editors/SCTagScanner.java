package edu.uwm.twee.editors;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.rules.*;

import edu.uwm.eclipse.util.ColorManager;

public class SCTagScanner extends RuleBasedScanner {

	public SCTagScanner(ColorManager manager) {
		IToken string = new Token(new TextAttribute(manager.getColor(ITweeColorConstants.STRING)));
		IToken template = new Token(new TextAttribute(manager.getColor(ITweeColorConstants.JS_TEMPLATE)));
		IToken link = new Token(new TextAttribute(manager.getColor(ITweeColorConstants.SC_LINK)));
		IRule[] rules = new IRule[5];

		// Add rule for double quotes
		rules[0] = new SingleLineRule("\"", "\"", string, '\\');
		// Add a rule for single quotes
		rules[1] = new SingleLineRule("'", "'", string, '\\');
		rules[2] = new MultiLineRule("`", "`", template);
		rules[3] = new MultiLineRule("[[","]]", link);
		// Add generic whitespace rule.
		rules[4] = new WhitespaceRule(new TweeWhitespaceDetector());

		setRules(rules);
	}
}
