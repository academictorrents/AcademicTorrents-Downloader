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

package org.eclipse.bittorrent.example.cli;

import java.io.File;
import java.io.IOException;

import org.eclipse.bittorrent.Torrent;
import org.eclipse.bittorrent.TorrentConfiguration;
import org.eclipse.bittorrent.TorrentFactory;
import org.eclipse.bittorrent.TorrentFile;
import org.eclipse.bittorrent.TorrentServer;
import org.eclipse.bittorrent.TorrentConfiguration.IDebugListener;

public class CLI {

	private static void printUsage() {
		System.out.println("Usage: java org.eclipse.bittorrent.cli.CLI "
				+ "[-remove <torrent...>] [-log] [port] <torrent> <target>");
		System.out.println("\t-remove\tRemoves all records of the torrents "
				+ "specified after this argument.");
		System.out.println("\t-log\tOutputs logging information instead of "
				+ "the standard CLI output.");
		System.out.println("\tport\tThe port to use to listen for incoming "
				+ "connections");
		System.exit(-1);
	}

	public static void main(String[] args) {
		TorrentConfiguration.setConfigurationPath(new File(System
				.getProperty("user.home"), ".hilberteffect"));
		int offset = 0;
		File torrentFile = null;
		boolean debug = false;
		int port = -1;

		try {
			if (args.length > 1 && args[0].equals("-remove")) {
				for (int i = 1; i < args.length; i++) {
					TorrentConfiguration.remove(new TorrentFile(new File(
							args[i])).getHexHash());
				}
				return;
			}
			switch (args.length) {
			case 2:
				torrentFile = new File(args[offset]);
				if (!torrentFile.exists()) {
					System.out.println("The file '"
							+ torrentFile.getAbsolutePath()
							+ "' could not be found.");
					printUsage();
				}
				break;
			case 3:
				offset++;
				if (args[0].equals("-log")) {
					debug = true;
				} else {
					try {
						port = Integer.parseInt(args[0]);
					} catch (NumberFormatException e) {
						System.out.println(args[0] + " is not a number.");
						printUsage();
					}
				}
				torrentFile = new File(args[offset]);
				if (!torrentFile.exists()) {
					System.out.println("The file '"
							+ torrentFile.getAbsolutePath()
							+ "' could not be found.");
					printUsage();
				}
				break;
			case 4:
				offset += 2;
				if (args[0].equals("-log")) {
					debug = true;
				} else {
					System.out.println("An unknown argument was passed.");
					printUsage();
				}
				try {
					port = Integer.parseInt(args[1]);
				} catch (NumberFormatException e) {
					System.out.println(args[1] + " is not a number.");
					printUsage();
				}
				torrentFile = new File(args[offset]);
				if (!torrentFile.exists()) {
					System.out.println("The file '"
							+ torrentFile.getAbsolutePath()
							+ "' could not be found.");
					printUsage();
				}
				break;
			default:
				printUsage();
				break;
			}
			if (port != -1) {
				TorrentServer.setPort(port);
			}
			TorrentFile file = new TorrentFile(torrentFile);
			file.setTargetFile(new File(args[offset + 1]));

			Torrent host = TorrentFactory.createTorrent(file);

			if (debug) {
				TorrentConfiguration.DEBUG = true;
				TorrentConfiguration.setDebugListener(new DebugListener());
			} else {
				OutputThread thread = new OutputThread(host, file
						.getTargetFile(), file.getTotalLength());
				thread.start();
			}
			host.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static class DebugListener implements IDebugListener {

		public void print(String message) {
			System.out.println(message);
		}

	}

	public static class OutputThread extends Thread {

		private Torrent torrent;

		private File file;

		private double total;

		public OutputThread(Torrent torrent, File file, double total) {
			this.torrent = torrent;
			this.file = file;
			this.total = total;
		}

		public void run() {
			while (true) {
				System.out.println("Saving:\t\t\t" + file.getName());
				System.out.println("Connected Peers:\t"
						+ torrent.getConnectedPeers());
				System.out.println("Seeds/Peers:\t\t"
						+ (torrent.getSeeds() == -1 ? "Unknown" : Integer
								.toString(torrent.getSeeds()))
						+ "/"
						+ (torrent.getPeers() == -1 ? "Unknown" : Integer
								.toString(torrent.getPeers())));
				System.out.println("Percent Done:\t\t"
						+ ((total - torrent.getRemaining()) / total * 100)
						+ "%");
				System.out.println("Downloading To:\t\t"
						+ file.getAbsolutePath());
				System.out.println("Download Total:\t\t"
						+ torrent.getDownloaded() + " bytes");
				System.out.println("Upload Total:\t\t" + torrent.getUploaded()
						+ " bytes");
				long eta = torrent.getTimeRemaining();
				System.out.println("ETA:\t\t\t"
						+ (eta == -1 ? "Unknown" : eta + " seconds"));
				System.out.println("Download Speed:\t\t"
						+ (torrent.getDownSpeed() / 1024) + " kb");
				System.out.println("Upload Speed:\t\t"
						+ (torrent.getUpSpeed() / 1024) + " kb");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
