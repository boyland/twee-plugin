package edu.uwm.twee.editors;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;

public class SCTagRule extends MultiLineRule {

	public SCTagRule(IToken token) {
		super("<<", ">>", token);
	}

}
