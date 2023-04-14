package Protocol;

import java.io.*;
import java.nio.ByteBuffer;

public class Utilities {
    public static ByteBuffer convertObjectToBytes(Object obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream)) {
            oos.writeObject(obj);
            oos.flush();
            ByteBuffer byteBuffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
            oos.close();
            byteArrayOutputStream.close();
            return byteBuffer;
        }
    }

    public static Object convertBytesToObject(ByteBuffer byteBuffer){
        byte[] bytes = new byte[byteBuffer.getInt()];
        byteBuffer.get(bytes);
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return ois.readObject();
        } catch (IOException e) {
            //System.out.println("IOEXCEPTION");
            byteBuffer.position(0);
            return convertBytesToObject(byteBuffer);
        } catch (ClassNotFoundException e) {
            System.out.println("CLASSNOTFOUNDEXCEPTION");
            throw new RuntimeException(e);
        }
    }
}
