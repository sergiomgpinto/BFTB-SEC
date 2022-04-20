DROP DATABASE IF EXISTS bftbServer;

CREATE DATABASE IF NOT EXISTS bftbServer;

USE bftbServer;

CREATE TABLE Account (
	AccountId INTEGER NOT NULL AUTO_INCREMENT,
	Balance INTEGER NOT NULL,
	PublicKeyString VARCHAR(12) NOT NULL,
	Username VARCHAR(7) NOT NULL,
	PublicKey MEDIUMBLOB NOT NULL,
	CONSTRAINT PK_Account PRIMARY KEY (AccountId)
);

CREATE TABLE Pending (
	PendingId INTEGER NOT NULL AUTO_INCREMENT,
	Amount INTEGER NOT NULL,
	TransactionStatus VARCHAR(8) NOT NULL,
	TransactionType VARCHAR(10) NOT NULL,
	SourceUserKey VARCHAR(12) NOT NULL,
	DestinationUserKey VARCHAR(12) NOT NULL,
	TransactionId INTEGER NOT NULL,
    DigitalSignature MEDIUMBLOB NOT NULL,
    /*Digital signature of transaction request sent by the user with the SourceUserKey*/
	CONSTRAINT PK_Pending PRIMARY KEY (PendingId)
);

CREATE TABLE Transaction (
	TransactionId INTEGER NOT NULL AUTO_INCREMENT,
	Amount INTEGER NOT NULL,
	TransactionType VARCHAR(10) NOT NULL,
	SourceUserKey VARCHAR(12) NOT NULL,
	DestinationUserKey VARCHAR(12) NOT NULL,
    DigitalSignature MEDIUMBLOB NOT NULL,
    /*Digital signature of transaction acceptance sent by the user with the DestinationUserKey since
      he is the one receiving the money.
     */
	CONSTRAINT PK_Transaction PRIMARY KEY (TransactionId)
);