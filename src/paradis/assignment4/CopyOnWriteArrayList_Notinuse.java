package paradis.assignment4;

import java.util.ArrayList;
import java.util.Arrays;

public class CopyOnWriteArrayList_Notinuse<T> {
    Object lock = new Object();
    volatile private Object[] array;

   boolean add(T t){
       synchronized (this.lock){
           Object[] ts = this.array;
           int len = ts.length;
           ts = Arrays.copyOf(ts, len + 1);
           ts[len] = t;
           this.array = ts;
           return true;
       }
   }




}
