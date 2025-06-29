package iuh.fit.smartexambuilderbe.dtos.responses;

import iuh.fit.smartexambuilderbe.enums.Role;
import iuh.fit.smartexambuilderbe.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class UserResponse extends BaseResponse{
    String userId;
    String email;
    String phone;
    String username;
    String displayName;
    String avatar;
    LocalDate dob;
    UserStatus status;

    Role role;
}