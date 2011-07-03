package org.bouncycastle.jce.provider;

import org.bouncycastle.jce.interfaces.PKCS12BagAttributeCarrier;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1InputStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

@SuppressWarnings("unchecked")
class PKCS12BagAttributeCarrierImpl
    implements PKCS12BagAttributeCarrier
{
	private Hashtable<DERObjectIdentifier, DEREncodable> pkcs12Attributes;
    private Vector<DERObjectIdentifier> pkcs12Ordering;

    PKCS12BagAttributeCarrierImpl(Hashtable<DERObjectIdentifier, DEREncodable> attributes, Vector<DERObjectIdentifier> ordering)
    {
        this.pkcs12Attributes = attributes;
        this.pkcs12Ordering = ordering;
    }

    public PKCS12BagAttributeCarrierImpl()
    {
        this(new Hashtable<DERObjectIdentifier, DEREncodable>(), new Vector<DERObjectIdentifier>());
    }

    public void setBagAttribute(
        DERObjectIdentifier oid,
        DEREncodable        attribute)
    {
        if (pkcs12Attributes.containsKey(oid))
        {                           // preserve original ordering
            pkcs12Attributes.put(oid, attribute);
        }
        else
        {
            pkcs12Attributes.put(oid, attribute);
            pkcs12Ordering.addElement(oid);
        }
    }

    public DEREncodable getBagAttribute(
        DERObjectIdentifier oid)
    {
        return (DEREncodable)pkcs12Attributes.get(oid);
    }

    public Enumeration<DERObjectIdentifier> getBagAttributeKeys()
    {
        return pkcs12Ordering.elements();
    }

    int size()
    {
        return pkcs12Ordering.size();
    }

    Hashtable<DERObjectIdentifier, DEREncodable> getAttributes()
    {
        return pkcs12Attributes;
    }

    Vector<DERObjectIdentifier> getOrdering()
    {
        return pkcs12Ordering;
    }

    public void writeObject(ObjectOutputStream out)
        throws IOException
    {
        if (pkcs12Ordering.size() == 0)
        {
            out.writeObject(new Hashtable<Object, Object>());
            out.writeObject(new Vector<Object>());
        }
        else
        {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ASN1OutputStream aOut = new ASN1OutputStream(bOut);

            Enumeration<DERObjectIdentifier>             e = this.getBagAttributeKeys();

            while (e.hasMoreElements())
            {
                DERObjectIdentifier    oid = (DERObjectIdentifier)e.nextElement();

                aOut.writeObject(oid);
                aOut.writeObject(pkcs12Attributes.get(oid));
            }

            out.writeObject(bOut.toByteArray());
        }
    }

    public void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        Object obj = in.readObject();

        if (obj instanceof Hashtable)
        {
            this.pkcs12Attributes = (Hashtable<DERObjectIdentifier, DEREncodable>)obj;
            this.pkcs12Ordering = (Vector<DERObjectIdentifier>)in.readObject();
        }
        else
        {
            ASN1InputStream aIn = new ASN1InputStream((byte[])obj);

            DERObjectIdentifier    oid;

            while ((oid = (DERObjectIdentifier)aIn.readObject()) != null)
            {
                this.setBagAttribute(oid, aIn.readObject());
            }
        }
    }
}
