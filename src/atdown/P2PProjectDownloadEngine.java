package atdown;


public class P2PProjectDownloadEngine {
	
	private static final int PORT = 6881;

	public static void main(String[] args) throws Exception {
		System.out.println("Welcome to the Academic Torrents Download tool!");

		
        // read torrent filename from command line arg
        String filename = args[0];

   
        
        
        
        
        
        
        
        
        
        
        
        
        System.out.println("\nFinished");
        System.exit(0);
        //torrent.interrupt();
        //peerListener.interrupt();
        
		
		
		
	}
	
	
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}

}
