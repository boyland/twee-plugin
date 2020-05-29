package edu.uwm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import edu.uwm.twee.Version;

/**
 * A class to simplify getting a resource
 * from this binary.  It first tries to use Java's resource
 * system and then if that doesn't work, tries to use the parent
 * directory of the directory where the class is loaded from.
 */
public class Resource {
	private Resource() { }

	/**
	 * Find a resource with the given name.
	 * @param name name to look for.
	 * @return stream for this resource, or null if not found
	 * @throws nothing
	 */
	public static InputStream getStream(String name) {
		return getStream(null,name);
	}
	
	/**
	 * Find a resource with the given name 
	 * using the given class to get a class loader
	 * @param cl class to use (if null, use this class)
	 * @param name name of resource to look for
	 * @return input stream or null
	 */
	public static InputStream getStream(Class<?> cl, String name) {
		if (cl == null) cl = Resource.class;
		InputStream s = cl.getClassLoader().getResourceAsStream(name);
		if (s != null) return s;
		URL execdir = Version.class.getClassLoader().getResource(".");
        URI uri;
        try {
                uri = execdir.toURI();
        } catch (URISyntaxException e) {
                // muffle exception
                return null;
        }
        if (uri.getScheme().equals("file")) {
                File dir = new File(uri.getPath());
                File rfile = new File(dir.getParentFile(),name);
                try {
                        s = new FileInputStream(rfile);
                } catch (FileNotFoundException ex) {
                        // muffle exception
                        return null;
                }
        }
        return s;
	}
}
