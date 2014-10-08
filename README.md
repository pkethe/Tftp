Tftp
====

Trivial file transfer protocol(GET Implementation according to RFC1350)

Programing Language used JAVA

INSTRUCTIONS

1) Compile source java files
prompt> javac TftpClient.java

2) Running java program 
prompt> sudo java TftpClient

3) On tftp prompt, 
** TO GET A TEXT FILE **
tftp> connect glados.cs.rit.edu
Connected to 129.21.30.38
tftp> get foo.txt
tftp> quit

prompt> cat foo.txt
Hi There

** GETTING A NON ASCII FILE**
tftp> get EdwardMaya.mp3

** IF SERVER doesn't respond, default timeout happens on the socket which is 5 secs **
