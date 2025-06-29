package iuh.fit.smartexambuilderbe.dtos.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> implements java.io.Serializable {
    @Builder.Default
    private int statusCode = 200;
    private String message;
    private T data;

}