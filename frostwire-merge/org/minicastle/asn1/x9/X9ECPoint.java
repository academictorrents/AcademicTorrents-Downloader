package org.minicastle.asn1.x9;

import org.minicastle.asn1.ASN1OctetString;
import org.minicastle.asn1.DEREncodable;
import org.minicastle.asn1.DERObject;
import org.minicastle.asn1.DEROctetString;
import org.minicastle.math.ec.ECCurve;
import org.minicastle.math.ec.ECPoint;

/**
 * class for describing an ECPoint as a DER object.
 */
public class X9ECPoint
    implements DEREncodable
{
    ECPoint p;

    public X9ECPoint(
        ECPoint p)
    {
        this.p = p;
    }

    public X9ECPoint(
        ECCurve          c,
        ASN1OctetString  s)
    {
        this.p = c.decodePoint(s.getOctets());
    }

    public ECPoint getPoint()
    {
        return p;
    }

    /**
     * Produce an object suitable for an ASN1OutputStream.
     * <pre>
     *  ECPoint ::= OCTET STRING
     * </pre>
     * <p>
     * Octet string produced using ECPoint.getEncoded().
     */
    public DERObject getDERObject()
    {
        return new DEROctetString(p.getEncoded());
    }
}
