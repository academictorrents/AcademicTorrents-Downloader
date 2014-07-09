package org.minicastle.jce.provider;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.minicastle.crypto.params.AsymmetricKeyParameter;
import org.minicastle.crypto.params.ECDomainParameters;
import org.minicastle.crypto.params.ECPrivateKeyParameters;
import org.minicastle.crypto.params.ECPublicKeyParameters;
import org.minicastle.jce.interfaces.ECPrivateKey;
import org.minicastle.jce.interfaces.ECPublicKey;
import org.minicastle.jce.spec.ECParameterSpec;

/**
 * utility class for converting jce/jca ECDSA, ECDH, and ECDHC
 * objects into their org.bouncycastle.crypto counterparts.
 */
public class ECUtil
{
    static public AsymmetricKeyParameter generatePublicKeyParameter(
        PublicKey    key)
        throws InvalidKeyException
    {
        if (key instanceof ECPublicKey)
        {
            ECPublicKey    k = (ECPublicKey)key;
            ECParameterSpec s = k.getParams();

            return new ECPublicKeyParameters(
                            k.getQ(),
                            new ECDomainParameters(s.getCurve(), s.getG(), s.getN()));
        }

        throw new InvalidKeyException("can't identify EC public key.");
    }

    static public AsymmetricKeyParameter generatePrivateKeyParameter(
        PrivateKey    key)
        throws InvalidKeyException
    {
        if (key instanceof ECPrivateKey)
        {
            ECPrivateKey    k = (ECPrivateKey)key;
            ECParameterSpec s = k.getParams();

            return new ECPrivateKeyParameters(
                            k.getD(),
                            new ECDomainParameters(s.getCurve(), s.getG(), s.getN()));
        }
                        
        throw new InvalidKeyException("can't identify EC private key.");
    }
}
