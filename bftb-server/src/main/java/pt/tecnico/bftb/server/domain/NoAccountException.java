package pt.tecnico.bftb.server.domain;

public class NoAccountException extends Exception {
    public NoAccountException(String errorMessage) {
        super(errorMessage);
    }

}
