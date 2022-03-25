package pt.tecnico.bftb.server.domain.exception;

public class NonExistentTransaction extends Exception{
    public NonExistentTransaction(String errorMessage) { super(errorMessage); }
}
