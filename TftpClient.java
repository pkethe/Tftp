/* 
 * TftpClient.java 
 * 
 * Version: 
 *     $Id$ 1.1
 *     
 * @author	Pranav Sai Kethe
 */
import java.io.*;
import java.net.*;

/**
 * The program TftpClient connects to the respective server,
 * transfers files from the server.
 *
 */

public class TftpClient {
	public static final short RRQ = 01;
	public static final short WRQ = 02;
	public static final short DATA = 03;
	public static final short ACK = 04;
	public static final short ERROR = 05;
	public static final short TFTP_PORT = 69;
	public static final int DEFAULT_BLOCK_SIZE = 512;
	public static final int DEFAULT_RECV_SIZE = 516;
	public static final int DEFAULT_SRC_PORT = 4000;
	public static final String DEFAULT_TRANSFER_MODE = "octet";

	File fileObj;
	boolean endHostProvided = false;
	byte receiveData[];
	DatagramSocket clientSocket;
	FileWriter fw;
	BufferedWriter bw;
	DatagramPacket sendPacket, receivePacket;
	InetAddress IPAddress;

	String usage = 
			"Commands may be abbreviated.  Commands are:"
					+ "\nconnect\t\tconnect to remote tftp"
					+ "\nmode\t\tcurrent file transfer mode"
					+ "\nput\t\tsend file"
					+ "\nget\t\treceive file"
					+ "\nquit\t\texit tftp"
					+ "\nbinary\t\tset mode to octet"
					+ "\nascii\t\tset mode to netascii"
					+ "\n?\t\tprint help information";

	// Constructor
	TftpClient() {
		receiveData = new byte[DEFAULT_RECV_SIZE];
	}

	/**
	 * Run prompt function.
	 *
	 * @param    no parameters
	 */

	void runPrompt() {
		BufferedReader brObject;
		String defaultTransferMode = DEFAULT_TRANSFER_MODE;

		// Loops until quit is encountered
		while (true) {
			String userInput = null;

			// prompt
			System.out.println();
			System.out.print("tftp> ");

			brObject = new BufferedReader(new InputStreamReader(System.in));

			try {
				userInput = brObject.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			String container[] = userInput.split(" ");

			// Connect command
			if (container[0].equalsIgnoreCase("connect")) {
				if (container.length > 1) {
					try {
						if (endHostProvided) {
							clientSocket.close();
						}
						IPAddress = InetAddress.getByName(container[1]);
						clientSocket = new DatagramSocket(DEFAULT_SRC_PORT);
						System.out.println("Connected to " + IPAddress.getHostAddress());
						endHostProvided = true;
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						endHostProvided = false;
						System.out.println("Invalid Hostname provided");
					} catch (SocketException e) {
						// TODO Auto-generated catch block
						endHostProvided = false;
						System.out.println("Socket probably already in use");
					}

				} else {
					System.out.println("Hostname not provided");
				}

				// Get command
			} else if (container[0].equalsIgnoreCase("get")) {
				// Check if file name is provided and has valid socket to server
				if (container.length <= 1 || !endHostProvided) {
					System.out.println("File name missing or not connected to server");
				} else {
					// Place RRQ
					RequestPacket rp = new RequestPacket(RRQ, container[1], defaultTransferMode);
					boolean isData = true;
					fileObj = new File(rp.fileName);
					FileOutputStream fos;

					try {	 
						if (!fileObj.exists()) {
							fileObj.createNewFile();
						} else {
							fileObj.delete();
							fileObj.createNewFile();
						} 

						fos = new FileOutputStream(rp.fileName);
						// Send RRQ
						rp.buildPacket();
						sendPacket(rp.sendData, TFTP_PORT);

						// Receive Response
						receivePacket = new DatagramPacket(receiveData, receiveData.length);
						byte fileData[] = new byte[DEFAULT_BLOCK_SIZE];
						short replyOpcode;
						AckPacket ak;
						// Client Socket timeout 5000ms
						clientSocket.setSoTimeout(5000);

						do  {
							clientSocket.receive(receivePacket);
							replyOpcode = (short) receiveData[1]; 
							switch (replyOpcode) {
							case DATA:
								System.arraycopy(receiveData, 4, fileData, 0, DEFAULT_BLOCK_SIZE);
								fos.write(fileData, 0, receivePacket.getLength() - 4);			
								ak = new AckPacket(receiveData[2], receiveData[3]);
								for(int i = 0; i < DEFAULT_BLOCK_SIZE; i++) {
									ak.sendData[i] = 0;
								}	
								ak.buildPacket();
								sendPacket(ak.sendData, receivePacket.getPort());
								break;
							case ERROR:
								isData = false;
								System.arraycopy(receiveData, 4, fileData, 0, receivePacket.getLength()-4);
								System.out.println(new String(fileData));
								break;
							case RRQ:
							case WRQ:
							case ACK:
								isData = false;
							default:
								System.err.println("Unexpected packet");
								break;
							}
						} while (!(receivePacket.getLength() < DEFAULT_BLOCK_SIZE) && (replyOpcode != ERROR) &&  (replyOpcode != RRQ) && (replyOpcode != WRQ) && (replyOpcode != ACK) );
						fos.close();
					} catch (SocketTimeoutException e) {
						// timeout exception.
						System.out.println("Timeout reached, Server not responding");
						isData = false; 
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (!isData) {
						fileObj.delete();
					}
				}
				// Place a WRQ
			} else if (container[0].equalsIgnoreCase("put")) {
				System.out.println("Put not supported yet");
			} else if (container[0].equalsIgnoreCase("binary")) {
				defaultTransferMode = "octet";
			} else if (container[0].equalsIgnoreCase("ascii")) {
				defaultTransferMode = "netascii";
			} else if (container[0].equalsIgnoreCase("mode")) { 
				System.out.println("Using " + defaultTransferMode + " mode to transfer files.");
			} else if (container[0].equalsIgnoreCase("quit")) {
				System.exit(0);
			} else if (container[0].equalsIgnoreCase("?")) {
				System.out.println(usage);
			} else {
				System.out.println("Invalid Command");
			}
		}
	}

	/**
	 * Send packet function
	 *
	 * @param    byte	data[]
	 * @param	 int 	port
	 */

	void sendPacket(byte data[], int port) {
		sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
		try {
			clientSocket.send(sendPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	/**
	 * Main function program
	 *
	 * @param    String	args
	 *
	 */

	public static void main(String args[]) throws Exception {
		TftpClient tc = new TftpClient();

		if (args.length > 0) {
			try {
				tc.IPAddress = InetAddress.getByName(args[0]);
				tc.clientSocket = new DatagramSocket(DEFAULT_SRC_PORT);
				System.out.println("Connected to " + tc.IPAddress.getHostAddress());
				tc.endHostProvided = true;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		tc.runPrompt();
	}


	/**
	 * Request Packet 
	 */

	// For RRQ/WRQ
	class RequestPacket {
		short opcode;
		byte sendData[];	
		String fileName, mode;

		RequestPacket(short opcode, String fileName, String mode) {
			this.opcode = opcode;
			this.fileName = fileName;
			this.mode = mode;
			sendData = new byte[DEFAULT_BLOCK_SIZE];
		}

		void buildPacket() {
			int i, j;

			sendData[0] = 0;  					// 	0 byte
			sendData[1] = (byte) (opcode);		//	place opcode RRQ/WRQ

			byte []a = fileName.getBytes();

			for (i = 0; (i+2 < DEFAULT_BLOCK_SIZE && i < a.length); i++ ) {
				sendData[i+2] = a[i];
			}
			sendData[i+2] = 0;

			a = mode.getBytes();

			for (j = 0, i++; (i < DEFAULT_BLOCK_SIZE && j < a.length); i++, j++) {
				sendData[i+2] = a[j];
			}
			sendData[i+2] = 0;
		}

	}

	/**
	 * Ack Packet 
	 */

	class AckPacket {
		byte opcode;
		short byte1, byte2;
		byte sendData[];

		AckPacket(byte byte1, byte byte2) {
			this.opcode = ACK;
			this.byte1 = byte1;
			this.byte2 = byte2;
			sendData = new byte[DEFAULT_BLOCK_SIZE];
		}	

		void buildPacket() {
			sendData[1] = opcode;
			sendData[2] = (byte) byte1;
			sendData[3] = (byte) byte2;
		}
	}

}
