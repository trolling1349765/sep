package fpt.capstone.dto.response;

public class DecisionDocumentResponseWrapper {
    private DecisionDocumentResponse responseData;
    private byte[] fileContent;
    private String fileName;

    // Constructor, Getter, Setter...
    public DecisionDocumentResponseWrapper(DecisionDocumentResponse responseData, byte[] fileContent, String fileName) {
        this.responseData = responseData;
        this.fileContent = fileContent;
        this.fileName = fileName;
    }
    public byte[] getFileContent() { return fileContent; }
    public String getFileName() { return fileName; }
    public DecisionDocumentResponse getResponseData() { return responseData; }
}