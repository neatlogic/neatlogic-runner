/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/
package com.neatlogic.autoexecrunner.codehub.git;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

public class GitCredentialsProvider extends CredentialsProvider {
	private String username = null;
	private String password = null;

	public GitCredentialsProvider(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public boolean supports(CredentialItem... items) {
		for (CredentialItem i : items) {
			if (i instanceof CredentialItem.Username)
				continue;

			else if (i instanceof CredentialItem.Password)
				continue;

			else
				return false;
		}
		return true;
	}

	@Override
	public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
		for (CredentialItem item : items) {
			if (item instanceof CredentialItem.YesNoType) {
				((CredentialItem.YesNoType) item).setValue(true);
				continue;
			}
			
			if (item instanceof CredentialItem.Username) {
				((CredentialItem.Username) item).setValue(username);
				continue;
			}
			
			if (item instanceof CredentialItem.Password) {
				((CredentialItem.Password) item).setValue(password.toCharArray());
				continue;
			}
			
			if (item instanceof CredentialItem.StringType) {
				if (item.getPromptText().equals("Password: ")) { //$NON-NLS-1$
					((CredentialItem.StringType) item).setValue(password);
					continue;
				}
			}
			
			throw new UnsupportedCredentialItem(uri, item.getClass().getName() + ":" + item.getPromptText()); //$NON-NLS-1$
		}
		
		return true;
	}

	@Override
	public boolean isInteractive() {
		return false;
	}
	
	/**
	 * Destroy the saved username and password..
	 */
	public void clear() {
		username = null;
		password = null;
	}
}
