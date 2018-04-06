import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by prog on 12.03.15.
 * Edited By linjingyun12 Lnwza
 */
public class Client {
    private Socket client;
    private ReceiveListener listener;
    private InputStream inputStream;
    private OutputStream outputStream;
    private String id = UUID.randomUUID().toString(); //ID สำหรับ Generate ค่าไม่ให้ซ้ำกันโดยเด็ดขาด
    public volatile Position position;
    public volatile Rotation rotation;

    /**
     * กรณีการสร้างเชื่อมต่อเข้ามาใหม่ก็จะมีต้องรับ Socket ต่่อ กับ Clients ที่เป็น lobby นั่นเอง ซึ่ง listener นั่น
     * ก็คือ Interface ReceiveListener ที่เป็น Implement บน Clients แล้วทำให้เราสามารถรับผ่าน Interface ของพ่อได้นั่นเอง
     *
     * @param client   ตัว Client ที่จะต่อเข้าไปต้องใช้ว่ามาจาก Socket ที่ใด
     * @param listener Interface ReceiveListener ที่Implement บน Clients แล้ว
     * @throws IOException
     */
    public Client(Socket client, ReceiveListener listener) throws IOException {
        this.client = client;
        this.listener = listener;
        //Input สำหรับ Client เราข้อมูลอะไรเข้ามา หรือจะส่งตอบสนองอะไรกลับไป่ผ่าน Output
        inputStream = client.getInputStream();
        outputStream = client.getOutputStream();
        //บริเวณนี้คือ Attriute คุยกันระหว่าง Server กับ Client
        position = new Position();
        rotation = new Rotation();
        //การสร้าง Thread สำหรับเตรียมอ่าน JSON ที่ส่งมาจาก Unity Client
        //ซึ่ง Thread นี้จะไม่ทำงานในครั้งแรกที่พึ่งต่อ Socket เข้ามาเพราะว่าไม่มาคำสั่งใดๆ
        //จะทำงานเป้น json เมื่อมีการขยับ ตัวอย่าง output
        //Thread Read 98 : {"action":"move","position":{"X":"4.409635","Y":"0.4999999","Z":"1.325126"},"rotation":{"X":"4.259384E-08","Y":"-0.1881928","Z":"1.730067E-08","W"
        new ReadThread().start();
        sendStart();
    }

    public String getId() {
        return id;
    }

    /**
     * Method นี้จะ "ทำงานครั้งแรกทันที" ที่มีการเชื่อมต่อเข้ามา เพื่อสร้างโครงให้กับ json
     *  JSON Fomat => {"rotation":{"W":"0","X":"0","Y":"0","Z":"0"},"action":"start","id":"4166cfde-035e-4275-bbe3-f4a084019ff3","position":{"X":"0","Y":"0","Z":"0"}}
     *  โครงสร้างของ JSON
         {
         "action" : "start",
         "id" : ค่าid,
         "X",  ค่า position.x,
         "ัy",  ค่า position.y,
         "z",  ค่า position.z,
         "position", pos;
         }
         ซึ่งถ้าอยากทำ Array ซ้อนข้างในก็ใช้ผ่าน
         JSONArray list = new JSONArray();
         list.add("msg 1");
         list.add("msg 2");
         list.add("msg 3");
         json.put("messages", list);
         ----- output -------
         {
         "age":100,
         "name":"mkyong.com",
         "messages":["msg 1","msg 2","msg 3"]
         }

     * */
    private void sendStart() {
        try {
            JSONObject json = new JSONObject();
            json.put("action", "start");
            json.put("id", id);
            JSONObject pos = new JSONObject();
            pos.put("X", position.x);
            pos.put("Y", position.y);
            pos.put("Z", position.z);
            json.put("position", pos);
            JSONObject rot = new JSONObject();
            rot.put("X", rotation.x);
            rot.put("Y", rotation.y);
            rot.put("Z", rotation.z);
            rot.put("W", rotation.w);
            json.put("rotation", rot);
            //จะมี json object สองตัวคือ position กับ rotation ซ้อนทับอยู่ข้างในเ่วยนั้น่เอง
            //System.out.println("Method Send to client Work!!!! For Start first time");
            System.out.println("sendStart "+json.toString());
            sendToClient(json.toString()); //ได้มาเป้นค่า json พื้นฐานตอนเริ่มต้นคือจุดเกิดของ Wippo เป็น จุด x x x x
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * เป็น Method ที่รับค่า config พื้นฐานจาก json ใน sendStart() ส่งพวกจุดกำดนิด Wippo บลาๆพื้นฐานที่มั้นจะเกิดขึ้นมา
     * ไปที่ฝั่ง Client ใหม่นั่นเอง (ทดลองอย่างการ login เข้ามาแล้วตัวผู้เล่น player เกิดที่จุด .... )
     * ซึ่งเราก็จะเปลี่ยนเป็น generate ตัวละครแทน แล้ว response กลับไป
     *  @param json
     */
    public void sendToClient(String json) {
        System.out.println("sendToClient() Method !");
        try {
            byte[] bytes = json.getBytes();
            byte[] bytesSize = intToByteArray(json.length()); //ขนาด byte [] ตาม ขนาด json
            //outputSTream ตัวนี้จริงๆก็รับมาจาก client.getOutputStream()   ั้นเองซึ่งก็คือ Socket client ตัวนั้นๆที่ส่งเข้ามาตั้งแต่ตอนแรก
            outputStream.write(bytesSize, 0, 4);
            outputStream.write(bytes, 0, bytes.length);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * !!!ยังไม่ทำงานตอนเชื่อมต่อเข้ามา !!!!
     * !!! ต้องมีการขยับ Move ที่ส่งมาจาก Unity Server !!!
     * เป็น Thread ที่เขียนมาเพื่ออ่านค่า JSON ที่ส่งมาจาก ImputStream เข้ามาหา Java Server
     */

    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            byte[] bytes = new byte[1024];
            //ถ้าเกิด Client ยังเชื่อมต่ออยู่ก็ทำงานวนไปเพื่อ่านค่าตลอด ที่ต้องเป็น Thread เพราะว่ามันเป็นการอ่านค่าของใครของมัน
            while (!client.isClosed()) {
                try {
                    int data = inputStream.read(bytes);
                    if (data != -1) {
                        String string = new String(bytes, 0, data);
                        System.out.println("Thread Read 98 : " + string);
                        JSONObject jsonObject = new JSONObject(string);
                        JSONObject pos = jsonObject.optJSONObject("position");
                        position.x = pos.optString("X");
                        position.y = pos.optString("Y");
                        position.z = pos.optString("Z");

                        JSONObject rot = jsonObject.optJSONObject("rotation");
                        rotation.x = rot.optString("X");
                        rotation.y = rot.optString("Y");
                        rotation.z = rot.optString("Z");
                        rotation.w = rot.optString("W");

                        listener.dataReceive(Client.this, string);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Close");
        }
    }

    public static byte[] intToByteArray(int a) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (a & 0xFF);
        ret[1] = (byte) ((a >> 8) & 0xFF);
        ret[2] = (byte) ((a >> 16) & 0xFF);
        ret[3] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }
}
