/******************************************************************************
 * Copyright (c) 2006 Remy Suen. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0, which accompanies this distribution and is available at
 * http://www.eclipse.org/legal/epl-v10.html, and also the MIT license, which
 * also accompanies this distribution. This dual licensing scheme allows a
 * developer to choose either license for use when developing applications with
 * this code.
 * 
 * Contributors:
 * 	Remy Suen <remy.suen@gmail.com> - initial API and implementation
 ******************************************************************************/

package org.eclipse.bittorrent;

/**
 * This listener reports on the overall progress of the current download.
 */
public interface ITorrentProgressListener {

	/**
	 * This method is called when a piece has been identified as being completed
	 * after a hash check verification has completed.
	 * 
	 * @param completed
	 *            the number of pieces completed thus far
	 */
	public void pieceCompleted(int completed);

}
