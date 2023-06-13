/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
