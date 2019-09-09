package org.jetbrains.slp.counting.io;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.slp.counting.Counter;

import java.io.*;

public class CounterIO {
    
	@Nullable
    public static Counter readCounter(File file) {
		try {
            FileInputStream fileInputStream = new FileInputStream(file);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Counter counter = (Counter) objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();

            return counter;
        } catch (Exception e) {
		    e.printStackTrace();
		    return null;
        }
	}

	public static void writeCounter(Counter counter, File file) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);

            objectOutputStream.writeObject(counter);

            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
