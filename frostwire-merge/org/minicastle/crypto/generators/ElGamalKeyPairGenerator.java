package org.minicastle.crypto.generators;

import java.math.BigInteger;

import org.minicastle.crypto.AsymmetricCipherKeyPair;
import org.minicastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.minicastle.crypto.KeyGenerationParameters;
import org.minicastle.crypto.params.ElGamalKeyGenerationParameters;
import org.minicastle.crypto.params.ElGamalParameters;
import org.minicastle.crypto.params.ElGamalPrivateKeyParameters;
import org.minicastle.crypto.params.ElGamalPublicKeyParameters;

/**
 * a ElGamal key pair generator.
 * <p>
 * This generates keys consistent for use with ElGamal as described in
 * page 164 of "Handbook of Applied Cryptography".
 */
public class ElGamalKeyPairGenerator
    implements AsymmetricCipherKeyPairGenerator
{
    private ElGamalKeyGenerationParameters param;

    public void init(
        KeyGenerationParameters param)
    {
        this.param = (ElGamalKeyGenerationParameters)param;
    }

    public AsymmetricCipherKeyPair generateKeyPair()
    {
        BigInteger           p, g, x, y;
        int                  qLength = param.getStrength() - 1;
        ElGamalParameters    elParams = param.getParameters();

        p = elParams.getP();
        g = elParams.getG();
    
        //
        // calculate the private key
        //
		x = new BigInteger(qLength, param.getRandom());

        //
        // calculate the public key.
        //
        y = g.modPow(x, p);

        return new AsymmetricCipherKeyPair(
                new ElGamalPublicKeyParameters(y, elParams),
                new ElGamalPrivateKeyParameters(x, elParams));
    }
}
