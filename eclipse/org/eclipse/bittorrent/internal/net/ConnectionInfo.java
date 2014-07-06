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

package org.eclipse.bittorrent.internal.net;

import java.nio.channels.SocketChannel;

class ConnectionInfo {

	private SocketChannel channel;

	private String ip;

	private int port;

	ConnectionInfo(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}

	ConnectionInfo(SocketChannel channel) {
		this.channel = channel;
	}

	String getIP() {
		return ip;
	}

	int getPort() {
		return port;
	}

	SocketChannel getChannel() {
		return channel;
	}

	boolean isChannel() {
		return channel != null;
	}

}
