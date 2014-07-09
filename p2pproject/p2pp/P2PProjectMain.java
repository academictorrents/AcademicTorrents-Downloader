package p2pp;

public class P2PProjectMain {

	public static void main(String[] args) throws InterruptedException {
		PeerCoordinatorFactory fac = null;
		
		if(args.length == 0)
			usage();
		else if(args.length == 1)
			fac = new BittorrentPeerCoordinatorFactory(0);
		else if(args[1].equals("--with")) {
			try {
				String algo = args[2];
				int bitrate = Integer.parseInt(args[3]);
				
				if(algo.equals("bass"))
					fac = new BassPeerCoordinatorFactory(bitrate, args[4], true);
				else if(algo.equals("vodbt"))
					fac = new VoDBTPeerCoordinatorFactory(bitrate, args[4], true);
				else if(algo.equals("livebt"))
					fac = new LiveBTPeerCoordinatorFactory(bitrate);
				else
					usage();
			} 
			catch(NumberFormatException e) {
				//e.printStackTrace();
				usage("Bitrate must be a integer!");
			}
			catch(IndexOutOfBoundsException e) {
				//e.printStackTrace();
				usage("Too few arguments given!");
			}
		}
		else
			usage();
		
		Thread prog = new Thread(new Driver(args[0], fac));
		prog.start();
		
		prog.join();
	}
	
	public static void usage() {
		usage(null);
	}
	
	public static void usage(String msg) {
		if(msg != null) {
			System.err.println("Error: " + msg);
			System.out.println();
		}
		
		System.out.println("chickensoup (cs)");
		System.out.println(
			"Usage: java -jar cs.jar <torrent> [--with <algo> <bitrate> [server]]");
		System.out.println("  <torrrent>\t Path to the torrent file");
		System.out.println("  --with    \t Which algorithm to run");
		System.out.println("            \t Default is ordinary BitTorrent");
		System.out.println("    algo    \t Can be one of the following values:");
		System.out.println("            \t bass, vodbt or livebt");
		System.out.println("    bitrate \t The bitrate of the file in bits/second");
		System.out.println("    server  \t The full url to a HTTP server with the file");
		System.out.println("            \t NOTE: BASS and VodBT require a server to run");
		System.out.println();
		System.out.println("Examples: $java -jar cs.jar legal_movie.torrent --with bass 1923001 http://cs.au.dk/~thatguy/files/movie.avi");
		System.out.println("          $java -jar cs.har legal_movie.torrent");
		System.exit(0);
	}

}
