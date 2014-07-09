package org.minicastle.jce.interfaces;

import org.minicastle.jce.spec.ECParameterSpec;

/**
 * generic interface for an Elliptic Curve Key.
 */
public interface ECKey
{
    /**
     * return a parameter specification representing the EC domain parameters
     * for the key.
     */
	public ECParameterSpec getParams();
}
