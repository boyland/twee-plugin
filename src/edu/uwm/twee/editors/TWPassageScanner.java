package edu.uwm.twee.editors;

import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.swt.SWT;

import edu.uwm.eclipse.util.ColorManager;

public class TWPassageScanner extends RuleBasedScanner {

	public TWPassageScanner(ColorManager manager) {
		IToken string = new Token(new TextAttribute(manager.getColor(ITweeColorConstants.STRING)));
		IToken tag = new Token(new TextAttribute(manager.getColor(ITweeColorConstants.TW_TAG),null,SWT.BOLD));
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
