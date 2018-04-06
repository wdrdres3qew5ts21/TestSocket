import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by prog on 12.03.15.
 * Edited By linjingyun12 Lnwza
 */
public class ServerMain {
    public static void main(String[] args){
        try {
            Clients clients = new Clients();

            //Class ServerSocket คือหัวห้องที่จะสร้าง ทิ้งไว้ที่ Port 16000 นั่นเอง
            ServerSocket serverSocket = new ServerSocket(16000);
            //หลงจากนี้โค้ดจะค้างวนแค่อยู่ใน loop true เท่านั้นทำให้เราจะไม่สร้าง ServerSocket ใหม่ขึ้นมา
            //ไม่อย่างนั้นก็จะหลุดกันไปคนล่ะเซิฟหากันไม่เจอสักที
            //Clients ที่เป้นตัวใช้ในการเก็บ ก็เช่นกันสร้างแค่ครั้งเดียว
            while (true) {
                System.out.println("Wait client");
                //.accept() คือการยอมสร้างตัว Socket ใหม่ขึ้นมาบน NW
                Socket socket = serverSocket.accept();
                //เช็คดูว่าใครมันต่อเข้ามาในระบบ
                System.out.println("Client connected : "+socket.getRemoteSocketAddress().toString());
                Client client = new Client(socket, clients); //แค่ endpoint ปลายทางตัวหนึ่ง
                clients.addClient(client);//ไปเก็บในคลัง Lobby คนเล่น
            }
        } catch (IOException e) {
            //กรณีมีคนหลุดการเชื่อมต่อมันก็จะบึ้นตัวแดงมาไม่ต้องตกใจไป
            e.printStackTrace();
        }
    }
}
