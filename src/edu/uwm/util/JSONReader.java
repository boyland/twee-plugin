package edu.uwm.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A simple class for reading JSON from input.
 * JSON arrays are returned as arrays of objects.
 * JSON objects are returned as Maps.
 * An error is returns as a Throwable object.
 */
public class JSONReader implements Iterator<Object>, AutoCloseable {
	private final StreamTokenizer tok;
	private final Reader source;
	
	public JSONReader(Reader r) {
		source = r;
		tok = new StreamTokenizer(r);
		tok.ordinaryChar('/');
	}
	
	@Override
	public boolean hasNext() {
		int c;
		try {
			c = tok.nextToken();
		} catch (IOException e) {
			return true; // error token
		}
		if (c == StreamTokenizer.TT_EOF) return false; 
		tok.pushBack();
		return true;
	}

	@Override
	public Object next() {
		int c;
		try {
			c = tok.nextToken();
		} catch (IOException e) {
			return e;
		}
		switch (c) {
		case StreamTokenizer.TT_EOF: throw new NoSuchElementException("no more");
		case StreamTokenizer.TT_NUMBER:
			return tok.nval;
		case '"': case '\'':
			return tok.sval;
		case StreamTokenizer.TT_WORD:
			switch (tok.sval) {
			case "null": return null;
			case "true": return true;
			case "false": return false;
			default: return new JSONParseError(tok.lineno() + ": Illegal word: " + tok.sval);
			}
		case '[':
			return readArray();
		case '{':
			return readObject();
		default:
			return new JSONParseError(tok.lineno() + ": Illegal character: " + (char)c);
		}
	}
	
	private Object readArray() {
		List<Object> result = new ArrayList<>();
		try {
			if (tok.nextToken() != ']') {
				tok.pushBack();
				loop: for (;;) {
					if (!hasNext()) return new JSONParseError(tok.lineno() + ": Unterminated array");
					Object val = next();
					if (val instanceof Throwable) return val;
					result.add(val);
					switch (tok.nextToken()) {
					case ',': break;
					case ']': break loop;
					default: return new JSONParseError(tok.lineno() + ": bad array value");
					}
				}
			}
		} catch (IOException e) {
			return e;
		}
		return result.toArray();
	}
	
	private Object readObject() {
		Map<String,Object> result = new HashMap<>();
		try {
			if (tok.nextToken() != '}') {
				tok.pushBack();
				loop: for (;;) {
					if (!hasNext()) return new JSONParseError(tok.lineno() + ": Unterminated Object");
					Object key = next();
					if (key instanceof Throwable) return key;
					if (!(key instanceof String)) return new JSONParseError(tok.lineno() + ": Bad Object field name: " + key);
					if (tok.nextToken() != ':') return new JSONParseError(tok.lineno() + ": expected ':' after field name " + key);
					Object val = next();
					if (val instanceof Throwable) return val;
					result.put((String)key,val);
					switch (tok.nextToken()) {
					case ',': break;
					case '}': break loop;
					default: return new JSONParseError(tok.lineno() + ": bad Object value");
					}
				}
			}
		} catch (IOException e) {
			return e;
		}
		return result;
	}
	
	public void close() throws IOException {
		source.close();
	}
	
	public static void main(String[] args) throws IOException {
		for (String f : args) {
			try (Reader r = new BufferedReader(new FileReader(f));
				 JSONReader jr = new JSONReader(r);) {
				Object x = jr.next();
				System.out.println("From " + f + " read " + x);
			}
		}
	}
}
