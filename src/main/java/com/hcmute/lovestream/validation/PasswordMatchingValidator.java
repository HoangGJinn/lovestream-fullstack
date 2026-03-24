//package com.hcmute.lovestream.validation;
//
//import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
//import jakarta.validation.ConstraintValidator;
//import jakarta.validation.ConstraintValidatorContext;
//
///**
// * Logic kiểm tra xem `password` có khớp với `confirmPassword` trong đối tượng yêu cầu hay không.
// */
//public class PasswordMatchingValidator implements ConstraintValidator<PasswordMatching, CreateContentManagerRequest> {
//
//    @Override
//    public boolean isValid(CreateContentManagerRequest request, ConstraintValidatorContext context) {
//        if (request == null) {
//            return true;
//        }
//
//        String password = request.getPassword();
//        String confirmPassword = request.getConfirmPassword();
//
//        boolean isValid = password != null && password.equals(confirmPassword);
//
//        if (!isValid) {
//            // Vô hiệu hoá lỗi mặc định và tuỳ biến field chứa lỗi về `confirmPassword` để UI dễ bind.
//            context.disableDefaultConstraintViolation();
//            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
//                   .addPropertyNode("confirmPassword")
//                   .addConstraintViolation();
//        }
//
//        return isValid;
//    }
//}
