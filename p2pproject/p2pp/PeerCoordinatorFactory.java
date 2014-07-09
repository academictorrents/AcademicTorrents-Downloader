/*
 * Used by the Snark class to create a new instance of PeerCoordinator. Implement
 * this interface and return a custom peer coordinator (see Driver.BassFactory).
 * Set a factory with Snark.setPeerCoordinatorFactory.
 */

package p2pp;

import org.klomp.snark.CoordinatorListener;
import org.klomp.snark.MetaInfo;
import org.klomp.snark.PeerCoordinator;
import org.klomp.snark.Storage;

public interface PeerCoordinatorFactory {
	// This method is called when the Snark class needs a instance
	// of a PeerCoordinator. The given arguments can be used to
	// initialize a default PeerCoordinator.
	PeerCoordinator getPeerCoordinator(byte[] id, MetaInfo meta, 
			Storage storage, CoordinatorListener listener);
}
