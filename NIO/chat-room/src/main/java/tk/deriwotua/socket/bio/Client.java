package tk.deriwotua.socket.bio;

import java.io.*;
import java.net.Socket;

/**
 * BIO socket编程 客户端
 */
public class Client {

    public static void main(String[] args) {
        final String DEFAULT_SERVER_HOST = "127.0.0.1";
        final int DEFAULT_PORT = 8888;

        /**
         * 创建客户端socket 创建IO流
         */
        try (Socket socket = new Socket(DEFAULT_SERVER_HOST, DEFAULT_PORT);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));) {

            // 等待用户输入信息
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            String input = consoleReader.readLine();

            // 发送消息给服务器
            writer.write(input + "\n");
            writer.flush();

            // 读取服务器返回的消息
            String msg = reader.readLine();
            System.out.println("" + msg);
        } catch (Exception e) {

        }
    }
}