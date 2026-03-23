package com.hcmute.lovestream.dto.admin.user;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateContentManagerRequestTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidRequest() {
        CreateContentManagerRequest request = CreateContentManagerRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .phone("0123456789")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Set<ConstraintViolation<CreateContentManagerRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void testMissingFullName() {
        CreateContentManagerRequest request = CreateContentManagerRequest.builder()
                .email("john@example.com")
                .phone("0123456789")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Set<ConstraintViolation<CreateContentManagerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Họ và tên không được để trống");
    }

    @Test
    void testInvalidEmail() {
        CreateContentManagerRequest request = CreateContentManagerRequest.builder()
                .fullName("John Doe")
                .email("not-an-email")
                .phone("0123456789")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Set<ConstraintViolation<CreateContentManagerRequest>> violations = validator.validateProperty(request, "email");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Email không hợp lệ");
    }

    @Test
    void testPasswordTooShort() {
        CreateContentManagerRequest request = CreateContentManagerRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .phone("0123456789")
                .password("123")
                .confirmPassword("123")
                .build();

        Set<ConstraintViolation<CreateContentManagerRequest>> violations = validator.validateProperty(request, "password");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Mật khẩu phải có ít nhất 6 ký tự");
    }

    @Test
    void testPasswordMatchingFails() {
        CreateContentManagerRequest request = CreateContentManagerRequest.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .phone("0123456789")
                .password("password123")
                .confirmPassword("password456")
                .build();

        Set<ConstraintViolation<CreateContentManagerRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        
        // Custom validator should put the error on 'confirmPassword' property
        ConstraintViolation<CreateContentManagerRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("confirmPassword");
        assertThat(violation.getMessage()).isEqualTo("Mật khẩu xác nhận không khớp");
    }
}
