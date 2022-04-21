package pt.tecnico.bftb.server.domain.exception;

public class InvalidProofOfWorkException extends Exception{
    public InvalidProofOfWorkException(String errorMessage) { super(errorMessage); }
}
