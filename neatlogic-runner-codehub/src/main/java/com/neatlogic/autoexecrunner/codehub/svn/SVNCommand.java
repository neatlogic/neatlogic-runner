/**
 * 
 */
package com.neatlogic.autoexecrunner.codehub.svn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**   
 * @ClassName   SVNCommand   
 * @Description 执行 SVN 命令相关方法
 * @author      zouye
 * @date        2021-05-21   
 *    
 */
public class SVNCommand {
	private List<String> globalOptions;
	private List<String> cmds;
	
	public SVNCommand(String username, String password) {
		cmds = new ArrayList<>();
		cmds.add("svn");
		
		globalOptions = new ArrayList<>(Arrays.asList("--username", username, "--password", password, "--no-auth-cache", "--non-interactive", "--trust-server-cert"));
	}
	
	public SVNCommand(String username, String password, String configDir, String configOption) {
		this(username, password);
		
		if (configDir != null && !configDir.equals("")) {
			globalOptions.add("--config-dir");
			globalOptions.add(configDir);
		}
		
		if (configOption != null && !configOption.equals("")) {
			globalOptions.add("--config-option");
			globalOptions.add(configOption);
		}
	}
	
	public SVNCommand mergeinfo() {
		cmds.add("mergeinfo");
		return this;
	}
	
	public SVNCommand cleanup () {
		cmds.add("cleanup");
		return this;
	}
	
	public SVNCommand revert () {
		cmds.add("revert");
		return this;
	}
	
	public SVNCommand update() {
		cmds.add("update");
		return this;
	}
	
	public SVNCommand checkout() {
		cmds.add("checkout");
		return this;
	}
	
	public SVNCommand merge() {
		cmds.add("merge");
		return this;
	}
	
	public SVNCommand setOptions(String ...options) {
		cmds.addAll(Arrays.asList(options));
		return this;
	}
	
	public String[] end() {
		cmds.addAll(globalOptions);
		
		String[] ret = cmds.toArray(new String[] {});
		
		cmds.clear();
		cmds.add("svn");
		
		return ret;
	}
}
