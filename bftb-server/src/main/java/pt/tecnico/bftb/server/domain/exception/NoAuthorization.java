package pt.tecnico.bftb.server.domain.exception;

public class NoAuthorization extends Exception{

    public NoAuthorization (String errorMessage) { super(errorMessage); }
}
