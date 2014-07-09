package p2pp;

import org.klomp.snark.Storage;
import org.klomp.snark.StorageListener;

public class NullStorageListener implements StorageListener {

	@Override
	public void storageCreateFile(Storage storage, String name, long length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void storageAllocated(Storage storage, long length) {
		// TODO Auto-generated method stub

	}

	@Override
	public void storageChecked(Storage storage, int num, boolean checked) {
		// TODO Auto-generated method stub

	}

	@Override
	public void storageAllChecked(Storage storage) {
		// TODO Auto-generated method stub

	}

}
