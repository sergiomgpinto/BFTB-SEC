package pt.tecnico.bftb.client;

public class ResponseException extends Exception {

    public ResponseException(String errorMessage) {
        super(errorMessage);
    }
}
