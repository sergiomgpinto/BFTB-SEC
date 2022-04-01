package pt.tecnico.bftb.client;

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

  String INVALIDCOMMAND = "Invalid command. Please use the command \"help\" for more information.";

  String SERVER_DOWN = "Server's down.";

  String CLIENT_NAME = "Name of the user who owns the account: ";

  String COMMANDPROMPT = "Insert a command: ";

  String INVALID_ARGS_AUDIT = "Invalid number of arguments. Format is:[audit PublicKey].";

  String INVALID_ARGS_SND_AMT = "Invalid number of arguments. Format is:[send_amount receiver_publicKey amount].";

  String INVALID_ARGS_CHECK_ACCOUNT = "Invalid number of arguments. Format is:[check_amount PublicKey].";

  String BALANCE = "Balance: ";

  String TYPE_HELP = "Type \"help\" to see the commands available.";

  String INVALID_AMOUNT_TYPE = "Argument must be a positive number.";

  String INVALID_ARGS_RCV_AMOUNT = "Invalid number of arguments. Format is:[receive_amount senderPublicKey " +
      "transactionId answer]. Answer \"yes\" to accept transaction or \"no\" to reject.";

  String INVALID_ARGS_RCV_AMOUNT_ANSWER = "Argument answer must be either \"yes\" or \"no\".";
}