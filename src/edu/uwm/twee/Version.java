package edu.uwm.twee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.uwm.util.Resource;

public class Version {

	private final String versionString;

	private Version() {
		versionString = Version.getVersionString();
	}

	@Override
	public String toString() { return versionString; }

	/**
	 * Compute the version string by trying to find the file "README.md"
	 * and reading the appropriate line
	 * @return version string
	 */
	private static String getVersionString() {
		String version = VERSION_PREFIX + " ???";
		InputStream s = Resource.getStream(Version.class,"README.md");
		if (s == null) return version;
        try {
                BufferedReader br = new BufferedReader(new InputStreamReader(s));
                String line;
                while ((line = br.readLine()) != null) {
                	if (line.startsWith(VERSION_PREFIX)) {
                		version = line;
                		break;
                	}
                }
        } catch (IOException e) {
                // muffle exception
        }
        return version;
	}

	private static Version instance = null;
	private static final String VERSION_PREFIX = "Twee Plugin version";

	/**
	 * Get the current version, which can then be printed/displayed.
	 * @return current version (never null)
	 */
	public static Version getInstance() {
		synchronized (Version.class) {
			if (instance == null) {
				instance = new Version();
			}
		}
		return instance;
	}

	public static void main(String[] args) {
		System.out.println(getInstance());
	}
}
