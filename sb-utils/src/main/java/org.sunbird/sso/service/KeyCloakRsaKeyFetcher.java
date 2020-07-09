package org.sunbird.sso.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sunbird.util.JsonKey;
import org.sunbird.util.helper.PropertiesCache;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.Map;

/** Class to fetch SSO public key from Keycloak server using 'certs' HTTP API call. */
public class KeyCloakRsaKeyFetcher {
  private static final String MODULUS = "modulusBase64";
  private static final String EXPONENT = "exponentBase64";
  
  private Logger logger = LoggerFactory.getLogger(KeyCloakRsaKeyFetcher.class);

  /**
   * This method will accept keycloak base URL and realm name. Based on provided values it will
   * fetch public key from keycloak.
   *
   * @param url A string value having keycloak base URL
   * @param realm Keycloak realm name
   * @return Public key used to verify user access token.
   */
  public PublicKey getPublicKeyFromKeyCloak(String url, String realm) {
    try {
      Map<String, String> valueMap = null;
      Decoder urlDecoder = Base64.getUrlDecoder();
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      String publicKeyString = requestKeyFromKeycloak(url, realm);
      if (publicKeyString != null) {
        valueMap = getValuesFromJson(publicKeyString);
        if (valueMap != null) {
          BigInteger modulus = new BigInteger(1, urlDecoder.decode(valueMap.get(MODULUS)));
          BigInteger publicExponent = new BigInteger(1, urlDecoder.decode(valueMap.get(EXPONENT)));
          PublicKey key = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
          saveToCache(key);
          return key;
        }
      }
    } catch (Exception e) {
      logger.error(
          "KeyCloakRsaKeyFetcher:getPublicKeyFromKeyCloak: Exception occurred with message = "
              + e.getMessage());
    }
    return null;
  }

  /**
   * This method will save the public key string value to cache
   *
   * @param key Public key to save in cache
   */
  private void saveToCache(PublicKey key) {
    byte[] encodedPublicKey = key.getEncoded();
    String publicKey = Base64.getEncoder().encodeToString(encodedPublicKey);
    PropertiesCache cache = PropertiesCache.getInstance();
    cache.saveConfigProperty(JsonKey.SSO_PUBLIC_KEY, publicKey);
  }

  /**
   * This method will connect to keycloak server using API call for getting public key.
   *
   * @param url A string value having keycloak base URL
   * @param realm Keycloak realm name
   * @return Public key JSON response string
   */
  private String requestKeyFromKeycloak(String url, String realm) {
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(url + "realms/" + realm + "/protocol/openid-connect/certs");

    try {
      HttpResponse response = client.execute(request);
      HttpEntity entity = response.getEntity();

      if (entity != null) {
        return EntityUtils.toString(entity);
      } else {
        logger.error(
            "KeyCloakRsaKeyFetcher:requestKeyFromKeycloak: Not able to fetch SSO public key from keycloak server");
      }
    } catch (IOException e) {
      logger.error(
          "KeyCloakRsaKeyFetcher:requestKeyFromKeycloak: Exception occurred with message = "
              + e.getMessage());
    }
    return null;
  }

  /**
   * This method will return a map containing values extracted from public key JSON string.
   *
   * @param response Public key JSON response string
   */
  private Map<String, String> getValuesFromJson(String response) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> values = new HashMap<>();
    try {
      JsonNode res = mapper.readTree(response);
      JsonNode keys = res.get("keys");
      if (keys != null) {

        JsonNode value = keys.get(0);
        values.put(MODULUS, value.get("n").asText());
        values.put(EXPONENT, value.get("e").asText());
      }
    } catch (Exception e) {
      logger.error(
          "KeyCloakRsaKeyFetcher:getValuesFromJson: Exception occurred with message = "
              + e.getMessage());
      return null;
    }

    return values;
  }
}
