package edu.uwm.twee.macro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Syntax of macros in SugarCube.
 * Macros can have different kinds of argument,
 * and can have different kinds of contents.
 * Macros that only live in other macros are special.
 */
public class MacroSyntax {
	public static enum ArgumentSyntax {
		NORMAL, EXPRESSION, COMMASEP
	};
	
	private final ArgumentSyntax argSyntax;
	private final int minArguments;
	private final int maxArguments;
	private final List<String> nestedMacros;
	private final boolean isNested;
	
	public MacroSyntax(ArgumentSyntax as, int min, int max, List<String> nested, boolean in) {
		argSyntax = as;
		minArguments = min;
		maxArguments = max;
		isNested = in;
		nestedMacros = new ArrayList<>(nested);
	}
	
	public MacroSyntax(ArgumentSyntax as, int min, int max, boolean in) {
		argSyntax = as;
		minArguments = min;
		maxArguments = max;
		isNested = in;
		nestedMacros = null;
	}
	
	public MacroSyntax(ArgumentSyntax as, int min, int max, String... inside) {
		this(as,min,max,Arrays.asList(inside));
	}

	public MacroSyntax(ArgumentSyntax as, int min, int max, List<String> inside) {
		argSyntax = as;
		minArguments = min;
		maxArguments = max;
		nestedMacros = new ArrayList<>(inside);
		isNested = false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		if (argSyntax != null) {
			sb.append(" \"argumentSyntax\": \"" + argSyntax + "\",");
		}
		if (minArguments > 0) {
			sb.append(" \"minArguments\": " + minArguments + ",");
		}
		if (maxArguments > 0) {
			sb.append(" \"maxArguments\": " + maxArguments + ",");
		}
		if (isNested) {
			sb.append(" \"isNested\": true,");
		}
		if (nestedMacros != null) {
			sb.append(" \"nestedMacros\": [");
			if (!nestedMacros.isEmpty()) {
				for (String n : nestedMacros) {
					sb.append(" \"" + n + "\",");
				}
				sb.setLength(sb.length()-1);
			} 
			sb.append("],");
		}
		if (sb.charAt(sb.length()-1) == ',') sb.setLength(sb.length()-1);
		sb.append("}");
		return sb.toString();
	}
	
	private static final String[] LEGAL_FIELDS = {
			"argumentSyntax", "minArguments", "maxArguments",
			"nestedMacros", "isNested"
	};
	
	public static MacroSyntax fromJSON(Object json) {
		if (json == null) {
			return new MacroSyntax(null,0,0,false);
		} else if (json instanceof Number) {
			int m = ((Number)json).intValue();
			return new MacroSyntax(ArgumentSyntax.NORMAL,m,m,false);
		} else if (json instanceof String) {
			String s = (String)json;
			return new MacroSyntax(ArgumentSyntax.valueOf(s),1,1,false);
		} else if (json instanceof Object[]) {
			return new MacroSyntax(null,0,0,getNestedMacroList(json));
		} else if (json instanceof Map<?,?>) {
			Map<?,?> object = ((Map<?,?>)json);
			for (Map.Entry<?,?> ent : object.entrySet()) {
				if (!Arrays.asList(LEGAL_FIELDS).contains(ent.getKey())) {
					throw new IllegalArgumentException("bad macro syntax key '" + ent.getKey() + "'");
				}
			}
			ArgumentSyntax as = null;
			Object asObj = object.get(LEGAL_FIELDS[0]);
			if (asObj != null) {
				if (!(asObj instanceof String)) {
					throw new IllegalArgumentException(LEGAL_FIELDS[0] + " value is not a String: " + asObj);
				}
				as = ArgumentSyntax.valueOf((String)asObj);
			}
			int min = getIntField(object,LEGAL_FIELDS[1],as == ArgumentSyntax.EXPRESSION ? 1 : 0);
			int max = getIntField(object,LEGAL_FIELDS[2],min);
			if (max > 0 && as == null) as = ArgumentSyntax.NORMAL;
			List<String> nested = getNestedMacroList(object.get(LEGAL_FIELDS[3]));
			return new MacroSyntax(as,min,max,nested,getBooleanField(object,LEGAL_FIELDS[4],false));
		} else {
			throw new IllegalArgumentException("Illegal value: " + json);
		}
	}

	private static List<String> getNestedMacroList(Object json) {
		if (json == null) return Collections.emptyList();
		if (!(json instanceof Object[])) {
			throw new IllegalArgumentException("value not an array: " + json);
		}
		Object[] array = (Object[])json;
		List<String> nested = new ArrayList<>();
		for (Object x : array) {
			if (x instanceof String) {
				nested.add((String)x);
			} else {
				throw new IllegalArgumentException("value is not a String: " + x);
			}
		}
		List<String> result = nested;
		return result;
	}
	
	private static int getIntField(Map<?,?> object, String fieldName, int defaultValue) {
		Object x = object.get(fieldName);
		if (x == null) return defaultValue;
		if (!(x instanceof Number)) throw new IllegalArgumentException(fieldName + " value not an Integer: " + x);
		return ((Number)x).intValue();
	}
	
	private static boolean getBooleanField(Map<?,?> object, String fieldName, boolean defaultValue) {
		Object x = object.get(fieldName);
		if (x == null) return defaultValue;
		if (!(x instanceof Boolean)) throw new IllegalArgumentException(fieldName + " value not a Boolean: " + x);
		return ((Boolean)x).booleanValue();
	}
	
	/**
	 * Return the syntax for parsing arguments.
	 * @return argument syntax, null if none expected.
	 */
	public ArgumentSyntax getArgumentSyntax() {
		return argSyntax;
	}
	
	private static final String[] EMPTY_ARGUMENTS = {};
	
	/**
	 * Parse the arguments of the macro with this syntax
	 * @param argString after the macro name and up to (not including) ">>"
	 * @param errorHandler where errors should be reported
	 * @return array of strings from arguments
	 */
	public String[] parseArguments(String argString, Consumer<String> errorHandler) {
		argString = argString.trim();
		if (argString.isEmpty()) {
			if (minArguments > 0) errorHandler.accept("Missing arguments");
			return EMPTY_ARGUMENTS;
		}
		if (maxArguments == 0 || argSyntax == null) {
			errorHandler.accept("Arguments not expected");
			return EMPTY_ARGUMENTS;
		}
		List<Object> results = new ArrayList<>();
		int n = argString.length();
		int start = 0;
		for (int i=0; i < n; ++i) {
			switch (argString.charAt(i)) {
			case ',':
				if (argSyntax == ArgumentSyntax.COMMASEP) {
					results.add(argString.substring(start, i));
					start = i+1;
					break;
				}
				if (argSyntax == ArgumentSyntax.NORMAL) break;
				// fallthrough
			case ' ':
			case '\t':
			case '\n':
			case '\r':
				if (argSyntax == ArgumentSyntax.NORMAL) {
					results.add(argString.substring(start, i));
					while (i < n && Character.isWhitespace(argString.charAt(i))) {
						++i;
					}
					start = i;
					--i;
					break;
				}
				// fallthrough
			default:
				// TODO
				break;
			case '"':
			case '\'':
				i = skipString(argString,i);
				break;
			case '`':
				for (++i; i < n && argString.charAt(i) != '`'; ++i) {
					int ch = argString.charAt(i);
					if (ch == '\'' || ch == '"') {
						i = skipString(argString,i);
					}
				}
				break;
			case '[':
			case '(':
			case '{':
				i = skipParen(argString,i);
				break;
			}
		}
		if (start < n) {
			results.add(argString.substring(start));
		}
		if (minArguments > results.size()) {
			errorHandler.accept("Missing " + (minArguments-results.size()) + " parameter(s)");
		} else if (maxArguments < results.size()) {
			errorHandler.accept("Extra parameter: " + results.get(maxArguments));
		}
		return results.toArray(new String[results.size()]);
	}
	
	private int skipString(String s, int i) {
		int ch = s.charAt(i);
		int n = s.length();
		for (++i; i < n && s.charAt(i) != ch; ++i) {
			if (s.charAt(i) == '\\') ++i;
		}
		return i;
	}
	
	private int skipParen(String s, int i) {
		int ch = s.charAt(i);
		int n = s.length();
		int endCh = (ch == '(')? ')' : (ch == '[') ? ']' : '}';
		for (++i; i < n && s.charAt(i) != endCh; ++i) {
			ch = s.charAt(i);
			switch (ch) {
			case '"':
			case '\'':
				i = skipString(s,i);
				break;
			case '(':
			case '[':
			case '{':
				i = skipParen(s,i);
				if (i == n) return i;
				break;
			default:
				break;
			}
		}
		return i;
	}

	/**
	 * Return whether this macro accepts an end tag <</name>>
	 * @return whether this macro had a body
	 */
	public boolean needsEndTag() {
		return (!isNested && nestedMacros != null);
	}
}
