package dev.windfury.keycloak.bizbox.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class User {
    /** 로그인 아이디 */
    private String username;
    /** 이름(First Name) */
    private String firstName;
    /** 성(Last Name) */
    private String lastName;
    /** 이메일 주소 */
    private String email;
    /** 부여할 역할 목록 */
    private List<String> roles;
    /** 회원 시퀀스 (empSeq/seq) */
    private String userSeq;
    /** 사원 시퀀스 (empSeq) */
    private String employeeSeq;
    /** 생년월일 */
    private String birthDay;
    /** 구분 */
    private String gender;
    /** 휴대전화 번호 */
    private String mobileTelephoneNumber;
    /** 내선 번호 */
    private String innerTelephoneNumber;
    /** 팩스 번호 */
    private String faxTelephoneNumber;
    /** 직위 코드 */
    private String positionCode;
    /** 직위명 */
    private String positionName;
    /** 직무 코드 */
    private String dutyCode;
    /** 직무명 */
    private String dutyName;
    /** 주요 업무 */
    private String mainWork;
    /** 사진 파일 ID */
    private String pictureFileId;
    /** 그룹 시퀀스 */
    private String groupSeq;
    /** 회사 코드 */
    private String bizSeq;
    /** 회사 시퀀스 */
    private String companySeq;
    /** 회사명 */
    private String companyName;
    /** 부서 시퀀스 */
    private String departmentSeq;
    /** 부서명 */
    private String departmentName;
    /** 부서 주소 */
    private String departmentAddress;
    /** 부서 상세 주소 */
    private String departmentDetailAddress;
    /** 부서 우편번호 */
    private String departmentZipCode;
    /** 조직도 깊이 */
    private Integer departmentDepth;
    /** 상위 부서 시퀀스 */
    private String parentSeq;
    /** 조직 경로명 */
    private String pathName;

    public User() {
    }

    public User(
        String username,
        String firstName,
        String lastName,
        String email,
        List<String> roles
    ) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        // this.roles = roles.stream().map(RoleDTO::getName).collect(Collectors.toList());
        this.roles = roles;
    }
}
