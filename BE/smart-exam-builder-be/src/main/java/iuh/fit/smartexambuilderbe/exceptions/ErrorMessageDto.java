package iuh.fit.smartexambuilderbe.exceptions;

import lombok.*;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ErrorMessageDto {
    private int statusCode;
    private String message;



}
