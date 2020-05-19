package edu.uwm.twee.editors;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

import edu.uwm.eclipse.util.WholeLineRule;

public class TweePartitionScanner extends RuleBasedPartitionScanner {
	public final static String XML_COMMENT = "__xml_comment";
	public final static String XML_TAG = "__xml_tag";
	public final static String SC_TAG = "__sc_tag";
	public final static String SC_CODE = "__sc_code";
	public final static String SC_LINK = "__sc_link";
	public final static String SC_HEADER = "__sc_header";
	public final static String TW_PASSAGE = "__tw_passage";

	public TweePartitionScanner() {

		IToken xmlComment = new Token(XML_COMMENT);
		IToken twPassage = new Token(TW_PASSAGE);
		IToken scCode = new Token(SC_CODE);
		IToken scLink = new Token(SC_LINK);
		IToken scHeader = new Token(SC_HEADER);
		IToken tag = new Token(XML_TAG);
		IToken sctag = new Token(SC_TAG);

		IPredicateRule[] rules = new IPredicateRule[7];

		rules[0] = new MultiLineRule("<!--", "-->", xmlComment);
		rules[1] = new MultiLineRule("{{{","}}}", scCode);
		rules[2] = new MultiLineRule("[[","]]", scLink);
		rules[3] = new WholeLineRule("!",scHeader);
		rules[4] = new WholeLineRule("::",twPassage);
		rules[5] = new TagRule(tag);
		rules[6] = new SCTagRule(sctag);

		setPredicateRules(rules);
	}
}
