package iuh.fit.smartexambuilderbe.dtos.responses;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseResponse {
    protected boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

}