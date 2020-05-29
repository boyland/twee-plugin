package edu.uwm.twee.editors;

import org.eclipse.jface.text.rules.*;

import edu.uwm.eclipse.util.ColorManager;

import org.eclipse.jface.text.*;

public class XMLScanner extends RuleBasedScanner {

	public XMLScanner(ColorManager manager) {
		IToken procInstr =
			new Token(
				new TextAttribute(
					manager.getColor(ITweeColorConstants.PROC_INSTR)));

		IRule[] rules = new IRule[2];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", procInstr);
		// Add generic whitespace rule.
		rules[1] = new WhitespaceRule(new TweeWhitespaceDetector());

		setRules(rules);
	}
}
