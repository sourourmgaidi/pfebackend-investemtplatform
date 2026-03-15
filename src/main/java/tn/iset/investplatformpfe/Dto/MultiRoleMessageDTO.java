package tn.iset.investplatformpfe.Dto;

import lombok.Data;
import tn.iset.investplatformpfe.Entity.Role;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MultiRoleMessageDTO {
    private Long id;
    private String content;
    private boolean isRead;
    private LocalDateTime sentAt;
    private Role senderType;
    private Long senderId;
    private String senderName;
    private Role receiverType;
    private Long receiverId;
    private String receiverName;
    private List<MultiRoleAttachmentDTO> attachments;
}
