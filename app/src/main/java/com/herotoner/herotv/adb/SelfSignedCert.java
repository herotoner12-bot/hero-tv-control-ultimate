package com.herotoner.herotv.adb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * يولّد شهادة X.509 ذاتية التوقيع (RSA + SHA-256) بدون أي مكتبة خارجية.
 * Wireless Debugging يتحقق من المفتاح العام فقط، لذلك تكفي شهادة بسيطة بنفس مفتاح ADB.
 */
public final class SelfSignedCert {

    private static final byte[] OID_SHA256_WITH_RSA = {
            0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x0B};
    private static final byte[] OID_COMMON_NAME = {0x55, 0x04, 0x03};

    private SelfSignedCert() {
    }

    public static X509Certificate generate(KeyPair keyPair, String commonName, long validityMillis)
            throws GeneralSecurityException {
        long now = System.currentTimeMillis();
        Date notBefore = new Date(now - 24L * 3600L * 1000L);
        Date notAfter = new Date(now + validityMillis);

        byte[] algorithm = sequence(oid(OID_SHA256_WITH_RSA), new byte[]{0x05, 0x00});
        byte[] name = sequence(set(sequence(oid(OID_COMMON_NAME), tlv(0x0C, commonName.getBytes(StandardCharsets.UTF_8)))));

        BigInteger serial = new BigInteger(63, new java.security.SecureRandom()).add(BigInteger.ONE);

        byte[] tbs = sequence(
                tlv(0xA0, tlv(0x02, new byte[]{0x02})),            // version v3
                tlv(0x02, serial.toByteArray()),
                algorithm,
                name,
                sequence(utcTime(notBefore), utcTime(notAfter)),
                name,
                keyPair.getPublic().getEncoded()                      // SubjectPublicKeyInfo جاهزة بصيغة DER
        );

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(tbs);
        byte[] sig = signature.sign();

        byte[] bitString = new byte[sig.length + 1];
        System.arraycopy(sig, 0, bitString, 1, sig.length);

        byte[] cert = sequence(tbs, algorithm, tlv(0x03, bitString));
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(cert));
    }

    private static byte[] utcTime(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return tlv(0x17, format.format(date).getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] oid(byte[] value) {
        return tlv(0x06, value);
    }

    private static byte[] set(byte[]... parts) {
        return tlv(0x31, concat(parts));
    }

    private static byte[] sequence(byte[]... parts) {
        return tlv(0x30, concat(parts));
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.write(part, 0, part.length);
        }
        return out.toByteArray();
    }

    private static byte[] tlv(int tag, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        int length = value.length;
        if (length < 0x80) {
            out.write(length);
        } else if (length <= 0xFF) {
            out.write(0x81);
            out.write(length);
        } else {
            out.write(0x82);
            out.write(length >> 8);
            out.write(length & 0xFF);
        }
        out.write(value, 0, value.length);
        return out.toByteArray();
    }
}
