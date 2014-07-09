package org.minicastle.crypto.params;

import org.minicastle.crypto.CipherParameters;

public class AsymmetricKeyParameter
	implements CipherParameters
{
    boolean privateKey;

    public AsymmetricKeyParameter(
        boolean privateKey)
    {
        this.privateKey = privateKey;
    }

    public boolean isPrivate()
    {
        return privateKey;
    }
}
