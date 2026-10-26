package org.devlive.metaforge.common.utils;

import org.devlive.metaforge.common.response.SignResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SignUtils
{
    private SignUtils()
    {}

    /**
     * Generates a signature using the provided secret.
     *
     * @param secret the secret key used for signing
     * @return the SignResponse object containing the generated signature and timestamp
     * @throws Exception if an error occurs during the signing process
     */
    public static SignResponse sign(String secret)
            throws Exception
    {
        Long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        Base64.Encoder encoder = Base64.getEncoder();
        String sign = URLEncoder.encode(encoder.encodeToString(signData), "UTF-8");
        return SignResponse.builder()
                .timestamp(timestamp)
                .sign(sign)
                .build();
    }
}
