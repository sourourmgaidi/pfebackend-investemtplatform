package tn.iset.investplatformpfe.Dto;
import lombok.Data;

@Data
public class MultiRoleAttachmentDTO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl;
}
