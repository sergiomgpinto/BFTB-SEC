package pt.tecnico.bftb.server.database;

import pt.tecnico.bftb.server.domain.Label;
import pt.tecnico.bftb.server.domain.TransactionStatus;
import pt.tecnico.bftb.server.domain.TransactionType;

import java.security.PublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BFTBMySqlDriver {

    public String dbParser(String function, String[] args, PublicKey publicKey) {

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/bftbServer","root","password");

            // we trade off performance for correctness since we have very few calls to the server
            con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            // it saves all the changes that have been done till this particular point
            con.setAutoCommit(true);
            String sql = null;
            PreparedStatement stmt = null;

            switch (function) {

                case "openAccount":
                    sql = "INSERT INTO Account(Balance, PublicKeyString, PublicKey) VALUES (?,?,?)";
                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, args[0]);
                    stmt.setString(2, args[1]);

                    byte[] bytePublicKey = publicKey.getEncoded();
                    Blob blobData = con.createBlob();
                    blobData.setBytes(1,bytePublicKey);
                    stmt.setBlob(3,blobData);
                    stmt.executeUpdate();
                    blobData.free();
                    return Label.SUCCESS;

                case "sendAmount":

                    //Subtract amount to balance off sender account.
                    sql = "UPDATE Account SET Balance = ?" +
                            "WHERE Balance = ? AND PublicKeyString = ?";
                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, args[0]);
                    stmt.setString(2, args[1]);
                    stmt.setString(3, args[2]);
                    stmt.executeUpdate();

                    //Create pending transaction for sender.
                    String transactionType = TransactionType.WITHDRAWAL.toString();
                    sql = "INSERT INTO Pending(Amount, TransactionStatus, TransactionType, " +
                            "SourceUserKey, DestinationUserKey,TransactionId) VALUES (?,?,?,?,?,?)";

                    stmt = con.prepareStatement(sql);
                    stmt.setString(1, args[3]);
                    stmt.setString(2, args[4]);
                    stmt.setString(3, transactionType);
                    stmt.setString(4, args[5]);
                    stmt.setString(5, args[6]);
                    stmt.setString(6, args[7]);
                    stmt.executeUpdate();

                    return Label.SUCCESS;

                case "receiveAmount":

                    if (args[0].equals("true")) {//Transaction is accepted.

                        //Add amount to balance off receiver account.
                        sql = "UPDATE Account SET Balance = ?" +
                                "WHERE Balance = ? AND PublicKeyString = ?";
                        stmt = con.prepareStatement(sql);
                        stmt.setString(1, args[1]);
                        stmt.setString(2, args[2]);
                        stmt.setString(3, args[3]);
                        stmt.executeUpdate();

                        //Remove pending transaction.

                        sql = "DELETE FROM Pending WHERE SourceUserKey = ? " +
                                "AND TransactionId = ?";
                        stmt = con.prepareStatement(sql);
                        stmt.setString(1, args[5]);
                        stmt.setString(2, args[6]);
                        stmt.executeUpdate();

                        //Add transaction.
                        sql = "INSERT INTO Transaction(Amount, TransactionType, " +
                                "SourceUserKey, DestinationUserKey) VALUES (?,?,?,?)";

                        stmt = con.prepareStatement(sql);
                        stmt.setString(1, args[7]);
                        stmt.setString(2, args[8]);
                        stmt.setString(3, args[5]);
                        stmt.setString(4, args[4]);
                        stmt.executeUpdate();
                    }
                    else { //Transaction is rejected.

                        //Add amount to balance off sender account.
                        sql = "UPDATE Account SET Balance = ?" +
                                "WHERE Balance = ? AND PublicKeyString = ?";

                        stmt = con.prepareStatement(sql);
                        stmt.setString(1, args[1]);
                        stmt.setString(2, args[2]);
                        stmt.setString(3, args[3]);
                        stmt.executeUpdate();

                        //Remove pending transaction.
                        sql = "DELETE FROM Pending WHERE SourceUserKey = ? " +
                                "AND TransactionId = ?";

                        stmt = con.prepareStatement(sql);
                        stmt.setString(1, args[5]);
                        stmt.setString(2, args[6]);
                        stmt.executeUpdate();
                    }
                    return Label.SUCCESS;
            }
            con.close();
            return Label.UNKNOWN_ERROR;
        }
        catch (ClassNotFoundException cnfe) {
            return Label.UNKNOWN_ERROR;
        }
        catch (SQLTimeoutException ste) {
            return Label.ERROR_TRANSACTION_TIMEOUT;
        }
        catch (SQLException se) {
            return Label.ERROR_TRANSACTION;
        }
    }
}
