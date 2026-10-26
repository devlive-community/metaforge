package org.devlive.metaforge.service.body;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.devlive.metaforge.service.enums.UploadMode;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UploadBody
{
    private Long id;
    private String code;
    private UploadMode mode;
    private MultipartFile file;
}
