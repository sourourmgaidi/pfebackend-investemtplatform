package tn.iset.investplatformpfe.Dto;


// src/main/java/tn/iset/investplatformpfe/DTO/CustomMessageRequest.java
public class CustomMessageRequest {
    private String subject;
    private String rawMessage;
    private String imageBase64;   // image encodée en base64
    private String imageName;     // nom du fichier image

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getRawMessage() { return rawMessage; }
    public void setRawMessage(String rawMessage) { this.rawMessage = rawMessage; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }
}

