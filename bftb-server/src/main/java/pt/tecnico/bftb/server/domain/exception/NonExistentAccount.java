package pt.tecnico.bftb.server.domain.exception;

public class NonExistentAccount extends Exception{

    public NonExistentAccount(String errorMessage) { super(errorMessage); }
}
