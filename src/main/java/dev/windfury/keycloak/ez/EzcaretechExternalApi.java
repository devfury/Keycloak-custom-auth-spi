package dev.windfury.keycloak.ez;

import dev.windfury.keycloak.bizbox.dto.UserResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EzcaretechExternalApi {
    public static final String API_URL = "https://gwa.ezcaretech.com";
    private final Logger log = LoggerFactory.getLogger(EzcaretechExternalApi.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Method used for user authentication. A call is made to an external API for authentication.
     * @param username username of the user
     * @param password password of the user
     * @return Token of the user
     * @throws IOException
     */
    public String getTokenAuthenticateToExternalApi(String username, String password) throws IOException {
        String plainPassword = new String(Base64.getDecoder().decode(password), StandardCharsets.UTF_8);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //Define url for the request
        HttpPost httpPost = new HttpPost(API_URL + "/gw/uat/uia/actionLogin.do");

        Map<String, String> params = generateLoginParams(username, plainPassword);
        StringBuilder requestBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (requestBuilder.length() > 0) {
                requestBuilder.append('&');
            }
            requestBuilder
                .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        String reqBody = requestBuilder.toString();

        //Define content type and attach json object to the request
        log.info("Login request body: {}", reqBody);
        StringEntity entity = new StringEntity(reqBody, ContentType.APPLICATION_FORM_URLENCODED);
        httpPost.setEntity(entity);

        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        httpPost.setHeader(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
        httpPost.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        httpPost.setHeader("Origin", API_URL);
        httpPost.setHeader("Referer", API_URL);

        //Execute request
        CloseableHttpResponse response = httpClient.execute(httpPost);

        //Read response
        String resBody = EntityUtils.toString(response.getEntity());

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK || !resBody.contains("j_spring_security_check")) {
            log.error(String.format("Failed to POST login: %s %s",  response.getStatusLine().getStatusCode(), resBody));
            throw new IOException("로그인 실패");
        }

        final String SET_COOKIE_NAME = "Set-Cookie";
        final String JSESSIONID_NAME = "JSESSIONID=";

        String setCookieHeader = response.getFirstHeader(SET_COOKIE_NAME) != null ? response.getFirstHeader(SET_COOKIE_NAME).getValue() : "";
        String sessionId = Arrays.stream(setCookieHeader.split(";"))
            .filter(term -> term.trim().startsWith(JSESSIONID_NAME))
            .map(term -> term.trim().substring(JSESSIONID_NAME.length()))
            .findFirst()
            .orElse("");

        log.info("JSESSIONID: {}", sessionId);

        response.close();


        String formAction = extractFormAction(resBody);
        String formActionUrl = buildFormActionUrl(formAction);
        String formUsername = extractInputValue(resBody, "j_username");
        String formPassword = extractInputValue(resBody, "j_password");

        Map<String, String> springLoginParams = new LinkedHashMap<>();
        springLoginParams.put("j_username", formUsername);
        springLoginParams.put("j_password", formPassword);

        StringBuilder formRequestBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : springLoginParams.entrySet()) {
            if (formRequestBuilder.length() > 0) {
                formRequestBuilder.append('&');
            }
            formRequestBuilder
                .append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        String formReqBody = formRequestBuilder.toString();

        log.info("Spring Security login action: {}", formActionUrl);

        HttpPost formPost = new HttpPost(formActionUrl);
        formPost.setEntity(new StringEntity(formReqBody, ContentType.APPLICATION_FORM_URLENCODED));
        formPost.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        formPost.setHeader("Cookie", "JSESSIONID=" + sessionId);

        CloseableHttpResponse springResponse = httpClient.execute(formPost);
        int springStatus = springResponse.getStatusLine().getStatusCode();
        if (springStatus >= 400) {
            String errorBody = EntityUtils.toString(springResponse.getEntity(), StandardCharsets.UTF_8);
            log.error("Failed to POST Spring Security login: {} {}", springStatus, errorBody);
            throw new IOException("Spring Security 로그인 실패");
        }
        EntityUtils.consume(springResponse.getEntity());
        springResponse.close();

        httpClient.close();
        return sessionId;
    }

    /**
     * Method used to get user profile. A call is made to an external API for user profile.
     * @param token token of the user
     * @return UserResponseDTO Object containing user profile
     * @throws IOException
     */
    public UserResponseDTO getProfileToExternalApi(String token) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //Define url for the request
        HttpGet httpGet = new HttpGet(String.format("%s/gw/cmm/systemx/userProfileInfo.do", API_URL));
        //Add token to the request
        httpGet.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        httpGet.setHeader("Cookie", "JSESSIONID=" + token);

        //Execute request
        CloseableHttpResponse httpResponse = httpClient.execute(httpGet, HttpClientContext.create());

        //Read response
        HttpEntity entity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(entity, StandardCharsets.UTF_8);

        //check the response status
        if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            //log error
            log.error(String.format("Failed to POST login: %s %s",  httpResponse.getStatusLine().getStatusCode(), responseString));
            httpResponse.close();
            httpClient.close();
            return null;
        } else {
            //convert json into my response object
            UserResponseDTO userResponse = objectMapper.readValue(responseString, UserResponseDTO.class);

            httpResponse.close();
            httpClient.close();
            return userResponse;
        }
    }

    private static final String[] ENCRYPTION_CONSTS = {
        "duzon@1234123412",
        "fwxdu#*1g@138@l3",
        "parse",
        "Utf8",
        "enc",
        "CBC",
        "mode",
        "Pkcs7",
        "pad",
        "encrypt",
        "AES",
        "!"
    };

    private String securityEncrypt(String data, int type) {
        String key = type == 1 ? ENCRYPTION_CONSTS[1] : ENCRYPTION_CONSTS[0];
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String encrypted = Base64.getEncoder().encodeToString(encryptedBytes);
            String result = ENCRYPTION_CONSTS[11] + encrypted;
            
            String encoded = URLEncoder.encode(result, StandardCharsets.UTF_8);
            //! javascript의 encodeURIComponent와 동일하게 변환
            return encoded
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt", e);
        }
    }

    public Map<String, String> generateLoginParams(String id, String pw) {
        String userId0 = securityEncrypt(id, 0);
        String userId1 = "";
        String userId2 = "";
        if (userId0.length() > 50) {
            userId1 = userId0.substring(50);
            userId0 = userId0.substring(0, 50);
            if (userId1.length() > 50) {
                userId2 = userId1.substring(50);
                userId1 = userId1.substring(0, 50);
            }
        }
        String encryptedPw = securityEncrypt(pw, 0);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("isScLogin", "");
        params.put("scUserId", "");
        params.put("scUserPwd", "");
        params.put("id", userId0);
        params.put("id_sub1", userId1);
        params.put("id_sub2", userId2);
        params.put("password", encryptedPw);
        return params;
    }

    private String extractFormAction(String html) {
        if (html == null) {
            return "";
        }
        Matcher matcher = Pattern.compile("<form[^>]*action\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = Pattern.compile("<form[^>]*action\\s*=\\s*'([^']*)'", Pattern.CASE_INSENSITIVE).matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractInputValue(String html, String inputName) {
        if (html == null || inputName == null) {
            return "";
        }
        String namePattern = Pattern.quote(inputName);
        Pattern inputTagPattern = Pattern.compile(
            "<input[^>]*name\\s*=\\s*(?:\"" + namePattern + "\"|'" + namePattern + "')[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = inputTagPattern.matcher(html);
        if (!matcher.find()) {
            return "";
        }
        String inputTag = matcher.group();
        Matcher valueMatcher = Pattern.compile("value\\s*=\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(inputTag);
        if (valueMatcher.find()) {
            return valueMatcher.group(1);
        }
        valueMatcher = Pattern.compile("value\\s*=\\s*'([^']*)'", Pattern.CASE_INSENSITIVE).matcher(inputTag);
        return valueMatcher.find() ? valueMatcher.group(1) : "";
    }

    private String buildFormActionUrl(String action) {
        if (action == null || action.isBlank()) {
            return "";
        }
        if (action.startsWith("http://") || action.startsWith("https://")) {
            return action;
        }
        if (action.startsWith("/")) {
            return API_URL + action;
        }
        return API_URL + "/" + action;
    }

}
