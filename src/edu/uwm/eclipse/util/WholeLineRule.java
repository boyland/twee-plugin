package edu.uwm.eclipse.util;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;

/**
 * A rule that matches lines start at the beginning of the line
 * and continuing to the end.
 */
public class WholeLineRule extends SingleLineRule {

	/**
	 * Create a rule which has the given prefix at the start of the line.
	 * @param startSequence string sequence that indicates this rule applies
	 * @param token how to show this rule
	 */
	public WholeLineRule(String startSequence, IToken token) {
		super(startSequence,null, token);
		setColumnConstraint(0);
	}

}
