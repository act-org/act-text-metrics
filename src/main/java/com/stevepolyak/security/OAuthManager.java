package com.stevepolyak.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.oauth.OAuth;
import net.oauth.OAuthProblemException;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.rs.security.oauth.data.AccessToken;

import org.apache.cxf.rs.security.oauth.data.Client;
import org.apache.cxf.rs.security.oauth.data.OAuthPermission;
import org.apache.cxf.rs.security.oauth.data.RequestToken;
import org.apache.cxf.rs.security.oauth.data.RequestTokenRegistration;
import org.apache.cxf.rs.security.oauth.data.Token;
import org.apache.cxf.rs.security.oauth.provider.MD5SequenceGenerator;
import org.apache.cxf.rs.security.oauth.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth.provider.OAuthServiceException;

public class OAuthManager implements OAuthDataProvider {

    public static final String CALLBACK = "http://www.example.com/callback";
    public static final String APPLICATION_NAME = "Test Oauth 1.0 application";
    public static final String CLIENT_ID = "12345678";
    public static final String CLIENT_SECRET = "secret";

    private static final ConcurrentHashMap<String, OAuthPermission> AVAILABLE_PERMISSIONS = 
        new ConcurrentHashMap<String, OAuthPermission>();

    static {
        AVAILABLE_PERMISSIONS
                .put("read_info", new OAuthPermission("read_info", "Read your personal information",
                                                      Collections.singletonList("ROLE_USER")));
        AVAILABLE_PERMISSIONS.put("modify_info",
                new OAuthPermission("modify_info", "Modify your personal information", 
                                    Collections.singletonList("ROLE_ADMIN")));
    }

    protected ConcurrentHashMap<String, Client> clientAuthInfo = new ConcurrentHashMap<String, Client>();

    protected MetadataMap<String, String> userRegisteredClients = new MetadataMap<String, String>();

    protected MetadataMap<String, String> userAuthorizedClients = new MetadataMap<String, String>();

    protected ConcurrentHashMap<String, Token> oauthTokens = new ConcurrentHashMap<String, Token>();

    protected MD5SequenceGenerator tokenGenerator = new MD5SequenceGenerator();

    public OAuthManager() {
        Client client = new Client(CLIENT_ID, CLIENT_SECRET, APPLICATION_NAME, CALLBACK);
        clientAuthInfo.put(CLIENT_ID, client);
    }
    
    @Override
    public List<OAuthPermission> getPermissionsInfo(List<String> requestPermissions) {
        List<OAuthPermission> permissions = new ArrayList<OAuthPermission>();
        for (String requestScope : requestPermissions) {
            OAuthPermission oAuthPermission = AVAILABLE_PERMISSIONS.get(requestScope);
            permissions.add(oAuthPermission);
        }
    
        return permissions;
    }

    
    public Client getClient(String consumerKey) {
        return clientAuthInfo.get(consumerKey);
    }

    public RequestToken createRequestToken(RequestTokenRegistration reg) throws OAuthServiceException {
        String token = generateToken();
        String tokenSecret = generateToken();

        RequestToken reqToken = new RequestToken(reg.getClient(), token, tokenSecret, 
                                                 reg.getLifetime(), reg.getIssuedAt());
        reqToken.setScopes(reg.getScopes());
        reqToken.setCallback(reg.getCallback());
        oauthTokens.put(token, reqToken);
        return reqToken;
    }

    public RequestToken getRequestToken(String tokenString) throws OAuthServiceException {

        Token token = oauthTokens.get(tokenString);
        if (token == null || (!RequestToken.class.isAssignableFrom(token.getClass()))) {
            throw new OAuthServiceException(new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED));
        }
        return (RequestToken) token;
    }

	

	@Override
	public String setRequestTokenVerifier(RequestToken arg0)
			throws OAuthServiceException {
		arg0.setVerifier(generateToken());
        return arg0.getVerifier();
	}
	
    public AccessToken createAccessToken(RequestToken reg) throws
        OAuthServiceException {

        Client client = reg.getClient();

        String accessTokenString = generateToken();
        String tokenSecretString = generateToken();

        AccessToken accessToken = new AccessToken(client, accessTokenString,
            tokenSecretString, 3600, System.currentTimeMillis() / 1000);

        accessToken.setScopes(reg.getScopes());
 
        synchronized (oauthTokens) {
            oauthTokens.remove(reg.getTokenKey());
            oauthTokens.put(accessTokenString, accessToken);
            synchronized (userAuthorizedClients) {
                userAuthorizedClients.add(client.getConsumerKey(), client.getConsumerKey());
            }
        }

        return accessToken;
    }

    public AccessToken getAccessToken(String accessToken) throws OAuthServiceException
    {
        return (AccessToken) oauthTokens.get(accessToken);
    }

    public void removeAllTokens(String consumerKey) {
        //TODO: implement
    }

    public void removeToken(Token t) {
        
        for (Token token : oauthTokens.values()) {
            Client authNInfo = token.getClient();
            if (t.getClient().getConsumerKey().equals(authNInfo.getConsumerKey())) {
                oauthTokens.remove(token.getTokenKey());
                break;
            }
        }
        
    }

    protected String generateToken() throws OAuthServiceException {
        String token;
        try {
            token = tokenGenerator.generate(UUID.randomUUID().toString().getBytes("UTF-8"));
        } catch (Exception e) {
            throw new OAuthServiceException("Unable to create token ", e.getCause());
        }
        return token;
    }

    public void setClientAuthInfo(Map<String, Client> clientAuthInfo) {
        this.clientAuthInfo.putAll(clientAuthInfo);
    }

}   
		
