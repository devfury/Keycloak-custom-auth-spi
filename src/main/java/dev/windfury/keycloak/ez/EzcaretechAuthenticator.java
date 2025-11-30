package dev.windfury.keycloak.ez;

import dev.windfury.keycloak.bizbox.dto.PersonName;
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
                userModel.setSingleAttribute("userSeq", user.getUserSeq());
                userModel.setSingleAttribute("empSeq", user.getEmployeeSeq());
                userModel.setSingleAttribute("birthDay", user.getBirthDay());
                userModel.setSingleAttribute("mobileTelNumber", user.getMobileTelephoneNumber());
                userModel.setSingleAttribute("innerTelNumber", user.getInnerTelephoneNumber());
                userModel.setSingleAttribute("faxTelNumber", user.getFaxTelephoneNumber());
                userModel.setSingleAttribute("positionCode", user.getPositionCode());
                userModel.setSingleAttribute("positionName", user.getPositionName());
                userModel.setSingleAttribute("dutyCode", user.getDutyCode());
                userModel.setSingleAttribute("dutyName", user.getDutyName());
                userModel.setSingleAttribute("mainWork", user.getMainWork());
                userModel.setSingleAttribute("picFileId", user.getPictureFileId());
                userModel.setSingleAttribute("groupSeq", user.getGroupSeq());
                userModel.setSingleAttribute("bizSeq", user.getBizSeq());
                userModel.setSingleAttribute("compSeq", user.getCompanySeq());
                userModel.setSingleAttribute("compName", user.getCompanyName());
                userModel.setSingleAttribute("deptSeq", user.getDepartmentSeq());
                userModel.setSingleAttribute("deptName", user.getDepartmentName());
                userModel.setSingleAttribute("deptAddr", user.getDepartmentAddress());
                userModel.setSingleAttribute("deptDetailAddr", user.getDepartmentDetailAddress());
                userModel.setSingleAttribute("deptZipCode", user.getDepartmentZipCode());
                userModel.setSingleAttribute("deptDepth", user.getDepartmentDepth() == null ? null : user.getDepartmentDepth().toString());
                userModel.setSingleAttribute("parentSeq", user.getParentSeq());
                userModel.setSingleAttribute("pathName", user.getPathName());
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

        PersonName personName = userMember.getPersonName();

        User user = new User(
            userMember.getLoginId(),
            personName.firstName(),
            personName.lastName(),
            userMember.getEmail(),
            Collections.singletonList("default-roles-ezcaretech")
        );

        user.setUserSeq(userMember.getSeq());
        user.setEmployeeSeq(userMember.getEmployeeSeq());
        user.setBirthDay(userMember.getBirthDay());
        user.setMobileTelephoneNumber(userMember.getMobileTelephoneNumber());
        user.setInnerTelephoneNumber(userMember.getTelephoneNumber());
        user.setFaxTelephoneNumber(userMember.getFaxNumber());
        user.setPositionCode(userMember.getPositionCode());
        user.setPositionName(userMember.getPositionCodeName());
        user.setDutyCode(userMember.getDutyCode());
        user.setDutyName(userMember.getDutyCodeName());
        user.setMainWork(userMember.getMainWork());
        user.setPictureFileId(userMember.getPictureFileId());
        user.setGroupSeq(userMember.getGroupSeq());
        user.setBizSeq(userMember.getBizSeq());
        user.setCompanySeq(userMember.getCompanySeq());
        user.setCompanyName(userMember.getCompanyName());
        user.setDepartmentSeq(userMember.getDepartmentSeq());
        user.setDepartmentName(userMember.getDepartmentName());
        user.setDepartmentAddress(userMember.getDepartmentAddress());
        user.setDepartmentDetailAddress(userMember.getDepartmentDetailAddress());
        user.setDepartmentZipCode(userMember.getDepartmentZipCode());
        user.setDepartmentDepth(userMember.getDepth());
        user.setParentSeq(userMember.getParentSeq());
        user.setPathName(userMember.getPathName());
        return user;
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
