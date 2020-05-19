package edu.uwm.twee;

public class Version {

	private final String versionString;

	private Version() {
		versionString = Version.getVersionString();
	}

	@Override
	public String toString() { return versionString; }

	/**
	 * (Eventually)
	 * Compute the version string by trying to find the file "README.md"
	 * and reading the first line. 
	 * @return version string
	 */
	private static String getVersionString() {
		return "Twee Editor version 0.1";
	}

	private static Version instance = null;

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
