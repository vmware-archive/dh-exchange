package io.pivotal.cf.dh;

import org.springframework.beans.factory.annotation.Autowired;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class Party {

    @Autowired
    private KeyPairGenerator keyPairGenerator;

    @Autowired
    private KeyFactory keyFactory;

    @Autowired
    private Util util;

    private byte[] secret;

    private byte[] derivedKey;

    private PublicKey pubKey;

    private PrivateKey privKey;

    void sharedSecret(byte[] counterPartyPublicKey) throws GeneralSecurityException {
        X509EncodedKeySpec pkSpec = new X509EncodedKeySpec(counterPartyPublicKey);
        PublicKey otherPublicKey = keyFactory.generatePublic(pkSpec);

        // Perform key agreement
        KeyAgreement ka = KeyAgreement.getInstance(Config.ELLIPTIC_KEY_AGREEMENT_TYPE);
        ka.init(getPrivKey());
        ka.doPhase(otherPublicKey, true);

        // Read shared secret
        secret = ka.generateSecret();

        MessageDigest hash = MessageDigest.getInstance(Config.DIGEST_TYPE);
        hash.update(secret);

        // Simple deterministic ordering
        List<ByteBuffer> keys = Arrays.asList(ByteBuffer.wrap(getPubKey().getEncoded()), ByteBuffer.wrap(counterPartyPublicKey));
        Collections.sort(keys);
        hash.update(keys.get(0));
        hash.update(keys.get(1));

        derivedKey = hash.digest();
        privKey = null;
        pubKey = null;
    }

    private void init() {
        KeyPair kp = keyPairGenerator.generateKeyPair();
        pubKey = kp.getPublic();
        privKey = kp.getPrivate();
    }

    byte[] getPublicKey() {
        return getPubKey().getEncoded();
    }

    private PrivateKey getPrivKey() {
        if (privKey == null && secret == null) {
            init();
        }
        return privKey;
    }

    private PublicKey getPubKey() {
        if (pubKey == null && secret == null) {
            init();
        }
        return pubKey;
    }

    String encrypt(String message) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(Config.CIPHER_TYPE);
        c.init(Cipher.ENCRYPT_MODE, getCipherKey());
        return util.fromBytes(c.doFinal(message.getBytes()));
    }

    String decrypt(String encryptedData) throws GeneralSecurityException {
        Cipher c = Cipher.getInstance(Config.CIPHER_TYPE);
        c.init(Cipher.DECRYPT_MODE, getCipherKey());
        return new String(c.doFinal(util.toBytes(encryptedData)));
    }

    private Key getCipherKey() {
        return new SecretKeySpec(derivedKey, Config.CIPHER_TYPE);
    }
}