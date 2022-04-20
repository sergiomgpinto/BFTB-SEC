package pt.tecnico.bftb.client;

import pt.tecnico.bftb.grpc.Bftb.AuditResponse;
import pt.tecnico.bftb.grpc.Bftb.CheckAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.OpenAccountResponse;
import pt.tecnico.bftb.grpc.Bftb.ReceiveAmountResponse;
import pt.tecnico.bftb.grpc.Bftb.SearchKeysResponse;
import pt.tecnico.bftb.grpc.Bftb.SendAmountResponse;

public class Responses {
    private OpenAccountResponse open = null;
    private CheckAccountResponse check = null;
    private AuditResponse audit = null;
    private SendAmountResponse send = null;
    private ReceiveAmountResponse receive = null;
    private SearchKeysResponse search = null;
    private String error = null;
    private int ocurrences = 0;

    public Responses(OpenAccountResponse _open, CheckAccountResponse _check, AuditResponse _audit,
            SendAmountResponse _send,
            ReceiveAmountResponse _receive, SearchKeysResponse _search, String _error) {
        open = _open;
        check = _check;
        audit = _audit;
        send = _send;
        receive = _receive;
        search = _search;
        error = _error;
    }

    public Integer getOcurrences() {
        return ocurrences;
    }

    public void addOcurrence() {
        ocurrences++;
    }

    public String getError() {
        return error;
    }

    public void setError(String _error) {
        error = _error;
    }

    public OpenAccountResponse getOpen() {
        return open;
    }

    public CheckAccountResponse getCheck() {
        return check;
    }

    public AuditResponse getAudit() {
        return audit;
    }

    public SendAmountResponse getSend() {
        return send;
    }

    public ReceiveAmountResponse getReceive() {
        return receive;
    }

    public SearchKeysResponse getSearch() {
        return search;
    }

    public void setOpen(OpenAccountResponse _open) {
        open = _open;
    }

    public void setCheck(CheckAccountResponse _check) {
        check = _check;
    }

    public void setAudit(AuditResponse _audit) {
        audit = _audit;
    }

    public void setSend(SendAmountResponse _send) {
        send = _send;
    }

    public void setReceive(ReceiveAmountResponse _receive) {
        receive = _receive;
    }

    public void setSearch(SearchKeysResponse _search) {
        search = _search;
    }
}
