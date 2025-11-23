package dev.windfury.keycloak.bizbox.dto;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserMemberDTO {
    @JsonProperty("bday")
    @JsonPropertyDescription("생년월일 (예: \"1961-08-25\")")
    private String birthDay;

    @JsonProperty("bizSeq")
    @JsonPropertyDescription("회사 코드")
    private String bizSeq;

    @JsonProperty("compName")
    @JsonPropertyDescription("회사명")
    private String companyName;

    @JsonProperty("compSeq")
    @JsonPropertyDescription("회사 시퀀스")
    private String companySeq;

    @JsonProperty("deptAddr")
    @JsonPropertyDescription("부서 주소")
    private String departmentAddress;

    @JsonProperty("deptDetailAddr")
    @JsonPropertyDescription("부서 상세 주소")
    private String departmentDetailAddress;

    @JsonProperty("deptName")
    @JsonPropertyDescription("부서명")
    private String departmentName;

    @JsonProperty("deptSeq")
    @JsonPropertyDescription("부서 시퀀스")
    private String departmentSeq;

    @JsonProperty("deptZipCode")
    @JsonPropertyDescription("부서 우편번호")
    private String departmentZipCode;

    @JsonProperty("depth")
    @JsonPropertyDescription("조직도 깊이")
    private Integer depth;

    @JsonProperty("dutyCode")
    @JsonPropertyDescription("직무 코드")
    private String dutyCode;

    @JsonProperty("dutyCodeName")
    @JsonPropertyDescription("직무명")
    private String dutyCodeName;

    @JsonProperty("emailAddr")
    @JsonPropertyDescription("이메일 아이디 (예: `testuser`)")
    private String emailAddr;

    @JsonProperty("emailDomain")
    @JsonPropertyDescription("이메일 도메인 (예: `test.com`)")
    private String emailDomain;

    @JsonProperty("empSeq")
    @JsonPropertyDescription("사원 시퀀스 (예: `815279`)")
    private String employeeSeq;

    @JsonProperty("faxNum")
    @JsonPropertyDescription("팩스 번호")
    private String faxNumber;

    @JsonProperty("gbn")
    @JsonPropertyDescription("구분")
    private String gbn;

    @JsonProperty("groupSeq")
    @JsonPropertyDescription("그룹 시퀀스")
    private String groupSeq;

    @JsonProperty("loginId")
    @JsonPropertyDescription("로그인 아이디")
    private String loginId;

    @JsonProperty("mailDelYn")
    @JsonPropertyDescription("메일 삭제 여부")
    private String mailDeleteYn;

    @JsonProperty("mainWork")
    @JsonPropertyDescription("주요 업무")
    private String mainWork;

    @JsonProperty("mobileTelNum")
    @JsonPropertyDescription("휴대폰 번호")
    private String mobileTelephoneNumber;

    @JsonProperty("name")
    @JsonPropertyDescription("이름")
    private String name;

    @JsonProperty("outDomain")
    @JsonPropertyDescription("외부 도메인")
    private String outDomain;

    @JsonProperty("outMail")
    @JsonPropertyDescription("외부 메일")
    private String outMail;

    @JsonProperty("parentSeq")
    @JsonPropertyDescription("상위 부서 시퀀스")
    private String parentSeq;

    @JsonProperty("passwdStatusCode")
    @JsonPropertyDescription("비밀번호 상태 코드")
    private String passwordStatusCode;

    @JsonProperty("pathName")
    @JsonPropertyDescription("조직 경로명")
    private String pathName;

    @JsonProperty("picFileId")
    @JsonPropertyDescription("사진 파일 ID")
    private String pictureFileId;

    @JsonProperty("positionCode")
    @JsonPropertyDescription("직위 코드")
    private String positionCode;

    @JsonProperty("positionCodeName")
    @JsonPropertyDescription("직위명")
    private String positionCodeName;

    @JsonProperty("privateYn")
    @JsonPropertyDescription("비공개 여부")
    private String privateYn;

    @JsonProperty("seq")
    @JsonPropertyDescription("시퀀스")
    private String seq;

    @JsonProperty("signType")
    @JsonPropertyDescription("서명 타입")
    private String signType;

    @JsonProperty("telNum")
    @JsonPropertyDescription("전화번호")
    private String telephoneNumber;

    public String getEmail() {
        if (emailAddr == null || emailAddr.isBlank()) {
            return emailAddr;
        }
        if (emailDomain == null || emailDomain.isBlank()) {
            return emailAddr;
        }
        return emailAddr + "@" + emailDomain;
    }

    public PersonName getPersonName() {
        if (name == null) {
            return new PersonName("", "");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return new PersonName("", "");
        }
        int firstCodePoint = trimmed.codePointAt(0);
        if (isHangul(firstCodePoint)) {
            String lastName = new String(Character.toChars(firstCodePoint));
            String remaining = trimmed.substring(Character.charCount(firstCodePoint)).trim();
            return new PersonName(remaining, lastName);
        }
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 1) {
            return new PersonName(parts[0], "");
        }
        String lastName = parts[parts.length - 1];
        StringBuilder givenName = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                givenName.append(' ');
            }
            givenName.append(parts[i]);
        }
        return new PersonName(givenName.toString(), lastName);
    }
    
    private boolean isHangul(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        return block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_A
            || block == Character.UnicodeBlock.HANGUL_JAMO_EXTENDED_B
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }
}
