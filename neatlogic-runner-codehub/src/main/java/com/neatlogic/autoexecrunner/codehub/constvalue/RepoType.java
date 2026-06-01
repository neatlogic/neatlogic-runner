package com.neatlogic.autoexecrunner.codehub.constvalue;

public enum RepoType {
	SVN("svn", "SVN"),
	GITLAB("gitlab", "GITLAB"),
	GIT("git", "GIT");
	
	private String value;
	private String text;
	
	private RepoType(String value, String text) {
		this.value = value;
		this.text = text;
	}
	
	public String getValue() {
		return value;
	}

	public String getText() {
		return text;
	}
	
	public boolean myEquals(Object other) {
		if (other == null) {
			return false;
		}
		
		if (getClass() == other.getClass()) {
			return this.equals(other);
		}
		
		if (String.class == other.getClass()) {
			return this.value.equals(other);
		}
		
		return false;
	}
}
