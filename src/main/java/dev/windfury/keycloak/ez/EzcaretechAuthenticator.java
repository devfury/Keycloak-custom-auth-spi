package dev.windfury.keycloak.ez;

import dev.windfury.keycloak.bizbox.dto.User;
import dev.windfury.keycloak.bizbox.dto.UserMemberDTO;
import dev.windfury.keycloak.bizbox.dto.UserResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class EzcaretechAuthenticator implements Authenticator {

    private final Logger log = LoggerFactory.getLogger(EzcaretechAuthenticator.class);

    private ObjectMapper objectMapper = new ObjectMapper();

    private final KeycloakSession session;

    public EzcaretechAuthenticator(KeycloakSession session) {
        this.session = session;
    }

    /**
     * Method is used for user authentication. It makes a call to an external API that returns a jwt token if the user is authenticated
     * If the user is authenticated an authenticated user is set.
     * Whereas if the user is not authenticated, an error is set.
     * @param context
     */
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        log.info("CUSTOMER PROVIDER authenticate");
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String username = formData.getFirst("username");
        String password = formData.getFirst("password");
        log.debug("AUTHENTICATE custom provider: " + username);

        User user = null;
        try {
            user = callExternalApi(username, password);
        } catch (IOException e) {
            log.error("Errore durante la chiamata all'API esterna", e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }

        if (user != null) {
            try {
                UserModel userModel = context.getSession().users().getUserByUsername(context.getRealm(), user.getUsername());
                if (userModel == null) {
                    // create user if not exists
                    userModel = context.getSession().users().addUser(context.getRealm(), user.getUsername());
                }
                userModel.setFirstName(user.getFirstName());
                userModel.setLastName(user.getLastName());
                userModel.setEmail(user.getEmail());
                userModel.setSingleAttribute("mobile-tel-number", user.getMobileTelephoneNumber());
                userModel.setSingleAttribute("inner-tel-number", user.getInnerTelephoneNumber());
                userModel.setSingleAttribute("fax-tel-number", user.getFaxTelephoneNumber());
                userModel.setEnabled(true);
                userModel.setEmailVerified(true);
                for (String role : user.getRoles()) {
                    userModel.grantRole(context.getRealm().getRole(role));
                }
                //userModel.grantRole(context.getRealm().getRole("user"));
                context.setUser(userModel);
            }
            catch (Exception e) {
                log.error("Authentication error", e);
                context.failure(AuthenticationFlowError.INTERNAL_ERROR);
            }
            context.success();
        } else {
            // User not authenticated set unauthorized error
            context.failure(AuthenticationFlowError.INVALID_USER, Response.status(Response.Status.UNAUTHORIZED)
                    .entity("You must be authenticated to access this resource.")
                    .build());
            return;
        }
        // It is also possible to use the challenge() method to request the user to provide further information to complete the authentication.
    }

    /**
     * Call to external API for authentication
     * @param username Username of the user
     * @param password Password of the user
     * @return User authenticated
     * @throws IOException
     */
    private User callExternalApi(String username, String password) throws IOException {
        EzcaretechExternalApi api = new EzcaretechExternalApi();
        String token = api.getTokenAuthenticateToExternalApi(username, password);
        if(token == null) {
            return null;
        }
        UserResponseDTO userResponseDTO = api.getProfileToExternalApi(token);
        if (userResponseDTO == null) {
            log.warn("User profile response is empty for {}", username);
            return null;
        }
        List<UserMemberDTO> members = userResponseDTO.getList();
        if (members == null) {
            log.warn("User profile list is empty for {}", username);
            return null;
        }
        UserMemberDTO userMember = members
            .stream()
            .filter(member -> username.equalsIgnoreCase(member.getLoginId()))
            .findFirst()
            .orElse(null);
        if (userMember == null) {
            log.warn("Unable to find member with loginId {} in user profile list", username);
            return null;
        }

        String[] nameParts = splitName(userMember.getName());
        String firstName = nameParts[0];
        String lastName = nameParts[1];

        return new User(
            userMember.getLoginId(),
            firstName,
            lastName,
            buildEmail(userMember),
            userMember.getMobileTelephoneNumber(),
            userMember.getTelephoneNumber(),
            userMember.getFaxNumber(),
            Collections.singletonList("default-roles-ezcaretech"));
    }

    private String buildEmail(UserMemberDTO userMember) {
        String emailAddr = userMember.getEmailAddr();
        String emailDomain = userMember.getEmailDomain();
        if (emailAddr == null || emailAddr.isBlank()) {
            return emailAddr;
        }
        if (emailDomain == null || emailDomain.isBlank()) {
            return emailAddr;
        }
        return emailAddr + "@" + emailDomain;
    }

    private String[] splitName(String displayName) {
        if (displayName == null) {
            return new String[]{"", ""};
        }
        String trimmed = displayName.trim();
        if (trimmed.isEmpty()) {
            return new String[]{"", ""};
        }
        int firstCodePoint = trimmed.codePointAt(0);
        if (isHangul(firstCodePoint)) {
            String lastName = new String(Character.toChars(firstCodePoint));
            String remaining = trimmed.substring(Character.charCount(firstCodePoint)).trim();
            return new String[]{remaining, lastName};
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        String lastName = parts[parts.length - 1];
        StringBuilder givenName = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                givenName.append(' ');
            }
            givenName.append(parts[i]);
        }
        return new String[]{givenName.toString(), lastName};
    }

    private boolean isHangul(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_A
            || block == Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_B
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        log.debug("CUSTOMER PROVIDER action");
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // Set the required actions for the user after authentication
    }

    @Override
    public void close() {
        // Closes any open resources
    }
}
