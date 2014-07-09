package org.minicastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.minicastle.crypto.AsymmetricCipherKeyPair;
import org.minicastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.minicastle.crypto.KeyGenerationParameters;
import org.minicastle.crypto.params.DSAKeyGenerationParameters;
import org.minicastle.crypto.params.DSAParameters;
import org.minicastle.crypto.params.DSAPrivateKeyParameters;
import org.minicastle.crypto.params.DSAPublicKeyParameters;

/**
 * a DSA key pair generator.
 *
 * This generates DSA keys in line with the method described 
 * in FIPS 186-2.
 */
public class DSAKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private static BigInteger ZERO = BigInteger.valueOf(0);

    private DSAKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (DSAKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        BigInteger      p, q, g, x, y;
        DSAParameters   dsaParams = param.getParameters();
        SecureRandom    random = param.getRandom();

        q = dsaParams.getQ();
        p = dsaParams.getP();
        g = dsaParams.getG();

        do
        {
            x = new BigInteger(160, random);
        }
        while (x.equals(ZERO)  || x.compareTo(q) >= 0);

        //
        // calculate the public key.
        //
        y = g.modPow(x, p);

        return new AsymmetricCipherKeyPair(
                new DSAPublicKeyParameters(y, dsaParams),
                new DSAPrivateKeyParameters(x, dsaParams));
    }
}
