package pt.tecnico.bftb.server;

public interface Label {

  /** Menu title. */
  String HELPGUIDE = "+++++++++++++++++++++++++++++++++++++++++++++++++\n" +
      "IMPORTANT:\" \" represents a space, \"[\" " +
      "start of predicate and \"]\" end of predicate.\n" +
      "For all public keys arguments we send a corresponding string to be more practical. Type \"search_keys\" to see all available public key strings.\n"
      +
      "- [open_account]\n" +
      "\tCreates a new account in the system with an auto generated key. Each user can only create one.\n" +
      "- [send_amount dstPublicKey amount]\n" +
      "\tTransfer the amount specified to the dst account.\n" +
      "- [check_account publicKey]\n" +
      "\tObtains the balance of the account and returns the list of pending incoming transfers awaiting approval.\n" +
      "- [receive_amount senderPublicKey transactionId answer]\n" +
      "\tReceives a pending incoming transfer. Answer \"yes\" to accept transaction or \"no\" to reject.\n" +
      "- [audit publicKey]\n" +
      "\tObtains the full transaction history of the account.\n" +
      "- [search_keys]\n" +
      "\tLists all the available public keys for all accounts.\n" +
      "- [exit]\n" +
      "\tCloses client console.\n" +
      "+++++++++++++++++++++++++++++++++++++++++++++++++";

  String SERVERNAME = "Please insert the server name: ";
}