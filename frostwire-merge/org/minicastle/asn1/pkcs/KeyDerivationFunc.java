package org.minicastle.asn1.pkcs;

import org.minicastle.asn1.ASN1Sequence;
import org.minicastle.asn1.x509.AlgorithmIdentifier;

public class KeyDerivationFunc
    extends AlgorithmIdentifier
{
    KeyDerivationFunc(
        ASN1Sequence  seq)
    {
        super(seq);
    }
}
